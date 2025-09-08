package com.nhom4.xoxo.dto.req;

import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportTargetType;
import com.nhom4.xoxo.validation.ValidEnum;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportRequest {
    private Long reporterId;
    
    @NotNull(message = "Loại đối tượng báo cáo không được để trống")
    private ReportTargetType reportTargetType;
    
    @NotNull(message = "ID đối tượng báo cáo không được để trống")
    @Min(value = 1, message = "ID đối tượng phải lớn hơn 0")
    private Long reportTargetId;
    
    @NotNull(message = "Lý do báo cáo không được để trống")
    private ReportReason reportReason;
    
    @Size(max = 1000, message = "Thông tin bổ sung không được vượt quá 1000 ký tự")
    private String additionalInfo;
    
    private Boolean isAnonymous = false;
    
    @Min(value = 1, message = "Mức độ ưu tiên phải từ 1 đến 4")
    @Max(value = 4, message = "Mức độ ưu tiên phải từ 1 đến 4")
    private Integer priority = 1; // 1=Low, 2=Medium, 3=High, 4=Critical
}
