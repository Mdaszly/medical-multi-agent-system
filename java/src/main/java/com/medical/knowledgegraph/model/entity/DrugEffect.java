package com.medical.knowledgegraph.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 药效实体
 * 表示药品的功效和作用
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DrugEffect extends BaseNode {

    /**
     * 药效编码
     */
    private String effectCode;

    /**
     * 药效分类 (如: 解热镇痛、抗菌消炎等)
     */
    private String category;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 适应症
     */
    private String indications;

    /**
     * 不良反应
     */
    private String adverseReactions;

    /**
     * 禁忌症
     */
    private String contraindications;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 作用机制
     */
    private String mechanism;

    /**
     * 默认构造函数
     */
    public DrugEffect() {
        super();
        this.setLabel("DrugEffect");
    }

    @Override
    public java.util.Map<String, Object> toNeo4jProperties() {
        java.util.Map<String, Object> props = super.toNeo4jProperties();
        if (effectCode != null) props.put("effectCode", effectCode);
        if (category != null) props.put("category", category);
        if (description != null) props.put("description", description);
        if (indications != null) props.put("indications", indications);
        if (adverseReactions != null) props.put("adverseReactions", adverseReactions);
        if (contraindications != null) props.put("contraindications", contraindications);
        if (precautions != null) props.put("precautions", precautions);
        if (mechanism != null) props.put("mechanism", mechanism);
        return props;
    }
}
