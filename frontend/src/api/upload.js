import request from './request'

/**
 * 图片上传（走统一的 axios 实例：自动携带 token、统一错误提示）
 */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData)
}
