import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pinia } from '@/stores'
import { useUserStore } from '@/stores/user'

const Layout = () => import('@/layout/AppLayout.vue')

export const menuRoutes = [
  {
    path: '/dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '数据看板', perm: 'stats:view', icon: 'DataBoard' }
  },
  {
    path: '/contracts',
    component: () => import('@/views/contract/List.vue'),
    meta: { title: '合同台账', perm: 'contract:list', icon: 'Document' }
  },
  {
    path: '/contracts/new',
    component: () => import('@/views/contract/Form.vue'),
    meta: { title: '合同录入', perm: 'contract:add', activeMenu: '/contracts', hidden: true }
  },
  {
    path: '/contracts/:id/edit',
    component: () => import('@/views/contract/Form.vue'),
    meta: { title: '编辑合同', perm: 'contract:edit', activeMenu: '/contracts', hidden: true }
  },
  {
    path: '/contracts/:id',
    component: () => import('@/views/contract/Detail.vue'),
    meta: { title: '合同详情', perm: 'contract:view', activeMenu: '/contracts', hidden: true }
  },
  {
    path: '/archive/review',
    component: () => import('@/views/archive/Review.vue'),
    meta: { title: '归档审核', perm: 'archive:review', icon: 'Checked' }
  },
  {
    path: '/archive/changes',
    component: () => import('@/views/archive/Changes.vue'),
    meta: { title: '档案变更', perm: 'change:apply', icon: 'EditPen' }
  },
  {
    path: '/borrow/my',
    component: () => import('@/views/borrow/MyBorrow.vue'),
    meta: { title: '我的借阅', perm: 'borrow:apply', icon: 'Reading' }
  },
  {
    path: '/borrow/approval',
    component: () => import('@/views/borrow/Approval.vue'),
    meta: { title: '借阅审批', perm: 'borrow:approve', icon: 'Stamp' }
  },
  {
    path: '/warnings',
    component: () => import('@/views/warning/index.vue'),
    meta: { title: '风险预警', perm: 'warning:view', icon: 'Warning' }
  },
  {
    path: '/stats',
    component: () => import('@/views/stats/index.vue'),
    meta: { title: '统计分析', perm: 'stats:view', icon: 'TrendCharts' }
  },
  {
    path: '/ledger',
    component: () => import('@/views/ledger/index.vue'),
    meta: { title: '档案台账', perm: 'ledger:view', icon: 'Collection' }
  },
  {
    path: '/system/users',
    component: () => import('@/views/system/Users.vue'),
    meta: { title: '用户管理', perm: 'system:user:list', icon: 'User' }
  },
  {
    path: '/system/roles',
    component: () => import('@/views/system/Roles.vue'),
    meta: { title: '角色权限', perm: 'system:role:list', icon: 'Avatar' }
  },
  {
    path: '/system/depts',
    component: () => import('@/views/system/Depts.vue'),
    meta: { title: '组织机构', perm: 'system:dept:list', icon: 'OfficeBuilding' }
  },
  {
    path: '/system/dicts',
    component: () => import('@/views/system/Dicts.vue'),
    meta: { title: '字典配置', perm: 'system:dict:list', icon: 'Notebook' }
  },
  {
    path: '/system/config',
    component: () => import('@/views/system/Config.vue'),
    meta: { title: '系统参数', perm: 'system:config:list', icon: 'Tools' }
  },
  {
    path: '/system/logs',
    component: () => import('@/views/system/Logs.vue'),
    meta: { title: '操作日志', perm: 'system:log:list', icon: 'Tickets' }
  }
]

export function getLandingPath(store) {
  if (store.hasPerm('stats:view')) return '/dashboard'
  if (store.hasPerm('contract:list')) return '/contracts'
  if (store.hasPerm('ledger:view')) return '/ledger'
  if (store.hasPerm('warning:view')) return '/warnings'
  if (store.hasPerm('borrow:apply')) return '/borrow/my'
  return '/warnings'
}

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        redirect: () => {
          const store = useUserStore(pinia)
          if (!store.token) return '/login'
          return getLandingPath(store)
        }
      },
      ...menuRoutes
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  // BASE_URL 由 Vite 注入：独立部署为 /，war 部署到 /archive/ 时为 /archive/。
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const store = useUserStore(pinia)
  document.title = to.meta.title ? `${to.meta.title} - 档案合同管理` : '企业档案合同资料管理分析系统'
  if (to.meta.public) {
    if (store.token && to.path === '/login') {
      try {
        await store.ensureProfile()
        return getLandingPath(store)
      } catch {
        return true
      }
    }
    return true
  }
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  try {
    await store.ensureProfile()
  } catch {
    store.clear()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.perm && !store.hasPerm(to.meta.perm)) {
    ElMessage.warning('您没有访问该页面的权限')
    return getLandingPath(store)
  }
  return true
})

export default router
