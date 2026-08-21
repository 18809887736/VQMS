import request from '@/utils/request'

// 查询电压合格率年报列表
export function listYearly(query) {
  return request({
    url: '/vqms/stats/yearly/list',
    method: 'get',
    params: query
  })
}
