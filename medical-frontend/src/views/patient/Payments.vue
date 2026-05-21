<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Wallet, View } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { listByUserId2, getBillById } from '@/services/medical/zhangdanguanli'
import { createPayment, listByBillId, pay } from '@/services/medical/zhifuguanli'

const authStore = useAuthStore()

const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const bills = ref<API.BillVO[]>([])
const activeTab = ref<'all' | 'UNPAID' | 'PAID'>('all')
const detailVisible = ref(false)
const detailBill = ref<API.BillVO | null>(null)

const statusMap: Record<string, { label: string; type: string }> = {
  UNPAID: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'success' },
  REFUNDED: { label: '已退款', type: 'info' },
  CANCELLED: { label: '已取消', type: 'info' },
}

const itemTypeMap: Record<string, string> = {
  PRESCRIPTION: '处方药品',
  REGISTRATION: '挂号费',
  EXAMINATION: '检查费',
  TREATMENT: '诊疗费',
}

const filteredBills = computed(() => {
  if (activeTab.value === 'all') return bills.value
  return bills.value.filter((b) => b.status === activeTab.value)
})

const unpaidCount = computed(() => bills.value.filter((b) => b.status === 'UNPAID').length)

const formatMoney = (val?: number) => {
  if (val == null || Number.isNaN(val)) return '0.00'
  return Number(val).toFixed(2)
}

const formatDate = (val?: string) => {
  if (!val) return '-'
  return val.replace('T', ' ').slice(0, 16)
}

const loadBills = async () => {
  if (!authStore.userInfo?.id) return
  loading.value = true
  try {
    const res = await listByUserId2({ userId: authStore.userInfo.id })
    bills.value = res.data ?? []
  } catch {
    ElMessage.error('加载账单失败')
  } finally {
    loading.value = false
  }
}

const openDetail = async (bill: API.BillVO) => {
  if (!bill.id) return
  detailVisible.value = true
  detailLoading.value = true
  detailBill.value = bill
  try {
    const res = await getBillById({ id: bill.id })
    if (res.data) detailBill.value = res.data
  } catch {
    ElMessage.error('加载账单详情失败')
  } finally {
    detailLoading.value = false
  }
}

const resolvePendingPaymentId = async (bill: API.BillVO): Promise<number | null> => {
  if (!bill.id) return null
  const listRes = await listByBillId({ billId: bill.id })
  const pending = listRes.data?.find((p) => p.status === 0)
  if (pending?.id) return pending.id

  const createRes = await createPayment({
    billId: bill.id,
    amount: bill.selfPayAmount,
    paymentType: 'WECHAT',
  })
  return createRes.data?.id ?? null
}

