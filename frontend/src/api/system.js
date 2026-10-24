import request from './request'

export const getUsers = (params) => request.get('/users', { params })
export const addUser = (data) => request.post('/users', data)
export const updateUser = (data) => request.put('/users', data)
export const resetUserPassword = (id, password) => request.put(`/users/${id}/password`, { password })
export const changeUserStatus = (id, status) =>
  request.put(`/users/${id}/status?status=${status}`)
export const deleteUser = (id) => request.delete(`/users/${id}`)

export const getRoles = () => request.get('/roles')
export const getPermissionTree = () => request.get('/roles/permissions')
export const saveRole = (data) => request.post('/roles', data)
export const deleteRole = (id) => request.delete(`/roles/${id}`)

export const getDeptTree = () => request.get('/depts/tree')
export const saveDept = (data) => request.post('/depts', data)
export const deleteDept = (id) => request.delete(`/depts/${id}`)

export const getDicts = () => request.get('/dicts')
export const saveDict = (data) => request.post('/dicts', data)
export const deleteDict = (id) => request.delete(`/dicts/${id}`)
export const saveDictItem = (data) => request.post('/dicts/items', data)
export const deleteDictItem = (id) => request.delete(`/dicts/items/${id}`)

export const getConfigs = () => request.get('/configs')
export const updateConfig = (data) => request.put('/configs', data)

export const getLogs = (params) => request.get('/logs', { params })

export const getOptionUsers = () => request.get('/options/users')
export const getOptionDepts = () => request.get('/options/depts')
export const getOptionDict = (code) => request.get(`/options/dict/${code}`)
export const getOptionRoles = () => request.get('/options/roles')
