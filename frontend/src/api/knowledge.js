import request from './request'
export const listNotes = params => request.get('/knowledge/notes', { params })
export const getNote = id => request.get(`/knowledge/notes/${id}`)
export const createNote = data => request.post('/knowledge/notes', data)
export const updateNote = (id, data) => request.put(`/knowledge/notes/${id}`, data)
export const removeNote = (id, version) => request.delete(`/knowledge/notes/${id}`, { params: { version } })
export const listFolders = () => request.get('/knowledge/folders')
export const createFolder = data => request.post('/knowledge/folders', data)
export const updateFolder = (id, data) => request.put(`/knowledge/folders/${id}`, data)
export const removeFolder = id => request.delete(`/knowledge/folders/${id}`)
export const listApiTokens = () => request.get('/knowledge/tokens')
export const createApiToken = data => request.post('/knowledge/tokens', data)
export const revokeApiToken = id => request.delete(`/knowledge/tokens/${id}`)

export const listMcpCalls = () => request.get('/knowledge/tokens/calls')

// 备份与导出（blob 直接返回，用于浏览器端触发下载）
export const exportNotesMarkdown = () => request.get('/knowledge/export/markdown', { responseType: 'blob' })
export const exportKnowledgeSnapshot = () => request.get('/knowledge/backup/export', { responseType: 'blob' })
