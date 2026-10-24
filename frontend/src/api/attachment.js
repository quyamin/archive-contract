import request from './request'

export const getAttachments = (contractId) => request.get(`/attachments/contract/${contractId}`)
export const uploadAttachments = (formData) =>
  request.post('/attachments/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
export const deleteAttachment = (id) => request.delete(`/attachments/${id}`)
export const previewAttachment = (id) =>
  request.get(`/attachments/${id}/preview`, { responseType: 'blob' })
export const downloadAttachment = (id) =>
  request.get(`/attachments/${id}/download`, { responseType: 'blob' })
