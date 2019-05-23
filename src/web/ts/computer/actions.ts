export class Semaphore {
  private readonly listeners: Set<() => void> = new Set();

  public attach(listener: () => void) {
    this.listeners.add(listener);
  }

  public detach(listener: () => void) {
    this.listeners.delete(listener);
  }

  public signal() {
    for (const listener of this.listeners) listener();
  }
}

/**
 * Types admissable to the Lua side
 */
export type LuaValue = number | string | boolean;

/**
 * An object on which one can perform computer actions.
 */
export interface IComputerActionable {
  /**
   * Queue an event on the computer
   */
  queueEvent(event: string, args: LuaValue[]): void;

  /**
   * Turn on the computer
   */
  turnOn(): void;

  /**
   * Shut down the computer
   */
  shutdown(): void;

  /**
   * Reboot the computer
   */
  reboot(): void;
}
