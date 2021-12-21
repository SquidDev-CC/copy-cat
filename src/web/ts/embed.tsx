import { Component, h, render } from "preact";
import { Semaphore, Terminal, TerminalData } from "@squid-dev/cc-web-term";
import type { ConfigGroup, PeripheralKind, Side } from "./classes";
import { ComputerAccess, splitName } from "./computer/access";
import { StoragePersistence, VoidPersistence } from "./computer/persist";
import termFont from "@squid-dev/cc-web-term/assets/term_font.png";
import termFontHd from "@squid-dev/cc-web-term/assets/term_font_hd.png";
import requirejs from "require";

type MainProps = {
  hdFont?: boolean | string,
  persistId?: number,
  files?: { [filename: string]: string | ArrayBuffer },
  label?: string,
  width?: number,
  height?: number,
  resolve?: (computer: ComputerAccess) => void,
  peripherals?: {
    [side in Side]: PeripheralKind | null
  },
}

type MainState = {
  on: boolean,
  font: string,
  label: string | null,
  terminal: TerminalData,
  terminalChanged: Semaphore,
  computer: ComputerAccess,
};

const emptyGroup: ConfigGroup = {
  addString: () => { },
  addBoolean: () => { },
  addInt: () => { },
};

class Computer extends Component<MainProps, MainState> {
  public constructor(props: MainProps, context: unknown) {
    super(props, context);

    const { persistId, hdFont } = props;

    const terminal = new TerminalData();
    const terminalChanged = new Semaphore();

    const computer = new ComputerAccess(
      persistId === undefined ? new VoidPersistence() : new StoragePersistence(persistId),
      terminal, terminalChanged,
      (label, on) => this.setState({ label, on }),
    );
    props.resolve?.(computer);

    const font = typeof hdFont === "string" ? hdFont :
      // We need to do some terrible path hackery to get this to resolve relative to the
      // current script (and thus copy-cat.squiddev.ccc).
      // termFont{Hd,} will be of the form "termFont_xxx.png" - we convert it to
      // "./termFont_xxx.png", and then resolve.
      requirejs.toUrl("./" + (hdFont === undefined || hdFont ? termFontHd : termFont));

    // Set up the file system from the list of files given.
    const files = props.files || {};
    for (const fileName in files) {
      if (!Object.prototype.hasOwnProperty.call(files, fileName)) continue;

      const [dirName,] = splitName(fileName);
      if (dirName) {
        const dir = computer.createDirectory(dirName);
        if (dir.value === null) throw new Error(dir.error);
      }

      const contents = files[fileName];
      const file = computer.createFile(fileName);
      if (file.value === null) throw new Error(file.error);

      const written = file.value.setContents(contents);
      if (written.value === null) throw new Error(written.error);
    }

    const peripherals = props.peripherals;
    if (peripherals) {
      for (const side in peripherals) {
        if (!Object.prototype.hasOwnProperty.call(peripherals, side)) continue;
        const kind = peripherals[side as Side];
        if (kind !== null) computer.setPeripheral(side as Side, kind);
      }
    }

    this.setState({
      on: false,
      label: null,
      font, terminal, terminalChanged, computer
    });
  }

  public componentDidMount(): void {
    this.state.computer.start(() => emptyGroup, this.props);
  }

  public computerDidUnmount(): void {
    this.state.computer.dispose();
  }

  public render(_: MainProps, { font, computer, terminal, terminalChanged, label, on }: MainState) {
    return <Terminal terminal={terminal} changed={terminalChanged} focused={true} computer={computer}
      font={font}
      id={0} label={label} on={on} />;
  }
}

const exported = (element: HTMLElement, options?: MainProps): Promise<ComputerAccess> => {
  return new Promise((resolve, _) =>
    render(<Computer resolve={resolve} {...(options || {})} />, element));
};

export default exported;

exported.h = h;
exported.Component = Component;
exported.render = render;
exported.Computer = Computer;
