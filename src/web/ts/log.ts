const debug = window.location.protocol === "file:" || window.location.hostname === "localhost";

/* eslint-disable no-console */
export default debug
  ? (name: string) => (msg: string) => console.log(`[${name}] ${msg}`)
  : () => () => 0;
