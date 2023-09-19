module.exports = {
  root: true,
  env: {
    browser: true,
    node: true,
    es6: true
  },
  extends: [
    "eslint:recommended",
  ],
  rules: {
    "semi": ["warn", "always"],
    "quotes": ["warn", "double"],
    "no-constant-condition": ["warn", { checkLoops: false }],
    "arrow-parens": ["warn", "as-needed"],
    "curly": ["warn", "multi-line", "consistent"],
    "indent": ["warn", 2, { SwitchCase: 1 }],
    "no-console": ["warn", { allow: ["error", "warn"] }],
    "object-shorthand": ["warn", "always", { avoidQuotes: true }],
    "quote-props": ["warn", "consistent-as-needed"],
    "no-useless-rename": "warn",
    "sort-imports": ["warn", {
      ignoreDeclarationSort: true
    }],
  },

  overrides: [
    {
      files: ["rollup.config.js"],
      parserOptions: {
        sourceType: "module",
        ecmaVersion: 2018,
      },
    },

    // The default TS config.
    {
      files: ["*.ts", "*.tsx"],
      parser: "@typescript-eslint/parser",
      parserOptions: {
        tsconfigRootDir: __dirname,
        project: true,
      },
      plugins: [
        "@typescript-eslint",
      ],
      extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended-type-checked",
        "plugin:@typescript-eslint/stylistic-type-checked",
      ],
      rules: {
        // Aesthetics
        "@typescript-eslint/member-delimiter-style": ["warn", {
          multiline: { delimiter: "comma" },
          singleline: { delimiter: "comma" },
          overrides: {
            interface: { multiline: { delimiter: "semi" } },
          },
        }],
        "@typescript-eslint/array-type": ["warn", { default: "generic" }],
        "@typescript-eslint/consistent-type-definitions": "off",
        "@typescript-eslint/consistent-indexed-object-style": "warn", // Useful, but not always!
        "@typescript-eslint/no-inferrable-types": ["error", { ignoreProperties : true } ],
        "@typescript-eslint/no-empty-function": "off",
        "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],

        // Semantics
        "@typescript-eslint/explicit-function-return-type": ["warn", { allowExpressions: true }],
        // "@typescript-eslint/no-non-null-assertion": "off", // TODO: Audit these.
        "@typescript-eslint/prefer-for-of": "warn",
        "@typescript-eslint/prefer-includes": "warn",
        "@typescript-eslint/prefer-nullish-coalescing": "warn",
        "@typescript-eslint/prefer-optional-chain": "warn",
      },
    },
  ]
};
