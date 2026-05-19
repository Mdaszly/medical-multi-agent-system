package com.medical.knowledgegraph.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 药品实体
 * 表示医疗系统中使用的药物
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Drug extends BaseNode {

    /**
     * 药品编码
     */
    private String drugCode;

    /**
     * 通用名称
     */
    private String genericName;

    /**
     * 商品名称
     */
    private String brandName;

    /**
     * 英文名称
     */
    private String englishName;

    /**
     * 药品分类 (西药/中成药/中药饮片)
     */
    private String category;

    /**
     * 药品类型 (处方药/非处方药)
     */
    private String type;

    /**
     * 规格
     */
    private String specification;

    /**
     * 单位
     */
    private String unit;

    /**
     * 生产厂家
     */
    private String manufacturer;

    /**
     * 批准文号
     */
    private String approvalNumber;

    /**
     * 价格
     */
    private Double price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 用药途径 (口服/注射/外用等)
     */
    private String routeOfAdministration;

    /**
     * 用法用量
     */
    private String dosage;

    /**
     * 是否基药 (true-国家基本药物)
     */
    private Boolean essential;

    /**
     * 是否医保 (true-医保药品)
     */
    private Boolean medicalInsurance;

    /**
     * 默认构造函数
     */
    public Drug() {
        super();
        this.setLabel("Drug");
    }

    @Override
    public java.util.Map<String, Object> toNeo4jProperties() {
        java.util.Map<String, Object> props = super.toNeo4jProperties();
        if (drugCode != null) props.put("drugCode", drugCode);
        if (genericName != null) props.put("genericName", genericName);
        if (brandName != null) props.put("brandName", brandName);
        if (englishName != null) props.put("englishName", englishName);
        if (category != null) props.put("category", category);
        if (type != null) props.put("type", type);
        if (specification != null) props.put("specification", specification);
        if (unit != null) props.put("unit", unit);
        if (manufacturer != null) props.put("manufacturer", manufacturer);
        if (approvalNumber != null) props.put("approvalNumber", approvalNumber);
        if (price != null) props.put("price", price);
        if (stock != null) props.put("stock", stock);
        if (routeOfAdministration != null) props.put("routeOfAdministration", routeOfAdministration);
        if (dosage != null) props.put("dosage", dosage);
        if (essential != null) props.put("essential", essential);
        if (medicalInsurance != null) props.put("medicalInsurance", medicalInsurance);
        return props;
    }
}
