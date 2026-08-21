import request from '@/utils/request'

// 查询电压合格率日报列表
export function listDaily(query) {
  return request({
    url: '/vqms/stats/daily/list',
    method: 'get',
    params: query
  })
}
