/**
 * Rather ugly wrapper for jszip which sets 'global' before loading it.
 *
 * Yes, it's horrible, but it's needed.
 */
export default async () => {
  const globalObj = window as any;
  if (!globalObj.global) globalObj.global = globalObj;

  return new (await import("jszip")).default();
};
