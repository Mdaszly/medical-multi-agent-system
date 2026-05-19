package com.medical.knowledgegraph.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 疾病实体
 * 表示医学上的疾病诊断
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Disease extends BaseNode {

    /**
     * 疾病编码
     */
    private String diseaseCode;

    /**
     * ICD-10编码
     */
    private String icd10Code;

    /**
     * 疾病分类
     */
    private String category;

    /**
     * 所属系统 (如: 呼吸系统、心血管系统等)
     */
    private String system;

    /**
     * 严重程度
     */
    private String severity;

    /**
     * 传染性 (true-有传染性)
     */
    private Boolean infectious;

    /**
     * 遗传性 (true-有遗传性)
     */
    private Boolean hereditary;

    /**
     * 描述
     */
    private String description;

    /**
     * 病因
     */
    private String etiology;

    /**
     * 治疗方法
     */
    private String treatment;

    /**
     * 预后
     */
    private String prognosis;

    /**
     * 默认构造函数
     */
    public Disease() {
        super();
        this.label = "Disease";
    }

    @Override
    public String getLabel() {
        return "Disease";
    }

    @Override
    public java.util.Map<String, Object> toNeo4jProperties() {
        java.util.Map<String, Object> props = super.toNeo4jProperties();
        if (diseaseCode != null) props.put("diseaseCode", diseaseCode);
        if (icd10Code != null) props.put("icd10Code", icd10Code);
        if (category != null) props.put("category", category);
        if (system != null) props.put("system", system);
        if (severity != null) props.put("severity", severity);
        if (infectious != null) props.put("infectious", infectious);
        if (hereditary != null) props.put("hereditary", hereditary);
        if (description != null) props.put("description", description);
        if (etiology != null) props.put("etiology", etiology);
        if (treatment != null) props.put("treatment", treatment);
        if (prognosis != null) props.put("prognosis", prognosis);
        return props;
    }
}
