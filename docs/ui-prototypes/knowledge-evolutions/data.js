/* 五种「当前 UI 演化」原型共享的示例数据。
 * 与 07-knowledge / 当前 KnowledgeSeven 实现同一组演示笔记，仅保留原型所需字段。
 * 仅为静态演示数据，不代表真实知识库内容。 */
'use strict';

const FOLDERS = [
  { id: 'guide', name: '知识库指南', children: [{ id: 'rules', name: '规范与约定' }, { id: 'methods', name: '整理方法' }] },
  { id: 'engineering', name: '工程与开发', children: [{ id: 'frontend', name: '前端工程' }, { id: 'backend', name: '后端与数据' }, { id: 'ops', name: '部署与运维' }] },
  { id: 'ai', name: 'AI 与自动化', children: [{ id: 'mcp', name: 'MCP 集成' }, { id: 'retrieval', name: '检索与索引' }] },
  { id: 'product', name: '产品与设计', children: [{ id: 'research', name: '产品研究' }, { id: 'design', name: '界面与交互' }] },
];

/* [标题, 目录, 标签, 来源, 摘要] */
const CATALOG = [
  ['知识库组织规范：从收集到可复用', 'rules', '知识管理', 'manual', '用稳定的目录组织知识，以轻量标签连接不同主题。每一篇记录都应有明确的上下文、来源与下一步。'],
  ['笔记命名与元数据约定', 'rules', '知识管理', 'manual', '标题使用“对象 + 具体问题”，避免“随手记”“备忘”等无法检索的名称。'],
  ['目录层级与标签的边界', 'rules', '知识管理', 'manual', '目录回答“主要属于哪里”，标签回答“还可以从哪些角度找到它”。'],
  ['每周知识整理检查清单', 'methods', '工作流', 'manual', '每周五留出二十分钟处理收件箱：删除重复、补齐来源、拆分过长记录。'],
  ['渐进式总结：从摘录到判断', 'methods', '知识管理', 'manual', '保留原文关键事实，将理解单独成段，让未来的自己能快速重建上下文。'],
  ['收件箱归档决策流程', 'methods', '工作流', 'manual', '先判断是否值得长期保存，再决定目录；暂时无法分类的留在收件箱。'],
  ['Vue 组件边界与状态归属', 'frontend', 'Vue', 'manual', '状态放在使用它的最小公共祖先，可复用组件只暴露明确的输入与事件。'],
  ['前端请求竞态的处理策略', 'frontend', '性能', 'mcp', '使用请求序号或 AbortController，确保旧结果不能覆盖新状态。'],
  ['长列表阅读的虚拟化权衡', 'frontend', '性能', 'manual', '虚拟列表降低 DOM 数量，但影响查找、滚动定位与屏幕阅读器。'],
  ['Markdown 安全渲染边界', 'frontend', '安全', 'manual', '导入与缓存正文属于不可信输入，渲染时禁用原始 HTML 并限制链接协议。'],
  ['键盘可达性与焦点管理', 'frontend', '无障碍', 'manual', '弹窗打开后焦点进入交互区域，关闭后回到触发按钮。'],
  ['Spring Boot 接口错误约定', 'backend', 'API', 'manual', '错误返回使用稳定业务码与可读消息，客户端不展示堆栈或内部路径。'],
  ['MySQL 索引设计与慢查询定位', 'backend', '数据库', 'mcp', '从真实查询条件出发设计组合索引，用 EXPLAIN 确认扫描范围。'],
  ['Redis 缓存失效与一致性', 'backend', '数据库', 'manual', '缓存是数据库的派生视图，更新后失效对应缓存并用 TTL 控制异常窗口。'],
  ['分页接口的稳定排序', 'backend', 'API', 'manual', '按更新时间排序需补充唯一 ID，高频变化数据评估游标分页。'],
  ['数据库备份恢复演练记录', 'ops', '运维', 'mcp', '备份只有经过恢复验证才有意义，演练记录快照时间与恢复耗时。'],
  ['Nginx 反向代理部署清单', 'ops', '运维', 'manual', '上线前检查代理超时、上传限制、静态缓存与 API 路径，保留回滚入口。'],
  ['服务健康检查与告警分层', 'ops', '运维', 'manual', '存活探针判断是否重启，就绪探针判断能否接收请求。'],
  ['MCP 写入知识库的字段协议', 'mcp', 'MCP', 'mcp', '导入工具应显式传递标题、正文、来源 URI 与外部记录 ID。'],
  ['MCP 导入的幂等与去重', 'mcp', 'MCP', 'mcp', '用来源系统与外部 ID 组成幂等键，重复导入只更新同一记录。'],
  ['工具权限与写入审计', 'mcp', '安全', 'manual', '写入操作保留操作者、工具名、变更摘要与执行时间。'],
  ['外部来源失效后的处理', 'mcp', 'MCP', 'manual', '保留来源快照和摘要，远端失效时标记链接不可用并允许人工修正。'],
  ['知识库全文检索的字段权重', 'retrieval', '检索', 'mcp', '标题权重高于正文，标签精确命中提供加分，结果呈现命中上下文。'],
  ['索引状态与失败重试机制', 'retrieval', '检索', 'manual', '索引状态属于后台事实，不等同于文档是否可阅读。'],
  ['中文分词与专有名词检索', 'retrieval', '检索', 'mcp', '中英文混合词需用自定义词典、短语匹配与前缀补全改善检索。'],
  ['检索效果的小样本评估', 'retrieval', '检索', 'manual', '先维护真实问题与期望命中的笔记，再评估前五条结果的相关性。'],
  ['知识工作者访谈提纲', 'research', '研究', 'manual', '围绕最近一次找不到资料的经历提问，观察实际搜索与整理行为。'],
  ['阅读与整理任务的优先级', 'research', '研究', 'manual', '高频任务是查找、阅读和归档，问答属于辅助能力。'],
  ['个人知识库竞品观察', 'research', '研究', 'manual', '比较层级导航、列表密度、全文检索、导入可信度与维护成本。'],
  ['从反馈中区分需求与方案', 'research', '研究', 'manual', '界面偏好是线索，需要还原背后的任务：资料多时容易迷失。'],
  ['三栏布局的信息密度基线', 'design', '设计系统', 'manual', '左栏建立空间感，中栏帮助扫读，右栏容纳连续阅读，宽度不平均分配。'],
  ['冷灰与克制蓝色的语义使用', 'design', '设计系统', 'manual', '蓝色只用于当前选中、关键操作和可交互链接，边界用浅灰分隔。'],
  ['正文排版与表格可读性', 'design', '设计系统', 'manual', '控制行长，表格使用轻边框、紧凑行高和横向滚动。'],
  ['移动端目录与阅读切换', 'design', '无障碍', 'manual', '小屏把空间并列转成任务切换，各自拥有完整宽度并保留选中态。'],
  ['导入内容的来源可信度标注', 'research', 'MCP', 'mcp', '来源应展示具体工具与最近同步时间，区分导入时间与原文更新时间。'],
  ['批量整理前的低风险策略', 'methods', '工作流', 'manual', '先对少量记录验证目录规则，保留修改历史与恢复路径。'],
  ['API 文档版本化备忘', 'backend', 'API', 'mcp', '每个接口变更说明兼容性、迁移窗口与受影响的调用者。'],
  ['静态资源缓存策略', 'ops', '性能', 'manual', '带内容哈希的资源长期缓存，入口 HTML 短缓存，先传资源再切入口。'],
  ['笔记引用与双向链接约定', 'rules', '知识管理', 'manual', '链接写清被引用内容的作用，引用他人资料保留出处与自己的解释。'],
  ['设计决策记录模板', 'design', '工作流', 'manual', '记录背景、候选方案、最终选择和代价，让后来的人知道当时的理由。'],
  ['待整理：检索服务选型资料', 'inbox', '检索', 'mcp', '初步收集数据库全文检索与独立搜索服务对比，仍需确认分词与成本。'],
  ['待整理：前端性能排查摘录', 'inbox', '性能', 'mcp', '长列表滚动卡顿来自同步计算和过多节点，先定位瓶颈再决定方案。'],
  ['待整理：用户访谈片段', 'inbox', '研究', 'manual', '受访者更常浏览已存资料而不是直接提问，把两个意图分开研究。'],
  ['待整理：MCP 写入失败样本', 'inbox', 'MCP', 'mcp', '示例请求缺少外部记录 ID，接入层应返回明确错误并提示补齐来源。'],
  ['待整理：产品会议结论', 'inbox', '工作流', 'manual', '首轮验证关注目录浏览、搜索和归档效率，AI 收起为辅助入口。'],
  ['待整理：中文检索测试词表', 'inbox', '检索', 'mcp', '覆盖缩写、同义词、中英混合、精确短语和错别字，标注期望命中。'],
  ['待整理：阅读模式观察', 'inbox', '设计系统', 'manual', '阅读在列表和正文间来回切换，需要稳定选中态与可预测滚动位置。'],
];

