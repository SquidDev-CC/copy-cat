import { decode, encode } from "../files/encode";
import { IComputerAccess, IFileSystemEntry, QueueEventHandler, Result } from "../java";
import * as storage from "../storage";
import { TerminalData } from "../terminal/data";
import { IComputerActionable, LuaValue, Semaphore } from "./actions";

const colours = "0123456789abcdef";

export const splitName = (file: string) => {
  const lastIndex = file.lastIndexOf("/");
  if (lastIndex < 0) return ["", file];
  return [file.substring(0, lastIndex), file.substring(lastIndex + 1)];
};

export const joinName = (parent: string, child: string) => parent === "" ? child : `${parent}/${child}`;

const empty = new Int8Array(0);
const decoder = new TextDecoder("UTF-8", { fatal: false });
const encoder = new TextEncoder();

export class FileSystemEntry implements IFileSystemEntry {
  private readonly path: string;
  private children: string[] | null;
  private contents: Int8Array | null;
  private exists: boolean = true;
  private semaphore?: Semaphore;

  constructor(path: string, children: string[] | null, contents: Int8Array | null) {
    this.path = path;
    this.children = children;
    this.contents = contents;
  }

  public static create(path: string, directory: boolean) {
    const instance = new FileSystemEntry(path, directory ? [] : null, directory ? null : empty);
    instance.save();
    return instance;
  }

  public isDirectory(): boolean {
    return this.children != null;
  }

  public getChildren(): string[] {
    if (this.children === null) throw Error("Not a directory");
    return this.children;
  }

  public setChildren(children: string[]): void {
    if (this.children === null) throw Error("Not a directory");
    this.children = children;
    if (this.semaphore) this.semaphore.signal();
    this.save();
  }

  public getContents(): Int8Array {
    if (this.contents !== null) return this.contents;
    if (this.children !== null) throw Error("Not a file");

    const contents = storage.get(`computer[0].files[${this.path}].b64`);
    return this.contents = contents ? new Int8Array(decode(contents)) : empty;
  }

  public getStringContents(): string {
    return decoder.decode(this.getContents());
  }

  public setContents(contents: ArrayBuffer | string): Result<true> {
    if (this.children !== null) throw Error("Not a file");
    if (!this.exists) return { error: "File has been deleted", value: null };

    if (typeof contents === "string") {
      const encoded = encoder.encode(contents);
      this.contents = new Int8Array(encoded);
    } else {
      this.contents = contents instanceof Int8Array ? contents : new Int8Array(contents);
    }
    this.save();
    return { value: true };
  }

  public delete(): void {
    this.exists = false;
    storage.remove(this.children === null
      ? `computer[0].files[${this.path}].b64`
      : `computer[0].files[${this.path}].children`);
  }

  private save(): void {
    if (this.children !== null) {
      storage.set(`computer[0].files[${this.path}].children`, JSON.stringify(this.children));
    }

    if (this.contents !== null) {
      storage.set(`computer[0].files[${this.path}].b64`, encode(this.contents));
    }
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
  private label: string | null;

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

    this.label = storage.get(`computer[0].label`);

    const queue = [""];
    while (true) {
      const path = queue.pop();
      if (path === undefined) break;

      const children = storage.get(`computer[0].files[${path}].children`);
      if (children !== null) {
        let childList: string[];
        try {
          childList = JSON.parse(children);
        } catch (e) {
          console.error(`Error loading file "${path}"`);
          continue;
        }

        this.filesystem.set(path, new FileSystemEntry(path, childList, null));
        for (const child of childList) queue.push(joinName(path, child));
      } else if (path === "") {
        // Create a new entry
        this.filesystem.set("", new FileSystemEntry("", [], null));
      } else {
        // Assume it's a file
        this.filesystem.set(path, new FileSystemEntry(path, null, null));
      }
    }
  }

  public getLabel(): string | null {
    return this.label;
  }

  public setState(label: string | null, on: boolean): void {
    if (this.label !== label) {
      this.label = label;
      if (label) {
        storage.set(`computer[0].label`, label);
      } else {
        storage.remove(`computer[0].label`);
      }
    }

    this.stateChanged(label, on);
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

      const file = FileSystemEntry.create(path, true);
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

      const file = FileSystemEntry.create(path, false);
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

      if (!entry.isDirectory()) continue;
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
