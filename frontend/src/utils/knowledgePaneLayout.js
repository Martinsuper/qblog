export const PANE_LIMITS = [{ min: 150, max: 360 }, { min: 200, max: 560 }]
export const READER_MIN = 240
export const SPLITTER_WIDTH = 4

// Keep preferred widths separate from displayed widths so shrinking the window is reversible.
export function paneLayout(available, preferred, open) {
  const room = Math.max(0, available - 2 * SPLITTER_WIDTH)
  const widths = [0, 0]
  for (let i = 0; i < 2; i++) {
    if (!open[i]) continue
    const reserve = READER_MIN + (i === 0 && open[1] ? PANE_LIMITS[1].min : 0)
    widths[i] = Math.max(0, Math.min(preferred[i], PANE_LIMITS[i].max, room - widths[0] - widths[1] - reserve))
  }
  return { widths, reader: Math.max(0, room - widths[0] - widths[1]) }
}

export function paneResizeLimit(index, available, open, displayedWidths) {
  const other = index === 0 ? (open[1] ? PANE_LIMITS[1].min : 0) : displayedWidths[0]
  return Math.max(0, Math.min(PANE_LIMITS[index].max, available - 2 * SPLITTER_WIDTH - READER_MIN - other))
}

export function clampPaneWidth(index, value, available, open, displayedWidths) {
  const max = paneResizeLimit(index, available, open, displayedWidths)
  return Math.max(Math.min(PANE_LIMITS[index].min, max), Math.min(max, value))
}
