package com.medical.common;

import lombok.Data;

@Data
public class PageRequest {

    private Long current = 1L;

    private Long pageSize = 10L;

    private String sortField;

    private String sortOrder;
}
