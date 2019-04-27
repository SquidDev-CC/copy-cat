import { Component, h } from "preact";
import { start } from "../java";
import { Settings } from "../settings";
import { Terminal } from "../terminal/component";
import { TerminalData } from "../terminal/data";
import { ComputerAccess } from "./access";
import { Semaphore } from "./actions";

export type ComputerProps = {
  focused: boolean,
  settings: Settings,
};

type ComputerState = {
  terminal: TerminalData,
  terminalChanged: Semaphore,
  computer: ComputerAccess,

  id?: number,
  label?: string,
  on: boolean,
};

export class Computer extends Component<ComputerProps, ComputerState> {
  public constructor(props: ComputerProps, context: any) {
    super(props, context);

    const terminal = new TerminalData();
    const terminalChanged = new Semaphore();
    this.state = {
      terminal, terminalChanged,
      id: 0, on: false,
      computer: new ComputerAccess(
        terminal, terminalChanged,
        (label, on) => this.setState({ label: label ? label : undefined, on }),
      ),
    };
  }

  public componentDidMount() {
    start(this.state.computer);
  }

  public componentWillUnmount() {
    this.state.computer.shutdown();
  }

  public shouldComponentUpdate(
    { focused, settings }: Readonly<ComputerProps>,
    { id, label, on }: Readonly<ComputerState>,
  ): boolean {
    return focused !== this.props.focused || settings !== this.props.settings ||
      id !== this.state.id || label !== this.state.label || on !== this.state.on;
  }

  public render(
    { settings, focused }: ComputerProps,
    { terminal, terminalChanged, computer, id, label, on }: ComputerState,
  ) {
    console.log(`Render computer with id=${id}, label=${label}`);
    return <div class="computer-view">
        <div class="computer-split">
          <Terminal terminal={terminal} changed={terminalChanged} focused={focused}
            computer={computer} font={settings.terminalFont} id={id} label={label} on={on}/>
        </div>
      </div>;
  }
}