const MAIN_BODY = [
  '## 核心原则',
  '知识库的价值不在于收录了多少内容，而在于下一次需要时能否快速重建上下文。组织工作围绕三件事：**稳定的目录**、**克制的标签**、**可追溯的来源**。',
  '> 目录回答“这篇笔记主要属于哪里”，标签回答“还可以从哪些角度找到它”。两者重叠时，优先调整目录，而不是增加标签。',
  '## 目录设计',
  '- 顶级目录按“工作领域”划分，不超过 5 个，保持两到三级。',
  '- 收件箱是唯一的临时区域，每周固定时间清空。',
  '- 长期只有一篇笔记的目录，合并到上一级。',
  '## 来源与索引',
  '手动创建与工具导入分开标记。MCP 写入的记录必须携带来源 URI 与外部 ID，导入后先进入收件箱，确认目录归属后再归档。',
  '```json',
  '{',
  '  "external_id": "web-clip-20261030",',
  '  "source_uri": "https://example.com/post/42",',
  '  "tool": "web.collect_notes",',
  '  "folder": "inbox"',
  '}',
  '```',
  '## 持续维护',
  '规则服务于使用。如果经常通过同一个标签查找内容，再考虑建立独立目录；如果某个目录从不被打开，先怀疑目录划错了地方。',
  '| 检查项 | 推荐做法 |',
  '| --- | --- |',
  '| 标题 | 对象 + 问题，避免“随手记 01” |',
  '| 正文 | 结论在前，证据和过程在后 |',
  '| 来源 | 手动创建与工具导入明确区分 |',
  '| 状态 | 归档状态与索引状态独立表达 |',
].join('\n');

