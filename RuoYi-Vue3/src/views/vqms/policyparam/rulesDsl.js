// 戊·自由组合 规则行 DSL ↔ 结构化模型 互转（前端侧，与后端 FreeformPolicyParser 语法对齐）
// 语法：expr := operand (OP operand)*（同层单一连接词）；operand := ['!'] atom | ['!'] '(' inner ')'；
//       inner := ['!'] atom (OP ['!'] atom)*；规则行 := expr ' -> ' ACTION
// 模型：rule = { join:'AND'|'OR', terms:[term], action }
//        term = { kind:'atom', atom, neg } | { kind:'group', neg, join, atoms:[{atom,neg}] }

export const ATOM_OPTIONS = [
  { value: 'A1', label: 'A1 解码失败（总）' },
  { value: 'A1A', label: 'A1A 解码失败·编码脏写' },
  { value: 'A1B', label: 'A1B 解码失败·循环码非法' },
  { value: 'A1C', label: 'A1C 解码失败·缺 t₀ 电压' },
  { value: 'A2', label: 'A2 档不可判（全缺/L>H）' },
  { value: 'A3', label: 'A3 窗口数据不完整' },
  { value: 'A4', label: 'A4 可用度低于阈值 τ' }
]

export const ACTION_OPTIONS = [
  { value: 'COUNT_NORMAL', label: '正常记账（用剩余）' },
  { value: 'EXCLUDE_REPORTED', label: '剔除分母 + 计数上报' },
  { value: 'COUNT_UNQUALIFIED', label: '计不合格 + 计数' },
  { value: 'PEND_MARKED', label: '挂起标记（人工后审）' }
]

// 与后端 FreeformPolicyParser 对齐：仅跳过 ASCII 空白与全角空格（NBSP/FEFF 等粘贴污染物按非法字符报错）
function isSkippable (c) {
  return c === ' ' || c === '\t' || c === '\r' || c === '\n' || c === '　'
}

function tokenize (text) {
  const tokens = []
  let i = 0
  while (i < text.length) {
    const c = text[i]
    if (isSkippable(c)) { i++; continue }
    if ('!&|()'.includes(c)) { tokens.push({ t: c }); i++; continue }
    if (/[A-Za-z]/.test(c)) {
      let j = i
      while (j < text.length && /[A-Za-z0-9]/.test(text[j])) j++
      const v = text.slice(i, j).toUpperCase()
      if (v === 'A5') throw new Error('原子 A5（免考旗读取失败）不进入本规则表——属阶段三免考后置子表，随 §6.5 拍板另设')
      if (!ATOM_OPTIONS.some(o => o.value === v)) throw new Error('未知原子 "' + v + '"（合法: A1/A1A/A1B/A1C/A2/A3/A4）')
      tokens.push({ t: 'ATOM', v })
      i = j
      continue
    }
    const code = text.codePointAt(i).toString(16).toUpperCase()
    throw new Error('非法字符 "' + c + '"（U+' + code.padStart(4, '0') + '，疑似粘贴污染？合法: 原子名 与 ! & | ( ) ->）')
  }
  return tokens
}

function parseInner (tokens, pos) {
  // 括号内：平坦 取反?原子 序列 + 单一连接词
  const atoms = []
  let join = null
  for (;;) {
    if (pos >= tokens.length) throw new Error('括号未闭合')
    let neg = false
    if (tokens[pos].t === '!') { neg = true; pos++ }
    if (pos >= tokens.length || tokens[pos].t !== 'ATOM') throw new Error('括号内期望原子')
    atoms.push({ atom: tokens[pos].v, neg })
    pos++
    if (pos >= tokens.length || tokens[pos].t === ')') break
    if (tokens[pos].t !== '&' && tokens[pos].t !== '|') throw new Error('括号内期望连接词 & 或 |')
    const thisJoin = tokens[pos].t === '&' ? 'AND' : 'OR'
    if (join === null) join = thisJoin
    else if (join !== thisJoin) throw new Error('括号内 AND/OR 混用须拆分')
    pos++
  }
  return { atoms, join: join || 'AND', pos }
}

