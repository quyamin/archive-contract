import request from './request'

export const getMyBorrows = (params) => request.get('/borrows/my', { params })
export const getBorrowApprovals = (params) => request.get('/borrows/approval', { params })
export const applyBorrow = (data) => request.post('/borrows', data)
export const reviewBorrow = (id, data) => request.post(`/borrows/${id}/review`, data)
export const returnBorrow = (id) => request.post(`/borrows/${id}/return`)
