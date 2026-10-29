import test from 'node:test'
import assert from 'node:assert/strict'
import MarkdownIt from 'markdown-it'
import { tipContainerPlugin } from '../src/utils/markdownPlugins/tipContainer.js'
import { codeCopyPlugin } from '../src/utils/markdownPlugins/codeCopy.js'

function renderer() {
  const md = new MarkdownIt({ html: false })
  md.use(tipContainerPlugin).use(codeCopyPlugin)
  return md
}

for (const type of ['tip', 'warning', 'danger', 'info']) {
  test(`${type} title escapes HTML rather than creating executable markup`, () => {
    const html = renderer().render(`::: ${type} <img src=x onerror=alert(1)> & "title"\nbody\n:::`)
    assert.ok(!html.includes('<img'))
    assert.ok(html.includes('&lt;img src=x onerror=alert(1)&gt; &amp; &quot;title&quot;'))
    assert.ok(html.includes('<svg'))
  })
}

test('fence info cannot inject markup or break the data-lang attribute', () => {
  const info = '"><img src=x onerror=alert(1)>'
  const html = renderer().render('```' + info + '\nhello\n```')
  assert.ok(!html.includes('<img'))
  assert.ok(html.includes('data-lang="&quot;&gt;&lt;img src=x onerror=alert(1)&gt;"'))
  assert.ok(html.includes('<span class="code-lang">&quot;&gt;&lt;img'))
})

test('code content cannot escape the hidden copy textarea', () => {
  const html = renderer().render('```javascript\n</textarea><img src=x onerror=alert(1)>\n```')
  assert.ok(!html.includes('<img'))
  assert.ok(html.includes('&lt;/textarea&gt;'))
  assert.equal((html.match(/<textarea /g) || []).length, 1)
  assert.equal((html.match(/<\/textarea>/g) || []).length, 1)
})

test('normal code copy markup remains available', () => {
  const md = renderer()
  assert.ok(md.render('```js\nconst n = 1\n```').includes('class="code-copy-btn"'))
})

test('plantuml content stays local instead of being sent to an external renderer', () => {
  // 私有工作台不允许把笔记内容编码后发往 www.plantuml.com
  const html = renderer().render('```plantuml\n@startuml\nAlice -> Bob\n@enduml\n```')
  assert.ok(!html.includes('plantuml.com'))
  assert.ok(!html.includes('<img'))
  assert.ok(html.includes('@startuml'))
})
