package com.medical.service;

/**
 * 医学知识检索接口（由 {@link com.medical.tools.MedicalTools#searchMedicalKnowledge} 暴露给 LLM）。
 */
public interface MedicalKnowledgeService {

    /**
     * String search(String query)
     * <p>根据自然语言查询返回可读知识片段；未命中时返回提示文案。</p>
     */
    String search(String query);
}
