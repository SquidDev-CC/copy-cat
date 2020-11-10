import { Component, JSX, h, render } from "preact";
import { Computer } from "./computer";
import { Cog, Info } from "./font";
import { About } from "./screens";
import { ConfigGroup, SettingStore, Settings } from "./settings";
import { actionButton, dialogueOverlay, infoButtons } from "./styles.css";
import termFont from "@squid-dev/cc-web-term/assets/term_font.png";
import termFontHd from "@squid-dev/cc-web-term/assets/term_font_hd.png";

type MainState = {
  settings: Settings,
  settingStorage: SettingStore,
  configGroups: ConfigGroup[],

  currentVDom: (state: MainState) => JSX.Element,
  dialogue?: (state: MainState) => JSX.Element,
};

class Main extends Component<unknown, MainState> {
  public constructor(props: unknown, context: unknown) {
    super(props, context);
  }

  public componentWillMount() {
    const settingStorage = new SettingStore();

    const configEditor = new ConfigGroup("Editor", "Configure the built-in eidtor", settingStorage);
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
      "Only the editor currently, sorry.",
      x => this.setState(s => ({ settings: { ...s.settings, darkMode: x } })),
    );

    const fonts: { [font: string]: string } = {
      "standard": termFont,
      "hd": termFontHd,

      // Add some fallbacks for previous versions.
      [termFontHd]: termFontHd, "term_font_hd.png": termFontHd,
      [termFont]: termFont, "term_font.png": termFont
    };
    configTerminal.addOption("terminal.font", "Font", state.settings.terminalFont,
      [
        { key: "standard", value: "Standard font" },
        { key: "hd", value: "High-definition font" },
      ], "Which font the we should use within the terminal",
      x => this.setState(s => ({ settings: { ...s.settings, terminalFont: fonts[x] || termFontHd } })),
    );
  }

  public shouldComponentUpdate(_: unknown, newState: MainState) {
    return this.state.currentVDom !== newState.currentVDom ||
      this.state.dialogue !== newState.dialogue ||
      this.state.settings !== newState.settings;
  }

  public render(_: unknown, state: MainState) {
    return <div class="container">
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

  private openSettings = () => {
    this.setState({
      dialogue: ({ settingStorage, configGroups }: MainState) =>
        <Settings store={settingStorage} configGroups={configGroups} />,
    });
  }

  private closeDialogueClick = (e: MouseEvent) => {
    if (e.target === e.currentTarget) this.setState({ dialogue: undefined });
  }

  private computerVDom = ({ settings, dialogue }: MainState) => {
    return <Computer settings={settings} focused={dialogue === undefined} computerSettings={this.configFactory} />;
  }

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
  }
}

export default (): void => {
  // Start the window!
  const page = document.getElementById("page") as HTMLElement;
  render(<Main />, page, page.lastElementChild || undefined);
};
