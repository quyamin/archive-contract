import dayjs from 'dayjs'

export const formatMoney = (value, withSymbol = true) => {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  const text = num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return withSymbol ? `¥ ${text}` : text
}

export const formatCompactMoney = (value) => {
  const num = Number(value || 0)
  if (num >= 100000000) return `¥${(num / 100000000).toFixed(2)}亿`
  if (num >= 10000) return `¥${(num / 10000).toFixed(2)}万`
  return `¥${num.toLocaleString('zh-CN')}`
}

export const formatDate = (value) => (value ? dayjs(value).format('YYYY-MM-DD') : '-')
export const formatDateTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

export const fileSize = (bytes) => {
  const size = Number(bytes || 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export const STATUS_META = {
  DRAFT: { label: '待归档', type: 'info', color: '#909399' },
  PENDING: { label: '审核中', type: 'warning', color: '#e6a23c' },
  REJECTED: { label: '已驳回', type: 'danger', color: '#f56c6c' },
  ARCHIVED: { label: '已归档', type: 'success', color: '#20b26c' },
  EXPIRED: { label: '已到期', type: 'danger', color: '#7f8c8d' }
}

export const BORROW_STATUS_META = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  RETURNED: { label: '已归还', type: 'info' },
  EXPIRED: { label: '已到期', type: 'danger' }
}

export const CHANGE_STATUS_META = {
  PENDING: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' }
}

export const FILE_TYPE_META = {
  ORIGINAL: { label: '合同原件', type: 'primary' },
  SUPPLEMENT: { label: '补充协议', type: 'success' },
  APPROVAL: { label: '审批单', type: 'warning' },
  ACCEPTANCE: { label: '验收材料', type: 'info' },
  OTHER: { label: '其他附件', type: '' }
}

export const shorten = (text, length = 20) => {
  if (!text) return '-'
  return text.length > length ? `${text.slice(0, length)}…` : text
}
