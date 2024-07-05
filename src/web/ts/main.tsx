import termFont from "@squid-dev/cc-web-term/assets/term_font.png";
import termFontHd from "@squid-dev/cc-web-term/assets/term_font_hd.png";
import type * as monaco from "monaco-editor";
import { Component, type JSX, type VNode, render } from "preact";
import { Computer } from "./computer";
import { Cog, Info } from "./font";
import { About } from "./screens";
import { ConfigGroup, SettingStore, Settings } from "./settings";
import { actionButton, darkTheme, dialogueOverlay, infoButtons, lightTheme } from "./styles.css";
import { classNames } from "./utils";

type MainState = {
  settings: Settings,
  settingStorage: SettingStore,
  configGroups: Array<ConfigGroup>,

  currentVDom: (state: MainState) => JSX.Element,
  dialogue?: (state: MainState) => JSX.Element,
};

class Main extends Component<unknown, MainState> {
  public constructor(props: unknown, context: unknown) {
    super(props, context);
  }

  public componentWillMount(): void {
    const settingStorage = new SettingStore();

    const configEditor = new ConfigGroup("Editor", "Configure the built-in editor", settingStorage);
    const configTerminal = new ConfigGroup("Terminal", "Configure the terminal display", settingStorage);
    const configGroups = [configEditor, configTerminal];

    const state: MainState = {
      settingStorage, configGroups,
      settings: {
        showInvisible: true, trimWhitespace: true, darkMode: false,
        terminalFont: termFont,
      },
      currentVDom: this.computerVDom,
    };
    this.setState(state);

    // Declare our settings
    configEditor.addBoolean("editor.invisible", "Show invisible", state.settings.showInvisible,
      "Show invisible characters, such as spaces and tabs.",
      x => this.setState(s => ({ settings: { ...s.settings, showInvisible: x } })),
    );

    configEditor.addBoolean("editor.trim_whitespace", "Trim whitespace", state.settings.trimWhitespace,
      "Trim whitespace from files when saving.",
      x => this.setState(s => ({ settings: { ...s.settings, trimWhitespace: x } })),
    );

    configEditor.addBoolean("editor.dark", "Dark mode", state.settings.darkMode,
      "Enables dark mode.",
      x => {
        this.setState(s => ({ settings: { ...s.settings, darkMode: x } }));
      },
    );

    const fonts: Record<string, string> = {
      "standard": termFont,
      "hd": termFontHd,

      // Add some fallbacks for previous versions.
      [termFontHd]: termFontHd, "term_font_hd.png": termFontHd,
      [termFont]: termFont, "term_font.png": termFont
    };
    configTerminal.addOption("terminal.font", "Font", "standard",
      [
        { key: "standard", value: "Standard font" },
        { key: "hd", value: "High-definition font" },
      ], "Which font the we should use within the terminal",
      x => this.setState(s => ({ settings: { ...s.settings, terminalFont: fonts[x] || termFontHd } })),
    );
  }

  public shouldComponentUpdate(_: unknown, newState: MainState): boolean {
    return this.state.currentVDom !== newState.currentVDom ||
      this.state.dialogue !== newState.dialogue ||
      this.state.settings !== newState.settings;
  }

  public render(_: unknown, state: MainState): VNode {
    return <div class={classNames("container", state.settings.darkMode ? darkTheme : lightTheme)}>
      {state.currentVDom(state)}
      <div class={infoButtons}>
        <button class={actionButton} title="Configure how the emulator behaves" type="button"
          onClick={this.openSettings}>
          <Cog />
        </button>
        <button class={actionButton} title="Find out more about the emulator" type="button"
          onClick={() => this.setState({ dialogue: () => <About /> })}>
          <Info />
        </button>
      </div>
      {
        state.dialogue ?
          <div class={dialogueOverlay} onClick={this.closeDialogueClick}>
            {state.dialogue(state)}
          </div> : ""
      }
    </div>;
  }

  private openSettings = (): void => {
    this.setState({
      dialogue: ({ settingStorage, configGroups }: MainState) =>
        <Settings store={settingStorage} configGroups={configGroups} />,
    });
  };

  private closeDialogueClick = (e: MouseEvent): void => {
    if (e.target === e.currentTarget) this.setState({ dialogue: undefined });
  };

  private computerVDom = ({ settings, dialogue }: MainState): VNode => {
    return <Computer settings={settings} focused={dialogue === undefined} computerSettings={this.configFactory} />;
  };

  private configFactory = (name: string, description: string | null): ConfigGroup => {
    const existing = this.state.configGroups.find(x => x.name === name);
    if (existing) {
      if (existing.description !== description) {
        console.warn(`Different descriptions for ${name} ("${description}" and "${existing.description}")`);
      }

      return existing;
    }

    const group = new ConfigGroup(name, description, this.state.settingStorage);
    this.setState(s => ({ configGroups: [...s.configGroups, group] }));
    return group;
  };
}

{
  requirejs.config({ paths: { vs: "__monaco__/min/vs" } });

  (window as any).MonacoEnvironment = { // eslint-disable-line @typescript-eslint/no-explicit-any, @typescript-eslint/no-unsafe-member-access
    getWorkerUrl: (_workerId: string, _label: string) =>
      `data:text/javascript;charset=utf-8,${encodeURIComponent(`
      self.MonacoEnvironment = {
        baseUrl: "__monaco__/min/"
      };
      importScripts("__monaco__/min/vs/base/worker/workerMain.js");
    `)}`,
  } as monaco.Environment;

  // Start the window!
  const page = document.getElementById("page")!;
  render(<Main />, page, page.lastElementChild ?? undefined);
}
