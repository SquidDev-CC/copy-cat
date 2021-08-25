import type { ComputerAccess, ComputerCallbacks, ConfigGroup, Callbacks as ICallbacks } from "./classes";
export type { ComputerAccess, FileSystemEntry, QueueEventHandler, Result, ConfigGroup as IConfigGroup } from "./classes";
import { timeFormat } from "d3-time-format";

import "setimmediate";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

let loaded = false;
let doAddComputer: ((computer: ComputerAccess) => ComputerCallbacks) | null = null;

class Callbacks implements ICallbacks {
  public readonly config: ConfigFactory;

  public constructor(config: ConfigFactory) {
    this.config = config;
  }

  public setup(addComputer: (computer: ComputerAccess) => ComputerCallbacks): void {
    doAddComputer = addComputer;
  }

  public setInterval(callback: () => void, delay: number): void {
    setInterval(callback, delay);
  }

  public setImmediate(callback: () => void): void {
    // Bodge, as there's no types for 'setImmediate'
    (window as any).setImmediate(callback); // eslint-disable-line @typescript-eslint/no-explicit-any
  }

  public strftime(format: string, time: Date): string {
    return timeFormat(format)(time);
  }
}

export const start = async (computer: ComputerAccess, config: ConfigFactory): Promise<ComputerCallbacks> => {
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  const classes = await import("./classes");
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  loaded = true;
  classes.default(new Callbacks(config));
  if (!doAddComputer) throw new Error("Callbacks.setup was never called");

  return doAddComputer(computer);
};
