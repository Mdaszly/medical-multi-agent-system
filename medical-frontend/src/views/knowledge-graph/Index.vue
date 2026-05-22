<template>
  <div class="knowledge-graph-page">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">知识图谱管理</h1>
        <p class="page-description">管理和查询医疗知识图谱数据</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleSync" :loading="isSyncing">
          <Refresh class="btn-icon" />
          同步数据
        </el-button>
        <el-button @click="handleTestConnection" :loading="isTesting">
          <Connection class="btn-icon" />
          测试连接
        </el-button>
      </div>
    </div>

    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-icon-container bg-blue">
          <FolderOpened class="stat-icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.nodes }}</div>
          <div class="stat-label">节点数量</div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon-container bg-green">
          <Link class="stat-icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.relations }}</div>
          <div class="stat-label">关系数量</div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon-container bg-purple">
          <CollectionTag class="stat-icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.symptoms }}</div>
          <div class="stat-label">症状数量</div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon-container bg-orange">
          <Document class="stat-icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.diseases }}</div>
          <div class="stat-label">疾病数量</div>
        </div>
      </el-card>
    </div>

    <div class="main-content">
      <div class="search-section">
        <el-card title="症状搜索" class="search-card">
          <SymptomInput
            v-model="searchQuery"
            placeholder="输入症状关键词搜索"
            @select="handleSearch"
            @submit="handleSearch"
          />
          
          <div v-if="searchResults.length" class="search-results">
            <div class="results-header">搜索结果</div>
            <el-table :data="searchResults" border class="results-table">
              <el-table-column prop="symptom" label="症状" />
              <el-table-column prop="disease" label="疾病" />
              <el-table-column prop="icdCode" label="ICD编码" />
              <el-table-column prop="weight" label="权重" />
            </el-table>
          </div>
        </el-card>
      </div>

      <div class="visual-section">
        <el-card title="图谱概览" class="visual-card">
          <div class="visual-placeholder">
            <CircleCheck class="network-icon" />
            <p class="placeholder-text">图谱可视化区域</p>
            <p class="placeholder-hint">点击节点查看详细信息</p>
          </div>
        </el-card>
      </div>
    </div>

    <div class="quick-actions">
      <el-card title="快捷操作" class="actions-card">
        <div class="action-grid">
          <div class="action-item" @click="handleClearCache">
            <Delete class="action-icon" />
            <span class="action-text">清除缓存</span>
          </div>
          <div class="action-item" @click="handleExport">
            <Download class="action-icon" />
            <span class="action-text">导出数据</span>
          </div>
          <div class="action-item" @click="handleImport">
            <Upload class="action-icon" />
            <span class="action-text">导入数据</span>
          </div>
          <div class="action-item" @click="handleHealthCheck">
            <Cherry class="action-icon" />
            <span class="action-text">健康检查</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import {
  Refresh,
  Connection,
  DataAnalysis,
  Link,
  CollectionTag,
  Document,
  CircleCheck,
  Delete,
  Download,
  Upload,
  Cherry,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import SymptomInput from "@/components/consult/SymptomInput.vue";
import {
  getGraphStatistics,
  syncData,
  testConnection,
  queryDiagnosis,
} from "@/services/medical/knowledgeGraph";

const statistics = ref({
  nodes: 0,
  relations: 0,
  symptoms: 0,
  diseases: 0,
});

const searchQuery = ref("");
const searchResults = ref<API.SymptomDiagnosisRowVO[]>([]);
const isSyncing = ref(false);
const isTesting = ref(false);

const fetchStatistics = async () => {
  try {
    const data = await getGraphStatistics();
    statistics.value = {
      nodes: data.nodeCount || 0,
      relations: data.relationCount || 0,
      symptoms: data.symptomCount || 0,
      diseases: data.diseaseCount || 0,
    };
  } catch (error) {
    console.error("获取统计信息失败:", error);
  }
};

const handleSearch = async (query: string) => {
  if (!query.trim()) return;
  try {
    const result = await queryDiagnosis(query.trim());
    searchResults.value = result.records?.map((record: any) => ({
      symptom: record.symptom || record.SYMPTOM,
      disease: record.disease || record.DISEASE,
      icdCode: record.icdCode || record.ICD_CODE,
      weight: record.weight || record.WEIGHT,
    })) || [];
  } catch (error) {
    console.error("搜索失败:", error);
  }
};

const handleSync = async () => {
  isSyncing.value = true;
  try {
    await syncData();
    await fetchStatistics();
    ElMessage.success("数据同步成功");
  } catch (error) {
    ElMessage.error("数据同步失败");
  } finally {
    isSyncing.value = false;
  }
};

const handleTestConnection = async () => {
  isTesting.value = true;
  try {
    const result = await testConnection();
    if (result.status === "UP") {
      ElMessage.success("连接正常");
    } else {
      ElMessage.warning("连接状态异常");
    }
  } catch (error) {
    ElMessage.error("连接失败");
  } finally {
    isTesting.value = false;
  }
};

const handleClearCache = () => {
  ElMessage.info("缓存清除功能开发中");
};

const handleExport = () => {
  ElMessage.info("数据导出功能开发中");
};

const handleImport = () => {
  ElMessage.info("数据导入功能开发中");
};

const handleHealthCheck = () => {
  ElMessage.info("健康检查功能开发中");
};

onMounted(() => {
  fetchStatistics();
});
</script>

<style scoped>
.knowledge-graph-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.page-description {
  margin: 0;
  font-size: 14px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-icon {
  margin-right: 6px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.stat-icon-container {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-container.bg-blue {
  background: #eff6ff;
}

.stat-icon-container.bg-green {
  background: #ecfdf5;
}

.stat-icon-container.bg-purple {
  background: #f5f3ff;
}

.stat-icon-container.bg-orange {
  background: #fff7ed;
}

.stat-icon {
  font-size: 24px;
}

.stat-icon-container.bg-blue .stat-icon {
  color: #3b82f6;
}

.stat-icon-container.bg-green .stat-icon {
  color: #10b981;
}

.stat-icon-container.bg-purple .stat-icon {
  color: #8b5cf6;
}

.stat-icon-container.bg-orange .stat-icon {
  color: #f97316;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.main-content {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.search-card {
  border-radius: 12px;
}

.search-results {
  margin-top: 16px;
}

.results-header {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.results-table {
  font-size: 13px;
}

.visual-card {
  border-radius: 12px;
  min-height: 400px;
}

.visual-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 350px;
  background: #f9fafb;
  border-radius: 8px;
}

.network-icon {
  font-size: 64px;
  color: #9ca3af;
  margin-bottom: 16px;
}

.placeholder-text {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #6b7280;
}

.placeholder-hint {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

.quick-actions {
  margin-bottom: 24px;
}

.actions-card {
  border-radius: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
}

.action-icon {
  font-size: 24px;
  color: #6b7280;
}

.action-text {
  font-size: 13px;
  color: #374151;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .main-content {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>