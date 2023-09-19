import { decode, encode } from "../files/encode";
import * as storage from "../storage";

const empty = new Int8Array(0);

export type BasicAttributes = {
  creation: number,
  modification: number,
}

/**
 * A generic way of storing and loading computer information.
 *
 * This should not be treated as the cannonical source of file information (see {@link ComputerAccess} for that), but
 * rather as a backend for the computer to finally save to.
 */
export interface ComputerPersistance {
  getLabel(): string | null;

  setLabel(label: string | null): void;

  getContents(path: string): Int8Array;

  setContents(path: string, contents: Int8Array): void;

  removeContents(path: string): void;

  getChildren(path: string): Array<string> | null;

  setChildren(path: string, children: Array<string>): void;

  removeChildren(path: string): void;

  getAttributes(path: string): BasicAttributes | null;

  setAttributes(path: string, attributes: BasicAttributes): void;

  removeAttributes(path: string): void;
}

/**
 * A persistance instance which saves nothing, useful for temporary file systems.
 */
export class VoidPersistence implements ComputerPersistance {
  public getLabel(): null { return null; }
  public setLabel(): void { }
  public getContents(): Int8Array { return empty; }
  public setContents(): void { }
  public removeContents(): void { }
  public getChildren(): null { return null; }
  public setChildren(): void { }
  public removeChildren(): void { }
  public getAttributes(): null { return null; }
  public setAttributes(): void { }
  public removeAttributes(): void { }
}

/**
 * Persistance instance which saves to storage.
 */
export class StoragePersistence implements ComputerPersistance {
  private readonly prefix: string;

  public constructor(id: number) {
    this.prefix = `computer[${id}]`;
  }

  public getLabel(): string | null {
    return storage.get(`${this.prefix}.label`);
  }

  public setLabel(label: string | null): void {
    if (label === null) {
      storage.remove(`${this.prefix}.label`);
    } else {
      storage.set(`${this.prefix}.label`, label);
    }
  }

  public getContents(path: string): Int8Array {
    const contents = storage.get(`${this.prefix}.files[${path}].b64`);
    return contents ? new Int8Array(decode(contents)) : empty;
  }

  public setContents(path: string, contents: Int8Array): void {
    storage.set(`${this.prefix}.files[${path}].b64`, encode(contents));
  }

  public removeContents(path: string): void {
    storage.remove(`${this.prefix}.files[${path}].b64`);
  }

  public getChildren(path: string): Array<string> | null {
    const children = storage.get(`${this.prefix}.files[${path}].children`);
    if (children === null) return null;

    try {
      return JSON.parse(children) as Array<string>;
    } catch (e) {
      console.error(`Error loading path "${path}"`);
      return null;
    }
  }

  public setChildren(path: string, children: Array<string>): void {
    storage.set(`${this.prefix}.files[${path}].children`, JSON.stringify(children));
  }

  public removeChildren(path: string): void {
    storage.remove(`${this.prefix}.files[${path}].children`);
  }

  public getAttributes(path: string): BasicAttributes | null {
    const attributes = storage.get(`${this.prefix}.files[${path}].attributes`);
    if (attributes === null) return null;

    try {
      return JSON.parse(attributes) as BasicAttributes;
    } catch (e) {
      console.error(`Error loading attributes for "${path}"`);
      return null;
    }
  }

  public setAttributes(path: string, attr: BasicAttributes): void {
    storage.set(`${this.prefix}.files[${path}].attributes`, JSON.stringify(attr));
  }

  public removeAttributes(path: string): void {
    storage.remove(`${this.prefix}.files[${path}].attributes`);
  }
}
