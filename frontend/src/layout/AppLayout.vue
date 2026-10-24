<template>
  <div class="app-shell">
    <aside class="app-aside" :class="{ collapsed }">
      <div class="brand">
        <div class="brand-logo"><el-icon><Files /></el-icon></div>
        <div v-show="!collapseDesktop" class="brand-text">
          <div class="brand-title">档案合同管理</div>
          <div class="brand-subtitle">ARCHIVE · CONTRACT</div>
        </div>
      </div>
      <div class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapseDesktop"
          :collapse-transition="false"
          background-color="transparent"
          text-color="rgba(235,242,255,.7)"
          active-text-color="#ffffff"
          router
        >
          <template v-for="item in visibleMenus" :key="item.path || item.title">
            <el-menu-item v-if="!item.children" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
            <el-sub-menu v-else :index="item.title">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                <el-icon><component :is="child.icon" /></el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </div>
      <div class="sidebar-foot">企业档案合同资料管理分析系统 V1.0</div>
    </aside>

    <div class="app-main" :class="{ collapsed: collapseDesktop }">
      <header class="app-header">
        <div class="header-left">
          <button class="icon-button" @click="toggleCollapse" title="折叠菜单">
            <el-icon><Fold v-if="!collapseDesktop" /><Expand v-else /></el-icon>
          </button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>工作台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip content="预警消息" placement="bottom">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" class="notice-badge">
              <button class="icon-button" @click="goWarnings">
                <el-icon><Bell /></el-icon>
              </button>
            </el-badge>
          </el-tooltip>
          <div class="header-divider"></div>
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-trigger">
              <div class="user-avatar">{{ avatarText }}</div>
              <div class="user-meta">
                <div class="user-name">{{ userStore.displayName }}</div>
                <div class="user-role">{{ roleName }}</div>
              </div>
              <el-icon style="color: #98a2b3"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div style="line-height: 1.7">
                    <div style="font-weight: 600; color: #1f2a44">{{ userStore.profile?.realName }}</div>
                    <div style="font-size: 12px; color: #8b96ab">{{ userStore.profile?.deptName }} · {{ roleName }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="refresh">
                  <el-icon><Refresh /></el-icon> 刷新权限
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getUnreadCount } from '@/api/warning'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(window.innerWidth < 1180)
const unreadCount = ref(0)
let timer = null

const menuDefinitions = [
  { title: '数据看板', path: '/dashboard', icon: 'DataBoard', perm: 'stats:view' },
  { title: '合同档案', path: '/contracts', icon: 'Document', perm: 'contract:list' },
  {
    title: '归档管理',
    icon: 'FolderOpened',
    children: [
      { title: '归档审核', path: '/archive/review', icon: 'Checked', perm: 'archive:review' },
      { title: '档案变更', path: '/archive/changes', icon: 'EditPen', perm: 'change:apply' }
    ]
  },
  {
    title: '借阅管理',
    icon: 'Reading',
    children: [
      { title: '我的借阅', path: '/borrow/my', icon: 'Tickets', perm: 'borrow:apply' },
      { title: '借阅审批', path: '/borrow/approval', icon: 'Stamp', perm: 'borrow:approve' }
    ]
  },
  { title: '风险预警', path: '/warnings', icon: 'Warning', perm: 'warning:view' },
  { title: '统计分析', path: '/stats', icon: 'TrendCharts', perm: 'stats:view' },
  { title: '档案台账', path: '/ledger', icon: 'Collection', perm: 'ledger:view' },
  {
    title: '系统管理',
    icon: 'Setting',
    children: [
      { title: '用户管理', path: '/system/users', icon: 'User', perm: 'system:user:list' },
      { title: '角色权限', path: '/system/roles', icon: 'Avatar', perm: 'system:role:list' },
      { title: '组织机构', path: '/system/depts', icon: 'OfficeBuilding', perm: 'system:dept:list' },
      { title: '字典配置', path: '/system/dicts', icon: 'Notebook', perm: 'system:dict:list' },
      { title: '系统参数', path: '/system/config', icon: 'Tools', perm: 'system:config:list' },
      { title: '操作日志', path: '/system/logs', icon: 'Tickets', perm: 'system:log:list' }
    ]
  }
]

const visibleMenus = computed(() => {
  return menuDefinitions
    .map((item) => {
      if (item.children) {
        const children = item.children.filter((child) => userStore.hasPerm(child.perm))
        return children.length ? { ...item, children } : null
      }
      return userStore.hasPerm(item.perm) ? item : null
    })
    .filter(Boolean)
})

const activeMenu = computed(() => route.meta.activeMenu || route.path)
const currentTitle = computed(() => route.meta.title || '首页')
const roleName = computed(() => (userStore.profile?.roleNames || []).join(' / ') || '用户')
const avatarText = computed(() => (userStore.profile?.realName || 'U').slice(-2))
const isSmallScreen = computed(() => window.innerWidth < 900)
const collapseDesktop = computed(() => collapsed.value || isSmallScreen.value)

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function goWarnings() {
  if (userStore.hasPerm('warning:view')) router.push('/warnings')
}

async function loadUnread() {
  if (!userStore.hasPerm('warning:view')) return
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    unreadCount.value = 0
  }
}

async function handleCommand(command) {
  if (command === 'refresh') {
    await userStore.ensureProfile(true)
    ElMessage.success('权限信息已刷新')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消'
      })
      await userStore.logout()
      router.replace('/login')
    } catch {
      // cancelled
    }
  }
}

function handleResize() {
  if (window.innerWidth < 900) collapsed.value = true
}

function onWarningsUpdated() {
  loadUnread()
}

onMounted(() => {
  loadUnread()
  timer = window.setInterval(loadUnread, 60000)
  window.addEventListener('warnings-updated', onWarningsUpdated)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
  window.removeEventListener('warnings-updated', onWarningsUpdated)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.16s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.notice-badge :deep(.el-badge__content) {
  top: 5px;
  right: 8px;
}
</style>