function techBody(title, summary) {
  return [
    '## 核心结论',
    summary,
    '这份记录用于后续查阅与团队讨论。先明确问题的边界，再选择实现方式，避免把某次场景下的经验当作普遍结论。',
    '## 实践步骤',
    '- 记录当前场景、约束条件与已有证据。',
    '- 选择最小可验证方案，明确预期结果。',
    '- 通过样本或操作记录验证，保留异常情况。',
    '- 将稳定结论补充到相关目录，待确认的问题继续跟踪。',
    '## 配置示例',
    '```json',
    '{',
    '  "mode": "example",',
    '  "validate_before_write": true,',
    '  "retain_source": true,',
    '  "retry_limit": 3',
    '}',
    '```',
    '示例只展示结构，不是可直接用于生产的配置。请结合实际服务版本与权限边界调整。',
    '## 下一步',
    '完成一次小范围验证，补齐真实环境中的结果，再决定是否更新通用规范。',
  ].join('\n');
}

function generalBody(title, summary) {
  return [
    '## 核心结论',
    summary,
    '在整理“' + title.replace(/^待整理：/, '') + '”相关材料时，优先保留可复用判断。原始材料和个人解释分开记录，必要时补充访谈原话或观察样本。',
    '## 应用记录',
    '> 好的规则应该减少下一次使用的成本，而不是增加这一次记录的负担。',
    '- 摘录时保留出处与访问日期。',
    '- 自己的解释单独成段，不与原文混排。',
    '- 待确认的问题写进正文开头，而不是留在标题里。',
    '## 下一步',
    '完成一次小范围验证，补齐真实场景中的结果，再决定是否更新通用规范。当前内容为界面原型的示例笔记，不代表实际项目运行结论。',
  ].join('\n');
}

