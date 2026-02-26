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

export function createCategory(data) {
  return request({
    url: '/categories',
    method: 'post',
    data
  })
}

export function updateCategory(id, data) {
  return request({
    url: `/categories/${id}`,
    method: 'put',
    data
  })
}

export function deleteCategory(id) {
  return request({
    url: `/categories/${id}`,
    method: 'delete'
  })
}
export function updateCategorySort(ids) {
  return request({
    url: '/categories/sort',
    method: 'put',
    data: ids
  })
}
