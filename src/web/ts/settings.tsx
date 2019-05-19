import { h } from "preact";
import { IConfigGroup } from "./classes";

export type Settings = {
  // Editor Settings
  showInvisible: boolean,
  trimWhitespace: boolean,

  // Terminal settings
  terminalFont: string,

  // General settings
  darkMode: boolean,
  terminalBorder: boolean,

  computerSettings: { [key: string]: any },
};

export type IConfigProperty<K, T> = {
  type: K,
  id: string,
  name: string,
  description: string,
  value: T, changed: (value: T) => void,
};

export type ConfigProperty
  = IConfigProperty<"string", string>
  | IConfigProperty<"boolean", boolean>
  | (IConfigProperty<"int", number> & { min: number, max: number });

export class ConfigGroup implements IConfigGroup {
  public readonly name: string;
  public readonly description: string | null;
  public readonly properties: ConfigProperty[] = [];

  constructor(name: string, description: string | null) {
    this.name = name;
    this.description = description;
  }

  public addString(
    id: string, name: string, def: string, description: string,
    changed: (value: string) => void,
  ): void {
    this.properties.push({ type: "string", id, name, description, value: def, changed });
  }

  public addBoolean(
    id: string, name: string, def: boolean, description: string,
    changed: (value: boolean) => void,
  ): void {
    this.properties.push({ type: "boolean", id, name, description, value: def, changed });
  }

  public addInt(
    id: string, name: string, def: number, min: number, max: number, description: string,
    changed: (value: number) => void,
  ): void {
    this.properties.push({ type: "int", id, name, description, value: def, min, max, changed });
  }
}

export type SettingsProperties = {
  settings: Settings,
  computerConfigGroups: ConfigGroup[];
  update: (changes: Settings) => void,
};

const getString = (x: HTMLInputElement) => x.value;
const getNumber = (x: HTMLInputElement) => {
  const v = parseInt(x.value, 10);
  return Number.isNaN(v) ? undefined : v;
};
const getBool = (x: HTMLInputElement) => x.checked;

export const Settings = ({ settings, computerConfigGroups, update }: SettingsProperties): JSX.Element => {
  function updateWith<K extends keyof Settings>(changes: Pick<Settings, K>) {
    update({ ...settings, ...changes });
  }

  function getUpdater<K, T>(
    property: IConfigProperty<K, T>,
    extract: (e: HTMLInputElement) => T | undefined,
  ): (event: Event) => void {
    return e => {
      const value = extract(e.target as HTMLInputElement);
      if (value === undefined) return;

      property.changed(value);
      property.value = value;
      update({
        ...settings,
        computerSettings: { [property.id]: value, ...settings.computerSettings },
      });
    };
  }

  // TODO: Maybe move all settings into the 'ConfigGroup' system?
  return <div class="settings-box dialogue-box">
    <h2>Settings</h2>
    <h3>Editor settings</h3>
    <div class="form-group">
      <label>
        <input type="checkbox" checked={settings.showInvisible}
          onInput={(e: Event) => updateWith({ showInvisible: (e.target as HTMLInputElement).checked })} />
        Show whitespace
      </label>

      <label>
        <input type="checkbox" checked={settings.trimWhitespace}
          onInput={(e: Event) => updateWith({ trimWhitespace: (e.target as HTMLInputElement).checked })} />
        Trim whitespace
      </label>

      <label>
        <input type="checkbox" checked={settings.darkMode}
          onInput={(e: Event) => updateWith({ darkMode: (e.target as HTMLInputElement).checked })} />
        Dark Mode
        {settings.darkMode
          ? <p class="tiny-text">Only the editor currently, feel free to PR some fancy CSS.</p>
          : null}
      </label>
    </div>

    <h3>Terminal settings</h3>
    <div class="form-group">
      <label>
        Font style
        <select value={settings.terminalFont}
          onInput={(e: Event) => updateWith({ terminalFont: (e.target as HTMLInputElement).value })} >
          <option value="assets/term_font.png">Standard Font</option>
          <option value="assets/term_font_hd.png">High-definition font</option>
        </select>
      </label>
    </div>

    {computerConfigGroups.map(({ name, description, properties }) => [
      <h3>{name}</h3>,
      description ? <p class="tiny-text">{description}</p> : null,
      <div class="form-group">
        {properties.map(property => {
          switch (property.type) {
            case "string":
              return <label>
                {property.name}
                <input type="text" value={property.value} onChange={getUpdater(property, getString)}></input>
                <p class="tiny-text">{property.description}</p>
              </label>;
            case "int":
              return <label>
                {property.name}
                <input type="number" value={property.value} min={property.min} max={property.max} step={1}
                  onChange={getUpdater(property, getNumber)}></input>
                <p class="tiny-text">{property.description}</p>
              </label>;
            case "boolean":
              return <label>
                <input type="checkbox" checked={property.value} onInput={getUpdater(property, getBool)}></input>
                {property.name}
                <p class="tiny-text">{property.description}</p>
              </label>;
          }
        })}
      </div>,
    ])}
  </div >;
};
