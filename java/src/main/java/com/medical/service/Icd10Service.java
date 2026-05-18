package com.medical.service;

import com.medical.model.vo.Icd10CodeVO;

import java.util.List;

public interface Icd10Service {

    Icd10CodeVO lookupByCode(String code);

    List<Icd10CodeVO> searchByText(String text);

    String searchAsText(String diseaseName);
}
