package com.medical.service.kg.symptom;

/**
 * 向量数学工具类
 * 
 * 功能：提供向量相似度计算功能
 * 用途：在症状向量检索中，计算查询向量与症状向量的余弦相似度
 */
final class VectorMath {

    /** 私有构造函数，防止实例化 */
    private VectorMath() {
    }

    /**
     * 计算两个向量的余弦相似度
     * 
     * 公式：cos(θ) = (A·B) / (||A|| * ||B||)
     * 值域：[-1, 1]，值越大表示两个向量越相似
     * 
     * 在症状检索中的应用：
     * - 将用户描述转为向量（如"头疼"→[0.1, 0.5, ...]）
     * - 将症状词表转为向量（如"头痛"→[0.12, 0.48, ...]）
     * - 计算余弦相似度，找出最相似的标准症状
     * 
     * @param a 向量A（查询向量）
     * @param b 向量B（症状向量）
     * @return 余弦相似度（0.0~1.0），异常情况返回0.0
     */
    static double cosine(float[] a, float[] b) {
        // 1. 边界检查：确保两个向量有效且维度相同
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;  // 异常情况返回0（完全不相似）
        }

        // 2. 初始化累加器
        double dot = 0.0;    // 点积累加器
        double normA = 0.0;  // 向量A的模长平方
        double normB = 0.0;  // 向量B的模长平方

        // 3. 单次循环计算所有值（优化性能）
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];      // 点积：对应元素相乘后累加
            normA += a[i] * a[i];    // A的模长平方：元素平方累加
            normB += b[i] * b[i];    // B的模长平方：元素平方累加
        }

        // 4. 防除零：如果任一向量全为0，无法计算相似度
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        // 5. 计算最终结果：点积 / (模长A * 模长B)
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
