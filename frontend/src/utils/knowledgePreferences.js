export const LAYOUTS = ['knowledge', 'calm', 'paper', 'library']
export const COLORS = ['system', 'light', 'dark']
const KEY = 'younote.appearance.v1'
export function readAppearance(storage = localStorage) {
  try {
    const value = JSON.parse(storage.getItem(KEY)) || {}
    return { layout: LAYOUTS.includes(value.layout) ? value.layout : 'knowledge', color: COLORS.includes(value.color) ? value.color : 'light' }
  } catch { return { layout: 'knowledge', color: 'light' } }
}
export function saveAppearance(value, storage = localStorage) {
  try { storage.setItem(KEY, JSON.stringify(value)); return true } catch { return false }
}
