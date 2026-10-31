import { createLowlight } from 'lowlight'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'

 /**
 * 编辑器内代码块的语言注册表。
 * 语言集合与阅读端 useMarkdown.js 保持一致，避免两边高亮能力错位；
 * 未列出的语言自动退回自动检测（highlightAuto），不会报错。
 */
export const CODE_LANGUAGES = [
  { value: 'plaintext', label: '纯文本' },
  { value: 'javascript', label: 'JavaScript' },
  { value: 'typescript', label: 'TypeScript' },
  { value: 'xml', label: 'HTML / XML' },
  { value: 'css', label: 'CSS' },
  { value: 'json', label: 'JSON' },
  { value: 'bash', label: 'Shell' },
  { value: 'java', label: 'Java' },
  { value: 'python', label: 'Python' },
  { value: 'sql', label: 'SQL' },
  { value: 'yaml', label: 'YAML' }
]

/** 语言映射表：未收录时原样返回，兼容手写 ``` 围栏里的别名。 */
export function languageLabel(value) {
  return CODE_LANGUAGES.find(item => item.value === value)?.label || value
}

// highlight() 直接按语言取；'plaintext' 不属于 hljs 语言，
// 该别名在 KnowledgeEditor 里特殊处理为清空 language 属性。
const lowlight = createLowlight()
lowlight.register({ javascript, typescript, xml, css, json, bash, java, python, sql, yaml })

export default lowlight
