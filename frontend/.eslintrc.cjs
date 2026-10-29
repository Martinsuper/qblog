module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: ['eslint:recommended', 'plugin:vue/vue3-essential'],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  ignorePatterns: ['dist/', '.visual-qa/', 'node_modules/'],
  rules: {
    // Existing route components intentionally use names such as Index and Login.
    'vue/multi-word-component-names': 'off',
    'no-unused-vars': 'warn',
    'vue/no-unused-components': 'warn',
    // The shared draft object is owned by the page; layout components edit its fields.
    'vue/no-mutating-props': ['error', { shallowOnly: true }],
    'no-empty': ['error', { allowEmptyCatch: true }]
  },
  overrides: [{
    files: ['*.config.js', '*.cjs', 'tests/**/*.js', 'tests/**/*.mjs'],
    env: { node: true }
  }]
}
