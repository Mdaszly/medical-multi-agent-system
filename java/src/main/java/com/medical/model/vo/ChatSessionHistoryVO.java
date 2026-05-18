package com.medical.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatSessionHistoryVO {

    private List<ChatSessionVO> today = new ArrayList<>();
    private List<ChatSessionVO> last30Days = new ArrayList<>();
    private List<ChatSessionVO> lastYear = new ArrayList<>();
    private List<ChatSessionVO> olderThanYear = new ArrayList<>();
}
