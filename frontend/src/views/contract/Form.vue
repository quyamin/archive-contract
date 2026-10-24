<template>
  <div class="page-container" v-loading="loading">
    <div class="page-heading">
      <div>
        <div class="heading-back">
          <el-button link @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
          <el-tag v-if="isEdit" size="small" effect="plain">合同 ID {{ contractId }}</el-tag>
        </div>
        <h2 class="page-title">{{ isEdit ? '编辑合同' : '录入合同' }}</h2>
        <p class="page-desc">
          {{ isEdit ? '草稿或已驳回合同可继续完善，归档后仅能通过变更审批流程修改。' : '填写合同基础信息并保存草稿，随后上传合同附件并提交归档审核。' }}
        </p>
      </div>
      <div class="page-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button :loading="saving" @click="save(false)">
          <el-icon><DocumentAdd /></el-icon> 保存草稿
        </el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">
          <el-icon><Promotion /></el-icon> 保存并提交
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="!isEdit"
      type="info"
      :closable="false"
      show-icon
      title="提示：合同首次保存后将进入编辑页面，此时可上传合同原件、补充协议等附件，再提交归档审核。"
      style="margin-bottom: 16px"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right">
      <div class="panel-card form-card">
        <div class="section-title">基础信息</div>
        <el-row :gutter="26">
          <el-col :xs="24" :md="12" :xl="8">
            <el-form-item label="合同编号" prop="contractNo">
              <el-input v-model="form.contractNo" placeholder="如 HT-2026-0001" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12" :xl="16">
            <el-form-item label="合同名称" prop="contractName">
              <el-input v-model="form.contractName" placeholder="请输入合同名称" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="甲方" prop="partyA">
              <el-input v-model="form.partyA" placeholder="请输入甲方名称" maxlength="150" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="乙方" prop="partyB">
              <el-input v-model="form.partyB" placeholder="请输入乙方名称" maxlength="150" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="form.contractType" placeholder="请选择合同类型" style="width: 100%">
                <el-option v-for="item in typeOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="合同金额" prop="amount">
              <el-input-number
                v-model="form.amount"
                :min="0"
                :precision="2"
                :step="1000"
                :controls="false"
                placeholder="请输入金额"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="密级" prop="securityLevel">
              <el-select v-model="form.securityLevel" placeholder="请选择密级" style="width: 100%">
                <el-option v-for="item in securityOptions" :key="item.itemValue" :label="item.itemLabel" :value="item.itemValue" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="panel-card form-card">
        <div class="section-title">归属与期限</div>
        <el-row :gutter="26">
          <el-col :xs="24" :md="12">
            <el-form-item label="归属部门" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择归属部门" filterable style="width: 100%">
                <el-option v-for="item in deptOptions" :key="item.id" :label="item.deptName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="负责人" prop="ownerId">
              <el-select v-model="form.ownerId" placeholder="请选择合同负责人" filterable style="width: 100%">
                <el-option v-for="item in userOptions" :key="item.id" :label="`${item.name} · ${deptName(item.deptId)}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="签订日期" prop="signDate">
              <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" placeholder="选择签订日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="生效日期">
              <el-date-picker v-model="form.effectiveDate" type="date" value-format="YYYY-MM-DD" placeholder="选择生效日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="8">
            <el-form-item label="到期日期">
              <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" placeholder="选择到期日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注说明">
              <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="合同背景、特殊约定等补充说明" />
            </el-form-item>
          </el-col>
        </el-row>
      </div>

      <div class="panel-card form-card">
        <div class="section-title">付款节点</div>
        <el-alert
          v-if="!form.paymentNodes.length"
          type="info"
          :closable="false"
          title="可添加分期付款节点，系统将根据付款计划日期提前预警。"
          style="margin-bottom: 12px"
        />
        <el-table v-if="form.paymentNodes.length" :data="form.paymentNodes" size="small" class="payment-table">
          <el-table-column label="节点名称" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.nodeName" placeholder="如 预付款（30%）" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="计划付款日期" width="168">
            <template #default="{ row }">
              <el-date-picker v-model="row.planPayDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" size="small" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" width="160">
            <template #default="{ row }">
              <el-input-number v-model="row.amount" :min="0" :precision="2" :controls="false" size="small" placeholder="金额" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-select v-model="row.paidStatus" size="small" style="width: 100%">
                <el-option label="未支付" :value="0" />
                <el-option label="已支付" :value="1" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="付款说明" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="removeNode($index)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" plain size="small" style="margin-top: 10px" @click="addNode">
          <el-icon><Plus /></el-icon> 添加付款节点
        </el-button>
        <span v-if="paymentTotal > 0" class="payment-summary muted">
          节点合计 <b class="money">{{ formatMoney(paymentTotal) }}</b>
          <span v-if="form.amount && paymentTotal !== Number(form.amount)" class="text-danger">（与合同金额不一致）</span>
        </span>
      </div>

      <div class="panel-card form-card">
        <div class="section-title">合同附件</div>
        <template v-if="isEdit">
          <FileUploader
            ref="uploaderRef"
            :contract-id="contractId"
            :editable="true"
            :can-download="true"
            @change="attachmentCount = $event"
          />
        </template>
        <div v-else class="empty-tip">
          <el-icon size="30" color="#c4ccda"><FolderOpened /></el-icon>
          <div style="margin-top: 8px">请先保存合同草稿，保存后即可上传合同原件及补充材料。</div>
        </div>
      </div>
    </el-form>

    <div class="bottom-actions">
      <div class="muted">
        附件数量：<b>{{ attachmentCount }}</b> · 提交归档前至少需要上传 1 份附件
      </div>
      <div>
        <el-button @click="goBack">取消</el-button>
        <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">保存并提交</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getContract, saveContract, updateContract, submitContract } from '@/api/contract'
