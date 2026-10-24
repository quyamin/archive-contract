import request from './request'

export const getDashboard = () => request.get('/stats/dashboard')
export const getStats = (params) => request.get('/stats/overview', { params })
export const exportStats = (params) =>
  request.get('/stats/export', { params, responseType: 'blob' })
