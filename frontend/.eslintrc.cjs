module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: ['eslint:recommended', 'plugin:vue/vue3-recommended'],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  ignorePatterns: ['dist/', '.visual-qa/', 'tests/visual-seven/', 'node_modules/'],
  rules: {
    // Existing route components intentionally use names such as Index and Login.
    'vue/multi-word-component-names': 'off',
    'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_', ignoreRestSiblings: true }],
    'vue/no-unused-components': 'error',
    // v-html 仅用于 markdown-it 渲染结果（html:false + 链接协议白名单，见 useMarkdown.js）
    'vue/no-v-html': 'off',
    // 组合式 API 组件的 props 均由父组件显式传入，不逐个补 default
    'vue/require-default-prop': 'off',
    // The shared draft object is owned by the page; layout components edit its fields.
    'vue/no-mutating-props': ['error', { shallowOnly: true }],
    'no-empty': ['error', { allowEmptyCatch: true }]
  },
  overrides: [{
    files: ['*.config.js', '*.cjs', 'tests/**/*.js', 'tests/**/*.mjs'],
    env: { node: true }
  }]
}
