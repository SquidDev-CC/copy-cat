import type { ComputerAccess, ConfigGroup, Callbacks as ICallbacks } from "./classes";
export type { ComputerAccess, FileSystemEntry, QueueEventHandler, Result, ConfigGroup as IConfigGroup } from "./classes";

import "setimmediate";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

export class Callbacks implements ICallbacks {
  private readonly computer: ComputerAccess;
  public readonly config: ConfigFactory;

  public constructor(computer: ComputerAccess, config: ConfigFactory) {
    this.computer = computer;
    this.config = config;
  }

  public getComputer(): ComputerAccess {
    return this.computer;
  }

  public setInterval(callback: () => void, delay: number): void {
    setInterval(callback, delay);
  }

  public setImmediate(callback: () => void): void {
    // Bodge, as there's no types for 'setImmediate'
    (window as any).setImmediate(callback); // eslint-disable-line @typescript-eslint/no-explicit-any
  }
}

export const start = (computer: ComputerAccess, config: ConfigFactory) => {
  import("./classes")
    .then(x => x.default(new Callbacks(computer, config)))
    .catch(x => console.error("Cannot load classes", x));
};
