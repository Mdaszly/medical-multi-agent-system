<template>
  <div class="graph-evidence-panel">
    <div class="panel-header" @click="toggleExpand">
      <div class="header-left">
        <span class="panel-icon">
          <component :is="expandIcon" />
        </span>
        <span class="panel-title">图谱证据</span>
      </div>
      <span v-if="evidence?.rows?.length" class="row-count">
        {{ evidence?.rows?.length }} 条记录
      </span>
    </div>

    <transition name="expand">
      <div v-if="isExpanded" class="panel-content">
        <div v-if="evidence?.symptomMatches?.length" class="symptoms-section">
          <div class="section-title">术语标准化</div>
          <p class="section-desc">
            系统将口语描述映射为标准医学术语，用于知识图谱检索。
          </p>
          <div class="match-cards">
            <div
              v-for="(match, index) in evidence.symptomMatches"
              :key="index"
              class="match-card"
              :title="match.rationale"
            >
              <span class="phrase-user">{{ match.userPhrase }}</span>
              <span class="phrase-arrow" aria-hidden="true">→</span>
              <span class="phrase-canonical">{{ match.canonicalName }}</span>
              <el-tag v-if="match.method" size="small" type="info" class="method-tag">
                {{ match.method }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-else-if="evidence?.extractedSymptoms?.length" class="symptoms-section">
          <div class="section-title">标准症状</div>
          <div class="symptom-tags">
            <span
              v-for="(symptom, index) in evidence.extractedSymptoms"
              :key="index"
              class="symptom-tag"
            >
              {{ symptom }}
            </span>
          </div>
        </div>

        <div v-if="evidence?.rows?.length" class="table-section">
          <div class="section-title">症状-疾病映射</div>
          <div class="evidence-table">
            <table>
              <thead>
                <tr>
                  <th>症状</th>
                  <th>疾病</th>
                  <th>ICD编码</th>
                  <th>描述</th>
                  <th>权重</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="(row, index) in evidence.rows"
                  :key="index"
                  class="table-row"
                >
                  <td>{{ row.symptom }}</td>
                  <td>{{ row.disease }}</td>
                  <td>{{ row.icdCode || "-" }}</td>
                  <td class="icd-description">{{ row.icdDescription || "-" }}</td>
                  <td>{{ row.weight?.toFixed(2) || "-" }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="evidence?.formattedText" class="text-section">
          <div class="section-title">原始证据文本</div>
          <div class="evidence-text">{{ evidence.formattedText }}</div>
        </div>

        <div v-if="evidence?.queryTimeMs" class="footer-info">
          查询耗时：{{ evidence.queryTimeMs }}ms
        </div>

        <div v-if="isEmpty" class="empty-state">
          <component :is="FolderOpened" class="empty-icon" />
          <span class="empty-text">暂无图谱证据</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import {
  ArrowDown,
  ArrowRight,
  Document,
  FolderOpened,
} from "@element-plus/icons-vue";
import type { GraphEvidence } from "@/services/medical/types";

interface Props {
  evidence?: GraphEvidence;
  defaultExpanded?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  defaultExpanded: false,
});

const isExpanded = ref(props.defaultExpanded);

const expandIcon = computed(() => (isExpanded.value ? ArrowDown : ArrowRight));

const isEmpty = computed(() => {
  return (
    !props.evidence ||
    (!props.evidence.rows?.length &&
      !props.evidence.extractedSymptoms?.length &&
      !props.evidence.formattedText)
  );
});

const toggleExpand = () => {
  isExpanded.value = !isExpanded.value;
};
</script>

<style scoped>
.graph-evidence-panel {
  background: var(--consult-surface);
  border-radius: var(--consult-radius);
  border: 1px solid var(--consult-border);
  overflow: hidden;
  margin-top: var(--consult-spacing-md);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  transition: background-color 0.2s;
}

.panel-header:hover {
  background-color: #f5f5f5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-icon {
  color: var(--consult-info);
  font-size: 14px;
}

.section-desc {
  font-size: 12px;
  color: var(--consult-text-muted);
  margin: 0 0 12px;
  line-height: 1.5;
}

.match-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.match-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 14px;
  background: var(--consult-success-bg);
  border: 1px solid #a7f3d0;
  border-radius: var(--consult-radius);
}

.phrase-user {
  font-size: 13px;
  color: var(--consult-text-secondary);
}

.phrase-arrow {
  color: var(--consult-info);
  font-weight: 600;
}

.phrase-canonical {
  font-size: 13px;
  font-weight: 600;
  color: var(--consult-success);
}

.method-tag {
  margin-left: auto;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.row-count {
  font-size: 12px;
  color: #999;
}

.panel-content {
  padding: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: #666;
  margin-bottom: 12px;
}

.symptoms-section {
  margin-bottom: 16px;
}

.symptom-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.symptom-tag {
  padding: 4px 10px;
  background-color: #e6f7ff;
  color: #1890ff;
  border-radius: 4px;
  font-size: 12px;
}

.match-tag {
  background-color: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}

.match-tag em {
  font-style: normal;
  font-size: 11px;
  opacity: 0.85;
}

.table-section {
  margin-bottom: 16px;
  overflow-x: auto;
}

.evidence-table {
  width: 100%;
  min-width: 600px;
}

.evidence-table table {
  width: 100%;
  border-collapse: collapse;
}

.evidence-table th {
  text-align: left;
  padding: 10px 12px;
  background-color: #fafafa;
  font-weight: 500;
  font-size: 12px;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}

.evidence-table td {
  padding: 10px 12px;
  font-size: 13px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}

.table-row:hover {
  background-color: #fafafa;
}

.icd-description {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-section {
  margin-bottom: 16px;
}

.evidence-text {
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
}

.footer-info {
  font-size: 12px;
  color: #999;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
