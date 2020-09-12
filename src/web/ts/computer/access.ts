import { ComputerActionable, KeyCode, LuaValue, Semaphore, TerminalData, lwjgl3Code } from "@squid-dev/cc-web-term";
import { ConfigFactory, ComputerAccess as IComputerAccess, FileSystemEntry as IFileSystemEntry, Result, start } from "../java";
import type { BasicAttributes, ComputerPersistance } from "./persist";
import type { ComputerCallbacks, FileAttributes } from "../classes";

const colours = "0123456789abcdef";

export const splitName = (file: string): [string, string] => {
  const lastIndex = file.lastIndexOf("/");
  if (lastIndex < 0) return ["", file];
  return [file.substring(0, lastIndex), file.substring(lastIndex + 1)];
};

export const joinName = (parent: string, child: string): string => parent === "" ? child : `${parent}/${child}`;

const empty = new Int8Array(0);
const decoder = new TextDecoder("UTF-8", { fatal: false });
const encoder = new TextEncoder();

export class FileSystemEntry implements IFileSystemEntry {
  private readonly persistance: ComputerPersistance;
  private readonly path: string;
  private children: string[] | null;
  private contents: Int8Array | null;
  private exists: boolean = true;
  private semaphore?: Semaphore;
  private attributes: BasicAttributes;

  public constructor(persistance: ComputerPersistance, path: string, children: string[] | null, contents: Int8Array | null, attributes: BasicAttributes | null) {
    this.persistance = persistance;
    this.path = path;
    this.children = children;
    this.contents = contents;
    this.attributes = attributes === null ? { modification: 0, creation: 0 } : attributes;
  }

  public static create(persistance: ComputerPersistance, path: string, directory: boolean): FileSystemEntry {
    const now = Date.now();
    const instance = new FileSystemEntry(persistance, path, directory ? [] : null, directory ? null : empty, { creation: now, modification: now });
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
    return this.contents = this.persistance.getContents(this.path);
  }

  public getStringContents(): string {
    return decoder.decode(this.getContents());
  }

  public setContents(contents: ArrayBuffer | string): Result<true> {
    if (this.children !== null) throw Error("Not a file");
    if (!this.exists) return { error: "File has been deleted", value: null };
    this.attributes.modification = Date.now();

    if (typeof contents === "string") {
      const encoded = encoder.encode(contents);
      this.contents = new Int8Array(encoded);
    } else {
      this.contents = contents instanceof Int8Array ? contents : new Int8Array(contents);
    }
    this.save();
    if (this.semaphore) this.semaphore.signal();
    return { value: true };
  }

  public delete(): void {
    this.exists = false;
    if (this.children === null) {
      this.persistance.removeContents(this.path);
    } else {
      this.persistance.removeChildren(this.path);
    }
    if (this.semaphore) this.semaphore.signal();
  }

  private save(): void {
    if (this.children !== null) this.persistance.setChildren(this.path, this.children);
    if (this.contents !== null) this.persistance.setContents(this.path, this.contents);
    this.persistance.setAttributes(this.path, this.attributes);
  }

  public getSemaphore(): Semaphore {
    return this.semaphore || (this.semaphore = new Semaphore());
  }

  public doesExist(): boolean {
    return this.exists;
  }

  public getAttributes(): FileAttributes {
    const directory = this.isDirectory();
    return { directory, size: directory ? 0 : this.getContents().length, ...this.attributes };
  }
}

export class ComputerAccess implements IComputerAccess, ComputerActionable {
  private readonly persistance: ComputerPersistance;

  private readonly terminal: TerminalData;
  private readonly semaphore: Semaphore;
  private readonly stateChanged: (label: string | null, on: boolean) => void;

  private label: string | null;
  private readonly filesystem: Map<string, FileSystemEntry> = new Map<string, FileSystemEntry>();

  private handlers?: ComputerCallbacks;
  private removed: boolean = false;