const handlePay = async (bill: API.BillVO) => {
  if (!bill.id || bill.status !== 'UNPAID') return
  submitting.value = true
  try {
    const paymentId = await resolvePendingPaymentId(bill)
    if (!paymentId) {
      ElMessage.error('无法创建支付订单')
      return
    }
    await pay({ paymentId })
    ElMessage.success('支付成功')
    detailVisible.value = false
    await loadBills()
  } catch {
    ElMessage.error('支付失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadBills)
</script>

<template>
  <div class="payments-page">
    <div class="page-banner">
      <div class="banner-text">
        <h1>我的账单</h1>
        <p>发药后将自动生成待支付账单，支付完成后本次就诊费用结清</p>
      </div>
      <div v-if="unpaidCount > 0" class="banner-badge" aria-label="待支付账单数量">
        {{ unpaidCount }} 笔待支付
      </div>
    </div>

    <div class="filter-tabs" role="tablist" aria-label="账单筛选">
      <button
        type="button"
        class="filter-tab"
        :class="{ active: activeTab === 'all' }"
        @click="activeTab = 'all'"
      >
        全部
      </button>
      <button
        type="button"
        class="filter-tab"
        :class="{ active: activeTab === 'UNPAID' }"
        @click="activeTab = 'UNPAID'"
      >
        待支付
        <span v-if="unpaidCount" class="tab-count">{{ unpaidCount }}</span>
      </button>
      <button
        type="button"
        class="filter-tab"
        :class="{ active: activeTab === 'PAID' }"
        @click="activeTab = 'PAID'"
      >
        已支付
      </button>
    </div>

    <div class="bill-list" v-loading="loading">
      <article
        v-for="bill in filteredBills"
        :key="bill.id"
        class="bill-card"
      >
        <div class="bill-card-header">
          <div class="bill-no">
            <el-icon :size="18" aria-hidden="true"><Document /></el-icon>
            <span>{{ bill.billNo }}</span>
          </div>
          <el-tag :type="(statusMap[bill.status ?? '']?.type as any) || 'info'" size="small">
            {{ statusMap[bill.status ?? '']?.label ?? bill.status }}
          </el-tag>
        </div>

        <div class="bill-card-body">
          <div class="bill-amount">
            <span class="label">应付金额</span>
            <span class="value">¥{{ formatMoney(bill.selfPayAmount) }}</span>
          </div>
          <div class="bill-meta">
            <span>预约号：{{ bill.appointmentId ?? '-' }}</span>
            <span>{{ formatDate(bill.createTime) }}</span>
          </div>
        </div>

        <div class="bill-card-actions">
          <el-button
            type="primary"
            plain
            :icon="View"
            class="action-btn"
            @click="openDetail(bill)"
          >
            查看详情
          </el-button>
          <el-button
            v-if="bill.status === 'UNPAID'"
            type="primary"
            :icon="Wallet"
            class="action-btn pay-btn"
            :loading="submitting"
            @click="handlePay(bill)"
          >
            去支付
          </el-button>
        </div>
      </article>

      <el-empty
        v-if="!loading && filteredBills.length === 0"
        :description="activeTab === 'UNPAID' ? '暂无待支付账单' : '暂无账单记录'"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="账单详情"
      size="480px"
      direction="rtl"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-panel">
        <template v-if="detailBill">
          <section class="detail-summary">
            <div class="summary-row">
              <span class="summary-label">账单编号</span>
              <span class="summary-value">{{ detailBill.billNo }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">状态</span>
              <el-tag :type="(statusMap[detailBill.status ?? '']?.type as any) || 'info'" size="small">
                {{ statusMap[detailBill.status ?? '']?.label ?? detailBill.status }}
              </el-tag>
            </div>
            <div class="summary-row">
              <span class="summary-label">预约号</span>
              <span class="summary-value">{{ detailBill.appointmentId ?? '-' }}</span>
            </div>
            <div class="summary-row">
              <span class="summary-label">创建时间</span>
              <span class="summary-value">{{ formatDate(detailBill.createTime) }}</span>
            </div>
            <div v-if="detailBill.payTime" class="summary-row">
              <span class="summary-label">支付时间</span>
              <span class="summary-value">{{ formatDate(detailBill.payTime) }}</span>
            </div>
          </section>

          <section class="detail-fees">
            <h3>费用明细</h3>
            <el-table
              :data="detailBill.feeItems ?? []"
              size="small"
              stripe
              empty-text="暂无费用项"
            >
              <el-table-column label="项目" min-width="120">
                <template #default="{ row }">
                  <div class="fee-name">{{ row.itemName }}</div>
                  <div class="fee-type">{{ itemTypeMap[row.itemType ?? ''] ?? row.itemType }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="64" align="center" />
              <el-table-column label="单价" width="88" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.unitPrice) }}</template>
              </el-table-column>
              <el-table-column label="小计" width="88" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.selfPayAmount ?? row.totalAmount) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section class="detail-totals">
            <div class="total-row">
              <span>费用合计</span>
              <span>¥{{ formatMoney(detailBill.totalAmount) }}</span>
            </div>
            <div v-if="detailBill.discountAmount" class="total-row muted">
              <span>优惠</span>
              <span>-¥{{ formatMoney(detailBill.discountAmount) }}</span>
            </div>
            <div v-if="detailBill.insuranceAmount" class="total-row muted">
              <span>医保</span>
              <span>-¥{{ formatMoney(detailBill.insuranceAmount) }}</span>
            </div>
            <div class="total-row highlight">
              <span>自付应付</span>
              <span>¥{{ formatMoney(detailBill.selfPayAmount) }}</span>
            </div>
          </section>

          <div v-if="detailBill.status === 'UNPAID'" class="detail-pay">
            <el-button
              type="primary"
              size="large"
              class="pay-full-btn"
              :loading="submitting"
              @click="handlePay(detailBill)"
            >
              立即支付 ¥{{ formatMoney(detailBill.selfPayAmount) }}
            </el-button>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.payments-page {
  max-width: 960px;
  margin: 0 auto;
}

.page-banner {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  color: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 24px;
}

.page-banner h1 {
  margin: 0 0 8px;
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.3;
}

.page-banner p {
  margin: 0;
  opacity: 0.92;
  font-size: 0.95rem;
  line-height: 1.6;
  max-width: 36rem;
}

.banner-badge {
  flex-shrink: 0;
  background: #f97316;
  color: #fff;
  padding: 8px 14px;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 600;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.filter-tab {
  min-height: 44px;
  padding: 10px 18px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 0.9375rem;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.filter-tab:hover {
  border-color: #14b8a6;
  color: #0d9488;
}

.filter-tab.active {
  background: #f0fdfa;
  border-color: #14b8a6;
  color: #0f766e;
  font-weight: 600;
}

.filter-tab:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.tab-count {
  background: #f97316;
  color: #fff;
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 10px;
}

.bill-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
}

.bill-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.bill-card:hover {
  border-color: #99f6e4;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.08);
}

.bill-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.bill-no {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #0f172a;
  font-size: 0.95rem;
}

.bill-card-body {
  margin-bottom: 16px;
}

.bill-amount .label {
  display: block;
  font-size: 0.8125rem;
  color: #64748b;
  margin-bottom: 4px;
}

.bill-amount .value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #14b8a6;
}

.bill-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 10px;
  font-size: 0.875rem;
  color: #64748b;
}

