package com.medical.model.vo;

import com.medical.model.kg.GraphEvidence;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class GraphEvidenceVO {

    private List<SymptomDiagnosisRowVO> rows = new ArrayList<>();
    private List<String> extractedSymptoms = new ArrayList<>();
    private List<String> icdCandidateCodes = new ArrayList<>();
    private boolean graphHit;
    private Long queryTimeMs;
    private String formattedText;
    private String symptomResolutionTrace;
    private List<SymptomMatchVO> symptomMatches = new ArrayList<>();
    private String clinicalTextUsed;
    private String clinicalSpanSource;
    private String graphSkipReason;

    public static GraphEvidenceVO from(GraphEvidence evidence) {
        if (evidence == null) {
            return empty();
        }
        GraphEvidenceVO vo = new GraphEvidenceVO();
        vo.setGraphHit(evidence.isGraphHit());
        vo.setQueryTimeMs(evidence.getQueryTimeMs());
        vo.setFormattedText(evidence.getFormattedText());
        vo.setSymptomResolutionTrace(evidence.getSymptomResolutionTrace());
        vo.setClinicalTextUsed(evidence.getClinicalTextUsed());
        vo.setClinicalSpanSource(evidence.getClinicalSpanSource());
        vo.setGraphSkipReason(evidence.getGraphSkipReason());
        if (evidence.getExtractedSymptoms() != null) {
            vo.setExtractedSymptoms(new ArrayList<>(evidence.getExtractedSymptoms()));
        }
        if (evidence.getIcdCandidateCodes() != null) {
            vo.setIcdCandidateCodes(new ArrayList<>(evidence.getIcdCandidateCodes()));
        }
        if (evidence.getRows() != null) {
            vo.setRows(evidence.getRows().stream()
                    .map(SymptomDiagnosisRowVO::from)
                    .collect(Collectors.toList()));
        }
        if (evidence.getSymptomMatches() != null) {
            vo.setSymptomMatches(evidence.getSymptomMatches().stream()
                    .map(SymptomMatchVO::from)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    public static GraphEvidenceVO fromRows(List<SymptomDiagnosisRowVO> rows,
                                           Set<String> extractedSymptoms,
                                           String trace,
                                           List<SymptomMatchVO> matches,
                                           Long queryTimeMs,
                                           String formattedText) {
        GraphEvidenceVO vo = new GraphEvidenceVO();
        vo.setRows(rows != null ? rows : new ArrayList<>());
        vo.setGraphHit(vo.getRows() != null && !vo.getRows().isEmpty());
        if (extractedSymptoms != null) {
            vo.setExtractedSymptoms(new ArrayList<>(extractedSymptoms));
        }
        vo.setSymptomResolutionTrace(trace);
        vo.setSymptomMatches(matches != null ? matches : new ArrayList<>());
        vo.setQueryTimeMs(queryTimeMs);
        vo.setFormattedText(formattedText);
        if (vo.getRows() != null) {
            vo.setIcdCandidateCodes(vo.getRows().stream()
                    .map(SymptomDiagnosisRowVO::getIcdCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    public static GraphEvidenceVO empty() {
        GraphEvidenceVO vo = new GraphEvidenceVO();
        vo.setGraphHit(false);
        vo.setRows(new ArrayList<>());
        vo.setExtractedSymptoms(new ArrayList<>());
        vo.setIcdCandidateCodes(new ArrayList<>());
        vo.setSymptomMatches(new ArrayList<>());
        return vo;
    }
}
