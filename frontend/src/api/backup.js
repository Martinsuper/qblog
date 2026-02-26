import request from './request'

/**
 * 备份相关 API
 */

/**
 * 创建备份
 */
export function createBackup(type = 'database', description) {
  return request({
    url: '/backup/create',
    method: 'post',
    params: { type, description }
  })
}

/**
 * 获取备份列表
 */
export function getBackupList() {
  return request({
    url: '/backup/list',
    method: 'get'
  })
}

/**
 * 获取备份详情
 */
export function getBackupDetail(id) {
  return request({
    url: `/backup/${id}`,
    method: 'get'
  })
}

/**
 * 下载备份文件
 */
export function downloadBackup(id) {
  return request({
    url: `/backup/download/${id}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 恢复备份
 */
export function restoreBackup(id) {
  return request({
    url: `/backup/restore/${id}`,
    method: 'post'
  })
}

/**
 * 删除备份
 */
export function deleteBackup(id) {
  return request({
    url: `/backup/delete/${id}`,
    method: 'delete'
  })
}

/**
 * 导入备份文件
 */
export function importBackup(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/backup/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取备份设置
 */
export function getBackupSettings() {
  return request({
    url: '/backup/settings',
    method: 'get'
  })
}

/**
 * 更新备份设置
 */
export function updateBackupSettings(settings) {
  return request({
    url: '/backup/settings',
    method: 'put',
    data: settings
  })
}