.bill-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.action-btn {
  min-height: 40px;
  cursor: pointer;
}

.pay-btn {
  background: #f97316;
  border-color: #f97316;
}

.pay-btn:hover {
  background: #ea580c;
  border-color: #ea580c;
}

.detail-panel {
  padding: 0 4px 24px;
  line-height: 1.6;
}

.detail-summary {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 24px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 0.9375rem;
}

.summary-label {
  color: #64748b;
}

.summary-value {
  color: #0f172a;
  font-weight: 500;
}

.detail-fees h3 {
  margin: 0 0 12px;
  font-size: 1rem;
  color: #0f172a;
}

.fee-name {
  font-weight: 500;
  color: #0f172a;
}

.fee-type {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-top: 2px;
}

.detail-totals {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #e2e8f0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 0.9375rem;
  color: #334155;
}

.total-row.muted {
  color: #64748b;
  font-size: 0.875rem;
}

.total-row.highlight {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.total-row.highlight span:last-child {
  color: #14b8a6;
}

.detail-pay {
  margin-top: 24px;
}

.pay-full-btn {
  width: 100%;
  min-height: 48px;
  background: #f97316;
  border-color: #f97316;
  cursor: pointer;
}

.pay-full-btn:hover {
  background: #ea580c;
  border-color: #ea580c;
}

@media (prefers-reduced-motion: reduce) {
  .bill-card,
  .filter-tab {
    transition: none;
  }
}

@media (max-width: 768px) {
  .page-banner {
    flex-direction: column;
    padding: 20px;
  }

  .bill-card-actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