  public constructor(
    persistance: ComputerPersistance, terminal: TerminalData, semaphore: Semaphore,
    stateChange: (label: string | null, on: boolean) => void,
  ) {
    this.persistance = persistance;

    this.terminal = terminal;
    this.semaphore = semaphore;
    this.stateChanged = stateChange;

    this.label = persistance.getLabel();

    const queue = [""];
    while (true) {
      const path = queue.pop();
      if (path === undefined) break;

      const children = persistance.getChildren(path);
      const attributes = persistance.getAttributes(path);
      if (children !== null) {
        this.filesystem.set(path, new FileSystemEntry(persistance, path, children, null, attributes));
        for (const child of children) queue.push(joinName(path, child));
      } else if (path === "") {
        // Create a new entry
        this.filesystem.set("", new FileSystemEntry(persistance, "", [], null, attributes));
      } else {
        // Assume it's a file
        this.filesystem.set(path, new FileSystemEntry(persistance, path, null, null, attributes));
      }
    }
  }

  public getLabel(): string | null {
    return this.label;
  }

  public setState(label: string | null, on: boolean): void {
    if (this.label !== label) {
      this.label = label;
      this.persistance.setLabel(label);
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
      `rgb(${(r * 0xFF) & 0xFF},${(g * 0xFF) & 0xFF},${(b * 0xFF) & 0xFF})`;
  }

  public flushTerminal(): void {
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

      const file = FileSystemEntry.create(this.persistance, path, true);
      parent.value.setChildren([...parent.value.getChildren(), fileName]);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { value: entry };
    } else {
      return { error: `/${path}: File exists`, value: null };
    }
  }

  public createFile(path: string): Result<FileSystemEntry> {
    const entry = this.filesystem.get(path);
    if (!entry) {
      const [parentName, fileName] = splitName(path);
      const parent = this.filesystem.get(parentName);
      if (parent == null || !parent.isDirectory()) return { error: `/${path}: Access denied`, value: null };

      const file = FileSystemEntry.create(this.persistance, path, false);
      parent.setChildren([...parent.getChildren(), fileName]);
      this.filesystem.set(path, file);
      return { value: file };
    } else if (entry.isDirectory()) {
      return { error: `/${path}: Cannot write to directory`, value: null };
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

      this.filesystem.delete(file);
      entry.delete();

      if (!entry.isDirectory()) continue;
      for (const child of entry.getChildren()) queue.push(joinName(file, child));
    }
  }

  public start(config: ConfigFactory, options?: { width?: number, height?: number, label?: string }): void {
    start(this, config)
      .then(computer => {
        this.handlers = computer;
        if (this.removed) computer.dispose();

        const { width, height, label } = options || {};
        if (typeof width === "number" && typeof height === "number") computer.resize(width, height);

        if (typeof this.label === "string") computer.setLabel(this.label);
        else if (typeof label === "string") computer.setLabel(label);
      })
      .catch(e => console.error("Cannot start computer", e));
  }

  public queueEvent(event: string, args: LuaValue[]): void {
    if (this.handlers !== undefined) this.handlers.event(event, args.map(x => JSON.stringify(x)));
  }

  public keyDown(key: KeyCode, repeat: boolean): void {
    const code = lwjgl3Code(key);
    if (code !== undefined) this.queueEvent("key", [code, repeat]);
  }

  public keyUp(key: KeyCode): void {
    const code = lwjgl3Code(key);
    if (code !== undefined) this.queueEvent("key_up", [code]);
  }

  public turnOn(): void {
    if (this.handlers) this.handlers?.turnOn();
  }

  public shutdown(): void {
    if (this.handlers) this.handlers?.shutdown();
  }

  public reboot(): void {
    if (this.handlers) this.handlers?.reboot();
  }

  public dispose(): void {
    this.removed = true;
    if (this.handlers) this.handlers?.dispose();
  }
}
