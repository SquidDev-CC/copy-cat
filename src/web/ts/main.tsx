import { Component, h } from "preact";
import { Computer } from "./computer";
import { Cog, Info } from "./font";
import { Settings } from "./settings";
import { About } from "./screens";

export type MainProps = {};

type MainState = {
  settings: Settings,

  currentVDom: (state: MainState) => JSX.Element,
  dialogue?: (state: MainState) => JSX.Element,
};

export class Main extends Component<MainProps, MainState> {
  public constructor(props: MainProps, context: any) {
    super(props, context);
  }

  public componentWillMount() {
    const {  } = this.props;

    const settings: Settings = {
      showInvisible: true,
      trimWhitespace: true,

      terminalFont: "assets/term_font.png",

      darkMode: false,
      terminalBorder: false,
    };

    // Sync settings from local storage
    try {
      const settingJson = window.localStorage.getItem("settings");
      if (settingJson !== null) {
        const settingStorage = JSON.parse(settingJson);
        for (const key of Object.keys(settings)) {
          const value = settingStorage[key];
          if (value !== undefined) (settings as any)[key] = value;
        }
      }
    } catch {
      // Ignore localStorage errors - either the API isn't present, or it's disabled.
    }

    this.state = {
      settings,
      currentVDom: this.computerVDom,
    };
  }

  public shouldComponentUpdate(_props: MainProps, newState: MainState) {
    return this.state.currentVDom !== newState.currentVDom ||
      this.state.dialogue !== newState.dialogue ||
      this.state.settings !== newState.settings;
  }

  public componentDidUpdate() {
    // Sync settings back to local storage
    try {
      window.localStorage.setItem("settings", JSON.stringify(this.state.settings));
    } catch {
      // Ignore
    }
  }

  public render(_props: MainProps, state: MainState) {
    return <div class="container">
      {state.currentVDom(state)}
      <div class="info-buttons">
        <button class="action-button" title="Configure how the emulator behaves"
          onClick={this.openSettings}>
          <Cog />
        </button>
        <button class="action-button" title="Find out more about the emulator"
          onClick={() => this.setState({ dialogue: () => <About />})}>
          <Info />
        </button>
      </div>
      {
        state.dialogue ?
          <div class="dialogue-overlay" onClick={this.closeDialogueClick}>
            {state.dialogue(state)}
          </div> : ""
      }
    </div>;
  }

  private openSettings = () => {
    const update = (s: Settings) => this.setState({ settings: s });
    this.setState({ dialogue: (s: MainState) => <Settings settings={s.settings} update={update} /> });
  }

  private closeDialogueClick = (e: MouseEvent) => {
    if (e.target === e.currentTarget) this.setState({ dialogue: undefined });
  }

  private computerVDom = ({ settings, dialogue }: MainState) => {
    return <Computer settings={settings} focused={dialogue === undefined} />;
  }
}
