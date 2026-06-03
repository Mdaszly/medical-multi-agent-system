/**
 * 症状召回评测（gold 集基准测试）�? *
 * <p>在固定标注集 {@code kg/vector_topk_eval.json} 上对比：
 * <ul>
 *   <li>{@link SymptomSynonymOnlyEvaluator} �?同义�?/ 标准名精确匹�?baseline</li>
 *   <li>{@link SymptomVectorTopKEvaluator} �?向量 Top-K 语义召回</li>
 *   <li>{@link SymptomEvalComparisonService} �?并排输出 Recall@K、Hit@1、MRR 及提升幅�?/li>
 * </ul>
 *
 * <p>HTTP 入口�?{@link com.medical.controller.SymptomRecallBenchmarkController}�? * 线上解析链路�?{@link com.medical.service.kg.symptom.SymptomResolver}�? */
package com.medical.service.kg.symptom.benchmark;
