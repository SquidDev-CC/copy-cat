/** The backend used to store values.
 *
 *  This is injected by `rollup.config.js`.
 */
declare const __storageBackend__: string;

declare module "cct/classes" {

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
    getChildren(): Array<string>;

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

  export interface ComputerDisplay {
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
  export interface ComputerHandle {
    /**
     * Set the computer's label.
     *
     * @param label The computer's label.
      */
    setLabel(label: string | null): void;

    /**
     * Set the width and height of the computer. If not given, this will be synced with the
     *
     * @param width  The computer's width
     * @param height The computer's height.
     */
    resize(width: number, height: number): void;
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
     * Get or create a config group
     *
     * @param name        The display name of this group
     * @param description A short description of this group
     * @return The constructed config group
     */
    config(name: string, description: string | null): ConfigGroup;
  }
}
