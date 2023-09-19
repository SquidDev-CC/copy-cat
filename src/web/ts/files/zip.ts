import type JSZip from "jszip";

export default async (): Promise<JSZip> => {
  return new (await import("jszip")).default();
};
