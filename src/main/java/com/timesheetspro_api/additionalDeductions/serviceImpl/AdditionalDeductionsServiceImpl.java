package com.timesheetspro_api.additionalDeductions.serviceImpl;

import com.timesheetspro_api.additionalDeductions.service.AdditionalDeductionsService;
import com.timesheetspro_api.common.dto.additionalDeductions.AdditionalDeductionsDto;
import com.timesheetspro_api.common.model.CompanyEmployee.CompanyEmployee;
import com.timesheetspro_api.common.model.additionalDeductions.AdditionalDeductions;
import com.timesheetspro_api.common.model.salaryStatementHistory.SalaryStatementHistory;
import com.timesheetspro_api.common.repository.company.AdditionalDeductionsRepository;
import com.timesheetspro_api.common.repository.company.CompanyEmployeeRepository;
import com.timesheetspro_api.common.repository.company.SalaryStatementHistoryRepository;
import com.timesheetspro_api.common.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("AdditionalDeductionsService")
public class AdditionalDeductionsServiceImpl implements AdditionalDeductionsService {

    @Autowired
    private AdditionalDeductionsRepository additionalDeductionsRepository;

    @Autowired
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Autowired
    private SalaryStatementHistoryRepository salaryStatementHistoryRepository;

    @Autowired
    private CommonService commonService;

    @Override
    public List<AdditionalDeductionsDto> findByMonthAndUser(Integer userId, String month) {
        try {
            List<AdditionalDeductions> additionalDeductionsList = this.additionalDeductionsRepository
                    .findByUserIdAndMonth(userId, month);
            List<AdditionalDeductionsDto> additionalDeductionsDtoList = new ArrayList<>();
            if (!additionalDeductionsList.isEmpty()) {
                for (AdditionalDeductions additionalDeductions : additionalDeductionsList) {
                    additionalDeductionsDtoList.add(this.findById(additionalDeductions.getId()));
                }
            }
            return additionalDeductionsDtoList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AdditionalDeductionsDto findById(Integer id) {
        try {
            AdditionalDeductions additionalDeductions = this.additionalDeductionsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Additional Deductions not found"));
            AdditionalDeductionsDto additionalDeductionsDto = new AdditionalDeductionsDto();
            additionalDeductionsDto.setId(additionalDeductions.getId());
            additionalDeductionsDto.setTitle(additionalDeductions.getTitle());
            additionalDeductionsDto.setMonth(additionalDeductions.getMonth());
            additionalDeductionsDto.setAmount(additionalDeductions.getAmount());
            additionalDeductionsDto.setIsSalaryGenerated(additionalDeductions.getIsSalaryGenerated());
            additionalDeductionsDto.setType(additionalDeductions.getType());
            additionalDeductionsDto.setUserId(additionalDeductions.getCompanyEmployee().getEmployeeId());
            additionalDeductionsDto
                    .setCreatedDate(this.commonService.convertDateToString(additionalDeductions.getCreatedDate()));
            if (additionalDeductions.getSalaryStatementHistory() != null) {
                additionalDeductionsDto.setSalaryId(additionalDeductions.getSalaryStatementHistory().getId());
            }
            return additionalDeductionsDto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(List<AdditionalDeductionsDto> additionalDeductionsDtoList) {
        try {
            for (AdditionalDeductionsDto additionalDeductionsDto : additionalDeductionsDtoList) {
                AdditionalDeductions additionalDeductions = new AdditionalDeductions();
                if (additionalDeductionsDto.getId() != null) {
                    additionalDeductions = this.additionalDeductionsRepository.findById(additionalDeductionsDto.getId())
                            .orElseThrow(() -> new RuntimeException("Additional Deductions not found"));
                } else {
                    additionalDeductions.setCreatedDate(new Date());
                }
                if (additionalDeductionsDto.getUserId() != null) {
                    CompanyEmployee companyEmployee = this.companyEmployeeRepository
                            .findById(additionalDeductionsDto.getUserId())
                            .orElseThrow(() -> new RuntimeException("Company Employee not found"));
                    additionalDeductions.setCompanyEmployee(companyEmployee);
                }
                if (additionalDeductionsDto.getSalaryId() != null) {
                    additionalDeductions.setIsSalaryGenerated("Y");
                    SalaryStatementHistory salaryStatementHistory = this.salaryStatementHistoryRepository
                            .findById(additionalDeductionsDto.getSalaryId())
                            .orElseThrow(() -> new RuntimeException("Salary Statement History not found"));
                    additionalDeductions.setSalaryStatementHistory(salaryStatementHistory);
                } else {
                    additionalDeductions.setIsSalaryGenerated("N");
                    additionalDeductions.setSalaryStatementHistory(null);
                }

                additionalDeductions.setMonth(additionalDeductionsDto.getMonth());
                additionalDeductions.setTitle(additionalDeductionsDto.getTitle());
                additionalDeductions.setAmount(additionalDeductionsDto.getAmount());
                additionalDeductions.setType(additionalDeductionsDto.getType());
                this.additionalDeductionsRepository.save(additionalDeductions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            AdditionalDeductions additionalDeductions = this.additionalDeductionsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Additional Deductions not found"));
            this.additionalDeductionsRepository.delete(additionalDeductions);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
