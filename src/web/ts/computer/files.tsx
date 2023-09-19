import { Component, type ComponentChild, type VNode, h } from "preact";
import { active, fileEntryHead, fileEntryIcon, fileEntryName, fileTree } from "../styles.css";
import { ComputerAccess, FileSystemEntry, joinName } from "./access";
import { DownOpen, Lua, RightOpen, Text } from "../font";

export type Opener = (path: string, entry: FileSystemEntry) => void;

type FileEntryProperties = {
  computer: ComputerAccess,
  entry: FileSystemEntry,
  name: string,
  path: string,
  depth: number,

  opened: string | null,
  open: Opener,
};

type FileEntryState = {
  expanded?: boolean,
};

const getIcon = (name: string, directory: boolean, expanded: boolean): VNode => {
  if (directory) return expanded ? <DownOpen /> : <RightOpen />;
  if (name.endsWith(".lua")) return <Lua />;
  return <Text />;
};

class FileEntry extends Component<FileEntryProperties, FileEntryState> {
  public shouldComponentUpdate({ entry, depth, opened }: FileEntryProperties, { expanded }: FileEntryState): boolean {
    return entry !== this.props.entry || depth !== this.props.depth || opened !== this.props.opened ||
      expanded !== this.state.expanded;
  }

  public render(
    { computer, entry, name, path, depth, opened, open }: FileEntryProperties,
    { expanded }: FileEntryState,
  ): VNode {
    return <li>
      <div class={`${fileEntryHead} ${opened === path ? active : ""}`} style={`padding-left: ${depth}em`}
        onClick={entry.isDirectory() ? () => this.setState({ expanded: !expanded}) : () => open(path, entry)}>
        <span class={fileEntryIcon}>
          {getIcon(name, entry.isDirectory(), expanded ?? false)}
        </span>
        <span class={fileEntryName}>{name}</span>
      </div>
      {expanded
        ? <FileTree computer={computer} entry={entry} path={path} depth={depth} opened={opened} open={open} />
        : null}
    </li>;
  }
}

export type FileListProperties = {
  computer: ComputerAccess,
  entry: FileSystemEntry,
  path: string,
  depth?: number,

  opened: string | null,
  open: Opener,
};

type FileListState = {
  children?: Array<string>,
};

type ChildNode = { name: string, dir: boolean, node: VNode<unknown> };

export class FileTree extends Component<FileListProperties, FileListState> {
  public shouldComponentUpdate({ entry, depth, opened }: FileListProperties, { children }: FileListState): boolean {
    return entry !== this.props.entry || depth !== this.props.depth || children !== this.state.children ||
      opened !== this.props.opened;
  }

  public render({ computer, entry, path, depth, opened, open }: FileListProperties, { children }: FileListState): ComponentChild {
    // Handle the case when we may have been deleted.
    if (!entry.doesExist()) return "";

    // Gather all children, and then sort them.
    const entries: Array<ChildNode> = (children ?? entry.getChildren()).map(childName => {
      const childPath = joinName(path, childName);
      const child = computer.getEntry(childPath)!;
      return {
        name: childName, dir: child.isDirectory(),
        node: <FileEntry computer={computer} entry={child} path={childPath} name={childName}
          depth={depth === undefined ? 0 : depth + 1} opened={opened} open={open} />,
      };
    });

    entries.sort((a, b) => {
      if (a.dir !== b.dir) return a.dir ? -1 : 1;
      return a.name < b.name ? -1 : 1;
    });

    return <ul class={fileTree}>{entries.map(x => x.node)}</ul>;
  }

  public componentDidMount(): void {
    this.props.entry.getSemaphore().attach(this.listener);
  }

  public componentWillUnmount(): void {
    this.props.entry.getSemaphore().detach(this.listener);
  }

  public componentDidUpdate({ entry }: FileListProperties): void {
    if (this.props.entry !== entry) {
      this.props.entry.getSemaphore().detach(this.listener);
      entry.getSemaphore().attach(this.listener);
    }
  }

  private listener = (): void => this.setState({ children: this.props.entry.getChildren() });
}
