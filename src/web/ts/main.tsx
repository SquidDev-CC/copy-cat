import { Component, h } from "preact";
import { Computer } from "./computer";
import { Cog } from "./font";
import { Settings } from "./settings";

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
      const settingJson = window.localStorage.settings;
      if (settings !== undefined) {
        const settingStorage = JSON.parse(settingJson);
        for (const key of Object.keys(settings)) {
          const value = settingStorage[key];
          if (value !== undefined) (settings as any)[key] = value;
        }
      }
    } catch {
      // Ignore
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
      window.localStorage.settings = JSON.stringify(this.state.settings);
    } catch {
      // Ignore
    }
  }

  public render(_props: MainProps, state: MainState) {
    return <div class="container">
      {state.currentVDom(state)}
      <button class="action-button settings-cog" title="Configure how the emulator behaves" onClick={this.openSettings}>
        <Cog />
      </button>
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
