/**
 * A wrapper for gif.js, just ensuring we resolve at the correct location.
 */
import { GIF, Options } from "./gif";
export { GIF, Options, FrameOptions } from "./gif";

let factory: ((options: Partial<Options>) => GIF) | null = null;

const debug = window.location.protocol === "file:" || window.location.hostname === "localhost";

export const create = async (options: Partial<Options>) => {
  options = { workerScript: "assets/gif.worker.js", debug, ...options };
  if (factory) return factory(options);

  const gif = await import("./gif");
  factory = opts => new gif(opts);
  return factory(options);
};
