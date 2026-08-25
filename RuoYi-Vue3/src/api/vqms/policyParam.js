import request from '@/utils/request'

// 策略参数页（v5.0 §8.7）：当前四键原值行
export function listPolicyParam() {
  return request({
    url: '/vqms/policyParam/list',
    method: 'get'
  })
}

// 页面三态状态：未选套 / 已选套
export function getPolicyState() {
  return request({
    url: '/vqms/policyParam/state',
    method: 'get'
  })
}

// 选套应用：{ presetCode, thresholdPct? }——唯一写路径，整组 upsert 四约定键
export function applyPreset(data) {
  return request({
    url: '/vqms/policyParam/apply',
    method: 'post',
    data: data
  })
}

// 戊·自由组合应用（策略文档 §3.3）：{ rules: ["A1 -> EXCLUDE_REPORTED", ...], thresholdPct? }
// 规则行有序、首中即断；校验 fail-fast，整体拒绝时原生效策略不变
export function applyFreeform(data) {
  return request({
    url: '/vqms/policyParam/applyFreeform',
    method: 'post',
    data: data
  })
}
