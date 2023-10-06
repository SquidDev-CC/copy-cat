import { transform } from "lightningcss";
import { createRequire } from 'node:module';

const styleInject = JSON.stringify(createRequire(import.meta.url).resolve("style-inject/dist/style-inject.es.js"));

const compile = (filename, input, minify) => transform({
  filename,
  cssModules: true,
  pattern: "[local]-[hash]",
  code: Buffer.from(input),
  minify,
});

/**
 * Generate an ESM module from a list of {@code ligntingcss} exports.
 *
 * @param {string} filename The input file name.
 * @param {string} input The input code.
 * @param {boolean} minify Whether to minify the css.
 * @returns {string} The ESM module, containing the list of exports.
 */
export const cssToModule = (filename, input, minify) => {
  const { code, exports } = compile(filename, input, minify);

  let output = "";
  let importId = 0;
  for (const [name, symbol] of Object.entries(exports)) {
    let className = JSON.stringify(symbol.name);
    for (const dep of symbol.composes) {
      if (dep.type == "dependency") {
        output += `import { ${dep.name} as import_${++importId}$ } from ${JSON.stringify(dep.specifier)};\n`;
        className += `+ " " + import_${importId}$`;
      } else {
        className += `+ " " + ${JSON.stringify(dep.name)}`;
      }
    }
    output += `export const ${name} = ${className};\n`;
  }

  output += `import styleInject from ${styleInject};\nstyleInject(${JSON.stringify(new TextDecoder().decode(code))})`;

  return output;
}

/**
 * Get a list of CSS classes in a file.
 *
 * @param {string} filename The input file name.
 * @param {string} input The input contents.
 * @returns {string[]} A list of classes exported by the module.
 */
export const getExports = (filename, input) => Object.keys(compile(filename, input, false).exports);
