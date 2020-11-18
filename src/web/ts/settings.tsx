import clsx from "clsx";
import { JSX, h } from "preact";
import { useEffect, useState } from "preact/hooks";
import type { ConfigGroup as IConfigGroup } from "./classes";
import * as storage from "./storage";
import { dialogueBox, dialogueBoxDark, formGroup, formGroupDark, tinyText, tinyTextDark } from "./styles.css";

export type Settings = {
  // Editor Settings
  showInvisible: boolean,
  trimWhitespace: boolean,
  darkMode: boolean,

  // Terminal settings
  terminalFont: string,
};

/** The supported types of a property */
type PropertyTypes = {
  string: string,
  boolean: boolean,
  int: number,
  option: string,
};

/** Additional properties for a specific type */
type TypeExtensions = {
  string: unknown,
  boolean: unknown,
  int: { min: number, max: number },
  option: { choices: Array<{ key: string, value: string }> },
};

export type IConfigProperty<K extends keyof PropertyTypes> = {
  type: K,
  id: string,
  name: string,
  description: string,
  def: PropertyTypes[K], changed: (value: PropertyTypes[K]) => void,
} & TypeExtensions[K];

export type ConfigProperty
  = IConfigProperty<"string">
  | IConfigProperty<"boolean">
  | IConfigProperty<"int">
  | IConfigProperty<"option">;

/**
 * The persisted map for settings
 */
export class SettingStore {
  private data: { [key: string]: any } = {}; // eslint-disable-line @typescript-eslint/no-explicit-any

  public constructor() {
    const settingJson = storage.get("settings");
    if (settingJson !== null) {
      try {
        this.data = JSON.parse(settingJson);
      } catch (e) {
        console.error("Cannot read settings", e);
      }
    }
  }

  /** Get the value of a config property under the current storage */
  public get<K extends keyof PropertyTypes>(property: IConfigProperty<K>): PropertyTypes[K] {
    return property.id in this.data ? this.data[property.id] : property.def;
  }

  /** Set a value and fire any callbacks */
  public set<K extends keyof PropertyTypes>(property: IConfigProperty<K>, value: PropertyTypes[K]): void {
    if (this.get(property) === value) return;
    this.data[property.id] = value;
    property.changed(value);
    storage.set("settings", JSON.stringify(this.data));
  }
}

export class ConfigGroup implements IConfigGroup {
  private readonly store: SettingStore;

  public readonly name: string;
  public readonly description: string | null;
  public readonly properties: ConfigProperty[] = [];

  public constructor(name: string, description: string | null, store: SettingStore) {
    this.name = name;
    this.description = description;
    this.store = store;
  }

  private add<K extends keyof PropertyTypes>(property: IConfigProperty<K>): IConfigProperty<K> {
    this.properties.push(property as unknown as ConfigProperty); // FIXME: Work out an appropriate cast.
    const value = this.store.get(property);
    if (value !== property.def) property.changed(value);
    return property;
  }

  public addString(
    id: string, name: string, def: string, description: string,
    changed: (value: string) => void,
  ): IConfigProperty<"string"> {
    return this.add({ type: "string", id, name, description, def, changed });
  }

  public addBoolean(
    id: string, name: string, def: boolean, description: string,
    changed: (value: boolean) => void,
  ): IConfigProperty<"boolean"> {
    return this.add({ type: "boolean", id, name, description, def, changed });
  }

  public addOption(
    id: string, name: string, def: string, choices: Array<{ key: string, value: string }>, description: string,
    changed: (value: string) => void,
  ): IConfigProperty<"option"> {
    return this.add({ type: "option", id, name, description, choices, def, changed });
  }

  public addInt(
    id: string, name: string, def: number, min: number, max: number, description: string,
    changed: (value: number) => void,
  ): IConfigProperty<"int"> {
    return this.add({ type: "int", id, name, description, def, min, max, changed });
  }
}

function getUpdater<K extends keyof PropertyTypes>(
  store: SettingStore,
  property: IConfigProperty<K>,
  extract: (e: HTMLInputElement) => PropertyTypes[K] | undefined,
): (event: Event) => void {
  return e => {
    const value = extract(e.target as HTMLInputElement);
    if (value !== undefined) store.set(property, value);
  };
}

const getString = (x: HTMLInputElement) => x.value;
const getNumber = (x: HTMLInputElement) => {
  const v = parseInt(x.value, 10);
  return Number.isNaN(v) ? undefined : v;
};
const getBool = (x: HTMLInputElement) => x.checked;
const getOption = (def: string, choices: Array<{ key: string, value: string }>) => (x: HTMLInputElement) => {
  for (const { key } of choices) {
    if (key === x.value) return key;
  }

  return def;
};

export type SettingsProperties = {
  store: SettingStore,
  configGroups: ConfigGroup[],
};

export const Settings = ({ store, configGroups }: SettingsProperties): JSX.Element | null => {
  const [darkMode, setDarkMode] = useState<boolean | undefined>(undefined);

  useEffect(() => {
    for(const g of configGroups){
      for(const property of g.properties){
        if(property.id === "editor.dark") {
          // I have no idea how this works
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
          // @ts-ignore
          const p = store.get<"boolean">(property);
          // eslint-disable-next-line no-console
          setDarkMode(p);
          
        }
      }
    }
  });
  
  if(darkMode === undefined) return null;

  return <div class={clsx(dialogueBox, {[dialogueBoxDark]: darkMode})}>
    <h2>Settings</h2>
    {configGroups.map(({ name, description, properties }) => [
      <h3>{name}</h3>,
      description ? <p class={clsx(tinyText, {[tinyTextDark]: darkMode})}>{description}</p> : null,
      <div class={clsx(formGroup, {[formGroupDark]: darkMode})}>
        {properties.map(property => {
          switch (property.type) {
            case "string":
              return <label>
                {property.name}
                <input type="text" value={store.get(property)}
                  onChange={getUpdater(store, property, getString)}></input>
                <p class={clsx(tinyText, {[tinyTextDark]: darkMode})}>{property.description}</p>
              </label>;
            case "int":
              return <label>
                {property.name}
                <input type="number" value={store.get(property)} min={property.min} max={property.max} step={1}
                  onChange={getUpdater(store, property, getNumber)}></input>
                <p class={clsx(tinyText, {[tinyTextDark]: darkMode})}>{property.description}</p>
              </label>;
            case "boolean":
              return <label>
                <input type="checkbox" checked={store.get(property)}
                  onInput={getUpdater(store, property, getBool)}></input>
                {property.name}
                <p class={clsx(tinyText, {[tinyTextDark]: darkMode})}>{property.description}</p>
              </label>;
            case "option":
              return <label>
                {property.name}
                <select value={store.get(property)} onInput={getUpdater(store, property, getOption(property.def, property.choices))}>
                  {property.choices.map(({ key, value }) => <option value={key}>{value}</option>)}
                </select>
                <p class={clsx(tinyText, {[tinyTextDark]: darkMode})}>{property.description}</p>
              </label>;
          }
        })}
      </div>,
    ])};
  </div>}
