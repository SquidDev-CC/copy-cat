import "setimmediate";

import type { ComputerDisplay, ComputerHandle, ConfigGroup } from "cct/classes.js";
export type {
  ComputerDisplay, ComputerHandle, ConfigGroup, FileAttributes, FileSystemEntry, PeripheralKind, Result, Side
} from "cct/classes.js";
import { load as teaVMLoad } from "cct/wasm-gc-runtime.js";
import { exceptions, gc } from "wasm-feature-detect";
import wasmClasses from "cct/classes.wasm";

export type ConfigFactory = (name: string, description: string | null) => ConfigGroup;

const loadClasses = async (): Promise<{ main: (args: string[]) => void }> => {
    if (
        typeof WebAssembly === "object" && typeof WebAssembly.compileStreaming === "function" &&
        await exceptions() && await gc()
    ) {
        try {
            console.log("Loading WASM runtime");
            return (await teaVMLoad(wasmClasses)).exports;
        } catch (e) {
            console.error("Failed to load WebAssembly runtime", e);
        }
    }

    console.log("Using JS runtime");
    return await import("cct/classes.js");
}

let loaded = false;
let doAddComputer: ((computer: ComputerDisplay) => ComputerHandle) | null = null;

export const start = async (computer: ComputerDisplay, config: ConfigFactory): Promise<ComputerHandle> => {
  if (loaded) {
    if (!doAddComputer) throw new Error("Failed to load computer (see previous errors for a possible reason");
    return doAddComputer(computer);
  }

  const [classes, { version, resources }] = await Promise.all([loadClasses(), import("cct/resources.js")]);
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
  classes.main([]);
  if (!doAddComputer) throw new Error("Callbacks.setup was never called");

  return doAddComputer(computer);
};
