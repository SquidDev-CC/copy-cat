import { ICallbacks, IComputerAccess, IConfigGroup } from "./classes";
export { ICallbacks, IComputerAccess, IFileSystemEntry, QueueEventHandler, Result, IConfigGroup } from "./classes";

import "setimmediate";

export type ConfigFactory = (name: string, description: string | null) => IConfigGroup;

export class Callbacks implements ICallbacks {
  private readonly computer: IComputerAccess;
  public readonly config: ConfigFactory;

  constructor(computer: IComputerAccess, config: ConfigFactory) {
    this.computer = computer;
    this.config = config;
  }

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

export const start = (computer: IComputerAccess, config: ConfigFactory) => {
  import("./classes")
    .then(x => x(new Callbacks(computer, config)))
    .catch(x => console.log(x)); // TODO: error handling
};
