/** Serialize writes, snapshot payloads, and only acknowledge the saved version. */
export function createArticleSaveQueue({ getSnapshot, persist, onSaved }) {
  let savedSnapshot = ''
  let tail = Promise.resolve()
  let pending = 0
  let disposed = false

  const markSaved = (snapshot = getSnapshot()) => {
    savedSnapshot = JSON.stringify(snapshot)
  }

  async function save({ automatic = false, status } = {}) {
    if (disposed || (automatic && pending > 0)) return false
    pending++
    const operation = tail.then(async () => {
      if (disposed) return false
      const snapshot = getSnapshot()
      if (status !== undefined) snapshot.status = status
      if (automatic && JSON.stringify(snapshot) === savedSnapshot) return false
      await persist(snapshot)
      markSaved(snapshot)
      onSaved(snapshot)
      return true
    })
    tail = operation.catch(() => {})
    try {
      return await operation
    } finally {
      pending--
    }
  }

  return { save, markSaved, dispose: () => { disposed = true } }
}
