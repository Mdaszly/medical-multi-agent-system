package com.medical.service.kg.clinical;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 非临床话语过滤：寒暄、预约、医保等不应进入症状映射管道。
 */
@Component
public class NonClinicalPhraseFilter {

    private static final Pattern PURE_GREETING = Pattern.compile(
            "^(你好|您好|hi|hello|在吗|请问|谢谢|感谢)?\\s*(医生|大夫|老师|护士)?(你好|您好)?\\s*[！!。．.？?~～]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern NON_CLINICAL_INTENT = Pattern.compile(
            "(预约|挂号|报销|医保|体检|天气|工作压力|考试成绩|怎么收费|几点上班)");

    private static final Set<String> SYMPTOM_SIGNAL_CHARS = Set.of(
            "痛", "疼", "热", "咳", "喘", "晕", "吐", "泻", "痒", "疹", "闷", "慌", "乏", "疲",
            "嗽", "嚏", "血", "肿", "红", "麻", "寒", "汗", "眠", "食", "欲", "烧", "胀", "酸", "鸣");

    public boolean isPureNonClinical(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String trimmed = text.trim();
        if (PURE_GREETING.matcher(trimmed).matches()) {
            return true;
        }
        if (NON_CLINICAL_INTENT.matcher(trimmed).find() && !hasSymptomSignal(trimmed)) {
            return true;
        }
        return trimmed.length() <= 6
                && !hasSymptomSignal(trimmed)
                && (trimmed.contains("医生") || trimmed.contains("大夫") || trimmed.contains("你好") || trimmed.contains("您好"));
    }

    public boolean hasSymptomSignal(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (SYMPTOM_SIGNAL_CHARS.contains(String.valueOf(text.charAt(i)))) {
                return true;
            }
        }
        return false;
    }
}
