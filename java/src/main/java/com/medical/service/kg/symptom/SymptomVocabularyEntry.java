package com.medical.service.kg.symptom;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class SymptomVocabularyEntry {

    private String name;
    private String code;
    private String description;
    private String pinyin;
    private String categoryName;

    @Builder.Default
    private List<String> aliases = new ArrayList<>();

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

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
