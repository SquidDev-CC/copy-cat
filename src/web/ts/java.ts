import type { ComputerAccess, ComputerCallbacks, ConfigGroup } from "./classes";
export type { ComputerAccess, FileSystemEntry, Result, ConfigGroup as IConfigGroup } from "./classes";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

let loaded = false;
let doAddComputer: ((computer: ComputerAccess) => ComputerCallbacks) | null = null;

export const start = async (computer: ComputerAccess, config: ConfigFactory): Promise<ComputerCallbacks> => {
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  const [classes, { version, resources }] = await Promise.all([import("./classes"), import("./resources")]);
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  loaded = true;

  const encoder = new TextEncoder();
  window.copycatCallbacks = {
    config,
    setup: add => doAddComputer = add,
    modVersion: version,
    listResources: () => Object.keys(resources),
    getResource: path => new Int8Array(encoder.encode(resources[path]))
  };
  classes.main();
  if (!doAddComputer) throw new Error("Callbacks.setup was never called");

  return doAddComputer(computer);
};
