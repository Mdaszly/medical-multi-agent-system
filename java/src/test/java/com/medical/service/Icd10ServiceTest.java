package com.medical.service;

import com.medical.model.vo.Icd10CodeVO;
import com.medical.service.impl.Icd10ServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Icd10ServiceTest {

    private final Icd10Service service = new Icd10ServiceImpl();

    @Test
    void searchByText_findsHypertension() {
        List<Icd10CodeVO> results = service.searchByText("hypertension");
        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).getCode());
    }
}
