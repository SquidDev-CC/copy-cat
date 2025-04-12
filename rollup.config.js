import { createHash } from "crypto";
import { promises as fs } from "fs";
import path from "path";

import resolve from "@rollup/plugin-node-resolve";
import replace from "@rollup/plugin-replace";
import typescript from "@rollup/plugin-typescript";
import url from "@rollup/plugin-url";
import { minify as minifyJavascript } from "@swc/core";
import license from "rollup-plugin-license";

import { cssToModule } from "./tools/css-tools.js";

const description = `
Copy Cat is a web emulator for the popular Minecraft mod CC: Tweaked. Here you
can play with a ComputerCraft computer, write and test programs and experiment
to your heart's desire, without having to leave your browser!
`.replaceAll("\n", " ").trim();

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
      __monaco__: "https://cdn.jsdelivr.net/npm/monaco-editor@0.52.2",
    }),

    url({
      limit: 1024,
      fileName: "[name]-[hash][extname]",
      include: ["**/*.worker.js", "**/*.png"],
    }),

    typescript(),
    resolve({ browser: true }),

    license({
      banner:
        `<%= pkg.name %>: Copyright <%= pkg.author %> <%= moment().format("YYYY") %>
<% _.forEach(_.sortBy(dependencies, ["name"]), ({ name, author, license }) => { %>
  - <%= name %>: Copyright <%= author ? author.name : "" %> (<%= license %>)<% }) %>

@license
  `,
      thirdParty: { output: `${out}/dependencies.txt` },
    }),

    {
      name: "copy-cat",

      async transform(input, id) {
        if (id.endsWith(".css")) {
          return { code: cssToModule(id, input, minify), map: { mappings: "" } };
        }
      },

      async renderChunk(code) {
        return minify ? (await minifyJavascript(code)).code : code;
      },

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
          source: (await fs.readFile(`src/web/public/${x}`, { encoding: "utf-8" })).replaceAll("{{version}}", version).replaceAll("{{description}}", description),
        })));
      },

      async resolveId(source) {
        if (source === "cct/classes") return path.resolve("build/teaVM/classes.js");
        if (source === "cct/resources") return path.resolve("build/teaVM/resources.js");
        return null;
      },
    },
  ],
});

export default [
  makeSite("build/web", false),
  makeSite("build/webMin", true),
];
