package com.medical.constant;

public final class ConsultConstant {

    private ConsultConstant() {
    }

    public static final String SCENE_CONSULTATION = "consultation";
    public static final String SCENE_TRIAGE = "triage";
    public static final String SCENE_MEDICATION = "medication";

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";

    public static final int HISTORY_MAX_SIZE = 30;

    public static final String DEFAULT_SESSION_TITLE = "医疗问诊会话";

    public static final String DISCLAIMER =
            "以上内容仅用于健康科普、预问诊和就医参考，不能替代医生诊断、处方或治疗。";

    public static final String[] DEPARTMENTS = {
            "消化科", "妇产科", "皮肤科", "内分泌科", "神经内科", "骨科", "外科",
            "内科", "男科", "生殖医学科", "眼科", "肾脏病中心", "泌尿外科", "呼吸科", "药学门诊", "全科"
    };
}
