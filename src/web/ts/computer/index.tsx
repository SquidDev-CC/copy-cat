import { Component, h } from "preact";
import saveBlob from "../files/save";
import newZip from "../files/zip";
import { Download } from "../font";
import { ConfigFactory, start } from "../java";
import { Settings } from "../settings";
import { Terminal } from "../terminal/component";
import { TerminalData } from "../terminal/data";
import { ComputerAccess, FileSystemEntry, joinName } from "./access";
import { Semaphore } from "./actions";
import Editor, { LazyModel, createModel } from "./editor";
import { FileTree } from "./files";

export type ComputerProps = {
  focused: boolean,
  settings: Settings,
  computerSettings: ConfigFactory,
};

type EditedFile = {
  path: string,
  file: FileSystemEntry,
  model: LazyModel,
};

type ComputerState = {
  terminal: TerminalData,
  terminalChanged: Semaphore,
  computer: ComputerAccess,

  activeFile: EditedFile | null,

  id: number,
  label: string | null,
  on: boolean,
};

const createZip = async (computer: ComputerAccess) => {
  const zip = await newZip();

  const queue = [ "" ];
  while (true) {
    const path = queue.pop();
    if (path === undefined) break;
    const entry = computer.getEntry(path);
    if (!entry) continue;

    if (entry.isDirectory()) {
      if (path !== "") zip.folder(path);
      for (const child of entry.getChildren()) queue.push(joinName(path, child));
    } else {
      zip.file(path, entry.getContents());
    }
  }

  return zip.generateAsync({ type: "blob" });
};

export class Computer extends Component<ComputerProps, ComputerState> {
  public constructor(props: ComputerProps, context: any) {
    super(props, context);

    const terminal = new TerminalData();
    const terminalChanged = new Semaphore();
    const computer = new ComputerAccess(
      terminal, terminalChanged,
      (label, on) => this.setState({ label, on }),
    );
    this.state = {
      terminal, terminalChanged, computer,
      activeFile: null,

      id: 0, on: false, label: computer.getLabel(),
    };
  }

  public componentDidMount() {
    start(this.state.computer, this.props.computerSettings);
  }

  public componentWillUnmount() {
    this.state.computer.shutdown();
  }

  public shouldComponentUpdate(
    { focused, settings }: Readonly<ComputerProps>,
    { id, label, on, activeFile: activeFile }: Readonly<ComputerState>,
  ): boolean {
    return focused !== this.props.focused || settings !== this.props.settings ||
      id !== this.state.id || label !== this.state.label || on !== this.state.on ||
      activeFile !== this.state.activeFile;
  }

  public render(
    { settings, focused }: ComputerProps,
    { terminal, terminalChanged, computer, activeFile, id, label, on }: ComputerState,
  ) {
    return <div class="computer-view">
      <div class="computer-split">
        <div class="file-list">
          <div class="file-computer-control">
            <div class={`file-computer ${activeFile == null ? "active" : ""}`} onClick={this.openComputer}>
              {id ? `Computer #${id}` : "Computer"}
            </div>
            <div class="file-computer-actions">
              <button class="action-button" type="button" onClick={this.saveZip}
                title="Download all files as a zip">
                <Download />
              </button>
            </div>
          </div>

          <FileTree computer={computer} entry={computer.getEntry("")!} path=""
            opened={activeFile === null ? null : activeFile.path} open={this.openFile} />
        </div>
        {activeFile == null
          ? <Terminal terminal={terminal} changed={terminalChanged} focused={focused}
            computer={computer} font={settings.terminalFont} id={id} label={label} on={on} />
          : <Editor model={activeFile.model} settings={settings} focused={focused}
            doSave={contents => activeFile.file.setContents(contents)} />}
      </div>
    </div>;
  }

  private openFile = (path: string, file: FileSystemEntry) => {
    if (file.isDirectory()) return;

    const oldActive = this.state.activeFile;
    this.setState({
      activeFile: {
        file, path,
        model: createModel(file.getContents(), path.endsWith(".lua") ? "luax" : undefined),
      },
    }, () => {
      if (oldActive && oldActive.model.resolved) oldActive.model.text.dispose();
    });
  }

  private openComputer = () => {
    const oldActive = this.state.activeFile;
    this.setState({ activeFile: null }, () => {
      if (oldActive && oldActive.model.resolved) oldActive.model.text.dispose();
    });
  }

  private saveZip = (e: Event) => {
    e.preventDefault();
    e.stopPropagation();

    createZip(this.state.computer)
      .then(x => saveBlob("computer", "zip", x))
      .catch(err => console.error(err));
  }
}
