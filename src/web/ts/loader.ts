requirejs.config({
  urlArgs: (_, url) => url.startsWith("https://cdnjs.cloudflare.com") || url.includes("-")
    ? ""
    : "?v={{version}}",
  paths: {
    vs: "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.18.1/min/vs",
  },
});

(window as any).MonacoEnvironment = {
  getWorkerUrl: (_workerId: string, _label: string) =>
    `data:text/javascript;charset=utf-8,${encodeURIComponent(`
      self.MonacoEnvironment = {
        baseUrl: "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.18.1/min/"
      };
      importScripts("https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.18.1/min/vs/base/worker/workerMain.js");
    `)}`,
};

/* tslint:disable:no-var-requires */
require(["./main"], (main: () => void) => main());
