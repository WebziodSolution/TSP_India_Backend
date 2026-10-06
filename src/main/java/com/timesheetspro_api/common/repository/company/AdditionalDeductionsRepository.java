package com.timesheetspro_api.common.repository.company;

import com.timesheetspro_api.common.model.additionalDeductions.AdditionalDeductions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalDeductionsRepository extends JpaRepository<AdditionalDeductions, Integer> {
    @Query("SELECT a FROM AdditionalDeductions a WHERE a.companyEmployee.id=:userId AND a.month=:month AND a.isSalaryGenerated='N'")
    List<AdditionalDeductions> findByUserIdAndMonth(Integer userId, String month);

    @Query("SELECT a FROM AdditionalDeductions a WHERE a.companyEmployee.id=:userId AND a.month=:month")
    List<AdditionalDeductions> findAllByUserIdAndMonth(Integer userId, String month);
}
