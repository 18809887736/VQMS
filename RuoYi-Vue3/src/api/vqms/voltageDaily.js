import request from '@/utils/request'

// 查询电压合格率日报列表
export function listDaily(query) {
  return request({
    url: '/vqms/daily/list',
    method: 'get',
    params: query
  })
}

// 查询电压合格率日报详细
export function getDaily(id) {
  return request({
    url: '/vqms/daily/' + id,
    method: 'get'
  })
}
