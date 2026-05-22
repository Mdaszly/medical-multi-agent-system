package com.medical.model.dto.kg;

import lombok.Data;

import java.util.List;

@Data
public class VectorTopKEvalRequest {

    /** 评测 Top-K，默认 5 */
    private Integer k = 5;

    /** 多 K 对比实验，如 [1,3,5,10] */
    private List<Integer> kValues;

    /** 是否返回每条用例明细 */
    private Boolean includeCaseDetails = false;

    /** 可选：classpath 数据集路径，默认 kg/vector_topk_eval.json */
    private String datasetPath;
}
