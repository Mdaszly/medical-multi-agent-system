<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getSlotsBySchedule, generateDefaultSlots, updateSlot, deleteSlot } from '@/services/medical/haoyuanguanli';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const scheduleId = ref<number | null>(null);
const scheduleInfo = ref({
  doctorName: '',
  department: '',
  scheduleDate: '',
  shiftType: ''
});

const slots = ref<any[]>([]);
const editingSlot = ref<any>(null);
const showEditModal = ref(false);
const savingSlot = ref(false);

const shiftTypeMap: Record<string, string> = {
  MORNING: '上午',
  AFTERNOON: '下午',
  EVENING: '晚间'
};

const statusMap: Record<string, { label: string; type: string }> = {
  AVAILABLE: { label: '可预约', type: 'success' },
  SOLD_OUT: { label: '已满', type: 'danger' },
  LOCKED: { label: '锁定', type: 'warning' }
};

const loadSlots = async () => {
  loading.value = true;
  try {
    const id = Number(route.query.scheduleId);
    
    scheduleInfo.value = {
      doctorName: String(route.query.doctorName || '未知医生'),
      department: String(route.query.department || '未知科室'),
      scheduleDate: String(route.query.scheduleDate || '未指定日期'),
      shiftType: String(route.query.shiftType || 'MORNING')
    };

    if (!id || id <= 0) {
      slots.value = [];
      scheduleId.value = null;
      ElMessage.warning('请选择有效的排班信息');
      return;
    }

    scheduleId.value = id;
    
    const res = await getSlotsBySchedule({ scheduleId: id });
    if (res.code === 0 && res.data && res.data.length > 0) {
      slots.value = res.data;
    } else {
      slots.value = [];
    }
  } catch (error) {
    console.error('加载号源列表失败', error);
    slots.value = [];
    ElMessage.error('加载号源数据失败');
  } finally {
    loading.value = false;
  }
};

const handleGenerateDefault = async () => {
  if (!scheduleId.value) {
    ElMessage.warning('请先选择排班');
    return;
  }

  try {
    await ElMessageBox.confirm('确定要生成默认号源吗？这将为当前排班创建预设的号源时段。', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    loading.value = true;
    const res = await generateDefaultSlots({ scheduleId: scheduleId.value });
    if (res.code === 0) {
      ElMessage.success('默认号源生成成功');
      loadSlots();
    } else {
      ElMessage.error(res.message || '生成失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('生成默认号源失败', error);
      ElMessage.error(error.message || '生成失败');
    }
  } finally {
    loading.value = false;
  }
};

const handleEdit = (slot: any) => {
  editingSlot.value = { ...slot };
  showEditModal.value = true;
};

const handleSaveEdit = async () => {
  if (!editingSlot.value) return;

  const slot = editingSlot.value;
  const booked = (slot.maxSlots ?? 0) - (slot.availableSlots ?? 0);
  if (slot.maxSlots < booked) {
    ElMessage.warning(`号源数不能小于已预约数（${booked}）`);
    return;
  }

  savingSlot.value = true;
  try {
    const res = await updateSlot(
      { id: slot.id },
      { maxSlots: slot.maxSlots }
    );

    if (res.code === 0) {
      ElMessage.success('号源更新成功');
      showEditModal.value = false;
      await loadSlots();
    } else {
      ElMessage.error(res.message || '更新失败');
    }
  } catch (error: any) {
    console.error('更新号源失败', error);
    ElMessage.error(error.message || '更新失败');
  } finally {
    savingSlot.value = false;
  }
};

const handleDelete = async (slot: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除号源 ${slot.timeSlot} 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    });

    loading.value = true;
    const res = await deleteSlot({ id: slot.id });
    if (res.code === 0) {
      ElMessage.success('号源删除成功');
      loadSlots();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除号源失败', error);
      ElMessage.error(error.message || '删除失败');
    }
  } finally {
    loading.value = false;
  }
};

const handleBack = () => {
  router.push('/admin/schedules');
};

onMounted(() => {
  loadSlots();
});
</script>

