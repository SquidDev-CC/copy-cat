export const convertMouseButton = (btn: number): number | undefined => {
  switch (btn) {
    case 0: return 1; // Left
    case 1: return 3; // Middle
    case 2: return 2; // Right
    default: return undefined;
  }
};

export const convertMouseButtons = (btn: number): number | undefined => {
  if ((btn & 1) !== 0) return 1; // Left
  if ((btn & 2) !== 0) return 2; // Right
  if ((btn & 4) !== 0) return 3; // Middle
  return undefined;
};

/**
 * Mapping of KeyboardEvent.code and KeyboardEvent.key
 *
 * @see https://www.w3.org/TR/uievents-key/
 * @see https://www.w3.org/TR/uievents-code/
 */
const keyboardCodes: { [key: string]: number } = {
  "Digit1": 2,  "1": 2,
  "Digit2": 3,  "2": 3,
  "Digit3": 4,  "3": 4,
  "Digit4": 5,  "4": 5,
  "Digit5": 6,  "5": 6,
  "Digit6": 7,  "6": 7,
  "Digit7": 8,  "7": 8,
  "Digit8": 9,  "8": 9,
  "Digit9": 10, "9": 10,
  "Digit0": 11, "0": 11,
  "Minus": 12,  "-": 12,
  "Equal": 13,
  "Backspace": 14,
  "Tab": 15,
  "KeyQ": 16, "Q": 16,
  "KeyW": 17, "W": 17,
  "KeyE": 18, "E": 18,
  "KeyR": 19, "R": 19,
  "KeyT": 20, "T": 20,
  "KeyY": 21, "Y": 21,
  "KeyU": 22, "U": 22,
  "KeyI": 23, "I": 23,
  "KeyO": 24, "O": 24,
  "KeyP": 25, "P": 25,
  "BracketLeft": 26,  "(": 26,
  "BracketRight": 27, ")": 27,
  "Enter": 28,
  "ControlLeft": 29, "Control": 29,
  "KeyA": 30, "A": 30,
  "KeyS": 31, "S": 31,
  "KeyD": 32, "D": 32,
  "KeyF": 33, "F": 33,
  "KeyG": 34, "G": 34,
  "KeyH": 35, "H": 35,
  "KeyJ": 36, "J": 36,
  "KeyK": 37, "K": 37,
  "KeyL": 38, "L": 38,
  "Semicolon": 39, ";": 39,
  "Quote": 40,     "'": 40,
  "Backquote": 41, "`": 41,
  "ShiftLeft": 42, "Shift": 42,
  "IntlBackslash": 43,
  "KeyZ": 44, "Z": 44,
  "KeyX": 45, "X": 45,
  "KeyC": 46, "C": 46,
  "KeyV": 47, "V": 47,
  "KeyB": 48, "B": 48,
  "KeyN": 49, "N": 49,
  "KeyM": 50, "M": 50,
  "Comma": 51, ",": 51,
  "Period": 52, ".": 52,
  "Slash": 53, "/": 53,
  "ShiftRight": 54,
  "NumpadMultiply": 55,
  "AltLeft": 56, "Alt": 56,
  "Space": 57, " ": 57,
  "CapsLock": 58,
  "F1": 59,
  "F2": 60,
  "F3": 61,
  "F4": 62,
  "F5": 63,
  "F6": 64,
  "F7": 65,
  "F8": 66,
  "F9": 67,
  "F10": 68,
  "NumLock": 69,
  "ScollLock": 70,
  "Numpad7": 71,
  "Numpad8": 72,
  "Numpad9": 73,
  "NumpadSubtract": 74,
  "Numpad4": 75,
  "Numpad5": 76,
  "Numpad6": 77,
  "NumpadAdd": 78,
  "Numpad1": 79,
  "Numpad2": 80,
  "Numpad3": 81,
  "Numpad0": 82,
  "NumpadDecimal": 83,
  "F11": 87,
  "F12": 88,
  "F13": 100,
  "F14": 101,
  "F15": 102,

  // I have absolutely no clue about these. If someone has a keyboard with these
  // on, please confim that they're right.
  "KanaMode": 112,
  "Convert": 121,
  "NonConvert": 123,
  "IntlYen": 125,
  "NumpadEqual": 141,
  "Cimcumflex": 144,
  "At": 145,
  "Colon": 146,
  "Underscore": 147,
  "Kanji": 148,
  "Stop": 149,
  "Ax": 150,

  "NumpadEnter": 156,
  "ControlRight": 157,
  "NumpadComma": 179,
  "NumpadDivide": 181,
  "AltRight": 184,
  "Pause": 197,
  "Home": 199,
  "ArrowUp": 200,
  "PageUp": 201,
  "ArrowLeft": 203,
  "ArrowRight": 205,
  "End": 207,
  "ArrowDown": 208,
  "PageDown": 209,
  "Insert": 210,
  "Delete": 211,
};

export const convertKey = (key: string): number | undefined => keyboardCodes[key];
