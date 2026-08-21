import request from '@/utils/request'

// 查询电压合格率月报列表
export function listMonthly(query) {
  return request({
    url: '/vqms/stats/monthly/list',
    method: 'get',
    params: query
  })
}
