package com.timesheetspro_api.userInOut.service;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeCardPdfServiceTest {

    @Test
    void testGeneratePdfWithSpecialCharacters() {
        TimeCardPdfService service = new TimeCardPdfService();

        List<Map<String, Object>> users = new ArrayList<>();

        for (int u = 1; u <= 2; u++) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", "Employee " + u);
            user.put("department", "UI & UX");
            user.put("presentCount", 26);
            user.put("absentCount", 2);
            user.put("weeklyOffCount", 4);
            user.put("holidayCount", 0);
            user.put("daySalary", 1000.0);

            List<Map<String, Object>> data = new ArrayList<>();
            for (int day = 1; day <= 31; day++) {
                Map<String, Object> row = new HashMap<>();
                row.put("createdOn", String.format("2026-10-%02d, Day", day));
                row.put("timeIn", String.format("2026-10-%02d, 08:30 AM", day));
                row.put("timeOut", String.format("2026-10-%02d, 05:30 PM", day));
                row.put("workHours", "09:00");
                row.put("todaySalary", 1000.0);
                row.put("foodCharge", 33.33);
                row.put("netSalary", 966.67);
                row.put("status", "P");
                data.add(row);
            }
            user.put("data", data);

            List<com.timesheetspro_api.common.dto.deductions.DeductionsDto> allowances = new ArrayList<>();
            com.timesheetspro_api.common.dto.deductions.DeductionsDto a1 = new com.timesheetspro_api.common.dto.deductions.DeductionsDto();
            a1.setLabel("HRA");
            a1.setAmount(500);
            allowances.add(a1);
            com.timesheetspro_api.common.dto.deductions.DeductionsDto a2 = new com.timesheetspro_api.common.dto.deductions.DeductionsDto();
            a2.setLabel("Transport Allowance");
            a2.setAmount(300);
            allowances.add(a2);
            user.put("allowances", allowances);

            List<com.timesheetspro_api.common.dto.deductions.DeductionsDto> deductions = new ArrayList<>();
            com.timesheetspro_api.common.dto.deductions.DeductionsDto d1 = new com.timesheetspro_api.common.dto.deductions.DeductionsDto();
            d1.setLabel("TDS");
            d1.setAmount(300);
            deductions.add(d1);
            com.timesheetspro_api.common.dto.deductions.DeductionsDto d2 = new com.timesheetspro_api.common.dto.deductions.DeductionsDto();
            d2.setLabel("PF");
            d2.setAmount(1800);
            deductions.add(d2);
            user.put("deductions", deductions);

            users.add(user);
        }

        byte[] pdf = service.generatePdf(users, "Google", "01/09/2026", "30/09/2026", true);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(pdf)) {
            assertEquals(2, document.getNumberOfPages(), "Each user must fit on exactly 1 page!");
        } catch (Exception e) {
            fail(e);
        }
    }
}
