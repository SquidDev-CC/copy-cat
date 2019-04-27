import resolve from 'rollup-plugin-node-resolve';

export default {
  input: 'build/typescript/index.js',
  output: {
    file: 'build/rollup/main.js',
    name: 'start',
    format: "iife",
  },
  plugins: [
    resolve(),
  ],
};
