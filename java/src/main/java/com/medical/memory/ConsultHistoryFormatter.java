package com.medical.memory;

import com.medical.constant.ConsultConstant;
import com.medical.model.vo.ChatMessageVO;
import org.springframework.util.StringUtils;

import java.util.List;

public final class ConsultHistoryFormatter {

    private ConsultHistoryFormatter() {
    }

    public static String format(List<ChatMessageVO> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("历史对话：\n");
        for (ChatMessageVO message : messages) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            String roleLabel = ConsultConstant.ROLE_ASSISTANT.equals(message.getRole()) ? "助手" : "用户";
            sb.append(roleLabel).append("：").append(message.getContent()).append("\n");
        }
        return sb.toString();
    }
}
