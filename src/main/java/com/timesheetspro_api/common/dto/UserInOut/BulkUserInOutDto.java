package com.timesheetspro_api.common.dto.UserInOut;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserInOutDto {
    private String timeIn;
    private String timeOut;
    private String startDate;
    private String endDate;
    private List<Integer> userId;
    private Integer companyId;
}
