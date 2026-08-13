import request from '@/utils/request'

// 查询母线阈值列表
export function listThreshold(query) {
  return request({
    url: '/vqms/threshold/list',
    method: 'get',
    params: query
  })
}

// 查询母线阈值详细
export function getThreshold(id) {
  return request({
    url: '/vqms/threshold/' + id,
    method: 'get'
  })
}

// 新增母线阈值
export function addThreshold(data) {
  return request({
    url: '/vqms/threshold',
    method: 'post',
    data: data
  })
}

// 修改母线阈值
export function updateThreshold(data) {
  return request({
    url: '/vqms/threshold',
    method: 'put',
    data: data
  })
}

// 删除母线阈值
export function delThreshold(id) {
  return request({
    url: '/vqms/threshold/' + id,
    method: 'delete'
  })
}
