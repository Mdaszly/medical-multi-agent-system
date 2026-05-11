package com.medical.graph;

import com.medical.agent.*;
import com.medical.model.ClinicalState;
import org.springframework.stereotype.Component;

//1. 5-Agent临床决策流水线编排器（LangGraph4j风格实现）
//2. 核心流程：Intake → Diagnosis → Treatment → Coding → Audit
//3. 支持条件路由：Diagnosis发现信息不足时回退到Intake补充（最多重试2次）
@Component
public class ClinicalPipeline {

    //4. 依赖注入5个Agent，通过Spring DI自动装配
    private final IntakeAgent intakeAgent;
    private final DiagnosisAgent diagnosisAgent;
    private final TreatmentAgent treatmentAgent;
    private final CodingAgent codingAgent;
    private final AuditAgent auditAgent;

    //5. 诊断重试上限，防止LLM幻觉导致无限循环
    private static final int MAX_DIAGNOSIS_RETRIES = 2;

    //6. 构造函数注入所有Agent依赖
    public ClinicalPipeline(
            IntakeAgent intakeAgent,
            DiagnosisAgent diagnosisAgent,
            TreatmentAgent treatmentAgent,
            CodingAgent codingAgent,
            AuditAgent auditAgent) {
        this.intakeAgent = intakeAgent;
        this.diagnosisAgent = diagnosisAgent;
        this.treatmentAgent = treatmentAgent;
        this.codingAgent = codingAgent;
        this.auditAgent = auditAgent;
    }

    //7. 执行完整的临床决策Pipeline
    //8. 输入：患者描述文本；输出：包含所有Agent结果的ClinicalState
    public ClinicalState invoke(String rawInput) {
        //9. 初始化ClinicalState，作为整个Pipeline的共享状态容器
        ClinicalState state = ClinicalState.builder()
                .rawInput(rawInput)
                .build();

        //10. Step 1: Intake Agent - 将自然语言描述转换为结构化患者信息
        state = intakeAgent.process(state);

        //11. Step 2: Diagnosis Agent - 基于患者信息进行鉴别诊断
        //12. do-while循环实现条件路由：若needsMoreInfo=true则回退到Intake
        int retries = 0;
        do {
            state = diagnosisAgent.process(state);
            //13. 检查是否需要补充信息且未超过重试次数
            if (state.isNeedsMoreInfo() && retries < MAX_DIAGNOSIS_RETRIES) {
                state = intakeAgent.process(state);  //14. 回退到Intake补充信息
            }
            retries++;
        } while (state.isNeedsMoreInfo() && retries <= MAX_DIAGNOSIS_RETRIES);

        //15. Step 3: Treatment Agent - 根据诊断结果生成治疗方案（含DDI检查）
        state = treatmentAgent.process(state);

        //16. Step 4: Coding Agent - 将诊断映射为ICD-10编码和DRGs分组
        state = codingAgent.process(state);

        //17. Step 5: Audit Agent - HIPAA合规审计（纯规则引擎，不调用LLM）
        state = auditAgent.process(state);

        //18. 返回包含所有Agent输出的最终状态
        return state;
    }
}
