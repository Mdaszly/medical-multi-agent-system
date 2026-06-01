package com.medical.service.kg.clinical;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NonClinicalPhraseFilterTest {

    private NonClinicalPhraseFilter filter;

    @BeforeEach
    void setUp() {
        filter = new NonClinicalPhraseFilter();
    }

    @Test
    void pureGreeting_detected() {
        assertTrue(filter.isPureNonClinical("你好医生"));
        assertTrue(filter.isPureNonClinical("您好"));
        assertTrue(filter.isPureNonClinical("医生你好！"));
    }

    @Test
    void symptomText_notPureNonClinical() {
        assertFalse(filter.isPureNonClinical("我头疼"));
        assertFalse(filter.isPureNonClinical("有点发烧"));
    }

    @Test
    void mixedGreetingWithSymptom_notPureNonClinical() {
        assertFalse(filter.isPureNonClinical("医生你好，我头疼"));
    }
}
