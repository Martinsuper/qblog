export function noteListFeedback({ search, listSearch, tag, source, scope }) {
  if ([search, listSearch, tag, source].some(value => String(value || '').trim())) {
    return { title: '没有匹配的笔记', description: '试试其他关键词，或清除搜索与筛选条件。', action: 'clear' }
  }
  if (scope === 'favorites') return { title: '还没有收藏', description: '打开笔记后点击星标，即可在这里快速找到。', action: 'all' }
  if (scope === 'inbox') return { title: '收件箱已清空', description: '未归入目录的笔记会显示在这里。', action: 'create' }
  if (scope === 'all') return { title: '写下第一篇笔记', description: '从一个想法或一段资料开始，内容会自动保存。', action: 'create' }
  return { title: '这个目录还没有笔记', description: '在当前目录新建笔记，或将已有笔记移入这里。', action: 'create' }
}
