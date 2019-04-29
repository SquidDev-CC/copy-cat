export = Start;

declare function Start(callbacks: Start.ICallbacks): void;

declare namespace Start {
  /**
   * A handler for {@link ICallbacks.onEvent}
   */
  export type QueueEventHandler = (event: string, args: string[]) => void;

  /**
   * A naive Either type, instead of wrangling JS/Java exceptions.
   */
  export type Result<T> = { value: T } | { error: string, value: null };

  export interface IFileSystemEntry {
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
    getContents(): string;

    /**
     * Set the contents of this filesystem entry
     *
     * @param contents This entry's contents
     * @return Whether this file's contents was set or not
     * @throws If this is not a file
     */
    setContents(contents: string): Result<true>;
  }

  /**
   * Controls a specific computer on the Javascript side. See {@code js/ComputerAccess.java}.
   */
  export interface IComputerAccess {
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
    getEntry(path: string): IFileSystemEntry | null;

    /**
     * Create a new directory with the given path
     *
     * @param path The directory to create
     * @return A file entry if the directory exists or could be created, an empty one otherwise.
     */
    createDirectory(path: string): Result<IFileSystemEntry>;

    /**
     * Create a new file with the given path. The owning folder must exist already.
     *
     * @param path The file to create
     * @return A file entry if the file exists or could be created, an empty one otherwise.
     */
    createFile(path: string): Result<IFileSystemEntry>;

    /**
     * Recursively delete a file system entry.
     *
     * @param path The path to delete
     */
    deleteEntry(path: string): void;

    /**
     * Set the callback used when an event is received.
     *
     * @param handler The event handler
     */
    onEvent(listener: QueueEventHandler): void;

    /**
     * Set the callback used when the computer must be shut down
     *
     * @param handler The event handler
     */
    onShutdown(handler: () => void): void;

    /**
     * Set the callback used when the computer must be turned on
     *
     * @param handler The event handler
     */
    onTurnOn(handler: () => void): void;

    /**
     * Set the callback used when the computer must be restarted
     *
     * @param handler The event handler
     */
    onReboot(handler: () => void): void;
  }

  export interface ICallbacks {
    /**
     * Get the current callback instance
     *
     * @return The callback instance
     */
    getComputer(): IComputerAccess;

    setInterval(callback: () => void, delay: number): void;

    setImmediate(callback: () => void): void;
  }
}
