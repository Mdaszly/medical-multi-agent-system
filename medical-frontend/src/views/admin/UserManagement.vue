<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const users = ref([
  { id: 1, username: 'zhangsan', name: '张三', role: '患者', status: '正常', phone: '13800138001' },
  { id: 2, username: 'lisi', name: '李医生', role: '医生', status: '正常', phone: '13800138002' },
  { id: 3, username: 'admin', name: '管理员', role: '管理员', status: '正常', phone: '13800138003' }
])

const roleMap: Record<string, string> = {
  '患者': 'info',
  '医生': 'success',
  '管理员': 'warning'
}

const statusMap: Record<string, string> = {
  '正常': 'success',
  '禁用': 'danger'
}

const handleToggleStatus = (user: any) => {
  const newStatus = user.status === '正常' ? '禁用' : '正常'
  user.status = newStatus
  ElMessage.success(`用户${newStatus === '禁用' ? '禁用' : '启用'}成功`)
}

const handleEdit = (user: any) => {
  ElMessage.info(`编辑用户: ${user.name}`)
}

const handleDelete = async (user: any) => {
  try {
    await ElMessageBox.confirm('确定删除此用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    ElMessage.success('用户已删除')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="header-actions">
          <span>用户管理</span>
          <div class="search-box">
            <el-input v-model="searchKeyword" placeholder="搜索用户" clearable>
              <template #append>
                <el-button :icon="Search" />
              </template>
            </el-input>
          </div>
        </div>
      </template>
      
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="role" label="角色">
          <template #default="{ row }">
            <el-tag :type="roleMap[row.role]">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.status === '正常' ? 'warning' : 'success'" 
              size="small" 
              link 
              @click="handleToggleStatus(row)"
            >
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
.user-management {
  max-width: 1200px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-box {
  width: 300px;
}
</style>
