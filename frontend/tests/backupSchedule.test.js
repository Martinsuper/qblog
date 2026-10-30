import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { scheduleFromSettings, schedulePayload } from '../src/utils/backupSchedule.js'

const settings = { enabled: true, frequency: 'daily', hour: 2, minute: 8, dayOfWeek: 0, keepCount: 7, backupType: 'full' }

test('loads values without leaking non-editable fields into the update', () => {
  const schedule = scheduleFromSettings(settings)
  assert.equal(schedule.time, '02:08')
  assert.equal(schedule.minute, 8)
  assert.deepEqual(schedulePayload(schedule), { enabled: true, frequency: 'daily', hour: 2, minute: 8, dayOfWeek: 0, keepCount: 7 })
})

test('hourly uses the separately edited minute; daily and weekly use time', () => {
  const schedule = scheduleFromSettings(settings)
  schedule.frequency = 'hourly'
  schedule.minute = 45
  assert.equal(schedulePayload(schedule).minute, 45)
  schedule.frequency = 'weekly'
  schedule.time = '23:59'
  assert.equal(schedulePayload(schedule).minute, 59)
  assert.equal(schedulePayload(schedule).hour, 23)
})

test('invalid or missing settings and input cannot be saved', () => {
  assert.throws(() => scheduleFromSettings(undefined))
  assert.throws(() => scheduleFromSettings({ ...settings, minute: null }))
  for (const update of [{ time: null }, { time: '25:00' }, { minute: 60, frequency: 'hourly' }, { keepCount: null }, { dayOfWeek: 7 }, { frequency: 'invalid' }]) {
    assert.throws(() => schedulePayload({ ...scheduleFromSettings(settings), ...update }))
  }
})

test('page binds response data, uses a string time picker and gates saving on load', async () => {
  const page = await readFile(new URL('../src/views/Knowledge/Backup.vue', import.meta.url), 'utf8')
  assert.match(page, /const \{ data \} = await getBackupList\(\)/)
  assert.match(page, /const \{ data \} = await getBackupSettings\(\)/)
  assert.match(page, /value-format="HH:mm"/)
  assert.match(page, /v-if="schedule"/)
  assert.match(page, /if \(!schedule.value \|\| saving.value\) return/)
  assert.match(page, /scheduleError.value = true/)
})
