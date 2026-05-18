package com.medical.service.impl;

import com.medical.model.vo.Icd10CodeVO;
import com.medical.service.Icd10Service;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class Icd10ServiceImpl implements Icd10Service {

    private static final Map<String, Map<String, String>> ICD10_CODES = new LinkedHashMap<>();

    static {
        putCategory("Circulatory", "I10", "Essential (primary) hypertension");
        putCategory("Circulatory", "I21.9", "Acute myocardial infarction, unspecified");
        putCategory("Circulatory", "I50.9", "Heart failure, unspecified");
        putCategory("Respiratory", "J06.9", "Acute upper respiratory infection, unspecified");
        putCategory("Respiratory", "J18.9", "Pneumonia, unspecified organism");
        putCategory("Respiratory", "J44.1", "COPD with acute exacerbation");
        putCategory("Endocrine", "E11.9", "Type 2 diabetes mellitus without complications");
        putCategory("Digestive", "K21.0", "GERD with esophagitis");
        putCategory("Digestive", "K35.80", "Unspecified acute appendicitis");
        putCategory("Infectious", "A41.9", "Sepsis, unspecified organism");
        putCategory("Genitourinary", "N39.0", "Urinary tract infection, site not specified");
        putCategory("Nervous", "G43.909", "Migraine, unspecified, not intractable");
    }

    private static void putCategory(String category, String code, String description) {
        ICD10_CODES.put(code, Map.of(
                "code", code,
                "description", description,
                "category", category
        ));
    }

    @Override
    public Icd10CodeVO lookupByCode(String code) {
        Map<String, String> row = ICD10_CODES.get(code);
        if (row == null) {
            return null;
        }
        Icd10CodeVO vo = new Icd10CodeVO();
        vo.setCode(row.get("code"));
        vo.setDescription(row.get("description"));
        vo.setCategory(row.get("category"));
        return vo;
    }

    @Override
    public List<Icd10CodeVO> searchByText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String lower = text.toLowerCase();
        List<Icd10CodeVO> results = new ArrayList<>();
        for (Map<String, String> row : ICD10_CODES.values()) {
            if (row.get("description").toLowerCase().contains(lower)
                    || row.get("code").toLowerCase().contains(lower)) {
                Icd10CodeVO vo = new Icd10CodeVO();
                vo.setCode(row.get("code"));
                vo.setDescription(row.get("description"));
                vo.setCategory(row.get("category"));
                results.add(vo);
            }
        }
        return results;
    }

    @Override
    public String searchAsText(String diseaseName) {
        List<Icd10CodeVO> list = searchByText(diseaseName);
        if (list.isEmpty()) {
            return "未找到匹配的 ICD-10 编码";
        }
        return list.stream()
                .limit(5)
                .map(v -> v.getCode() + " - " + v.getDescription())
                .collect(Collectors.joining("\n"));
    }
}
