<template>
  <div class="login-page">
    <div class="login-visual">
      <div class="visual-inner">
        <div class="visual-brand">
          <div class="visual-logo"><el-icon><Files /></el-icon></div>
          <div>
            <div class="visual-title">档案合同资料管理分析系统</div>
            <div class="visual-sub">ARCHIVE & CONTRACT MANAGEMENT · V1.0</div>
          </div>
        </div>

        <div class="visual-copy">
          <div class="eyebrow">让每一份档案，都有迹可循</div>
          <h1>合同全生命周期<br />数字化管理平台</h1>
          <p>从合同录入、附件归集、归档审核，到借阅管控、风险预警与经营分析，构建企业档案资料的可信闭环。</p>
        </div>

        <div class="visual-features">
          <div class="feature-item">
            <el-icon><CircleCheck /></el-icon>
            <div>
              <div class="feature-title">归档不可篡改</div>
              <div class="feature-desc">唯一档案号 · 变更留痕 · 逻辑删除</div>
            </div>
          </div>
          <div class="feature-item">
            <el-icon><Lock /></el-icon>
            <div>
              <div class="feature-title">权限精细管控</div>
              <div class="feature-desc">角色 · 按钮 · 数据范围三重隔离</div>
            </div>
          </div>
          <div class="feature-item">
            <el-icon><Bell /></el-icon>
            <div>
              <div class="feature-title">风险主动预警</div>
              <div class="feature-desc">到期提醒 · 付款节点 · 定时扫描</div>
            </div>
          </div>
        </div>

        <div class="visual-foot">© 2026 星瀚科技有限公司 · 企业档案数字化中心</div>
      </div>
      <div class="visual-glow glow-1"></div>
      <div class="visual-glow glow-2"></div>
    </div>

    <div class="login-form-side">
      <div class="login-card">
        <div class="mobile-brand">
          <div class="visual-logo"><el-icon><Files /></el-icon></div>
          <span>档案合同管理</span>
        </div>
        <h2>欢迎登录</h2>
        <p class="login-tip">请使用企业账号进入档案合同工作台</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleLogin">
          <el-form-item label="登录账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入账号" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              placeholder="请输入密码"
              show-password
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
            {{ loading ? '正在验证...' : '登 录' }}
          </el-button>
        </el-form>

        <div class="register-entry">
          <span>还没有账号？</span>
          <el-link type="primary" :underline="false" @click="openRegister">立即注册</el-link>
        </div>

        <div class="demo-block">
          <div class="demo-title">
            <span>演示账号</span>
            <span class="muted">点击即可填入</span>
          </div>
          <div class="demo-grid">
            <button v-for="item in demoAccounts" :key="item.username" class="demo-item" @click="fillDemo(item)">
              <div class="demo-role">{{ item.role }}</div>
              <div class="demo-cred">{{ item.username }} / {{ item.password }}</div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="registerVisible"
      title="注册普通用户"
      width="480px"
      class="register-dialog"
      append-to-body
      destroy-on-close
    >
      <div class="register-hint">
        注册后默认为「普通查阅人员」，可查阅已归档合同、发起借阅申请并查看档案台账；如需其他权限，请联系管理员调整。
      </div>
      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        @keyup.enter="handleRegister"
      >
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="registerForm.username" size="large" maxlength="30" placeholder="字母开头，4-30 位字母、数字或下划线" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" size="large" maxlength="50" placeholder="请输入真实姓名" clearable>
            <template #prefix><el-icon><Avatar /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="手机号（选填）" prop="phone">
          <el-input v-model="registerForm.phone" size="large" maxlength="11" placeholder="用于接收借阅与到期提醒" clearable>
            <template #prefix><el-icon><Phone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="邮箱（选填）" prop="email">
          <el-input v-model="registerForm.email" size="large" maxlength="100" placeholder="请输入常用邮箱" clearable>
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="registerForm.password" size="large" type="password" maxlength="32" placeholder="6-32 位密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" size="large" type="password" maxlength="32" placeholder="请再次输入密码" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="large" @click="registerVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="registerLoading" @click="handleRegister">
          注册并登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getLandingPath } from '@/router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

const registerVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]{3,29}$/,
      message: '账号需以字母开头，长度为 4-30 位，可包含字母、数字和下划线',
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度需为 2-50 个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^$|^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
        } else if (value !== registerForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function openRegister() {
  Object.assign(registerForm, {
    username: '',
    realName: '',
    phone: '',
    email: '',
    password: '',
    confirmPassword: ''
  })
  registerVisible.value = true
  nextTick(() => registerFormRef.value?.clearValidate())
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  registerLoading.value = true
  try {
    await userStore.register({
      username: registerForm.username.trim(),
      realName: registerForm.realName.trim(),
      phone: registerForm.phone.trim(),
      email: registerForm.email.trim(),
      password: registerForm.password
    })
    registerVisible.value = false
    ElMessage.success('注册成功，已自动登录')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || getLandingPath(userStore))
  } finally {
    registerLoading.value = false
  }
}

