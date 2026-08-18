import request from '@/utils/request'

// 获取作品交互状态
export function getInteractionStatus(id) {
  return request({
    url: `/artworks/${id}/status`,
    method: 'get'
  })
}

// 点赞 / 取消点赞
export function toggleLike(id) {
  return request({
    url: `/artworks/${id}/like`,
    method: 'post'
  })
}

// 收藏 / 取消收藏
export function toggleFavorite(id) {
  return request({
    url: `/artworks/${id}/favorite`,
    method: 'post'
  })
}

// 获取作品评论列表
export function getComments(id) {
  return request({
    url: `/artworks/${id}/comments`,
    method: 'get'
  })
}

// 发表评论
export function addComment(data) {
  return request({
    url: '/artworks/comments',
    method: 'post',
    data
  })
}

// 删除评论
export function deleteComment(commentId) {
  return request({
    url: `/artworks/comments/${commentId}`,
    method: 'delete'
  })
}