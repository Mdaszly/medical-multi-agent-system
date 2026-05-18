package com.medical.service.impl;

import com.medical.model.vo.DrugInteractionVO;
import com.medical.service.DrugInteractionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DrugInteractionServiceImpl implements DrugInteractionService {

    private static final Map<String, String> DRUG_CLASS_MAP = Map.ofEntries(
            Map.entry("lisinopril", "ace_inhibitor"),
            Map.entry("enalapril", "ace_inhibitor"),
            Map.entry("fluoxetine", "ssri"),
            Map.entry("sertraline", "ssri"),
            Map.entry("ibuprofen", "nsaid"),
            Map.entry("naproxen", "nsaid"),
            Map.entry("布洛芬", "nsaid"),
            Map.entry("阿司匹林", "aspirin"),
            Map.entry("aspirin", "aspirin"),
            Map.entry("华法林", "warfarin"),
            Map.entry("warfarin", "warfarin"),
            Map.entry("二甲双胍", "metformin"),
            Map.entry("metformin", "metformin")
    );

    private record DdiRule(String drugA, String drugB, String severity, String description, String recommendation) {
    }

    private static final List<DdiRule> DDI_RULES = List.of(
            new DdiRule("warfarin", "aspirin", "major",
                    "Increased risk of bleeding", "Avoid combination unless indicated; monitor INR"),
            new DdiRule("metformin", "contrast_dye", "major",
                    "Risk of lactic acidosis", "Hold metformin 48h around contrast procedures"),
            new DdiRule("ssri", "maoi", "contraindicated",
                    "Serotonin syndrome risk", "Absolute contraindication"),
            new DdiRule("ace_inhibitor", "potassium_supplement", "moderate",
                    "Hyperkalemia risk", "Monitor serum potassium"),
            new DdiRule("nsaid", "warfarin", "major",
                    "Increased bleeding risk", "Avoid or monitor closely"),
            new DdiRule("nsaid", "ace_inhibitor", "moderate",
                    "Reduced antihypertensive effect", "Monitor blood pressure and renal function")
    );

    @Override
    public List<DrugInteractionVO> checkInteractions(List<String> newDrugs, List<String> currentDrugs) {
        List<String> newNorm = normalizeList(newDrugs);
        List<String> currentNorm = normalizeList(currentDrugs);
        List<DrugInteractionVO> results = new ArrayList<>();

        for (DdiRule rule : DDI_RULES) {
            boolean hit = (newNorm.contains(rule.drugA()) && currentNorm.contains(rule.drugB()))
                    || (newNorm.contains(rule.drugB()) && currentNorm.contains(rule.drugA()))
                    || (newNorm.contains(rule.drugA()) && newNorm.contains(rule.drugB()));
            if (hit) {
                DrugInteractionVO vo = new DrugInteractionVO();
                vo.setDrugA(rule.drugA());
                vo.setDrugB(rule.drugB());
                vo.setSeverity(rule.severity());
                vo.setDescription(rule.description());
                vo.setRecommendation(rule.recommendation());
                results.add(vo);
            }
        }
        return results;
    }

    @Override
    public String checkAsText(List<String> newDrugs, List<String> currentDrugs) {
        List<DrugInteractionVO> list = checkInteractions(newDrugs, currentDrugs);
        if (list.isEmpty()) {
            return "未发现已知严重药物相互作用（基于本地规则库）";
        }
        StringBuilder sb = new StringBuilder();
        for (DrugInteractionVO vo : list) {
            sb.append("- [").append(vo.getSeverity()).append("] ")
                    .append(vo.getDrugA()).append(" + ").append(vo.getDrugB())
                    .append(": ").append(vo.getDescription())
                    .append("；建议：").append(vo.getRecommendation()).append("\n");
        }
        return sb.toString();
    }

    private List<String> normalizeList(List<String> drugs) {
        List<String> result = new ArrayList<>();
        if (drugs == null) {
            return result;
        }
        for (String drug : drugs) {
            if (!StringUtils.hasText(drug)) {
                continue;
            }
            String lower = drug.toLowerCase(Locale.ROOT).trim();
            result.add(lower);
            if (DRUG_CLASS_MAP.containsKey(lower)) {
                result.add(DRUG_CLASS_MAP.get(lower));
            }
        }
        return result;
    }
}
