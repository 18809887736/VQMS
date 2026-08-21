import request from '@/utils/request'

// 查询 AVC 投运率列表
export function listRuntime(query) {
  return request({
    url: '/vqms/avc/runtime/list',
    method: 'get',
    params: query
  })
}

// 查询 AVC 投运率详细
export function getRuntime(id) {
  return request({
    url: '/vqms/avc/runtime/' + id,
    method: 'get'
  })
}
