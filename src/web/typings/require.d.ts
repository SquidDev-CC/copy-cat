/**
 * A terrible hack to allow importing requirejs. This compiles to
 * `require(["require"], require => {...})`, which provides a context-sensitive
 * module loader.
 */

declare const require: typeof requirejs;

export default require;
