import { IComputerAccess, QueueEventHandler } from "../java";
import { TerminalData } from "../terminal/data";
import { IComputerActionable, LuaValue, Semaphore } from "./actions";

import "setimmediate";

const colours = "0123456789abcdef";

export class ComputerAccess implements IComputerAccess, IComputerActionable {
  private queueEventHandler?: QueueEventHandler;
  private turnOnHandler?: () => void;
  private shutdownHandler?: () => void;
  private rebootHander?: () => void;

  private terminal: TerminalData;
  private semaphore: Semaphore;
  private stateChanged: (label: string | null, on: boolean) => void;

  constructor(
    terminal: TerminalData, semaphore: Semaphore,
    stateChange: (label: string | null, on: boolean) => void,
  ) {
    this.terminal = terminal;
    this.semaphore = semaphore;
    this.stateChanged = stateChange;
  }

  public setState(label: string | null, on: boolean): void {
    this.stateChanged(label , on);
  }

  public updateTerminal(
    width: number, height: number,
    x: number, y: number, blink: boolean, cursorColour: number,
  ): void {
    this.terminal.resize(width, height);
    this.terminal.cursorX = x;
    this.terminal.cursorY = y;
    this.terminal.cursorBlink = blink;
    this.terminal.currentFore = colours.charAt(cursorColour);
  }

  public setTerminalLine(line: number, text: string, fore: string, back: string): void {
    this.terminal.text[line] = text;
    this.terminal.fore[line] = fore;
    this.terminal.back[line] = back;
  }

  public setPaletteColour(colour: number, r: number, g: number, b: number): void {
    this.terminal.palette[colours.charAt(colour)] =
      `rgb(${(r * 0xFF) & 0xFF},${(g * 0xFF) & 0xFF},${(b * 0xFF) & 0xFF}`;
  }

  public flushTerminal() {
    this.semaphore.signal();
  }

  public onEvent(listener: QueueEventHandler): void {
    this.queueEventHandler = listener;
  }

  public onShutdown(handler: () => void): void {
    this.shutdownHandler = handler;
  }

  public onTurnOn(handler: () => void): void {
    this.turnOnHandler = handler;
  }

  public onReboot(handler: () => void): void {
    this.rebootHander = handler;
  }

  public queueEvent(event: string, args: LuaValue[]): void {
    if (this.queueEventHandler !== undefined) this.queueEventHandler(event, args.map(x => JSON.stringify(x)));
  }

  public turnOn(): void {
    if (this.turnOnHandler !== undefined) this.turnOnHandler();
  }

  public shutdown(): void {
    if (this.shutdownHandler !== undefined) this.shutdownHandler();
  }

  public reboot(): void {
    if (this.rebootHander !== undefined) this.rebootHander();
  }
}
