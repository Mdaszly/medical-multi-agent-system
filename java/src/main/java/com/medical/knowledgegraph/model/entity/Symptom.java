package com.medical.knowledgegraph.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 症状实体
 * 表示患者主诉或体检发现的身体异常
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Symptom extends BaseNode {

    /**
     * 症状编码
     */
    private String code;

    /**
     * 症状分类编码
     */
    private String categoryCode;

    /**
     * 症状分类名称
     */
    private String categoryName;

    /**
     * 症状描述
     */
    private String description;

    /**
     * 发生频率 (1-5, 5最高频)
     */
    private Integer frequency;

    /**
     * 严重程度 (1-轻微, 2-中等, 3-严重)
     */
    private Integer severity;

    /**
     * 身体部位
     */
    private String bodyPart;

    /**
     * 拼音首字母
     */
    private String pinyin;

    /**
     * 是否紧急 (true-紧急)
     */
    private Boolean urgent;

    /**
     * 默认构造函数
     */
    public Symptom() {
        super();
        this.label = "Symptom";
    }

    /**
     * 全属性构造函数
     */
    public Symptom(String id, String name, String code, String categoryCode,
                   String categoryName, String description, Integer frequency,
                   Integer severity, String bodyPart, String pinyin, Boolean urgent) {
        super();
        this.id = id;
        this.name = name;
        this.code = code;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.description = description;
        this.frequency = frequency;
        this.severity = severity;
        this.bodyPart = bodyPart;
        this.pinyin = pinyin;
        this.urgent = urgent;
        this.label = "Symptom";
    }

    @Override
    public String getLabel() {
        return "Symptom";
    }

    @Override
    public java.util.Map<String, Object> toNeo4jProperties() {
        java.util.Map<String, Object> props = super.toNeo4jProperties();
        if (code != null) props.put("code", code);
        if (categoryCode != null) props.put("categoryCode", categoryCode);
        if (categoryName != null) props.put("categoryName", categoryName);
        if (description != null) props.put("description", description);
        if (frequency != null) props.put("frequency", frequency);
        if (severity != null) props.put("severity", severity);
        if (bodyPart != null) props.put("bodyPart", bodyPart);
        if (pinyin != null) props.put("pinyin", pinyin);
        if (urgent != null) props.put("urgent", urgent);
        return props;
    }
}
