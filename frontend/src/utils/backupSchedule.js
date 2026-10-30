const integerInRange = (value, min, max) => Number.isInteger(value) && value >= min && value <= max

export function scheduleFromSettings(settings) {
  if (!settings || typeof settings !== 'object' || typeof settings.enabled !== 'boolean' ||
      !['hourly', 'daily', 'weekly'].includes(settings.frequency) ||
      !integerInRange(settings.hour, 0, 23) || !integerInRange(settings.minute, 0, 59) ||
      !integerInRange(settings.dayOfWeek, 0, 6) || !integerInRange(settings.keepCount, 1, 30)) {
    throw new Error('备份设置数据无效，请重试')
  }
  return {
    enabled: settings.enabled,
    frequency: settings.frequency,
    time: `${String(settings.hour).padStart(2, '0')}:${String(settings.minute).padStart(2, '0')}`,
    minute: settings.minute,
    dayOfWeek: settings.dayOfWeek,
    keepCount: settings.keepCount
  }
}

export function schedulePayload(schedule) {
  if (!schedule || typeof schedule.enabled !== 'boolean' ||
      !['hourly', 'daily', 'weekly'].includes(schedule.frequency) ||
      !integerInRange(schedule.keepCount, 1, 30) ||
      !integerInRange(schedule.dayOfWeek, 0, 6)) {
    throw new Error('请选择有效的备份频率、星期和保留份数')
  }
  const match = typeof schedule.time === 'string' && /^(\d{2}):(\d{2})$/.exec(schedule.time)
  if (!match || !integerInRange(Number(match[1]), 0, 23) || !integerInRange(Number(match[2]), 0, 59)) {
    throw new Error('请选择有效的备份时间')
  }
  const minute = schedule.frequency === 'hourly' ? schedule.minute : Number(match[2])
  if (!integerInRange(minute, 0, 59)) throw new Error('请输入 0 到 59 之间的整数分钟')
  return { enabled: schedule.enabled, frequency: schedule.frequency, hour: Number(match[1]), minute, dayOfWeek: schedule.dayOfWeek, keepCount: schedule.keepCount }
}
