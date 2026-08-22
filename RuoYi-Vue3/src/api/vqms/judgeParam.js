import request from '@/utils/request'

// 判定整定参数（v5.0 §6.2.5 / §10.1）
export function listJudgeParam() {
  return request({
    url: '/vqms/judgeParam/list',
    method: 'get'
  })
}

export function addJudgeParam(data) {
  return request({
    url: '/vqms/judgeParam',
    method: 'post',
    data: data
  })
}

export function updateJudgeParam(data) {
  return request({
    url: '/vqms/judgeParam',
    method: 'put',
    data: data
  })
}

export function delJudgeParam(id) {
  return request({
    url: '/vqms/judgeParam/' + id,
    method: 'delete'
  })
}
