let enabled = true;

export const get = (key: string): string | null => {
  if (!enabled) return null;
  try {
    return localStorage.getItem(key);
  } catch (e) {
    console.error("Error reading from storage, disabling all further access.", e);
    enabled = false;
    return null;
  }
};

export const set = (key: string, value: string): void => {
  if (!enabled) return;
  try {
    localStorage.setItem(key, value);
  } catch (e) {
    console.error("Error writing to localStorage, disabling all further access.", e);
    enabled = false;
  }
};

export const remove = (key: string): void => {
  if (!enabled) return;
  try {
    localStorage.removeItem(key);
  } catch (e) {
    console.error("Error writing to storage, disabling all further access.", e);
    enabled = false;
  }
};
