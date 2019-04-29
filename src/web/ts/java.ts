import { ICallbacks, IComputerAccess } from "./classes";
export { ICallbacks, IComputerAccess, IFileSystemEntry, QueueEventHandler, Result } from "./classes";

import "setimmediate";

export class Callbacks implements ICallbacks {
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
    // Bodge, as there's no types for 'setImmediate'
    (window as any).setImmediate(callback);
  }
}

export const start = (computer: IComputerAccess) => {
  import("./classes")
    .then(x => x(new Callbacks(computer)))
    .catch(x => console.log(x)); // TODO: error handling
};
