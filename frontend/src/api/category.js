import request from './request'

/**
 * 分类相关 API
 */
export function getCategoryList() {
  return request({
    url: '/categories',
    method: 'get'
  })
}

export function getCategoryDetail(id) {
  return request({
    url: `/categories/${id}`,
    method: 'get'
  })
}