<template>
  <div class="slot-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <div>
            <span class="title">号源管理</span>
            <div class="schedule-info mt-2">
              <span class="text-gray-500">医生：</span>
              <span>{{ scheduleInfo.doctorName }}</span>
              <span class="mx-3">|</span>
              <span class="text-gray-500">科室：</span>
              <span>{{ scheduleInfo.department }}</span>
              <span class="mx-3">|</span>
              <span class="text-gray-500">日期：</span>
              <span>{{ scheduleInfo.scheduleDate }}</span>
              <span class="mx-3">|</span>
              <span class="text-gray-500">班次：</span>
              <span>{{ shiftTypeMap[scheduleInfo.shiftType] || scheduleInfo.shiftType }}</span>
            </div>
          </div>
          <div class="header-buttons">
            <el-button 
              type="success" 
              @click="handleGenerateDefault" 
              :loading="loading"
              :disabled="!scheduleId"
            >
              生成默认号源
            </el-button>
            <el-button @click="handleBack">返回排班列表</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-loading text="加载中..." />
      </div>

      <div v-else-if="slots.length === 0" class="empty-container">
        <el-empty description="暂无号源信息">
          <template #description>
            <p class="empty-desc">当前排班尚未配置号源</p>
            <p class="empty-hint">点击下方按钮生成默认号源，或联系管理员添加</p>
          </template>
          <template #footer>
            <el-button 
              type="primary" 
              @click="handleGenerateDefault" 
              :disabled="!scheduleId"
              class="empty-action-btn"
            >
              生成默认号源
            </el-button>
          </template>
        </el-empty>
      </div>

      <div v-else>
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ slots.length }}</div>
            <div class="stat-label">总时段数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value text-green-500">{{ slots.reduce((sum, s) => sum + s.maxSlots, 0) }}</div>
            <div class="stat-label">总号源数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value text-blue-500">{{ slots.reduce((sum, s) => sum + s.availableSlots, 0) }}</div>
            <div class="stat-label">可预约数</div>
          </div>
          <div class="stat-card">
            <div class="stat-value text-yellow-500">{{ slots.reduce((sum, s) => sum + s.lockedSlots, 0) }}</div>
            <div class="stat-label">锁定数</div>
          </div>
        </div>

        <el-table :data="slots" style="width: 100%;" border>
          <el-table-column prop="timeSlot" label="时段" width="150" />
          <el-table-column label="号源情况" width="200">
            <template #default="{ row }">
              <div class="flex items-center">
                <div class="flex-1 mr-3">
                  <el-progress
                    :percentage="((row.maxSlots - row.availableSlots) / row.maxSlots) * 100"
                    :stroke-width="8"
                    :show-text="false"
                  />
                </div>
                <span class="text-sm">
                  <span :class="row.availableSlots > 0 ? 'text-green-500' : 'text-red-500'">
                    {{ row.availableSlots }}
                  </span>
                  <span class="text-gray-400">/{{ row.maxSlots }}</span>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="lockedSlots" label="锁定数" width="80" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="showEditModal"
      title="编辑号源"
      width="420px"
      destroy-on-close
      :close-on-click-modal="false"
      class="slot-edit-dialog"
    >
      <el-form v-if="editingSlot" :model="editingSlot" label-width="88px">
        <el-form-item label="时段">
          <span class="slot-time-text">{{ editingSlot.timeSlot }}</span>
        </el-form-item>
        <el-form-item label="当前可约">
          <span>{{ editingSlot.availableSlots }} / {{ editingSlot.maxSlots }}</span>
        </el-form-item>
        <el-form-item label="号源总数" required>
          <el-input-number
            v-model="editingSlot.maxSlots"
            :min="Math.max(1, (editingSlot.maxSlots ?? 0) - (editingSlot.availableSlots ?? 0))"
            :max="50"
            style="width: 160px;"
          />
          <div class="form-hint">
            已占用 {{ (editingSlot.maxSlots ?? 0) - (editingSlot.availableSlots ?? 0) }} 个，总数不可低于该值
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditModal = false" :disabled="savingSlot">取消</el-button>
        <el-button type="primary" :loading="savingSlot" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.slot-management {
  min-height: calc(100vh - 60px);
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.schedule-info {
  font-size: 14px;
  color: #666;
}

.header-buttons {
  display: flex;
  gap: 12px;
}

.loading-container {
  padding: 40px;
}

.empty-container {
  padding: 40px;
}

.empty-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.empty-hint {
  font-size: 12px;
  color: #909399;
}

.empty-action-btn {
  margin-top: 16px;
}

.stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.mt-2 {
  margin-top: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.mx-3 {
  margin-left: 12px;
  margin-right: 12px;
}

.text-gray-500 {
  color: #999;
}

.text-green-500 {
  color: #10b981;
}

.text-blue-500 {
  color: #3b82f6;
}

.text-yellow-500 {
  color: #f59e0b;
}

.slot-time-text {
  color: #374151;
  font-weight: 500;
}

.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

:deep(.slot-edit-dialog .el-button--primary) {
  --el-button-bg-color: #0d9488;
  --el-button-border-color: #0d9488;
  --el-button-hover-bg-color: #0f766e;
  --el-button-hover-border-color: #0f766e;
}
</style>