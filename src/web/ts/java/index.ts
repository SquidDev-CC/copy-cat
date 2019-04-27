/**
 * A handler for {@link ICallbacks.onEvent}
 */
export type QueueEventHandler = (event: string, args: string[]) => void;

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

class Callbacks {
  private computer: IComputerAccess;

  constructor(computer: IComputerAccess) {
    this.computer = computer;
  }

  /**
   * Get the current callback instance
   *
   * @return The callback instance
   */
  public getComputer(): IComputerAccess {
    return this.computer;
  }

  public setInterval(callback: () => void, delay: number): void {
    setInterval(callback, delay);
  }

  public setImmediate(callback: () => void): void {
    setImmediate(callback);
  }
}

export const start = (computer: IComputerAccess) => {
  const global = window as any;
  global.callbacks = new Callbacks(computer);
  global.main();
};
