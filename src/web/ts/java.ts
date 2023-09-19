import type { ComputerAccess, ComputerCallbacks, ConfigGroup, Callbacks as ICallbacks } from "./classes";
export type { ComputerAccess, FileSystemEntry, Result, ConfigGroup as IConfigGroup } from "./classes";

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
  (window as any).copycatCallbacks = new Callbacks(config); // eslint-disable-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-unsafe-member-access
  classes.main();
  if (!doAddComputer) throw new Error("Callbacks.setup was never called");

  return doAddComputer(computer);
};
