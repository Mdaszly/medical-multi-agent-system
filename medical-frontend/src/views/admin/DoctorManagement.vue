<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listDoctorPage } from '@/services/medical/yishengguanli'

const loading = ref(false)
const searchKeyword = ref('')
const selectedDepartment = ref('')

const departments = ['全部', '内科', '外科', '儿科', '妇产科', '眼科', '口腔科', '皮肤科', '骨科']

const doctors = ref<any[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const statusMap: Record<number, { label: string; type: string }> = {
  1: { label: '正常', type: 'success' },
  0: { label: '禁用', type: 'danger' }
}

const loadDoctors = async () => {
  loading.value = true
  try {
    const res = await listDoctorPage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize,
      department: selectedDepartment.value !== '全部' ? selectedDepartment.value : undefined,
      doctorName: searchKeyword.value || undefined
    })
    if (res.data?.records) {
      doctors.value = res.data.records
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载医生列表失败', error)
    ElMessage.error('加载医生列表失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (doctor: any) => {
  ElMessage.info(`编辑医生: ${doctor.doctorName}`)
}

const handleToggleStatus = async (doctor: any) => {
  const action = doctor.workStatus === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该医生吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    doctor.workStatus = doctor.workStatus === 1 ? 0 : 1
    ElMessage.success(`${action}成功`)
  } catch {
    // 用户取消
  }
}

const handleDelete = async (doctor: any) => {
  try {
    await ElMessageBox.confirm('确定删除该医生吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('医生已删除')
    loadDoctors()
  } catch {
    // 用户取消
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  loadDoctors()
}

onMounted(() => {
  loadDoctors()
})
</script>

<template>
  <div class="doctor-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>医生管理</span>
          <div class="header-right">
            <el-select v-model="selectedDepartment" placeholder="选择科室" style="width: 180px; margin-right: 12px;" @change="handleSearch">
              <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
            </el-select>
            <el-input v-model="searchKeyword" placeholder="搜索" clearable style="width: 250px; margin-right: 12px;" @keyup.enter="handleSearch">
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
            <el-button type="primary">+ 添加医生</el-button>
          </div>
        </div>
      </template>

      <el-table :data="doctors" style="width: 100%;" v-loading="loading">
        <el-table-column prop="doctorNo" label="医生编号" />
        <el-table-column prop="doctorName" label="姓名" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="title" label="职称" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="workStatus" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.workStatus]?.type">{{ statusMap[row.workStatus]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.workStatus === 1 ? 'warning' : 'success'" size="small" link @click="handleToggleStatus(row)">
              {{ row.workStatus === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="loadDoctors"
        @size-change="loadDoctors"
      />
      
      <el-empty v-if="!loading && doctors.length === 0" description="暂无医生" />
    </el-card>
  </div>
</template>

<style scoped>
.doctor-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
}
</style>