requirejs.config({
  // baseUrl: "assets/",
  // urlArgs: (_, url) => url.startsWith("https://cdnjs.cloudflare.com") ? url : url + "?v={version}",
  paths: {
    vs: "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.16.2/min/vs",
  },
});

(window as any).MonacoEnvironment = {
  getWorkerUrl: (_workerId: string, _label: string) =>
    `data:text/javascript;charset=utf-8,${encodeURIComponent(`
      self.MonacoEnvironment = {
        baseUrl: "https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.16.2/min/"
      };
      importScripts("https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.16.2/min/vs/base/worker/workerMain.js");
    `)}`,
};

/* tslint:disable:no-var-requires */
require(["./main"], (main: () => void) => main());
