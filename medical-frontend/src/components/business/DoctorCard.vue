<script setup lang="ts">
interface Props {
  doctor: any
}

defineProps<Props>()

defineEmits(['click'])
</script>

<template>
  <el-card class="doctor-card" shadow="hover" @click="$emit('click', doctor)">
    <div class="card-content">
      <el-avatar :size="80" class="avatar" />
      <div class="info">
        <h3 class="name">{{ doctor.doctorName }}</h3>
        <div class="tags">
          <el-tag size="small" type="info">{{ doctor.department }}</el-tag>
          <el-tag size="small" type="success">{{ doctor.title }}</el-tag>
        </div>
        <p class="intro">{{ doctor.description }}</p>
        <div class="footer">
          <span class="price">挂号费：¥{{ doctor.consultationFee }}</span>
          <el-button type="primary" size="small">查看详情</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.doctor-card {
  cursor: pointer;
}

.card-content {
  display: flex;
  gap: 16px;
}

.avatar {
  background: #14b8a6;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.name {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #1f2937;
}

.tags {
  margin-bottom: 12px;
}

.intro {
  margin: 0 0 12px 0;
  color: #6b7280;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #14b8a6;
  font-weight: bold;
}
</style>