import { getOptionDepts, getOptionDict, getOptionUsers } from '@/api/system'
import { formatMoney } from '@/utils/format'
import FileUploader from '@/components/FileUploader.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const contractId = computed(() => Number(route.params.id || 0))
const isEdit = computed(() => contractId.value > 0)

const formRef = ref(null)
const uploaderRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const attachmentCount = ref(0)
const typeOptions = ref([])
const securityOptions = ref([])
const deptOptions = ref([])
const userOptions = ref([])

const form = reactive({
  contractNo: '',
  contractName: '',
  partyA: '星瀚科技有限公司',
  partyB: '',
  signDate: '',
  effectiveDate: '',
  expireDate: '',
  amount: null,
  contractType: '',
  deptId: null,
  ownerId: null,
  securityLevel: 'INTERNAL',
  remark: '',
  paymentNodes: []
})

const rules = {
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  partyA: [{ required: true, message: '请输入甲方名称', trigger: 'blur' }],
  partyB: [{ required: true, message: '请输入乙方名称', trigger: 'blur' }],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择归属部门', trigger: 'change' }],
  ownerId: [{ required: true, message: '请选择合同负责人', trigger: 'change' }],
  securityLevel: [{ required: true, message: '请选择密级', trigger: 'change' }]
}

const paymentTotal = computed(() =>
  form.paymentNodes.reduce((sum, item) => sum + Number(item.amount || 0), 0)
)

function deptName(id) {
  return deptOptions.value.find((item) => item.id === id)?.deptName || ''
}

function addNode() {
  form.paymentNodes.push({ nodeName: '', planPayDate: '', amount: null, paidStatus: 0, remark: '' })
}

function removeNode(index) {
  form.paymentNodes.splice(index, 1)
}

function goBack() {
  if (isEdit.value) router.push(`/contracts/${contractId.value}`)
  else router.push('/contracts')
}

async function loadOptions() {
  const [types, security, depts, users] = await Promise.all([
    getOptionDict('contract_type'),
    getOptionDict('security_level'),
    getOptionDepts(),
    getOptionUsers()
  ])
  typeOptions.value = types || []
  securityOptions.value = security || []
  deptOptions.value = depts || []
  userOptions.value = users || []
}

