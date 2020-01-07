requirejs.config({
  urlArgs: (_, url) => url.startsWith("{{monaco}}") || url.includes("-")
    ? ""
    : "?v={{version}}",
  paths: {
    vs: "{{monaco}}/min/vs",
  },
});

(window as any).MonacoEnvironment = {
  getWorkerUrl: (_workerId: string, _label: string) =>
    `data:text/javascript;charset=utf-8,${encodeURIComponent(`
      self.MonacoEnvironment = {
        baseUrl: "{{monaco}}/min/"
      };
      importScripts("{{monaco}}/min/vs/base/worker/workerMain.js");
    `)}`,
};

/* tslint:disable:no-var-requires */
require(["./main"], (main: () => void) => main());
