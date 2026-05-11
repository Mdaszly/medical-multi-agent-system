package com.medical.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//1. 临床决策Pipeline的共享状态模型
//2. 所有Agent通过此对象传递数据，实现松耦合通信
//3. 使用Lombok注解简化代码：@Data生成getter/setter，@Builder支持链式构建
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  //4. 反序列化时忽略未知字段
public class ClinicalState {

    //5. 原始患者描述文本（外部输入）
    @Builder.Default
    private String rawInput = "";

    //6. 结构化患者信息（Intake Agent写入，Diagnosis/Treatment读取）
    private Map<String, Object> patientInfo;

    //7. 鉴别诊断结果（Diagnosis Agent写入，Treatment/Coding读取）
    private Map<String, Object> diagnosis;

    //8. 是否需要补充信息（Diagnosis Agent写入，Pipeline路由判断）
    @Builder.Default
    private boolean needsMoreInfo = false;

    //9. 治疗方案（Treatment Agent写入，Coding读取）
    private Map<String, Object> treatmentPlan;

    //10. ICD-10编码和DRGs分组（Coding Agent写入，Audit读取）
    private Map<String, Object> codingResult;

    //11. HIPAA合规审计结果（Audit Agent写入）
    private Map<String, Object> auditResult;

    //12. 当前正在执行的Agent名称（用于日志和监控）
    @Builder.Default
    private String currentAgent = "";

    //13. Pipeline执行过程中累积的错误信息（所有Agent可写入）
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
