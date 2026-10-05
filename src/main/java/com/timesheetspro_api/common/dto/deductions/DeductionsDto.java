package com.timesheetspro_api.common.dto.deductions;

import lombok.Data;

@Data
public class DeductionsDto {
    private Integer id;
    private Integer employeeId;
    private String type;
    private String label;
    private String name;
    private Integer amount;

    public void setLabel(String label) {
        this.label = label;
        if (this.name == null) {
            this.name = label;
        }
    }

    public void setName(String name) {
        this.name = name;
        if (this.label == null) {
            this.label = name;
        }
    }
}
