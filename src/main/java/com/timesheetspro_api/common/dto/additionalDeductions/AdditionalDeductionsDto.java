package com.timesheetspro_api.common.dto.additionalDeductions;

import lombok.Data;

@Data
public class AdditionalDeductionsDto {
    private Integer id;
    private Integer userId;
    private String month;
    private String title;
    private String amount;
    private String type;
    private String isSalaryGenerated;
    private String createdDate;
    private Integer salaryId;
}
