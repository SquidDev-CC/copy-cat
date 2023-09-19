import { promises as fs } from "fs";
import path from "path";
import { createHash } from "crypto";

import resolve from "@rollup/plugin-node-resolve";
import replace from "@rollup/plugin-replace";
import terser from '@rollup/plugin-terser';
import { minify as minifyJavascript } from "terser";
import typescript from "@rollup/plugin-typescript";
import url from "@rollup/plugin-url";
import license from "rollup-plugin-license";
import postcss from "rollup-plugin-postcss";

/**
 * @param {string} out
 * @param {boolean} minify
 * @returns {import("rollup").RollupOptions}
 */
const makeSite = (out, minify) => ({
  input: ["src/web/ts/main.tsx", "src/web/ts/embed.tsx"],
  output: {
    dir: out,
    format: "amd",
    paths: {
      "monaco-editor": "vs/editor/editor.main",
    },
    freeze: true,
    generatedCode: {
      constBindings: true,
      arrowFunctions: true,
    }
  },
  context: "window",
  external: ["monaco-editor", "require"],

  plugins: [
    replace({
      preventAssignment: true,

      __storageBackend__: JSON.stringify(process.env.COPY_CAT_STORAGE || "storage"),
      __monaco__: "https://cdn.jsdelivr.net/npm/monaco-editor@0.43.0",
    }),

    postcss({
      namedExports: true,
      modules: true,
      minimize: minify,
    }),
    url({
      limit: 1024,
      fileName: "[name]-[hash][extname]",
      include: ["**/*.worker.js", "**/*.png"],
    }),

    typescript(),
    resolve({ browser: true, }),

    minify && terser(),

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
      async generateBundle(_, bundle) {
        const contents = await fs.readFile("node_modules/requirejs/require.js", { encoding: "utf-8" });
        this.emitFile({
          type: "asset",
          name: "require.js",
          fileName: "require.js",
          source: minify ? (await minifyJavascript(contents)).code : contents,
        });

        const version = createHash("sha256")
          .update(bundle["main.js"].code)
          .update(await fs.readFile(`src/web/public/main.css`))
          .digest("hex").slice(0, 8);

        await Promise.all(["index.html", "404.html", "main.css"].map(async x => this.emitFile({
          type: "asset",
          name: x,
          fileName: x,
          source: (await fs.readFile(`src/web/public/${x}`, { encoding: "utf-8" })).replaceAll("{{version}}", version),
        })));
      },
      async resolveId(source) {
        if (source === "./classes") return path.resolve("build/teaVM/classes.js");
        return null;
      },
    },
  ],
});

export default [
  makeSite("build/web", false),
  makeSite("build/webMin", true),
];
