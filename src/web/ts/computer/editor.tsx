import { Component, h } from "preact";
import { Settings } from "../settings";

import * as mTypes from "../editor";

let monaco: typeof mTypes | null = null;

export type Model = {
  resolved: true,
  text: mTypes.editor.ITextModel,
  view: mTypes.editor.ICodeEditorViewState | null,
};

export type LazyModel = Model | {
  resolved: false,
  contents: string,
  name: string,
  promise: Promise<Model>,
};

let unique = 0;

const modelFactory = (m: typeof mTypes, out: {}, contents: string, name: string): Model => {
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

  const resolved = modelFactory(monaco!, model, model.contents, model.name);

  const old: { contents?: string, mode?: string } = model;
  delete old.contents;
  delete old.mode;

  return resolved;
};

export const createModel = (contents: string, name: string): LazyModel => {
  if (monaco) return modelFactory(monaco, {}, contents, name);

  const model: LazyModel = {
    resolved: false, contents, name,
    promise: import("../editor").then(m => {
      monaco = m;
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

export default class Editor extends Component<EditorProps, {}> {
  private editor?: mTypes.editor.IStandaloneCodeEditor;
  private editorPromise?: Promise<void>;

  public componentDidMount() {
    this.setupEditor();
  }

  private setupEditor() {
    if (!monaco) {
      const promise = this.editorPromise = import("../editor")
        .then(x => {
          monaco = x;
          if (this.editorPromise !== promise) return;
          this.setupEditor();
        })
        .catch(err => console.error(err));
        // TODO: Actually decent handling.
      return;
    }

    this.editorPromise = undefined;

    // Clear the body of any elements
    const base = this.base!;
    while (base.firstChild) base.firstChild.remove();

    this.editor = monaco.editor.create(this.base!, {
      roundedSelection: false,
      autoIndent: true,
    });

    this.editor.addAction({
      id: "save",
      label: "Save",
      keybindings: [
        monaco.KeyMod.CtrlCmd | monaco.KeyCode.KEY_S,
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

  public componentWillUnmount() {
    if (!this.editor) return;

    // Save the view state back to the model
    forceModel(this.props.model).view = this.editor.saveViewState();
    // And save the file
    this.props.doSave(this.editor.getValue());

    // We set a new session to prevent destroying it when losing the editor
    this.editor.dispose();
  }

  public componentWillUpdate() {
    if (!this.editor) return;

    // Save the view state back to the model
    forceModel(this.props.model).view = this.editor.saveViewState();
  }

  public componentDidUpdate() {
    if (!this.editor) return;
    this.syncOptions();
  }

  private syncOptions() {
    if (!this.editor) return;

    // No view patterns, alas.
    const settings = this.props.settings;
    const model = forceModel(this.props.model);

    this.editor.setModel(model.text);
    if (model.view) this.editor.restoreViewState(model.view);

    this.editor.updateOptions({
      renderWhitespace: settings.showInvisible ? "boundary" : "none",
    });

    if (monaco !== null) {
      monaco.editor.setTheme(settings.darkMode ? "vs-dark" : "vs");
    }

    // TODO: Tab size

    if (this.props.focused) this.editor.focus();
  }

  public render() {
    return <div class="editor-view">
      {monaco ? undefined : <div class="editor-placeholder">Loading...</div>}
    </div>;
  }
}
