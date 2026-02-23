import request from './request'

/**
 * 标签相关 API
 */
export function getTagList() {
  return request({
    url: '/tags',
    method: 'get'
  })
}
