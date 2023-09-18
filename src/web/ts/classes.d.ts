export const main: () => void;

export type Side = "up" | "down" | "left" | "right" | "front" | "back";
export type PeripheralKind = "speaker";

/**
 * A handler for {@link ICallbacks.onEvent}
 */
export type QueueEventHandler = (event: string, args: string[]) => void;

/**
 * A naive Either type, instead of wrangling JS/Java exceptions.
 */
export type Result<T> = { value: T } | { error: string, value: null };

/**
 * Attributes about a file.
 */
export type FileAttributes = {
  creation: number, /** When this file was created. */
  modification: number, /** When this file was last modified. */
  directory: boolean, /** Whether this file is a directory. */
  size: number, /** The size of this file. */
};

export interface FileSystemEntry {
  /**
   * If this entry is a directory.
   */
  isDirectory(): boolean;

  /**
   * Get the filenames of all child entries
   *
   * @return The child entries. Note, this is the relative path, rather than the absolute one.
   * @throws If this is not a directory
   */
  getChildren(): string[];

  /**
   * Get the contents of this filesystem entry
   *
   * @return This file's contents
   * @throws If this is not a file
   */
  getContents(): Int8Array;

  /**
   * Set the contents of this filesystem entry
   *
   * @param contents This entry's contents
   * @return Whether this file's contents was set or not
   * @throws If this is not a file
   */
  setContents(contents: Int8Array): Result<true>;

  /**
   * Get the attributes for a file.
   */
  getAttributes(): FileAttributes;
}

/**
 * Controls a specific computer on the Javascript side. See {@code js/ComputerAccess.java}.
 */
export interface ComputerAccess {
  /**
   * Set this computer's current state
   *
   * @param label This computer's label
   * @param on    If this computer is on right now
   */
  setState(label: string | null, on: boolean): void;

  /**
   * Update the terminal's properties
   *
   * @param width        The terminal width
   * @param height       The terminal height
   * @param x            The X cursor
   * @param y            The Y cursor
   * @param blink        Whether the cursor is blinking
   * @param cursorColour The cursor's colour
   */
  updateTerminal(width: number, height: number, x: number, y: number, blink: boolean, cursorColour: number): void;

  /**
   * Set a line on the terminal
   *
   * @param line The line index to set
   * @param text The line's text
   * @param fore The line's foreground
   * @param back The line's background
   */
  setTerminalLine(line: number, text: string, fore: string, back: string): void;

  /**
   * Set the palette colour for a specific index
   *
   * @param colour The colour index to set
   * @param r      The red value, between 0 and 1
   * @param g      The green value, between 0 and 1
   * @param b      The blue value, between 0 and 1
   */
  setPaletteColour(colour: number, r: number, g: number, b: number): void;

  /**
   * Mark the terminal as having changed. Should be called after all other terminal methods.
   */
  flushTerminal(): void;

  /**
   * Find a file system entry with the given name.
   *
   * @param path The path to find
   * @return A file entry, or {@code null} if none could be found.
   */
  getEntry(path: string): FileSystemEntry | null;

  /**
   * Create a new directory with the given path
   *
   * @param path The directory to create
   * @return A file entry if the directory exists or could be created, an empty one otherwise.
   */
  createDirectory(path: string): Result<FileSystemEntry>;

  /**
   * Create a new file with the given path. The owning folder must exist already.
   *
   * @param path The file to create
   * @return A file entry if the file exists or could be created, an empty one otherwise.
   */
  createFile(path: string): Result<FileSystemEntry>;

  /**
   * Recursively delete a file system entry.
   *
   * @param path The path to delete
   */
  deleteEntry(path: string): void;
}

export interface ComputerCallbacks {
  /**
     * Set the computer's label.
     *
     * @param label The computer's label.
     */
  setLabel(label: string | null): void;

  /**
   * Queue an event on the computer.
   */
  event(event: string, args: string[] | null): void;

  /**
   * Shut the computer down.
   */
  shutdown(): void;

  /**
   * Turn the computer on.
   */
  turnOn(): void;

  /**
   * Reboot the computer.
   */
  reboot(): void;

  /**
   * Dispose of this computer, marking it as no longer running.
   */
  dispose(): void;

  /**
   * Set the width and height of the computer. If not given, this will be synced with the
   *
   * @param width  The computer's width
   * @param height The computer's height.
   */
  resize(width: number, height: number): void;

  /**
   * Set a peripheral on a particular side
   *
   * @param side The side to set the peripheral on.
   * @param kind The kind of peripheral. For now, can only be "speaker".
   */
  setPeripheral(side: Side, kind: PeripheralKind | null): void;
}

export interface ConfigGroup {
  /**
   * Add a string property to this config group
   *
   * @param id Unique ID for this property
   * @param name        The display name of this group
   * @param description A short description of this group
   * @param def         The default value
   * @param changed     Called when it is changed
   */
  addString(id: string, name: string, def: string, description: string, changed: (value: string) => void): void;

  /**
   * Add a boolean property to this config group
   *
   * @param id Unique ID for this property
   * @param name        The display name of this group
   * @param description A short description of this group
   * @param def         The default value
   * @param changed     Called when it is changed
   */
  addBoolean(id: string, name: string, def: boolean, description: string, changed: (value: boolean) => void): void;

  /**
   * Add an integer property to this config group
   *
   * @param id Unique ID for this property
   * @param name        The display name of this group
   * @param description A short description of this group
   * @param min         The minimum value
   * @param max         The maximum value
   * @param def         The default value
   * @param changed     Called when it is changed
   */
  addInt(id: string, name: string, def: number, min: number, max: number, description: string, changed: (value: number) => void): void;
}

export interface Callbacks {
  /**
   * Get the current callback instance
   *
   * @param addComputer A computer to add a new computer.
   */
  setup(addComputer: (computer: ComputerAccess) => ComputerCallbacks): void;

  /**
* Get or create a config group
*
* @param name        The display name of this group
* @param description A short description of this group
* @return The constructed config group
*/
  config(name: string, description: string | null): ConfigGroup;

  setInterval(callback: () => void, delay: number): void;

  setImmediate(callback: () => void): void;

  strftime(format: string, time: Date): string;
}
