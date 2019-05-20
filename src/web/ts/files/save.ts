const pad = (val: number, len: number) => {
  const str = val.toString();
  return str.length >= len ? str : "0".repeat(len - str.length) + str;
};

/**
 * Save a Blob to a file
 *
 * @param prefix The file's prefix
 * @param extension The appropriate extension
 * @param blob The blob to save to
 */
export default (prefix: string, extension: string, blob: Blob | null) => {
  if (!blob) return;

  // Somewhat inspired by https://github.com/eligrey/FileSaver.js/blob/master/src/FileSaver.js
  // Goodness knows how well this works on non-modern browsers.
  const element = document.createElement("a") as HTMLAnchorElement;
  const url = URL.createObjectURL(blob);

  const now = new Date();
  element.download = `${prefix}-${now.getFullYear()}-${pad(now.getMonth() + 1, 2)}-${pad(now.getDate(), 2)}_` +
    `${pad(now.getHours(), 2)}-${pad(now.getMinutes(), 2)}.${extension}`;
  element.rel = "noopener";
  element.href = url;

  setTimeout(() => URL.revokeObjectURL(url), 60e3);
  setTimeout(() => {
    try {
      element.dispatchEvent(new MouseEvent("click"));
    } catch (e) {
      const mouseEvent = document.createEvent("MouseEvents");
      mouseEvent.initMouseEvent("click", true, true, window, 0, 0, 0, 80, 20, false, false, false, false, 0, null);
      element.dispatchEvent(mouseEvent);
    }
  }, 0);
};
