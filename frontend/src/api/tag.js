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

export function getTagDetail(id) {
  return request({
    url: `/tags/${id}`,
    method: 'get'
  })
}

export function createTag(data) {
  return request({
    url: '/tags',
    method: 'post',
    data
  })
}

export function updateTag(id, data) {
  return request({
    url: `/tags/${id}`,
    method: 'put',
    data
  })
}

export function deleteTag(id) {
  return request({
    url: `/tags/${id}`,
    method: 'delete'
  })
}
