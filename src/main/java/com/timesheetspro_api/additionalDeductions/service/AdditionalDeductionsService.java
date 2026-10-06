package com.timesheetspro_api.additionalDeductions.service;

import com.timesheetspro_api.common.dto.additionalDeductions.AdditionalDeductionsDto;

import java.util.List;

public interface AdditionalDeductionsService {
    List<AdditionalDeductionsDto> findByMonthAndUser(Integer userId, String month);

    AdditionalDeductionsDto findById(Integer id);

    void save(List<AdditionalDeductionsDto> additionalDeductionsDtoList);

    void delete(Integer id);
}
