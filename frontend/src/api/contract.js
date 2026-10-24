import request from './request'

export const getContracts = (params) => request.get('/contracts', { params })
export const getContract = (id) => request.get(`/contracts/${id}`)
export const saveContract = (data) => request.post('/contracts', data)
export const updateContract = (id, data) => request.put(`/contracts/${id}`, data)
export const submitContract = (id) => request.post(`/contracts/${id}/submit`)
export const deleteContract = (id) => request.delete(`/contracts/${id}`)
export const importContracts = (formData) =>
  request.post('/contracts/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const downloadImportTemplate = () =>
  request.get('/contracts/import-template', { responseType: 'blob' })
export const exportContracts = (params) =>
  request.get('/contracts/export', { params, responseType: 'blob' })
