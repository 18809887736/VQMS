import request from '@/utils/request'

// 母线下拉（行带 vGrade，供电压等级级联过滤；v5.0 §10.1）
export function listBusbar() {
  return request({
    url: '/vqms/vqms_busbar/list',
    method: 'get'
  })
}

// 母线详情
export function getBusbar(busbarNum) {
  return request({
    url: '/vqms/vqms_busbar/' + busbarNum,
    method: 'get'
  })
}

// 新增母线
export function addBusbar(data) {
  return request({
    url: '/vqms/vqms_busbar',
    method: 'post',
    data: data
  })
}

// 修改母线
export function updateBusbar(data) {
  return request({
    url: '/vqms/vqms_busbar',
    method: 'put',
    data: data
  })
}

// 删除母线
export function delBusbar(busbarNum) {
  return request({
    url: '/vqms/vqms_busbar/' + busbarNum,
    method: 'delete'
  })
}

// 母线的阈值配置引用数（删除前预检）
export function thresholdCount(busbarNum) {
  return request({
    url: '/vqms/vqms_busbar/' + busbarNum + '/thresholdCount',
    method: 'get'
  })
}
