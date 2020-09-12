import type { ComputerAccess, ConfigGroup, Callbacks as ICallbacks } from "./classes";
export type { ComputerAccess, FileSystemEntry, QueueEventHandler, Result, ConfigGroup as IConfigGroup } from "./classes";

import "setimmediate";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

let loaded = false;
let doAddComputer: ((computer: ComputerAccess) => void) | null = null;

export class Callbacks implements ICallbacks {
  public readonly config: ConfigFactory;

  public constructor(config: ConfigFactory) {
    this.config = config;
  }

  public setup(addComputer: (computer: ComputerAccess) => void): void {
    doAddComputer = addComputer;
  }

  public setInterval(callback: () => void, delay: number): void {
    setInterval(callback, delay);
  }

  public setImmediate(callback: () => void): void {
    // Bodge, as there's no types for 'setImmediate'
    (window as any).setImmediate(callback); // eslint-disable-line @typescript-eslint/no-explicit-any
  }
}

export const start = (computer: ComputerAccess, config: ConfigFactory): void => {
  if (loaded) {
    if (doAddComputer) doAddComputer(computer);
    return;
  }

  import("./classes")
    .then(x => {
      if (loaded) {
        if (doAddComputer) doAddComputer(computer);
        return;
      }

      loaded = true;
      x.default(new Callbacks(config));
      if (!doAddComputer) {
        console.error("Callbacks.setup was never called");
      } else {
        doAddComputer(computer);
      }
    })
    .catch(x => console.error("Cannot load classes", x));
};
