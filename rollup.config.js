import path from "path";
import { promises as fs } from "fs";

import license from "rollup-plugin-license";
import postcss from "rollup-plugin-postcss";
import replace from "@rollup/plugin-replace";
import resolve from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import url from "@rollup/plugin-url";

const out = "build/rollup";

export default {
  input: ["src/web/ts/main.tsx", "src/web/ts/embed.tsx", "src/web/ts/loader.ts"],
  output: {
    dir: out,
    format: "amd",
    paths: {
      "monaco-editor": "vs/editor/editor.main",
    },
    preferConst: true,
  },
  context: "window",
  external: ["monaco-editor", "require", "jszip"],

  plugins: [
    replace({
      __storageBackend__: JSON.stringify(process.env.COPY_CAT_STORAGE || "storage"),
    }),

    postcss({
      namedExports: true,
      modules: true,
    }),
    url({
      limit: 1024,
      fileName: "[name]-[hash][extname]",
      include: ["**/*.worker.js", "**/*.png"],
    }),

    typescript(),
    resolve({ browser: true, }),
    // commonjs(),

    license({
      banner:
        `<%= pkg.name %>: Copyright <%= pkg.author %> <%= moment().format('YYYY') %>
<% _.forEach(_.sortBy(dependencies, ["name"]), ({ name, author, license }) => { %>
  - <%= name %>: Copyright <%= author ? author.name : "" %> (<%= license %>)<% }) %>

@license
  `,
      thirdParty: { output: `${out}/dependencies.txt` },
    }),

    {
      name: "copy-cat",
      writeBundle: async () => {
        await Promise.all([
          fs.copyFile("node_modules/requirejs/require.js", `${out}/require.js`),
          fs.copyFile("node_modules/jszip/dist/jszip.js", `${out}/jszip.js`)
        ]);
      },
      resolveId: async (source) => {
        if (source !== "./classes") return null;
        return path.resolve("build/javascript/classes.js");
      },
    },
  ],
};
