const KEY = 'qblog:knowledge-density'

export function readKnowledgeDensity(storage) {
  try { return storage?.getItem(KEY) === 'compact' ? 'compact' : 'standard' } catch { return 'standard' }
}

export function saveKnowledgeDensity(storage, value) {
  try { storage?.setItem(KEY, value === 'compact' ? 'compact' : 'standard') } catch { /* Storage may be unavailable in private browsing. */ }
}
