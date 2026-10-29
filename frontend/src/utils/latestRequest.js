/** Only the most recently started request may update component state. */
export function createLatestRequest() {
  let version = 0
  return {
    start() {
      const current = ++version
      return () => current === version
    },
    invalidate() { version++ }
  }
}
