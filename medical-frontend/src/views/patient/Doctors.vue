<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import DoctorCard from '@/components/business/DoctorCard.vue'
import { listDoctorPage } from '@/services/medical/yishengguanli'

const router = useRouter()

const loading = ref(false)
const doctors = ref<any[]>([])
const searchKeyword = ref('')
const selectedDepartment = ref('')
const departments = ['全部', '内科', '外科', '儿科', '妇产科', '眼科', '口腔科', '皮肤科', '骨科']

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

const loadDoctors = async () => {
  loading.value = true
  try {
    const res = await listDoctorPage({
      current: pagination.value.page,
      pageSize: pagination.value.pageSize,
      department: selectedDepartment.value !== '全部' ? selectedDepartment.value : undefined,
      searchKey: searchKeyword.value || undefined
    })
    if (res.data?.records) {
      doctors.value = res.data.records
      pagination.value.total = res.data.total || 0
    }
  } catch (error) {
    console.error('加载医生列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadDoctors()
}

const handleDoctorClick = (doctor: any) => {
  router.push(`/patient/doctor/${doctor.id}`)
}

onMounted(() => {
  loadDoctors()
})
</script>

<template>
  <div class="doctors-page">
    <el-card class="search-card">
      <el-form inline>
        <el-form-item>
          <el-select v-model="selectedDepartment" placeholder="选择科室" style="width: 180px;" @change="handleSearch">
            <el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="searchKeyword" placeholder="搜索医生" clearable style="width: 250px;" @keyup.enter="handleSearch">
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-card>
    
    <div class="doctors-list" v-loading="loading">
      <DoctorCard 
        v-for="doctor in doctors" 
        :key="doctor.id" 
        :doctor="doctor"
        @click="handleDoctorClick(doctor)"
      />
    </div>
    
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      layout="total, prev, pager, next"
      style="margin-top: 20px; text-align: center;"
      @current-change="loadDoctors"
      @size-change="loadDoctors"
    />
  </div>
</template>

<style scoped>
.doctors-page {
  max-width: 1200px;
}

.search-card {
  margin-bottom: 24px;
}

.doctors-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}
</style>
