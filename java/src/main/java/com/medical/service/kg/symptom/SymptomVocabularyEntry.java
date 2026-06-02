package com.medical.service.kg.symptom;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 症状词汇表条目
 * 
 * 表示一个标准医学症状及其相关信息
 * 例如：name="头痛", code="S001", aliases=["头疼", "脑壳疼", "脑袋疼"]
 */
@Data
@Builder
public class SymptomVocabularyEntry {

    /** 标准症状名称（医学术语），如"头痛"、"腹泻" */
    private String name;
    
    /** 症状编码（用于知识图谱关联），如"S001" */
    private String code;
    
    /** 症状描述信息，如"头部持续性或阵发性疼痛" */
    private String description;
    
    /** 症状拼音（用于检索排序），如"toutong" */
    private String pinyin;
    
    /** 症状分类名称，如"神经系统症状"、"消化系统症状" */
    private String categoryName;

    /**
     * 别名/同义词列表（用户口语化表达）
     * 例如：对于"头痛"，aliases=["头疼", "脑壳疼", "脑袋疼"]
     * 数据来源：SymptomSynonymRegistry 同义词表 + SymptomVocabularyService 加载时合并
     */
    @Builder.Default
    private List<String> aliases = new ArrayList<>();

    /**
     * 构建向量检索的索引文本
     * 
     * 逻辑：
     * 1. 合并标准名 + 所有别名（去重）
     * 2. 拼接描述、分类、拼音、别名等所有字段
     * 3. 用于向量嵌入时的语义召回，提高匹配准确率
     * 
     * 示例返回："头部疼痛 神经系统症状 toutong 头痛 头疼 脑壳疼 脑袋疼"
     * 
     * @return 完整的索引文本，用于向量检索
     */
    public String indexText() {
        // 构建包含标准名和所有同义词的完整列表
        List<String> nameWithAliases = new ArrayList<>();
        if (name != null && !name.isBlank()) {
            nameWithAliases.add(name);
        }
        if (aliases != null && !aliases.isEmpty()) {
            // 过滤掉与name重复的别名，避免indexText中出现重复词
            aliases.stream()
                    .filter(alias -> !name.equals(alias))
                    .forEach(nameWithAliases::add);
        }
        String aliasPart = nameWithAliases.isEmpty() ? "" : String.join(" ", nameWithAliases);
        return String.join(" ",
                nullToEmpty(description),
                nullToEmpty(categoryName),
                nullToEmpty(pinyin),
                aliasPart).trim();
    }

    /**
     * 安全地将null转为空字符串
     * 
     * @param v 任意字符串
     * @return 原字符串或空字符串（若为null）
     */
    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
