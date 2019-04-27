import { IComputerAccess, IFileSystemEntry, QueueEventHandler, Result } from "../java";
import { TerminalData } from "../terminal/data";
import { IComputerActionable, LuaValue, Semaphore } from "./actions";

import "setimmediate";

const colours = "0123456789abcdef";

const splitName = (file: string) => {
  const lastIndex = file.lastIndexOf("/");
  if (lastIndex < 0) return ["", file];
  return [file.substring(0, lastIndex), file.substring(lastIndex + 1)];
};

class FileSystemEntry implements IFileSystemEntry {
  private readonly children: string[] | null;
  private contents: string | null;
  public exists: boolean = true;

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

  public getEntry(path: string): IFileSystemEntry | null {
    return this.filesystem.get(path) || null;
  }

  public createDirectory(path: string): Result<IFileSystemEntry> {
    const entry = this.filesystem.get(path);
    if (!entry) {
      const [parentName, fileName] = splitName(path);
      const parent = this.createDirectory(parentName);
      if (parent.value === null) return parent;

      const file = new FileSystemEntry([], null);
      parent.value.getChildren().push(fileName);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { value: entry };
    } else {
      return { error: `/$path: File exists`, value: null };
    }
  }

  public createFile(path: string): Result<IFileSystemEntry> {
    const entry = this.filesystem.get(path);
    if (!entry) {
      const [parentName, fileName] = splitName(path);
      const parent = this.filesystem.get(parentName);
      if (parent == null || !parent.isDirectory()) return { error: `/${path}: Access denied`, value: null };

      const file = new FileSystemEntry(null, "");
      parent.getChildren().push(fileName);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { error: `/$path: Cannot write to directory`, value: null };
    } else {
      return { value: entry };
    }
  }

  public deleteEntry(path: string): void {
    const queue = [path];

    while (true) {
      const file = queue.pop();
      if (file === undefined) break;

      const entry = this.filesystem.get(file);
      if (!entry) continue;

      entry.exists = false;
      this.filesystem.delete(path);

      if (entry.isDirectory()) continue;
      for (const child of entry.getChildren()) queue.push(`${file}/${child}`);
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