export function parseExpression (text) {
  const tokens = tokenize(text)
  if (tokens.length === 0) throw new Error('表达式不可为空')
  const terms = []
  let join = null
  let pos = 0
  for (;;) {
    let neg = false
    if (pos < tokens.length && tokens[pos].t === '!') { neg = true; pos++ }
    if (pos >= tokens.length) throw new Error('表达式意外结束')
    if (tokens[pos].t === '(') {
      const inner = parseInner(tokens, pos + 1)
      if (inner.pos >= tokens.length || tokens[inner.pos].t !== ')') throw new Error('括号未闭合')
      terms.push({ kind: 'group', neg, join: inner.join, atoms: inner.atoms })
      pos = inner.pos + 1
    } else if (tokens[pos].t === 'ATOM') {
      terms.push({ kind: 'atom', atom: tokens[pos].v, neg })
      pos++
    } else {
      throw new Error('期望原子或 "("，实得 "' + (tokens[pos].v || tokens[pos].t) + '"')
    }
    if (pos >= tokens.length) break
    if (tokens[pos].t !== '&' && tokens[pos].t !== '|') throw new Error('期望连接词 & 或 |')
    const thisJoin = tokens[pos].t === '&' ? 'AND' : 'OR'
    if (join === null) join = thisJoin
    else if (join !== thisJoin) throw new Error('同层 AND/OR 混用须经一层括号分组')
    pos++
  }
  return { join: join || 'AND', terms }
}

export const MAX_RULES = 16

export function parseRuleLine (line) {
  const text = (line || '').trim()
  const arrow = text.indexOf('->')
  if (arrow < 0) throw new Error('缺少 "->" 处置动作段')
  if (text.indexOf('->', arrow + 2) >= 0) throw new Error('多个 "->"，规则行只能是 表达式->动作')
  const action = text.slice(arrow + 2).trim().toUpperCase()
  if (!ACTION_OPTIONS.some(o => o.value === action)) throw new Error('非法处置动作 "' + action + '"')
  const expr = parseExpression(text.slice(0, arrow))
  return { ...expr, action }
}

function termToText (term) {
  if (term.kind === 'atom') {
    return (term.neg ? '!' : '') + term.atom
  }
  const inner = term.atoms
    .map(a => (a.neg ? '!' : '') + a.atom)
    .join(term.join === 'AND' ? ' & ' : ' | ')
  return (term.neg ? '!' : '') + '(' + inner + ')'
}

export function serializeRule (rule) {
  const expr = rule.terms.map(termToText).join(rule.join === 'AND' ? ' & ' : ' | ')
  return expr + ' -> ' + rule.action
}

export function refsOfRule (rule) {
  const set = new Set()
  for (const t of rule.terms) {
    if (t.kind === 'atom') set.add(t.atom)
    else t.atoms.forEach(a => set.add(a.atom))
  }
  return set
}

export function newRule () {
  return { join: 'AND', terms: [{ kind: 'atom', atom: 'A1', neg: false }], action: 'EXCLUDE_REPORTED' }
}

// 与后端 FreeformPolicyValidator 对齐的客户端预检（服务端仍为最终权威）：
// 去重键 = 仅表达式（后端按 expressionText() 去重，同表达式不同动作亦视为重复——首中即断下后者不可达）
export function validateRulesClient (rules) {
  const errors = []
  if (!rules.length) errors.push('规则表至少一条')
  if (rules.length > MAX_RULES) errors.push('规则数超上限 ' + MAX_RULES)
  const seen = new Set()
  rules.forEach((r, i) => {
    const no = i + 1
    if (!r.terms.length) { errors.push('规则 ' + no + '：至少一个条件项'); return }
    for (const t of r.terms) {
      if (t.kind === 'group' && !t.atoms.length) errors.push('规则 ' + no + '：括号组不可为空')
    }
    const exprText = serializeRule(r).replace(/\s*->\s*.*$/, '')
    if (seen.has(exprText)) errors.push('规则 ' + no + '：与先前规则表达式完全相同（同表达式不同动作亦视为重复）')
    seen.add(exprText)
    if (serializeRule(r).length > 255) {
      errors.push('规则 ' + no + '：超存储上限 255 字符，请精简条件项')
    }
    if (r.action === 'COUNT_NORMAL' && !refsOfRule(r).has('A3')) {
      errors.push('规则 ' + no + '：「正常记账（用剩余）」仅当触发含 A3 时可选')
    }
  })
  return errors
}
