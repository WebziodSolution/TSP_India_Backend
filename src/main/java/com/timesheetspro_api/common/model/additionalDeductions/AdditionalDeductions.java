package com.timesheetspro_api.common.model.additionalDeductions;

import com.timesheetspro_api.common.model.CompanyEmployee.CompanyEmployee;
import com.timesheetspro_api.common.model.salaryStatementHistory.SalaryStatementHistory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "additional_deductions")
@Setter
@Getter
@NoArgsConstructor
public class AdditionalDeductions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private CompanyEmployee companyEmployee;

    @Column(name = "month")
    private String month;

    @Column(name = "title")
    private String title;

    @Column(name = "amount")
    private String amount;

    @Column(name = "type")
    private String type;

    @Column(name = "is_salary_generated", columnDefinition = "CHAR(1)")
    private String isSalaryGenerated;

    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_id", referencedColumnName = "id")
    private SalaryStatementHistory salaryStatementHistory;
}
