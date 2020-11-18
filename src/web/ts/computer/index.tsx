import { Semaphore, Terminal, TerminalData, save as saveBlob } from "@squid-dev/cc-web-term";
import type JSZip from "jszip";
import { Component, VNode, h } from "preact";
import newZip from "../files/zip";
import { Download } from "../font";
import type { ConfigFactory } from "../java";
import type { Settings } from "../settings";
import { ComputerAccess, FileSystemEntry, joinName } from "./access";
import Editor, { LazyModel, createModel } from "./editor";
import { FileTree } from "./files";
import { StoragePersistence, VoidPersistence } from "./persist";
import {
  actionButton, active, computerSplit, computerView, dragging as draggingClass, fileComputer, fileComputerDark,
  fileComputerActions, fileComputerControl, fileComputerControlDark, fileDropMarker, fileList, fileListDark, 
  terminalView, terminalViewDark 
} from "../styles.css";
import clsx from "clsx";

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
  openFiles: Map<FileSystemEntry, { model: LazyModel, monitor: () => void }>,

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
      zip.file(path, entry.getContents().buffer);
    }
  }

  return zip.generateAsync({ type: "blob" });
};

/**
 * Determine if this is a simple archive - namely every child within it occurs
 * within a directory with the same name as the archive.
 * @param zip The zip to check.
 * @param name The zip file's name, without the `.zip` extension.
 * @return If this is a simple archive.
 */
const isSimpleZip = (zip: JSZip, name: string) => {
  for (const fileName in zip.files) {
    if (!Object.prototype.hasOwnProperty.call(zip.files, fileName)) continue;

    // Require every child to be in the ${name} directory.
    if (!fileName.startsWith(name + "/")) return false;
  }

  return true;
};

export class Computer extends Component<ComputerProps, ComputerState> {
  public constructor(props: ComputerProps, context: unknown) {
    super(props, context);
    const terminal = new TerminalData();
    const terminalChanged = new Semaphore();
    const computer = new ComputerAccess(
      __storageBackend__ === "none" ? new VoidPersistence() : new StoragePersistence(0),
      terminal, terminalChanged,
      (label, on) => this.setState({ label, on }),
    );

    // Create a startup file if specified.
    for (const field of window.location.search.substring(1).split("&")) {
      const [key, value] = field.split("=");
      if (key !== "startup") continue;

      let contents: string;
      try {
        contents = atob(value);
      } catch (e) {
        console.error(e);
        break;
      }

      contents = contents
        .replace(/(\\|\n|")/g, "\\$1")
        .replace("\r", "\\r").replace("\0", "\\0");

      // We create a startup script which deletes itself, and then runs the
      // original program. This allows it to be invisible, even in the event
      // of syntax errors.
      computer.createFile("startup.lua").value?.setContents(`
fs.delete("startup.lua")
local fn, err = load("${contents}", "@startup.lua", nil, _ENV)
if not fn then error(err, 0) end
fn()`);
    }

    this.setState({
      terminal, terminalChanged, computer,
      activeFile: null, openFiles: new Map(),

      id: 0, on: false, label: computer.getLabel(),

      dragging: false,
    });
  }

  public componentDidMount(): void {
    this.state.computer.start(this.props.computerSettings);
  }

  public componentWillUnmount(): void {
    this.state.computer.shutdown();
    for (const [file, { model, monitor }] of this.state.openFiles) {
      if (model.resolved) model.text.dispose();
      file.getSemaphore().detach(monitor);
    }
  }

  public shouldComponentUpdate(
    { focused, settings }: Readonly<ComputerProps>,
    { id, label, on, activeFile, dragging }: Readonly<ComputerState>,
  ): boolean {
    return focused !== this.props.focused || settings !== this.props.settings ||
      id !== this.state.id || label !== this.state.label || on !== this.state.on ||
      activeFile !== this.state.activeFile || dragging !== this.state.dragging;
  }

  public render(
    { settings, focused }: ComputerProps,
    { terminal, terminalChanged, computer, activeFile, id, label, on, dragging }: ComputerState,
  ): VNode<unknown> {
    const {darkMode} = settings;
    return <div class={computerView}>
      <div class={computerSplit}>
        <div class={clsx(fileList, {[draggingClass]: dragging, [fileListDark]: darkMode})}
          onDragOver={this.startDrag} onDragLeave={this.stopDrag} onDrop={this.dropFile}>
          <div class={clsx(fileComputerControl, {[fileComputerControlDark]: darkMode})}>
            <div class={clsx(fileComputer, {[active]: activeFile, [fileComputerDark]: darkMode})} onClick={this.openComputer}>
              {id ? `Computer #${id}` : "Computer"}
            </div>
            <div class={fileComputerActions}>
              <button class={actionButton} type="button" onClick={this.saveZip}
                title="Download all files as a zip">
                <Download />
              </button>
            </div>
          </div>

          <FileTree computer={computer} settings={settings} entry={computer.getEntry("")!} path=""
            opened={activeFile === null ? null : activeFile.path} open={this.openFile} />

          <div class={fileDropMarker}>
            <span>Upload to your computer!</span>
          </div>
        </div>
        {activeFile == null
          ? <div class={clsx(terminalView, {[terminalViewDark]: darkMode})}>
            <Terminal terminal={terminal} changed={terminalChanged} focused={focused} computer={computer}
              font={settings.terminalFont}
              id={id} label={label} on={on} />
          </div>
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
      return;
    }

    console.warn(`Cannot write contents of ${name}.`);
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

          const offset = isSimpleZip(zip, zipName) ? zipName.length + 1 : 0;
          for (const fileName in zip.files) {
            if (!Object.prototype.hasOwnProperty.call(zip.files, fileName) || fileName.length === offset) continue;

            let fullName = `${dirName}/${fileName.substr(offset)}`;
            const entry = zip.files[fileName];
            if (entry.dir) {
              if (fullName.endsWith("/")) fullName = fullName.substring(0, fullName.length - 1);
              if (!computer.createDirectory(fullName)) console.warn(`Cannot create directory ${fullName}.`);

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

    let entry = this.state.openFiles.get(file);
    if (typeof entry === "undefined") {
      const model = createModel(file.getStringContents(), path);

      const monitor = () => {
        if (!file.doesExist()) {
          // If the file has been deleted, dispose the model and remove from the cache.
          if (model.resolved) model.text.dispose();
          file.getSemaphore().detach(monitor);
          this.state.openFiles.delete(file);
        }
      };

      entry = { model, monitor };
      this.state.openFiles.set(file, entry);
      file.getSemaphore().attach(monitor);
    } else {
      // Update the contents from the file. Note, this may mess up the view a little - we'll have to cope.
      const model = entry.model;
      const contents = file.getStringContents();
      if (model.resolved) {
        if (contents !== model.text.getValue()) model.text.setValue(contents);
      } else {
        model.contents = contents;
      }
    }

    this.setState({ activeFile: { file, path, model: entry.model } });
  }

  private openComputer = () => {
    this.setState({ activeFile: null });
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
