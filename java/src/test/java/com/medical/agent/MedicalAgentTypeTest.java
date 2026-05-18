package com.medical.agent;

import com.medical.agent.enums.MedicalAgentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicalAgentTypeTest {

    @Test
    void fromCode_parsesInitial() {
        assertEquals(MedicalAgentType.INITIAL, MedicalAgentType.fromCode("INITIAL"));
        assertEquals(MedicalAgentType.MEDICATION, MedicalAgentType.fromCode("medication"));
    }

    @Test
    void fromCode_returnsNullForUnknown() {
        assertNull(MedicalAgentType.fromCode("UNKNOWN_TYPE"));
    }
}
