// Basic language support for Lua/CC.
//
// Everything we do here is very primitive - there's no analysis here, just
// regex, so we'll happily ignore scope or syntax in order to provide doc
// comments.
import { type IMarkdownString, Range, languages } from "monaco-editor";

interface IlluaminateItem {
  name: string;
  summary?: string;
  module: string;
  "module-kind": string;
  url: string;
}

type IlluaminateIndex = Record<string, IlluaminateItem>;

let index: Promise<IlluaminateIndex | null> | null;

/**
 * Fetch CC:T's documentation index.
 * @returns The documentation index, or {@code null} if downloading it failed.
 */
const getDocumentationIndex = (): Promise<IlluaminateIndex | null> => index ?? (index = fetch("https://tweaked.cc/index.json")
  .then(x => x.json() as Promise<IlluaminateIndex>)
  .catch(x => {
    console.error("Failed to fetch index", x);
    return null;
  }));

/**
 * Attempt to find a documented term.
 *
 * @param name The name to look up in the documentation.
 * @returns The documented term, or {@code null} if it could not be found.
 */
const findDocumentedTerm = async (name: string): Promise<IlluaminateItem | null> => {
  if (name.match(/\.[A-Z]/)) return null;
  const index = await getDocumentationIndex();
  if (!index) return null;
  return index[name] ?? index[`_G.${name}`];
};

const wordBefore = /[A-za-z_][\w.]*$/;
const wordAfter = /^[\w.]*/;

languages.registerHoverProvider("luax", {
  provideHover: async (model, position) => {
    const line = model.getLineContent(position.lineNumber);
    const before = line.substring(0, position.column).match(wordBefore)?.[0] ?? "";
    const after = line.substring(position.column).match(wordAfter)?.[0] ?? "";

    const definitionName = before + after;
    if (!definitionName) return;

    const item = await findDocumentedTerm(definitionName);
    if (!item || item["module-kind"] != "module") return null;

    const contents: Array<IMarkdownString> = [
      { value: `\`${item.name}\`` }
    ];
    if (item.summary) contents.push({ value: item.summary });
    contents.push({ value: `[View full documentation](https://tweaked.cc/${item.url})` });

    return {
      range: new Range(
        position.lineNumber,
        position.column - before.length,
        position.lineNumber,
        position.column + after.length,
      ),
      contents,
    };
  },
});

const makeColour = (x: number): languages.IColor => ({
  red: ((x >> 16) & 0xFF) / 255,
  green: ((x >> 8) & 0xFF) / 255,
  blue: (x & 0xFF) / 255,
  alpha: 1
});
const colourLookup: Record<string, languages.IColor> = {
  "colors.white": makeColour(0xF0F0F0),
  "colors.orange": makeColour(0xF2B233),
  "colors.magenta": makeColour(0xE57FD8),
  "colors.lightBlue": makeColour(0x99B2F2),
  "colors.yellow": makeColour(0xDEDE6C),
  "colors.lime": makeColour(0x7FCC19),
  "colors.pink": makeColour(0xF2B2CC),
  "colors.gray": makeColour(0x4C4C4C),
  "colors.lightGray": makeColour(0x999999),
  "colors.cyan": makeColour(0x4C99B2),
  "colors.purple": makeColour(0xB266E5),
  "colors.blue": makeColour(0x3366CC),
  "colors.brown": makeColour(0x7F664C),
  "colors.green": makeColour(0x57A64E),
  "colors.red": makeColour(0xCC4C4C),
  "colors.black": makeColour(0x111111),
  "colours.white": makeColour(0xF0F0F0),
  "colours.orange": makeColour(0xF2B233),
  "colours.magenta": makeColour(0xE57FD8),
  "colours.lightBlue": makeColour(0x99B2F2),
  "colours.yellow": makeColour(0xDEDE6C),
  "colours.lime": makeColour(0x7FCC19),
  "colours.pink": makeColour(0xF2B2CC),
  "colours.grey": makeColour(0x4C4C4C),
  "colours.lightGrey": makeColour(0x999999),
  "colours.cyan": makeColour(0x4C99B2),
  "colours.purple": makeColour(0xB266E5),
  "colours.blue": makeColour(0x3366CC),
  "colours.brown": makeColour(0x7F664C),
  "colours.green": makeColour(0x57A64E),
  "colours.red": makeColour(0xCC4C4C),
  "colours.black": makeColour(0x111111),
};

languages.registerColorProvider("luax", {
  provideColorPresentations: () => [],
  provideDocumentColors: model => {
    const colours: Array<languages.IColorInformation> = [];
    for (const { range } of model.findMatches("colou?rs\\.\\w+", false, true, true, "()[]{}<>`'\"-/;:,.?!", false)) {
      const color = colourLookup[model.getValueInRange(range)];
      if (color) colours.push({ color, range });
    }
    return colours;
  },
});
