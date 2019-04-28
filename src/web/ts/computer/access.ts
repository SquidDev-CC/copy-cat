import { IComputerAccess, IFileSystemEntry, QueueEventHandler, Result } from "../java";
import { TerminalData } from "../terminal/data";
import { IComputerActionable, LuaValue, Semaphore } from "./actions";

import "setimmediate";

const colours = "0123456789abcdef";

export const splitName = (file: string) => {
  const lastIndex = file.lastIndexOf("/");
  if (lastIndex < 0) return ["", file];
  return [file.substring(0, lastIndex), file.substring(lastIndex + 1)];
};

export const joinName = (parent: string, child: string) => parent === "" ? child : `${parent}/${child}`;

export class FileSystemEntry implements IFileSystemEntry {
  private children: string[] | null;
  private contents: string | null;
  private exists: boolean = true;
  private semaphore?: Semaphore;

  constructor(children: string[] | null, contents: string | null) {
    this.children = children;
    this.contents = contents;
  }

  public isDirectory(): boolean {
    return this.children != null;
  }

  public getChildren(): string[] {
    if (this.children == null) throw Error("Not a directory");
    return this.children;
  }

  public setChildren(children: string[]): void {
    if (this.children == null) throw Error("Not a directory");
    this.children = children;
    if (this.semaphore) this.semaphore.signal();
  }

  public getContents(): string {
    if (this.contents == null) throw Error("Not a file");
    return this.contents;
  }

  public setContents(contents: string): Result<true> {
    if (this.contents == null) throw Error("Not a file");
    if (!this.exists) return { error: "File has been deleted", value: null };
    this.contents = contents;
    return { value: true };
  }

  public delete(): void {
    this.exists = false;
  }

  public getSemaphore(): Semaphore {
    return this.semaphore || (this.semaphore = new Semaphore());
  }
}

export class ComputerAccess implements IComputerAccess, IComputerActionable {
  private queueEventHandler?: QueueEventHandler;
  private turnOnHandler?: () => void;
  private shutdownHandler?: () => void;
  private rebootHander?: () => void;

  private terminal: TerminalData;
  private semaphore: Semaphore;
  private stateChanged: (label: string | null, on: boolean) => void;

  private filesystem: Map<string, FileSystemEntry> = new Map<string, FileSystemEntry>();

  constructor(
    terminal: TerminalData, semaphore: Semaphore,
    stateChange: (label: string | null, on: boolean) => void,
  ) {
    this.terminal = terminal;
    this.semaphore = semaphore;
    this.stateChanged = stateChange;

    this.filesystem.set("", new FileSystemEntry([], null));
  }

  public setState(label: string | null, on: boolean): void {
    this.stateChanged(label , on);
  }

  public updateTerminal(
    width: number, height: number,
    x: number, y: number, blink: boolean, cursorColour: number,
  ): void {
    this.terminal.resize(width, height);
    this.terminal.cursorX = x;
    this.terminal.cursorY = y;
    this.terminal.cursorBlink = blink;
    this.terminal.currentFore = colours.charAt(cursorColour);
  }

  public setTerminalLine(line: number, text: string, fore: string, back: string): void {
    this.terminal.text[line] = text;
    this.terminal.fore[line] = fore;
    this.terminal.back[line] = back;
  }

  public setPaletteColour(colour: number, r: number, g: number, b: number): void {
    this.terminal.palette[colours.charAt(colour)] =
      `rgb(${(r * 0xFF) & 0xFF},${(g * 0xFF) & 0xFF},${(b * 0xFF) & 0xFF}`;
  }

  public flushTerminal() {
    this.semaphore.signal();
  }

  public getEntry(path: string): FileSystemEntry | null {
    return this.filesystem.get(path) || null;
  }

  public createDirectory(path: string): Result<FileSystemEntry> {
    const entry = this.filesystem.get(path);
    if (!entry) {
      const [parentName, fileName] = splitName(path);
      const parent = this.createDirectory(parentName);
      if (parent.value === null) return parent;

      const file = new FileSystemEntry([], null);
      parent.value.setChildren([...parent.value.getChildren(), fileName]);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { value: entry };
    } else {
      return { error: `/$path: File exists`, value: null };
    }
  }

  public createFile(path: string): Result<FileSystemEntry> {
    const entry = this.filesystem.get(path);
    if (!entry) {
      const [parentName, fileName] = splitName(path);
      const parent = this.filesystem.get(parentName);
      if (parent == null || !parent.isDirectory()) return { error: `/${path}: Access denied`, value: null };

      const file = new FileSystemEntry(null, "");
      parent.setChildren([...parent.getChildren(), fileName]);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { error: `/$path: Cannot write to directory`, value: null };
    } else {
      return { value: entry };
    }
  }

  public deleteEntry(path: string): void {
    const pathEntry = this.filesystem.get(path);
    if (!pathEntry) return pathEntry;

    // Remove from the parent
    const [parentName, fileName] = splitName(path);
    const parent = this.filesystem.get(parentName)!;
    parent.setChildren(parent.getChildren().filter(x => x !== fileName));

    // And delete any children
    const queue = [path];
    while (true) {
      const file = queue.pop();
      if (file === undefined) break;

      const entry = this.filesystem.get(file);
      if (!entry) continue;

      entry.delete();
      this.filesystem.delete(path);

      if (entry.isDirectory()) continue;
      for (const child of entry.getChildren()) queue.push(joinName(file, child));
    }
  }

  public onEvent(listener: QueueEventHandler): void {
    this.queueEventHandler = listener;
  }

  public onShutdown(handler: () => void): void {
    this.shutdownHandler = handler;
  }

  public onTurnOn(handler: () => void): void {
    this.turnOnHandler = handler;
  }

  public onReboot(handler: () => void): void {
    this.rebootHander = handler;
  }

  public queueEvent(event: string, args: LuaValue[]): void {
    if (this.queueEventHandler !== undefined) this.queueEventHandler(event, args.map(x => JSON.stringify(x)));
  }

  public turnOn(): void {
    if (this.turnOnHandler !== undefined) this.turnOnHandler();
  }

  public shutdown(): void {
    if (this.shutdownHandler !== undefined) this.shutdownHandler();
  }

  public reboot(): void {
    if (this.rebootHander !== undefined) this.rebootHander();
  }
}
