import builtins from "rollup-plugin-node-builtins";
import commonjs from "@rollup/plugin-commonjs";
import license from "rollup-plugin-license";
import postcss from "rollup-plugin-postcss";
import replace from "@rollup/plugin-replace";
import resolve from "@rollup/plugin-node-resolve";
import url from "@rollup/plugin-url";

export default {
  input: ["build/javascript/main.js", "build/javascript/embed.js"],
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

    postcss({
      extract: "build/rollup/main.css",
      namedExports: name => name.replace(/-([a-z])/g, (_, x) => x.toUpperCase()),
      modules: true,
    }),
    url({
      limit: 1024,
      fileName: "[name]-[hash][extname]",
      include: ["**/*.worker.js", "**/*.png"],
    }),

    builtins(),
    resolve({ browser: true, }),
    commonjs(),

    license({
      banner:
        `<%= pkg.name %>: Copyright <%= pkg.author %> <%= moment().format('YYYY') %>
<% _.forEach(_.sortBy(dependencies, ["name"]), ({ name, author, license }) => { %>
  - <%= name %>: Copyright <%= author ? author.name : "" %> (<%= license %>)<% }) %>

@license
  `,
      thirdParty: { output: "build/rollup/dependencies.txt" },
    }),
  ],
};
