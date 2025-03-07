import request from '@/utils/request'

// 查询所有角色
export function listAllRole() {
  return request({
    url: '/role/listAllRole',
    method: 'get'
  })
}

// 查询角色列表
export function listRole(query) {
  return request({
    url: '/role/list',
    method: 'get',
    params: query
  })
}
// 查询角色详细
export function getRole(roleId) {
  return request({
    url: '/role/' + roleId,
    method: 'get'
  })
}
// 修改角色
export function updateRole(data) {
  return request({
    url: '/role',
    method: 'put',
    data: data
  })
}
// 删除角色
export function delRole(roleId) {
  return request({
    url: '/role/' + roleId,
    method: 'delete'
  })
}
// 新增角色
export function addRole(data) {
  return request({
    url: '/role',
    method: 'post',
    data: data
  })
}
// 角色状态修改
export function changeRoleStatus(roleId, status) {
  const data = {
    roleId,
    status
  }
  return request({
    url: '/role/changeStatus',
    method: 'put',
    data: data
  })
}
