import request from './request'

/**
 * 评论相关 API
 */
export function getCommentList(params) {
  return request({
    url: '/comments',
    method: 'get',
    params
  })
}

export function createComment(data) {
  return request({
    url: '/comments',
    method: 'post',
    data
  })
}

export function deleteComment(id) {
  return request({
    url: `/comments/${id}`,
    method: 'delete'
  })
}

export function getCommentListByArticle(articleId, params) {
  return request({
    url: `/articles/${articleId}/comments`,
    method: 'get',
    params
  })
}
