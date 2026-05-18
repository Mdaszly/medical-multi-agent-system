package com.medical.service;

import com.medical.model.vo.DrugInteractionVO;
import com.medical.service.impl.DrugInteractionServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DrugInteractionServiceTest {

    private final DrugInteractionService service = new DrugInteractionServiceImpl();

    @Test
    void detectsWarfarinAspirinInteraction() {
        List<DrugInteractionVO> hits = service.checkInteractions(
                List.of("warfarin"), List.of("aspirin"));
        assertFalse(hits.isEmpty());
    }
}