const demoAccounts = [
  { role: '系统管理员', username: 'admin', password: 'admin123' },
  { role: '档案管理员', username: 'archivist', password: 'archive123' },
  { role: '业务录入员', username: 'entry', password: 'entry123' },
  { role: '普通查阅人员', username: 'viewer', password: 'viewer123' }
]

function fillDemo(item) {
  form.username = item.username
  form.password = item.password
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${userStore.displayName}`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    await router.replace(redirect || getLandingPath(userStore))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(440px, 0.95fr);
}

.login-visual {
  position: relative;
  background:
    radial-gradient(1100px 600px at 8% 12%, rgba(47, 107, 255, 0.24), transparent 60%),
    linear-gradient(150deg, #0b1428 0%, #101f3d 46%, #0d1730 100%);
  color: #fff;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 56px 64px;
}

.visual-inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 620px;
  margin: 0 auto;
}

.visual-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 72px;
}

.visual-logo {
  width: 45px;
  height: 45px;
  border-radius: 12px;
  background: linear-gradient(135deg, #5d8cff, #2f6bff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  box-shadow: 0 12px 28px rgba(47, 107, 255, 0.45);
  flex: 0 0 45px;
}

.visual-title {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 1px;
}

.visual-sub {
  font-size: 9.5px;
  letter-spacing: 1.5px;
  color: rgba(255, 255, 255, 0.45);
  margin-top: 5px;
}

.visual-copy .eyebrow {
  font-size: 12px;
  letter-spacing: 3px;
  color: #8fb4ff;
  margin-bottom: 18px;
}

.visual-copy h1 {
  font-size: 40px;
  line-height: 1.32;
  margin: 0 0 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.visual-copy p {
  font-size: 13.5px;
  line-height: 1.95;
  color: rgba(230, 238, 255, 0.6);
  max-width: 470px;
  margin: 0;
}

.visual-features {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 56px;
}

.feature-item {
  padding: 16px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  gap: 10px;
  backdrop-filter: blur(6px);
}

.feature-item .el-icon {
  color: #7ea7ff;
  font-size: 17px;
  margin-top: 1px;
}

.feature-title {
  font-size: 12.5px;
  font-weight: 600;
}

.feature-desc {
  font-size: 10.5px;
  color: rgba(255, 255, 255, 0.42);
  margin-top: 6px;
  line-height: 1.5;
}

.visual-foot {
  margin-top: 70px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.28);
}

.visual-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.glow-1 {
  width: 520px;
  height: 520px;
  right: -180px;
  bottom: -220px;
  background: radial-gradient(circle, rgba(47, 107, 255, 0.3), transparent 65%);
}

.glow-2 {
  width: 300px;
  height: 300px;
  left: -120px;
  top: 36%;
  background: radial-gradient(circle, rgba(93, 140, 255, 0.18), transparent 65%);
}

.login-form-side {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 42px 32px;
}

.login-card {
  width: 100%;
  max-width: 390px;
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  margin-bottom: 26px;
}

.mobile-brand .visual-logo {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  font-size: 18px;
}

.login-card h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
}

.login-tip {
  color: #8b96ab;
  font-size: 13px;
  margin: 10px 0 30px;
}

.login-button {
  width: 100%;
  margin-top: 6px;
  height: 46px;
  font-size: 15px;
  letter-spacing: 4px;
}

.register-entry {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #8b96ab;
}

.register-hint {
  margin-bottom: 18px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f3f7ff;
  color: #526079;
  font-size: 12px;
  line-height: 1.7;
}

.register-dialog :deep(.el-dialog) {
  max-width: 92vw;
}

.register-dialog :deep(.el-form-item) {
  margin-bottom: 16px;
}

.demo-block {
  margin-top: 34px;
  padding-top: 22px;
  border-top: 1px dashed #e7ebf3;
}

.demo-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
  color: #526079;
  margin-bottom: 12px;
}

.demo-title .muted {
  font-weight: 400;
  font-size: 11px;
}

.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}

.demo-item {
  text-align: left;
  padding: 10px 12px;
  border-radius: 9px;
  border: 1px solid #e7ebf3;
  background: #fafbff;
  cursor: pointer;
  transition: all 0.16s;
}

.demo-item:hover {
  border-color: #b9cdff;
  background: #f3f7ff;
  transform: translateY(-1px);
}

.demo-role {
  font-size: 12px;
  font-weight: 600;
  color: #1f2a44;
}

.demo-cred {
  font-size: 10.5px;
  color: #8b96ab;
  margin-top: 4px;
  font-family: "Roboto Mono", monospace;
}

@media (max-width: 1000px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-visual {
    display: none;
  }
  .mobile-brand {
    display: flex;
  }
  .login-form-side {
    min-height: 100vh;
    background: linear-gradient(180deg, #f7faff, #fff 30%);
  }
}
</style>
