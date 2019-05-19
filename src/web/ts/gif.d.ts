export = GIF;

declare class GIF {
  constructor(options: Partial<GIF.Options>);

  addFrame(image: ImageData | CanvasRenderingContext2D | WebGLRenderingContext | HTMLImageElement | SVGImageElement | HTMLVideoElement | HTMLCanvasElement | ImageBitmap, options?: Partial<GIF.FrameOptions> | undefined): void;

  render(): void;

  on(event: "finished", callback: (blob: Blob, data: Uint8Array) => void): this;
  on(event: "progress", callback: (progress: number) => void): this;
  on(event: "start", callback: () => void): this;
  on(event: "abort", callback: () => void): this;
}

type G = GIF;

declare namespace GIF {
  export type Options = {
    workerScript: string,
    workers: number,
    repeat: number,
    background: string,
    quality: number,
    width: number
    height: number
    transparent: boolean
    debug: boolean
    dither: boolean
  }

  export type FrameOptions = {
    delay: number,
    copy: boolean,
    dispose: number,
  }

  type GIF = G;
}
