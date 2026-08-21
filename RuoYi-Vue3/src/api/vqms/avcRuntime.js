import request from '@/utils/request'

// 查询 AVC 投运率列表
export function listRuntime(query) {
  return request({
    url: '/vqms/avc/runtime/list',
    method: 'get',
    params: query
  })
}
