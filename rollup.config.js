import resolve from "rollup-plugin-node-resolve";
import commonJs from "rollup-plugin-commonjs";
import builtins from "rollup-plugin-node-builtins";

export default {
  input: "build/javascript/main.js",
  output: {
    dir: "build/rollup/",
    format: "amd",
    paths: {
      "monaco-editor": "vs/editor/editor.main",
    },
    preferConst: true,
  },
  context: "window",
  external: [ "monaco-editor" ],

  plugins: [
    builtins(),
    resolve({ browser: true, }),
    commonJs(),
  ],
};
