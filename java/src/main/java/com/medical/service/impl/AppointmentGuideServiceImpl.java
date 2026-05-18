package com.medical.service.impl;

import com.medical.constant.ConsultConstant;
import com.medical.model.vo.DoctorVO;
import com.medical.service.AppointmentGuideService;
import com.medical.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentGuideServiceImpl implements AppointmentGuideService {

    private final DoctorService doctorService;

    @Override
    public String suggestDoctors(String department) {
        String resolved = resolveDepartment(department);
        List<DoctorVO> doctors = doctorService.listDoctorByDepartment(resolved);
        if (doctors == null || doctors.isEmpty()) {
            return "科室「" + resolved + "」暂无可预约医生，建议选择全科或其他相关科室。";
        }
        StringBuilder sb = new StringBuilder("推荐医生（" + resolved + "）：\n");
        int limit = Math.min(doctors.size(), 5);
        for (int i = 0; i < limit; i++) {
            DoctorVO d = doctors.get(i);
            sb.append("- ").append(d.getDoctorName());
            if (StringUtils.hasText(d.getTitle())) {
                sb.append("（").append(d.getTitle()).append("）");
            }
            if (d.getConsultationFee() != null) {
                sb.append("，挂号费 ¥").append(d.getConsultationFee());
            }
            sb.append("\n");
        }
        sb.append("可在患者端「预约挂号」中选择医生与时段完成预约。");
        return sb.toString();
    }

    private String resolveDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return "内科";
        }
        return Arrays.stream(ConsultConstant.DEPARTMENTS)
                .filter(d -> department.contains(d) || d.contains(department))
                .findFirst()
                .orElse(department);
    }
}
