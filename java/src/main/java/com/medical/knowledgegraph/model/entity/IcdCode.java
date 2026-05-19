package com.medical.knowledgegraph.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * ICD-10编码实体
 * 国际疾病分类第十版 (International Classification of Diseases)
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class IcdCode extends BaseNode {

    /**
     * ICD编码
     */
    private String code;

    /**
     * 英文描述
     */
    private String descriptionEn;

    /**
     * 中文描述
     */
    private String descriptionCn;

    /**
     * 章编码
     */
    private String chapterCode;

    /**
     * 章名称
     */
    private String chapterName;

    /**
     * 节编码
     */
    private String sectionCode;

    /**
     * 节名称
     */
    private String sectionName;

    /**
     * 疾病分类 (如: 传染病、肿瘤、精神障碍等)
     */
    private String diseaseClass;

    /**
     * 是否为纳入医保 (true-是)
     */
    private Boolean medicalInsurance;

    /**
     * DRGs分组编码
     */
    private String drgCode;

    /**
     * 默认构造函数
     */
    public IcdCode() {
        super();
        this.label = "ICD10";
    }

    @Override
    public String getLabel() {
        return "ICD10";
    }

    /**
     * 全属性构造函数
     */
    public IcdCode(String code, String descriptionEn, String descriptionCn,
                   String chapterCode, String chapterName) {
        super();
        this.setId(code);
        this.setCode(code);
        this.setName(descriptionCn != null ? descriptionCn : descriptionEn);
        this.setDescriptionEn(descriptionEn);
        this.setDescriptionCn(descriptionCn);
        this.setChapterCode(chapterCode);
        this.setChapterName(chapterName);
        this.setLabel("ICD10");
    }

    @Override
    public java.util.Map<String, Object> toNeo4jProperties() {
        java.util.Map<String, Object> props = super.toNeo4jProperties();
        if (code != null) props.put("code", code);
        if (descriptionEn != null) props.put("descriptionEn", descriptionEn);
        if (descriptionCn != null) props.put("descriptionCn", descriptionCn);
        if (chapterCode != null) props.put("chapterCode", chapterCode);
        if (chapterName != null) props.put("chapterName", chapterName);
        if (sectionCode != null) props.put("sectionCode", sectionCode);
        if (sectionName != null) props.put("sectionName", sectionName);
        if (diseaseClass != null) props.put("diseaseClass", diseaseClass);
        if (medicalInsurance != null) props.put("medicalInsurance", medicalInsurance);
        if (drgCode != null) props.put("drgCode", drgCode);
        return props;
    }
}