function buildSeed() {
  const isTech = { frontend: 1, backend: 1, ops: 1, mcp: 1, retrieval: 1 };
  const fav = new Set([0, 7, 18, 27, 30]);
  return CATALOG.map(function (n, i) {
    return {
      id: 'n' + String(i + 1).padStart(2, '0'),
      title: n[0],
      folder: n[1],
      tags: [n[2]].concat(n[1] === 'inbox' ? ['待整理'] : []),
      source: n[3],
      tool: n[3] === 'mcp' ? (i % 2 ? 'web.collect_notes' : 'knowledge.create_note') : '',
      body: i === 0 ? MAIN_BODY : (isTech[n[1]] ? techBody(n[0], n[4]) : generalBody(n[0], n[4])),
      summary: n[4],
      favorite: fav.has(i),
      updated: '2026-10-' + String(28 - Math.floor(i / 3)).padStart(2, '0') + 'T' + String(17 - i % 8).padStart(2, '0') + ':' + String(50 - i % 40).padStart(2, '0') + ':00',
      status: i === 43 ? 'failed' : (i >= 40 ? 'pending' : 'indexed'),
    };
  });
}

function folderLabel(id) {
  for (const g of FOLDERS) {
    if (g.id === id) return g.name;
    for (const c of g.children) if (c.id === id) return c.name;
  }
  return id === 'inbox' ? '收件箱' : id;
}

function folderPath(id) {
  for (const g of FOLDERS) {
    if (g.id === id) return g.name;
    for (const c of g.children) if (c.id === id) return g.name + ' / ' + c.name;
  }
  return '收件箱';
}

function shortDate(iso) {
  const d = new Date(iso);
  return (d.getMonth() + 1) + '月' + d.getDate() + '日';
}

/* 极简 Markdown 渲染：标题、列表、引用、表格、代码块、行内标记。 */
function renderMarkdown(md) {
  const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  const inline = s => esc(s)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="#" onclick="return false">$1</a>');
  const lines = md.split('\n');
  let html = '', i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (line.startsWith('```')) {
      let buf = []; i++;
      while (i < lines.length && !lines[i].startsWith('```')) { buf.push(lines[i]); i++; }
      i++;
      html += '<pre><code>' + esc(buf.join('\n')) + '</code></pre>';
      continue;
    }
    if (/^\| /.test(line)) {
      const rows = [];
      while (i < lines.length && /^\| /.test(lines[i])) { rows.push(lines[i]); i++; }
      const cells = r => r.replace(/^\||\|$/g, '').split('|').map(s => s.trim());
      html += '<table><thead><tr>' + cells(rows[0]).map(c => '<th>' + inline(c) + '</th>').join('') + '</tr></thead><tbody>'
        + rows.slice(2).map(r => '<tr>' + cells(r).map(c => '<td>' + inline(c) + '</td>').join('') + '</tr>').join('') + '</tbody></table>';
      continue;
    }
    if (/^[-*] /.test(line)) {
      let buf = [];
      while (i < lines.length && /^[-*] /.test(lines[i])) { buf.push(lines[i].slice(2)); i++; }
      html += '<ul>' + buf.map(x => '<li>' + inline(x) + '</li>').join('') + '</ul>';
      continue;
    }
    if (line.startsWith('> ')) {
      let buf = [];
      while (i < lines.length && lines[i].startsWith('> ')) { buf.push(lines[i].slice(2)); i++; }
      html += '<blockquote>' + buf.map(x => '<p>' + inline(x) + '</p>').join('') + '</blockquote>';
      continue;
    }
    const h = /^(#{1,3}) (.+)$/.exec(line);
    if (h) { html += '<h' + h[1].length + '>' + inline(h[2]) + '</h' + h[1].length + '>'; i++; continue; }
    if (line.trim() === '') { i++; continue; }
    html += '<p>' + inline(line) + '</p>';
    i++;
  }
  return html;
}
