export type PaletteColour = string;
export type Palette = { [colour: string]: PaletteColour };

export const defaultPalette: Palette = {
  0: "rgb(240,240,240)", // White
  1: "rgb(242,178,51)",  // Orange
  2: "rgb(229,127,216)", // Magenta
  3: "rgb(153,178,242)", // Light blue
  4: "rgb(222,222,108)", // Yellow
  5: "rgb(127,204,25)",  // Lime
  6: "rgb(242,178,204)", // Pink
  7: "rgb(76,76,76)",    // Grey
  8: "rgb(153,153,153)", // Light grey
  9: "rgb(76,153,178)",  // Cyan
  a: "rgb(178,102,229)", // Purple
  b: "rgb(37,49,146)",   // Blue
  c: "rgb(127,102,76)",  // Brown
  d: "rgb(87,166,78)",   // Green
  e: "rgb(204,76,76)",   // Red
  f: "rgb(0,0,0)",       // Black
};

export class TerminalData {
  public text: string[];
  public fore: string[];
  public back: string[];

  public palette: Palette = defaultPalette;

  public currentFore: string = "0";

  public sizeX: number = 0;
  public sizeY: number = 0;

  public cursorX: number = 0;
  public cursorY: number = 0;
  public cursorBlink: boolean = false;

  public constructor() {
    this.text = [];
    this.fore = [];
    this.back = [];
  }

  public resize(width: number, height: number): void {
    this.sizeX = width;
    this.sizeY = height;

    this.text = new Array(height);
    this.fore = new Array(height);
    this.back = new Array(height);

    let baseText = "";
    let baseFore = "";
    let baseBack = "";
    for (let x = 0; x < width; x++) {
      baseText += " ";
      baseFore += this.currentFore;
      baseBack += "f";
    }
    for (let y = 0; y < height; y++) {
      this.text[y] = baseText;
      this.fore[y] = baseFore;
      this.back[y] = baseBack;
    }
  }
}
