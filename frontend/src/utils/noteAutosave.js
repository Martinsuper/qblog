/** One queue per mounted note; failed saves never acknowledge unsaved content. */
export function createNoteAutosave({ snapshot, persist, onSaved, onError }) {
  let acknowledged = JSON.stringify(snapshot())
  let tail = Promise.resolve()
  let blocked = false
  let stopped = false
  let pending = 0
  async function flush() {
    if (stopped || blocked) return false
    pending++
    const task = tail.then(async () => {
      if (stopped || blocked) return false
      const draft = structuredClone(snapshot())
      if (JSON.stringify(draft) === acknowledged) return true
      try {
        const saved = await persist(draft)
        acknowledged = JSON.stringify(draft)
        if (!stopped) onSaved(saved, draft)
        return true
      } catch (error) {
        blocked = error.response?.status === 409
        if (!stopped) onError(error, blocked)
        return false
      }
    })
    tail = task.catch(() => false)
    try { return await task } finally { pending-- }
  }
  return { flush, isDirty: () => pending > 0 || JSON.stringify(snapshot()) !== acknowledged, isBlocked: () => blocked, dispose: () => { stopped = true } }
}
