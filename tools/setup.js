/** Converts our styles into tsc files */
import fs from "fs";
import postcss from "postcss";
import selectorParser from "postcss-selector-parser";

const selector = selectorParser();

// Convert styles.css into a ts.d file
const contents = fs.readFileSync("src/web/ts/styles.css");
const css = postcss.parse(contents, { from: "src/web/ts/styles.css" });

const rules = new Set();
css.walkRules(rule => selector.astSync(rule.selector).walkClasses(x => rules.add(x.value)));

const out = Array.from(rules).map(x => `export const ${x} : string;\n`).join("");
fs.writeFileSync("src/web/ts/styles.css.d.ts", out);
