import builtins from "rollup-plugin-node-builtins";
import commonjs from "@rollup/plugin-commonjs";
import license from "rollup-plugin-license"
import replace from "@rollup/plugin-replace";
import resolve from "@rollup/plugin-node-resolve";

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
  external: ["monaco-editor"],

  plugins: [
    replace({
      __storageBackend__: JSON.stringify(process.env.COPY_CAT_STORAGE || "storage"),
    }),

    builtins(),
    resolve({ browser: true, }),
    commonjs(),

    (() => {
      const plugin = license({
        banner:
          `<%= pkg.name %>: Copyright <%= pkg.author %> <%= moment().format('YYYY') %>
<% _.forEach(_.sortBy(dependencies, ["name"]), ({ name, author, license }) => { %>
  - <%= name %>: Copyright <%= author ? author.name : "" %> (<%= license %>)<% }) %>

@license
  `,
        thirdParty: { output: "build/rollup/dependencies.txt" },
      });

      // Add some additional packages which depend on elsewhere. Yes, this is horrible.
      /*
      for (const pkg of ["gif.js", "monaco-editor", "requirejs"]) {
        plugin.load(`${__dirname}/node_modules/${pkg}/_.js`);
      }
      */

      return plugin;
    })(),
  ],
};
