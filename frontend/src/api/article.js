import request from './request'

/**
 * 文章相关 API
 */
export function getArticleList(params) {
  return request({
    url: '/articles',
    method: 'get',
    params
  })
}

/**
 * 管理后台 - 获取所有文章（包括草稿）
 */
export function getAdminArticleList(params) {
  return request({
    url: '/articles/admin/list',
    method: 'get',
    params
  })
}

export function getArticleDetail(id) {
  return request({
    url: `/articles/${id}`,
    method: 'get'
  })
}

export function createArticle(data) {
  return request({
    url: '/articles',
    method: 'post',
    data
  })
}

export function updateArticle(id, data) {
  return request({
    url: `/articles/${id}`,
    method: 'put',
    data
  })
}

export function deleteArticle(id) {
  return request({
    url: `/articles/${id}`,
    method: 'delete'
  })
}

export function getHotArticles(params) {
  return request({
    url: '/articles/hot',
    method: 'get',
    params
  })
}

export function getLatestArticles(params) {
  return request({
    url: '/articles/latest',
    method: 'get',
    params
  })
}

export function likeArticle(id) {
  return request({
    url: `/articles/${id}/like`,
    method: 'post'
  })
}

export function unlikeArticle(id) {
  return request({
    url: `/articles/${id}/like`,
    method: 'delete'
  })
}
