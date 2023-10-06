import "setimmediate";

import type { ComputerDisplay, ComputerHandle, ConfigGroup } from "cct/classes";
export type {
  ComputerDisplay, ComputerHandle, ConfigGroup, FileAttributes, FileSystemEntry, PeripheralKind, Result, Side
} from "cct/classes";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

let loaded = false;
let doAddComputer: ((computer: ComputerDisplay) => ComputerHandle) | null = null;

export const start = async (computer: ComputerDisplay, config: ConfigFactory): Promise<ComputerHandle> => {
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  const [classes, { version, resources }] = await Promise.all([import("cct/classes"), import("cct/resources")]);
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  loaded = true;

  const encoder = new TextEncoder();
  window.$javaCallbacks = {
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
