import { Component, ComponentChild, h } from "preact";
import type { Settings } from "../settings";
import type * as monaco from "../editor";
import { editorPlaceholder, editorView } from "../styles.css";
import clsx from "clsx";

let monacoVal: typeof monaco | null = null;

export type Model = {
  resolved: true,
  text: monaco.editor.ITextModel,
  view: monaco.editor.ICodeEditorViewState | null,
};

export type LazyModel = Model | {
  resolved: false,
  contents: string,
  name: string,
  promise: Promise<Model>,
};

let unique = 0;

const modelFactory = (m: typeof monaco, out: unknown, contents: string, name: string): Model => {
  unique++; // We keep a unique id to ensure the Uri is not repeated.
  const mode = name.endsWith(".lua") ? "luax" : undefined;
  const text = m.editor.createModel(contents, mode, m.Uri.file(`f${unique.toString(16)}/${name}`));

  text.updateOptions({ trimAutoWhitespace: true });
  text.detectIndentation(true, 2);

  const model = out as Model;
  model.resolved = true;
  model.text = text;
  model.view = null;
  return model;
};

const forceModel = (model: LazyModel): Model => {
  if (model.resolved) return model;

  const resolved = modelFactory(monacoVal!, model, model.contents, model.name);

  const old: { contents?: string, mode?: string } = model;
  delete old.contents;
  delete old.mode;

  return resolved;
};

export const createModel = (contents: string, name: string): LazyModel => {
  if (monacoVal) return modelFactory(monacoVal, {}, contents, name);

  const model: LazyModel = {
    resolved: false, contents, name,
    promise: import("../editor").then(m => {
      monacoVal = m;
      return forceModel(model);
    }),
  };
  return model;
};

export type EditorProps = {
  // From the main state
  settings: Settings,
  focused: boolean,

  // From the computer session
  model: LazyModel,

  // A set of actions to call
  doSave: (contents: string) => void,
};

export default class Editor extends Component<EditorProps, unknown> {
  private editor?: monaco.editor.IStandaloneCodeEditor;
  private editorPromise?: Promise<void>;

  public componentDidMount(): void {
    window.addEventListener("resize", this.onResize);

    this.setupEditor();
  }

  private setupEditor() {
    if (!monacoVal) {
      const promise = this.editorPromise = import("../editor")
        .then(x => {
          monacoVal = x;
          if (this.editorPromise !== promise) return;
          this.setupEditor();
        })
        .catch(err => console.error(err));
        // TODO: Actually decent handling.
      return;
    }

    this.editorPromise = undefined;

    // Clear the body of any elements
    const base = this.base as HTMLElement;
    while (base.firstChild) base.firstChild.remove();

    this.editor = monacoVal.editor.create(base, {
      roundedSelection: false,
      autoIndent: "full",
    });

    this.editor.addAction({
      id: "save",
      label: "Save",
      keybindings: [
        monacoVal.KeyMod.CtrlCmd | monacoVal.KeyCode.KEY_S,
      ],
      contextMenuGroupId: "file",
      contextMenuOrder: 1.5,
      run: editor => {
        if (this.props.settings.trimWhitespace) {
          editor.getAction("editor.action.trimTrailingWhitespace").run();
        }

        this.props.doSave(editor.getValue());
      },
    });

    this.syncOptions();
  }

  public componentWillUnmount(): void {
    window.removeEventListener("resize", this.onResize);

    if (!this.editor) return;

    // Save the view state back to the model
    forceModel(this.props.model).view = this.editor.saveViewState();
    // And save the file
    this.props.doSave(this.editor.getValue());

    // We set a new session to prevent destroying it when losing the editor
    this.editor.dispose();
  }

  public componentWillUpdate(): void {
    if (!this.editor) return;

    // Save the view state back to the model
    forceModel(this.props.model).view = this.editor.saveViewState();
  }

  public componentDidUpdate(): void {
    if (!this.editor) return;
    this.syncOptions();
  }

  private syncOptions(): void {
    if (!this.editor) return;

    // No view patterns, alas.
    const settings = this.props.settings;
    const model = forceModel(this.props.model);

    this.editor.setModel(model.text);
    if (model.view) this.editor.restoreViewState(model.view);

    this.editor.updateOptions({
      renderWhitespace: settings.showInvisible ? "boundary" : "none",
    });

    if (monacoVal !== null) {
      monacoVal.editor.setTheme(settings.darkMode ? "vs-dark" : "vs");
    }

    // TODO: Tab size

    if (this.props.focused) this.editor.focus();
  }
  public render(): ComponentChild {
    return <div class={clsx(editorView)}>
      {monacoVal ? 
        undefined : 
        <div class={clsx(editorPlaceholder)}>Loading...</div>}
    </div>;
  }

  /**
   * When the window resizes, we also need to update the editor's dimensions.
   */
  private onResize = () => this.editor?.layout();
}
