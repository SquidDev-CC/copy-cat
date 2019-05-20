export * from "monaco-editor";

/**
 * Firefox doesn't support "onmousewheel", but Monaco seems insistent on using
 * it. Stub it for the time being, in the most ugly way possible.
 */
if (!("onmousewheel" in document)) {
  const add = EventTarget.prototype.addEventListener;
  const remove = EventTarget.prototype.removeEventListener;

  EventTarget.prototype.addEventListener = function(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions,
    ) {
      if (type === "mousewheel") add.call(this, "DOMMouseScroll", listener, options);
      return add.call(this, type, listener, options);
    };

  EventTarget.prototype.removeEventListener = function(
    type: string,
    listener: EventListenerOrEventListenerObject | null,
    options?: boolean | AddEventListenerOptions,
    ) {
      if (type === "mousewheel") remove.call(this, "DOMMouseScroll", listener, options);
      return remove.call(this, type, listener, options);
    };
}

import "../editor/lua";
