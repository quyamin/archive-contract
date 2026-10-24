import request from './request'

export const getWarnings = (params) => request.get('/warnings', { params })
export const getUnreadCount = () => request.get('/warnings/unread-count')
export const markWarningRead = (id) => request.post(`/warnings/${id}/read`)
export const markAllWarningsRead = () => request.post('/warnings/read-all')
export const scanWarnings = () => request.post('/warnings/scan')
