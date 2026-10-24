import request from './request'

export const getLedger = (params) => request.get('/ledger/contracts', { params })
export const exportLedger = (params) =>
  request.get('/ledger/export', { params, responseType: 'blob' })
