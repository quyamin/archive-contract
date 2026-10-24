import request from './request'

export const getReviewList = (params) => request.get('/archive/reviews', { params })
export const reviewContract = (id, data) => request.post(`/archive/reviews/${id}`, data)
export const getChanges = (params) => request.get('/archive/changes', { params })
export const applyChange = (data) => request.post('/archive/changes', data)
export const reviewChange = (id, data) => request.post(`/archive/changes/${id}/review`, data)
