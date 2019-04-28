import { Component, VNode, h } from "preact";
import { ComputerAccess, FileSystemEntry, joinName } from "./access";

type FileEntryProperties = {
  computer: ComputerAccess;
  entry: FileSystemEntry;
  name: string;
  path: string;
  depth: number;
};

type FileEntryState = {
  expanded?: boolean;
};

const getExtension = (name: string, directory: boolean, expanded: boolean) => {
  if (directory) return expanded ? "down-open" : "right-open";
  if (name.endsWith(".lua")) return "lua";
  return "doc-text";
};

class FileEntry extends Component<FileEntryProperties, FileEntryState> {
  public shouldComponentUpdate({ entry, depth }: FileEntryProperties, { expanded }: FileEntryState) {
    return entry !== this.props.entry || depth !== this.props.depth || expanded !== this.state.expanded;
  }

  public render({ computer, entry, name, path, depth }: FileEntryProperties, { expanded }: FileEntryState) {
    return <li class="file-entry">
      <div class="file-entry-head" style={`padding-left: ${depth}em`}
        onClick={entry.isDirectory() ? () => this.setState({ expanded: !expanded}) : undefined}>
        <span class={`file-entry-icon icon icon-${getExtension(name, entry.isDirectory(), expanded || false)}`}></span>
        <span class="file-entry-name">{name}</span>
      </div>
      {expanded ? <FileTree computer={computer} entry={entry} path={path} depth={depth} /> : null}
    </li>;
  }
}

export type FileListProperties = {
  computer: ComputerAccess;
  entry: FileSystemEntry;
  path: string;
  depth?: number;
};

type FileListState = {
  children?: string[];
};

type ChildNode = { name: string, dir: boolean, node: VNode<any> };

export class FileTree extends Component<FileListProperties, FileListState> {
  public shouldComponentUpdate({ entry, depth }: FileListProperties, { children }: FileListState) {
    return entry !== this.props.entry || depth !== this.props.depth || children !== this.state.children;
  }

  public render({ computer, entry, path, depth }: FileListProperties, { children }: FileListState) {
    // Gather all children, and then sort them.
    const entries: ChildNode[] = (children || entry.getChildren()).map(childName => {
      const childPath = joinName(path, childName);
      const child = computer.getEntry(childPath)!;
      return {
        name: childName, dir: child.isDirectory(),
        node: <FileEntry computer={computer} entry={child} path={childPath} name={childName}
          depth={depth === undefined ? 0 : depth + 1}/>,
      };
    });

    entries.sort((a, b) => {
      if (a.dir !== b.dir) return a.dir ? -1 : 1;
      return a.name < b.name ? -1 : 1;
    });

    return <ul class="file-tree">{entries.map(x => x.node)}</ul>;
  }

  public componentDidMount() {
    this.props.entry.getSemaphore().attach(this.listener);
  }

  public componentWillUnmount() {
    this.props.entry.getSemaphore().detach(this.listener);
  }

  public componentDidUpdate({ entry }: FileListProperties) {
    if (this.props.entry !== entry) {
      this.props.entry.getSemaphore().detach(this.listener);
      entry.getSemaphore().attach(this.listener);
    }
  }

  private listener = () => this.setState({ children: this.props.entry.getChildren() });
}
