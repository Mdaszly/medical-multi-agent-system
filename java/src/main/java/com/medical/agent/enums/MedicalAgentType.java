package com.medical.agent.enums;

import lombok.Getter;

@Getter
public enum MedicalAgentType {

    ROUTER("router", "路由Agent"),
    INITIAL("initial", "初诊Agent"),
    FOLLOWUP("followup", "复诊Agent"),
    MEDICATION("medication", "用药Agent"),
    REPORT("report", "报告解读Agent"),
    HEALTH("health", "健康咨询Agent"),
    APPOINTMENT("appointment", "挂号引导Agent");

    private final String code;
    private final String name;

    MedicalAgentType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static MedicalAgentType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase();
        for (MedicalAgentType type : values()) {
            if (type == ROUTER) {
                continue;
            }
            if (normalized.equals(type.name())
                    || normalized.equals(type.code.toUpperCase())
                    || normalized.contains(type.name())) {
                return type;
            }
        }
        return null;
    }
}
