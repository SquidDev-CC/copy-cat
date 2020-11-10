import type JSZip from "jszip";

/**
 * Rather ugly wrapper for jszip which sets 'global' before loading it.
 *
 * Yes, it's horrible, but it's needed.
 */
export default async (): Promise<JSZip> => {
  return new (await import("jszip")).default();
};
