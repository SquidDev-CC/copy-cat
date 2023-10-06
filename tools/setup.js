/** Converts our styles into tsc files */
import fs from "fs";

import { getExports } from "./css-tools.js";

const name = "src/web/ts/styles.css";
const out = getExports(name, fs.readFileSync(name)).map(x => `export const ${x} : string;\n`).join("");
fs.writeFileSync(`${name}.d.ts`, out);
