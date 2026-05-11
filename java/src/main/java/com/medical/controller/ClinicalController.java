package com.medical.controller;

import com.medical.graph.ClinicalPipeline;
import com.medical.model.ClinicalState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//1. REST API控制器，处理临床决策相关的HTTP请求
//2. @RestController = @Controller + @ResponseBody，自动将返回值转为JSON
@RestController
@RequestMapping("/api/v1/clinical")  //3. 所有接口前缀
public class ClinicalController {

    //4. 依赖注入ClinicalPipeline，Spring自动实例化并注入
    private final ClinicalPipeline pipeline;

    public ClinicalController(ClinicalPipeline pipeline) {
        this.pipeline = pipeline;
    }

    //5. POST /api/v1/clinical/analyze - 执行完整的5-Agent临床分析流程
    //6. @Valid触发Bean Validation，校验请求参数
    @PostMapping("/analyze")
    public ResponseEntity<ClinicalState> analyze(@Valid @RequestBody AnalyzeRequest request) {
        //7. 调用Pipeline执行完整流程：Intake→Diagnosis→Treatment→Coding→Audit
        ClinicalState result = pipeline.invoke(request.getPatientDescription());
        return ResponseEntity.ok(result);  //8. 返回200 OK + 完整的ClinicalState对象
    }

    //9. GET /api/v1/clinical/health - 健康检查接口
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "clinical-decision-system-java",
                "version", "1.0.0"
        ));
    }

    //10. 请求体数据模型，包含参数校验注解
    @Data
    public static class AnalyzeRequest {
        @NotBlank(message = "Patient description is required")   //11. 非空校验
        @Size(min = 10, message = "Description must be at least 10 characters")  //12. 最小长度校验
        private String patientDescription;
    }
}
