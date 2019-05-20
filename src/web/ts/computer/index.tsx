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

  dragging: boolean,
};

const createZip = async (computer: ComputerAccess) => {
  const zip = await newZip();

  const queue = [""];
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

    // Create a startup file if specified.
    for (const field of window.location.search.substring(1).split("&")) {
      const [key, value] = field.split("=");
      if (key === "startup") {
        const entry = computer.createFile("startup.lua");
        if (!entry.value) continue;

        let contents: string;
        try {
          contents = atob(value);
        } catch (e) {
          console.error(e);
          break;
        }

        contents = contents
          .replace(/(\\|\n|\")/g, "\\$1")
          .replace("\r", "\\r").replace("\0", "\\0");

        // We create a startup script which deletes itself, and then runs the
        // original program. This allows it to be invisible, even in the event
        // of syntax errors.
        entry.value.setContents(`
fs.delete("startup.lua")
local fn, err = load("${contents}", "@startup.lua", nil, _ENV)
if not fn then error(err, 0) end
fn()`);
        break;
      }
    }

    this.state = {
      terminal, terminalChanged, computer,
      activeFile: null,

      id: 0, on: false, label: computer.getLabel(),

      dragging: false,
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
    { id, label, on, activeFile: activeFile, dragging }: Readonly<ComputerState>,
  ): boolean {
    return focused !== this.props.focused || settings !== this.props.settings ||
      id !== this.state.id || label !== this.state.label || on !== this.state.on ||
      activeFile !== this.state.activeFile || dragging !== this.state.dragging;
  }

  public render(
    { settings, focused }: ComputerProps,
    { terminal, terminalChanged, computer, activeFile, id, label, on, dragging }: ComputerState,
  ) {
    return <div class="computer-view">
      <div class="computer-split">
        <div class={`file-list ${dragging ? "dragging" : ""}`}
          onDragOver={this.startDrag} onDragLeave={this.stopDrag} onDrop={this.dropFile}>
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

          <div class="file-drop-marker">
            <span>Upload to your computer!</span>
          </div>
        </div>
        {activeFile == null
          ? <Terminal terminal={terminal} changed={terminalChanged} focused={focused}
            computer={computer} font={settings.terminalFont} id={id} label={label} on={on} />
          : <Editor model={activeFile.model} settings={settings} focused={focused}
            doSave={contents => activeFile.file.setContents(contents)} />}
      </div>
    </div>;
  }

  private addOneFile(name: string, contents: ArrayBuffer) {
    const index = name.lastIndexOf(".");
    const prefix = index > 0 ? name.substring(0, index) : name;
    const suffix = index > 0 ? name.substring(index) : "";

    // Add a number until we find a unique file. Or just give up at 100.
    const computer = this.state.computer;
    for (let i = 0; i < 100; i++) {
      const uniqueName = i === 0 ? name : `${prefix}.${i}${suffix}`;
      if (computer.getEntry(uniqueName)) continue;

      const result = this.state.computer.createFile(uniqueName);
      if (!result.value) continue;

      result.value.setContents(contents);
      break;
    }
  }

  private addFile(file: File): void {
    if (file.name.endsWith(".zip")) {
      // We attempt to unpack a zip file into a folder with the same name.
      newZip()
        .then(async zip => {
          await zip.loadAsync(file);

          const computer = this.state.computer;

          const zipName = file.name.substring(0, file.name.length - 4);
          let dirName;
          for (let i = 0; i < 100; i++) {
            dirName = i === 0 ? zipName : `${zipName}.${i}`;
            if (computer.getEntry(dirName)) continue;

            const result = this.state.computer.createDirectory(dirName);
            if (result.value) break;
          }

          for (const fileName in zip.files) {
            if (!zip.files.hasOwnProperty(fileName)) continue;

            let fullName = `${dirName}/${fileName}`;
            const entry = zip.files[fileName];
            if (entry.dir) {
              if (fullName.endsWith("/")) fullName = fullName.substring(0, fullName.length - 1);
              computer.createDirectory(fullName);
            } else {
              this.addOneFile(fullName, await entry.async("arraybuffer"));
            }
          }
        })
        .catch(e => console.error(e));
    } else {
      const reader = new FileReader();
      reader.onload = () => this.addOneFile(file.name, reader.result as ArrayBuffer);
      reader.readAsArrayBuffer(file);
    }
  }

  private openFile = (path: string, file: FileSystemEntry) => {
    if (file.isDirectory()) return;

    const oldActive = this.state.activeFile;

    this.setState({
      activeFile: {
        file, path,
        model: createModel(file.getStringContents(), path),
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

  private startDrag = (e: DragEvent) => {
    e.preventDefault();
    if (!this.state.dragging) this.setState({ dragging: true });
  }

  private stopDrag = () => {
    this.setState({ dragging: false });
  }

  private dropFile = (e: DragEvent) => {
    e.preventDefault();
    this.setState({ dragging: false });

    if (!e.dataTransfer) return;

    if (e.dataTransfer.items) {
      const items = e.dataTransfer.items;
      // tslint:disable-next-line:prefer-for-of
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (item.kind === "file") this.addFile(item.getAsFile()!);
      }
    } else {
      const files = e.dataTransfer.files;
      // tslint:disable-next-line:prefer-for-of
      for (let i = 0; i < files.length; i++) this.addFile(files[i]);
    }
  }
}