async function loadContract() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getContract(contractId.value)
    if (!data.canEdit) {
      ElMessage.warning('当前合同状态不允许编辑')
      router.replace(`/contracts/${contractId.value}`)
      return
    }
    Object.assign(form, {
      contractNo: data.contractNo,
      contractName: data.contractName,
      partyA: data.partyA,
      partyB: data.partyB,
      signDate: data.signDate || '',
      effectiveDate: data.effectiveDate || '',
      expireDate: data.expireDate || '',
      amount: data.amount,
      contractType: data.contractType,
      deptId: data.deptId,
      ownerId: data.ownerId,
      securityLevel: data.securityLevel,
      remark: data.remark || '',
      paymentNodes: (data.paymentNodes || []).map((node) => ({
        id: node.id,
        nodeName: node.nodeName,
        planPayDate: node.planPayDate,
        amount: Number(node.amount || 0),
        paidStatus: node.paidStatus ?? 0,
        remark: node.remark || ''
      }))
    })
    attachmentCount.value = data.attachmentCount || 0
  } finally {
    loading.value = false
  }
}

function buildPayload() {
  return {
    contractNo: form.contractNo.trim(),
    contractName: form.contractName.trim(),
    partyA: form.partyA.trim(),
    partyB: form.partyB.trim(),
    signDate: form.signDate,
    effectiveDate: form.effectiveDate || null,
    expireDate: form.expireDate || null,
    amount: form.amount,
    contractType: form.contractType,
    deptId: form.deptId,
    ownerId: form.ownerId,
    securityLevel: form.securityLevel,
    remark: form.remark,
    paymentNodes: form.paymentNodes
      .filter((node) => node.nodeName && node.planPayDate)
      .map((node) => ({
        id: node.id,
        nodeName: node.nodeName,
        planPayDate: node.planPayDate,
        amount: node.amount || 0,
        paidStatus: node.paidStatus ?? 0,
        remark: node.remark
      }))
  }
}

async function save(submit) {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先完善合同必填信息')
    return
  }
  if (form.effectiveDate && form.expireDate && form.effectiveDate > form.expireDate) {
    ElMessage.error('生效日期不能晚于到期日期')
    return
  }
  if (submit && !isEdit.value) {
    ElMessage.warning('请先保存合同草稿并上传附件后再提交归档')
    return
  }
  if (submit && attachmentCount.value === 0) {
    ElMessage.warning('请至少上传一份合同附件后再提交归档')
    return
  }
  if (submit) {
    try {
      await ElMessageBox.confirm(
        '提交后合同将进入归档审核，审核期间不可编辑。确认继续？',
        '提交归档审核',
        { type: 'warning', confirmButtonText: '确认提交', cancelButtonText: '再检查一下' }
      )
    } catch {
      return
    }
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) {
      await updateContract(contractId.value, payload)
    } else {
      const newId = await saveContract(payload)
      ElMessage.success('合同草稿保存成功，请上传附件')
      await router.replace(`/contracts/${newId}/edit?created=1`)
      return
    }
    ElMessage.success(submit ? '合同已提交归档审核' : '合同草稿保存成功')
    if (submit) {
      await submitContract(contractId.value)
      router.push(`/contracts/${contractId.value}`)
    } else {
      await loadContract()
    }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await loadContract()
  if (route.query.created && isEdit.value) {
    ElMessage.info('合同已创建，请上传合同附件后提交归档')
  }
})
</script>

<style scoped>
.heading-back {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.form-card {
  padding: 20px 24px 6px;
  margin-bottom: 16px;
}
.payment-table {
  margin-bottom: 4px;
}
.payment-summary {
  margin-left: 14px;
  font-size: 12px;
}
.bottom-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e7ebf3;
  box-shadow: 0 8px 30px rgba(30, 52, 97, 0.06);
  flex-wrap: wrap;
}
</style>
