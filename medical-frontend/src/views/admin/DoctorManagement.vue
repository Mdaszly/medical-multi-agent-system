<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const searchKeyword = ref('')

// 模拟数据
const doctors = ref([
  { id: 1, userAccount: 'doctor001', doctorName: '李医生', department: '心内科', title: '主任医师', phone: '13800138002', status: '正常' },
  { id: 2, userAccount: 'doctor002', doctorName: '王医生', department: '神经内科', title: '副主任医师', phone: '13800138003', status: '正常' },
  { id: 3, userAccount: 'doctor003', doctorName: '张医生', department: '骨科', title: '主治医师', phone: '13800138004', status: '禁用' }
])

const handleEdit = (doctor: any) => {
  ElMessage.info(`编辑医生: ${doctor.doctorName}`)
}

const handleToggleStatus = async (doctor: any) => {
  const action = doctor.status === '正常' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该医生吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    doctor.status = doctor.status === '正常' ? '禁用' : '正常'
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
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="doctor-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>医生管理</span>
          <div class="header-right">
            <el-input v-model="searchKeyword" placeholder="搜索" clearable style="width: 250px; margin-right: 12px;" />
            <el-button type="primary">+ 添加医生</el-button>
          </div>
        </div>
      </template>

      <el-table :data="doctors" style="width: 100%;">
        <el-table-column prop="userAccount" label="账号" />
        <el-table-column prop="doctorName" label="姓名" />
        <el-table-column prop="department" label="科室" />
        <el-table-column prop="title" label="职称" />
        <el-table-column prop="phone" label="电话" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === '正常' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.status === '正常' ? 'warning' : 'success'" size="small" link @click="handleToggleStatus(row)">
              {{ row.status === '正常' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
