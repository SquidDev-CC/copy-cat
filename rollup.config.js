import resolve from "rollup-plugin-node-resolve";

export default {
  input: {
    main: "build/typescript/main.js",
    classes: "build/teaVM/classes.js",
    editor: "build/typescript/editor/index.js",
  },
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

  plugins: [ resolve() ],
};
