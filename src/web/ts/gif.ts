/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2018 Johan Nordberg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Typescript port of https://github.com/jnordberg/gif.js, largely to avoid dragging
 * in the entirity of EventEmitter.
 */

import logger from "./log";
const log = logger("GIF encoding");

export type Options = {
  workerScript: string,
  workers: number,
  repeat: number,
  background: string,
  quality: number,
  width: number,
  height: number,
  transparent: boolean | null,
  dither: false | string,
};

export type FrameType = ImageData | CanvasRenderingContext2D;

export type FrameOptions = {
  delay: number,
};

type Frame = FrameOptions & {
  transparent: boolean | null,
  data: Uint8ClampedArray,
};

type Task = {
  index: number,
  data: Uint8ClampedArray,
  last: boolean,
  delay: number,
  dispose: number,
  transparent: boolean | null,
  width: number,
  height: number,
  quality: number,
  dither: false | string,
  globalPalette: number[] | true,
  repeat: number,
  canTransfer: boolean,
};

type TaskResult = {
  index: number,
  globalPalette: number[] | true,
  data: Uint8Array[],
  pageSize: number,
  cursor: number,
};

const defaults = {
  workerScript: "assets/gif.worker.js",
  workers: 2,
  repeat: 0, // repeat forever, -1 = repeat once
  background: "#fff",
  quality: 10, // pixel sample interval, lower is better
  transparent: null,
  dither: false as false, // see GIFEncoder.js for dithering options
};

const frameDefaults: FrameOptions = {
  delay: 500, // ms
};

export class GIF {
  private running: boolean = false;
  private readonly options: Options;
  private readonly frames: Frame[] = [];

  private readonly freeWorkers: Worker[] = [];
  private readonly activeWorkers: Worker[] = [];

  private imageParts?: Array<TaskResult | null>;
  private nextFrame: number = 0;
  private finishedFrames: number = 0;
  private globalPalette: number[] | true = true;

  public onAbort?: () => void;
  public onProgress?: (progress: number) => void;
  public onFinished?: (data: Blob) => void;

  constructor(options: Partial<Options> & { width: number, height: number }) {
    this.options = { ...defaults, ...options };
  }

  public addFrame(image: FrameType, options?: Partial<FrameOptions>): void {
    let data: Uint8ClampedArray;
    if (image instanceof ImageData) {
      data = image.data;
    } else if (image instanceof CanvasRenderingContext2D) {
      data = this.getContextData(image);
    } else {
      throw new Error("Invalid image");
    }

    this.frames.push({
      ...frameDefaults, ...options,
      transparent: this.options.transparent, data,
    });
  }

  public render() {
    if (this.running) throw new Error("Already rendering");

    this.running = true;
    this.nextFrame = 0;
    this.finishedFrames = 0;
    this.imageParts = this.frames.map(() => null);
    const numWorkers = this.spawnWorkers();

    // We need to wait for the palette
    if (this.globalPalette === true) {
      this.renderNextFrame();
    } else {
      for (let i = 0; i < numWorkers; i++)  this.renderNextFrame();
    }

    if (this.onProgress) this.onProgress(0);
  }

  public abort() {
    while (true) {
      const worker = this.activeWorkers.shift();
      if (worker == null) break;

      log("Killing active worker");
      worker.terminate();
    }

    this.running = false;
    if (this.onAbort) this.onAbort();
  }

  private spawnWorkers() {
    const numWorkers = Math.min(this.options.workers, this.frames.length);
    for (let i = this.freeWorkers.length; i < numWorkers; i++) {
      log(`Spawning worker ${i}`);
      const worker = new Worker(this.options.workerScript);
      worker.onmessage = (event: MessageEvent) => {
        this.activeWorkers.splice(this.activeWorkers.indexOf(worker), 1);
        this.freeWorkers.push(worker);
        return this.frameFinished(event.data);
      };
      this.freeWorkers.push(worker);
    }

    return numWorkers;
  }

  private frameFinished(frame: TaskResult) {
    if (!this.imageParts) throw new Error("No image data!");

    log(`Frame ${frame.index} finished - ${this.activeWorkers.length} active`);
    this.finishedFrames++;
    if (this.onProgress) this.onProgress(this.finishedFrames / this.frames.length);
    this.imageParts[frame.index] = frame;

    // Remember calculated palette, spawn the rest of the workers
    if (this.globalPalette === true) {
      this.globalPalette = frame.globalPalette;
      log("Global palette analyzed");
      if (this.frames.length > 2) {
        for (let i = 1; i < this.freeWorkers.length; i++) this.renderNextFrame();
      }
    }
    if (this.imageParts.indexOf(null) >= 0) {
      return this.renderNextFrame();
    } else {
      return this.finishRendering();
    }
  }

  private finishRendering() {
    if (!this.imageParts) throw new Error("No image data!");
    const imageParts = this.imageParts as TaskResult[];

    let len = 0;
    for (const frame of imageParts) {
      len += (frame.data.length - 1) * frame.pageSize + frame.cursor;
    }
    const lastFrame = imageParts[this.frames.length - 1];
    len += lastFrame.pageSize - lastFrame.cursor;

    log(`Rendering finished - filesize ${Math.round(len / 1000)}kb`);

    const data = new Uint8Array(len);
    let offset = 0;
    for (const frame of imageParts) {
      for (let i = 0; i < frame.data.length; i++) {
        data.set(frame.data[i], offset);
        offset += i === frame.data.length - 1 ? frame.cursor : frame.pageSize;
      }
    }

    if (this.onFinished) this.onFinished(new Blob([data], { type: "image/gif" }));
  }

  private renderNextFrame() {
    if (this.freeWorkers.length === 0) throw new Error("No free workers");
    if (this.nextFrame >= this.frames.length) return;

    const worker = this.freeWorkers.shift()!;
    const task = this.getTask(this.nextFrame++);
    log(`Starting frame ${task.index + 1} of ${this.frames.length}`);
    this.activeWorkers.push(worker);
    return worker.postMessage(task);
  }

  private getContextData(ctx: CanvasRenderingContext2D) {
    return ctx.getImageData(0, 0, this.options.width, this.options.height).data;
  }

  private getTask(index: number) {
    const frame = this.frames[index];
    return {
      index,
      data: frame.data,
      last: index === (this.frames.length - 1),
      delay: frame.delay,
      dispose: -1,
      transparent: frame.transparent,
      width: this.options.width,
      height: this.options.height,
      quality: this.options.quality,
      dither: this.options.dither,
      globalPalette: this.globalPalette,
      repeat: this.options.repeat,
      canTransfer: true,
    };
  }
}
