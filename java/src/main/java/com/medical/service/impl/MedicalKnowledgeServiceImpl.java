package com.medical.service.impl;

import com.medical.service.MedicalKnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class MedicalKnowledgeServiceImpl implements MedicalKnowledgeService {

    private static final List<Map<String, String>> KNOWLEDGE = List.of(
            Map.of("topic", "高血压", "content", "成人高血压管理强调生活方式干预、限盐、规律监测血压，用药需遵医嘱。"),
            Map.of("topic", "糖尿病", "content", "2型糖尿病需关注血糖、并发症筛查；二甲双胍为常用一线药物，需警惕低血糖与肾功能。"),
            Map.of("topic", "肺炎", "content", "社区获得性肺炎常见症状为发热、咳嗽、咳痰；重症需及时就医评估氧合与影像学。"),
            Map.of("topic", "胃食管反流", "content", "GERD可表现为烧心、反酸；建议少食多餐、避免睡前进食，必要时使用PPI类药物。"),
            Map.of("topic", "布洛芬", "content", "NSAIDs 可能引起胃肠道不适，消化道溃疡、肾功能不全患者需谨慎。"),
            Map.of("topic", "血常规", "content", "白细胞升高常见于感染；需结合临床症状、CRP及影像学综合判断。"),
            Map.of("topic", "肝功能", "content", "ALT/AST升高提示肝细胞损伤；需排查病毒性肝炎、药物性肝损、脂肪肝等。"),
            Map.of("topic", "运动", "content", "慢性病患者运动宜循序渐进，每周至少150分钟中等强度有氧运动。")
    );

    @Override
    public String search(String query) {
        if (!StringUtils.hasText(query)) {
            return "无检索关键词";
        }
        String lower = query.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map<String, String> item : KNOWLEDGE) {
            String topic = item.get("topic");
            String content = item.get("content");
            if (lower.contains(topic.toLowerCase()) || topic.toLowerCase().contains(lower)
                    || content.toLowerCase().contains(lower)) {
                sb.append("- ").append(topic).append("：").append(content).append("\n");
                count++;
            }
        }
        if (count == 0) {
            return "本地知识库未命中，请结合患者具体情况与临床指南判断。";
        }
        return sb.toString();
    }
}
