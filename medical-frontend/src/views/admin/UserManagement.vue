<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listUserPage, disableUser, enableUser } from '@/services/medical/yonghuguanli'

const searchKeyword = ref('')
const loading = ref(false)

const users = ref<any[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const roleMap: Record<string, { label: string; type: string }> = {
  'user': { label: '患者', type: 'info' },
  'doctor': { label: '医生', type: 'success' },
  'admin': { label: '管理员', type: 'warning' },
  'pharmacist': { label: '药师', type: 'primary' }
}

const statusMap: Record<number, { label: string; type: string }> = {
  1: { label: '正常', type: 'success' },
  0: { label: '禁用', type: 'danger' }
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await listUserPage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize,
      userName: searchKeyword.value || undefined
    })
    if (res.data?.records) {
      users.value = res.data.records
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载用户列表失败', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleToggleStatus = async (user: any) => {
  try {
    if (user.userStatus === 1) {
      await disableUser({ id: user.id })
      user.userStatus = 0
      ElMessage.success('用户禁用成功')
    } else {
      await enableUser({ id: user.id })
      user.userStatus = 1
      ElMessage.success('用户启用成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (user: any) => {
  try {
    await ElMessageBox.confirm('确定删除此用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('用户已删除')
    loadUsers()
  } catch {
    // 用户取消
  }
}

const handleSearch = () => {
  pagination.value.page = 1
  loadUsers()
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>用户管理</span>
          <div class="header-right">
            <el-input v-model="searchKeyword" placeholder="搜索用户" clearable style="width: 250px; margin-right: 12px;" @keyup.enter="handleSearch">
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
            <el-button type="primary">+ 添加用户</el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="users" style="width: 100%" v-loading="loading">
        <el-table-column prop="userAccount" label="用户名" />
        <el-table-column prop="userName" label="姓名" />
        <el-table-column prop="userRole" label="角色">
          <template #default="{ row }">
            <el-tag :type="roleMap[row.userRole]?.type">{{ roleMap[row.userRole]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag 
              v-if="row.userRole === 'admin' && row.adminAccountType === 'TEMPORARY'" 
              type="warning" 
              size="small"
            >临时</el-tag>
            <el-tag 
              v-else-if="row.userRole === 'admin' && row.adminAccountType === 'FORMAL'" 
              type="success" 
              size="small"
            >正式</el-tag>
            <span v-else style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="userStatus" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.userStatus]?.type">{{ statusMap[row.userStatus]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button
                :type="row.userStatus === 1 ? 'warning' : 'success'"
                size="small"
                link
                @click="handleToggleStatus(row)"
              >
                {{ row.userStatus === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px; text-align: center;"
        @current-change="loadUsers"
        @size-change="loadUsers"
      />
      
      <el-empty v-if="!loading && users.length === 0" description="暂无用户" />
    </el-card>
  </div>
</template>

<style scoped>
.user-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
}

.table-actions :deep(.el-button.is-link) {
  min-height: 32px;
  padding: 4px 0;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.table-actions :deep(.el-button.is-link:hover) {
  opacity: 0.85;
}

@media (prefers-reduced-motion: reduce) {
  .table-actions :deep(.el-button.is-link) {
    transition: none;
  }
}
</style>