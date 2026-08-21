import request from '@/utils/request'

// 查询 AVC 调节合格率列表（两档平行：快速性 / 经济性）
export function listRegulation(query) {
  return request({
    url: '/vqms/avc/regulation/list',
    method: 'get',
    params: query
  })
}

// 查询 AVC 调节合格率详细
export function getRegulation(id) {
  return request({
    url: '/vqms/avc/regulation/' + id,
    method: 'get'
  })
}
