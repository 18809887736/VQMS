import request from '@/utils/request'

// 母线下拉（行带 vGrade，供电压等级级联过滤；v5.0 §10.1）
export function listBusbar() {
  return request({
    url: '/vqms/vqms_busbar/list',
    method: 'get'
  })
}
