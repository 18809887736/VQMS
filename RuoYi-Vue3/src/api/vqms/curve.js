import request from '@/utils/request'

// 查询电压曲线（按时间范围 + 母线）
export function listCurve(query) {
  return request({
    url: '/vqms/curve/list',
    method: 'get',
    params: query
  })
}
