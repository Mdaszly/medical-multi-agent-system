package com.medical.service;

import com.medical.model.vo.DrugInteractionVO;

import java.util.List;

public interface DrugInteractionService {

    List<DrugInteractionVO> checkInteractions(List<String> newDrugs, List<String> currentDrugs);

    String checkAsText(List<String> newDrugs, List<String> currentDrugs);
}
