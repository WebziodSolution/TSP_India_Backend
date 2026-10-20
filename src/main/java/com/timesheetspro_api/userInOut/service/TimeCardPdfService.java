package com.timesheetspro_api.userInOut.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.timesheetspro_api.common.dto.deductions.DeductionsDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TimeCardPdfService {

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat integerFormat = new DecimalFormat("#,##0");

    public byte[] generatePdf(List<Map<String, Object>> users, String companyName, String startDate, String endDate,
            boolean isDetail) {
        String html = buildHtml(users, companyName, startDate, endDate, isDetail);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            try {
                if (getClass().getResource("/fonts/Arial.ttf") != null) {
                    builder.useFont(() -> getClass().getResourceAsStream("/fonts/Arial.ttf"), "Arial", 400,
                            PdfRendererBuilder.FontStyle.NORMAL, true);
                    builder.useFont(() -> getClass().getResourceAsStream("/fonts/Arial-Bold.ttf"), "Arial", 700,
                            PdfRendererBuilder.FontStyle.NORMAL, true);
                } else {
                    File winArial = new File("C:/Windows/Fonts/arial.ttf");
                    File winArialBd = new File("C:/Windows/Fonts/arialbd.ttf");
                    if (winArial.exists()) {
                        builder.useFont(winArial, "Arial", 400, PdfRendererBuilder.FontStyle.NORMAL, true);
                    }
                    if (winArialBd.exists()) {
                        builder.useFont(winArialBd, "Arial", 700, PdfRendererBuilder.FontStyle.NORMAL, true);
                    }
                }
            } catch (Exception ignored) {
            }

            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TimeCard PDF", e);
        }
    }

    private String buildHtml(List<Map<String, Object>> users, String companyName, String startDate, String endDate,
            boolean isDetail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>");
        sb.append("@page { size: A4 portrait; margin: 8mm 12mm; }");
        sb.append("body { font-family: 'Arial', sans-serif; font-size: 8px; color: #1f2937; margin: 0; padding: 0; }");
        sb.append(".pdf-page { page-break-after: always; width: 100%; box-sizing: border-box; }");
        sb.append(".pdf-page:last-child { page-break-after: auto; }");
        // sb.append(".pdf-card { border: 1px solid #000000; border-radius: 4px;
        // padding: 6px 8px; box-sizing: border-box; background-color: #ffffff; }");
        sb.append(".header-table { width: 100%; border-collapse: collapse; margin-bottom: 2px; }");
        sb.append(
                ".header-title { font-size: 14px; font-weight: 800; text-align: right; color: #111827; margin-bottom: 1px; }");
        sb.append(".header-company { font-size: 13.5px; font-weight: bold; color: #111827; }");
        sb.append(".header-period { font-size: 9px; text-align: right; color: #374151; }");
        sb.append(".divider { border-top: 1.2px solid #000000; margin: 3px 0 4px 0; }");
        sb.append(
                ".user-title { text-align: center; font-size: 11.5px; font-weight: 700; margin-bottom: 2px; color: #000000; }");
        sb.append(".stats-bar { text-align: center; font-size: 8.5px; margin-bottom: 4px; color: #1f2937; }");
        sb.append(".stats-bar span { margin: 0 5px; }");
        sb.append(".stats-bar span b { font-weight: 700; color: #000000; }");
        sb.append(".pdf-table { width: 100%; border-collapse: collapse; table-layout: fixed; margin-bottom: 4px; }");
        sb.append(
                ".pdf-table th { background-color: #d1d5db; border: 1px solid #000000; padding: 2.5px 2px; font-size: 8px; font-weight: bold; text-align: center; }");
        sb.append(
                ".pdf-table td { border: 1px solid #000000; padding: 1.2px 2px; font-size: 7.5px; vertical-align: middle; text-align: center; }");
        sb.append(".pdf-time-cell { text-align: center; }");
        sb.append(".pdf-date-line { font-size: 7px; line-height: 8.5px; color: #4b5563; font-weight: 500; }");
        sb.append(
                ".pdf-time-line { font-size: 7.5px; line-height: 9px; color: #111827; font-weight: 600; margin-top: 0.5px; }");
        sb.append(".total-row { background-color: #f3f4f6; }");
        sb.append(
                ".total-row td { border: 1px solid #000000; padding: 2.5px 2px; font-size: 8px; font-weight: bold; }");
        sb.append(".pdf-footer-section { margin-top: 4px; padding-top: 3px; border-top: 1.2px solid #000000; }");
        sb.append(
                ".cards-table { width: 100%; border-collapse: separate; border-spacing: 6px 0; table-layout: fixed; }");
        sb.append(".card-cell { width: 33.33%; vertical-align: top; padding: 0; }");
        sb.append(".card-box { border: 1px solid #000000; background-color: #ffffff; box-sizing: border-box; }");
        sb.append(
                ".card-header { background-color: #e5e7eb; border-bottom: 1px solid #000000; padding: 2px 4px; font-size: 8.5px; font-weight: bold; text-transform: uppercase; text-align: center; color: #111827; letter-spacing: 0.025em; }");
        sb.append(".card-body { padding: 2px 4px; min-height: 42px; }");
        sb.append(".inner-table { width: 100%; border-collapse: collapse; font-size: 7.5px; }");
        sb.append(".inner-table td { padding: 1px 2px; border: none; border-bottom: 1px solid #e5e7eb; }");
        sb.append(".inner-table tr:last-child td { border-bottom: none; }");
        sb.append(
                ".card-footer { background-color: #f3f4f6; border-top: 1px solid #000000; padding: 2.5px 5px; font-size: 8px; font-weight: bold; color: #111827; }");
        sb.append(".text-left { text-align: left !important; }");
        sb.append(".text-right { text-align: right !important; }");
        sb.append(".text-center { text-align: center !important; }");
        sb.append("</style></head><body>");

        if (users == null || users.isEmpty()) {
            sb.append(
                    "<div class='pdf-page'><div class='pdf-card'><p>No attendance records found.</p></div></div></body></html>");
            return sb.toString();
        }

        for (Map<String, Object> user : users) {
            String userName = String.valueOf(user.getOrDefault("username", user.getOrDefault("userName", "")));
            String department = String.valueOf(user.getOrDefault("department", ""));
            Object presentCount = user.getOrDefault("presentCount", 0);
            Object absentCount = user.getOrDefault("absentCount", 0);
            Object weeklyOffCount = user.getOrDefault("weeklyOffCount", 0);
            Object holidayCount = user.getOrDefault("holidayCount", 0);

            sb.append("<div class='pdf-page'><div class='pdf-card'>");

            // Header
            sb.append("<table class='header-table'><tr>");
            sb.append("<td class='text-left header-company'>").append(escapeXml(companyName != null ? companyName : ""))
                    .append("</td>");
            sb.append("<td class='header-title'>Monthly Report<br/><span class='header-period'><b>Period:</b> ")
                    .append(escapeXml(startDate)).append(" To ").append(escapeXml(endDate)).append("</span></td>");
            sb.append("</tr></table>");
            sb.append("<div class='divider'></div>");

            // Employee Name & Stats Bar
            sb.append("<div class='user-title'>").append(escapeXml(userName));
            if (!department.isEmpty() && !"null".equalsIgnoreCase(department)) {
                sb.append(" - ").append(escapeXml(department));
            }
            sb.append("</div>");
            sb.append("<div class='stats-bar'>");
            sb.append("<span>Present: <b>").append(escapeXml(presentCount)).append("</b></span>");
            sb.append("<span>Absent: <b>").append(escapeXml(absentCount)).append("</b></span>");
            sb.append("<span>Weekly-Off: <b>").append(escapeXml(weeklyOffCount)).append("</b></span>");
            sb.append("<span>Holiday: <b>").append(escapeXml(holidayCount)).append("</b></span>");

            Object hourlyRate = user.get("hourlyRate");
            if (hourlyRate == null)
                hourlyRate = user.get("hourRate");
            Object daySalary = user.get("daySalary");

            if (hourlyRate != null && parseDouble(hourlyRate) > 0) {
                sb.append("<span>Hourly Rate: <b>₹")
                        .append(integerFormat.format(Math.round(parseDouble(hourlyRate)))).append("</b></span>");
            } else if (daySalary != null && parseDouble(daySalary) > 0) {
                sb.append("<span>Day Salary: <b>₹")
                        .append(currencyFormat.format(parseDouble(daySalary))).append("</b></span>");
            }
            sb.append("</div>");

            // Attendance Table
            List<Map<String, Object>> entries = (List<Map<String, Object>>) user.get("data");
            if (entries == null) {
                entries = Collections.emptyList();
            }

            sb.append("<table class='pdf-table'>");
            sb.append("<colgroup>");
            sb.append("<col style='width: 14%;'/>");
            sb.append("<col style='width: 13%;'/>");
            sb.append("<col style='width: 13%;'/>");
            sb.append("<col style='width: 12%;'/>");
            sb.append("<col style='width: 13%;'/>");
            sb.append("<col style='width: 12%;'/>");
            sb.append("<col style='width: 13%;'/>");
            sb.append("<col style='width: 10%;'/>");
            sb.append("</colgroup>");
            sb.append("<thead><tr>");
            sb.append(
                    "<th>Day</th><th>Clock In</th><th>Clock Out</th><th>Work Hours</th><th>Day Salary</th><th>Food Charge</th><th>Net Salary</th><th>Status</th>");
            sb.append("</tr></thead><tbody>");

            double totalDaySalary = 0;
            double totalFoodCharge = 0;
            double totalNetSalary = 0;
            int totalWorkMinutes = 0;

            for (Map<String, Object> row : entries) {
                String status = String.valueOf(row.getOrDefault("status", ""));
                boolean isOff = "H".equalsIgnoreCase(status) || "W".equalsIgnoreCase(status);

                // In TSP India backend, secondary punches on same day have totalHours == ""
                Object totalHoursObj = row.get("totalHours");
                boolean isSecondary = "".equals(totalHoursObj) || "".equals(status);

                String dayDisplay = isSecondary ? "" : formatDay(row.get("createdOn"));
                String timeInHtml = isOff ? "-" : formatDateTimeCell(row.get("timeIn"));
                String timeOutHtml = isOff ? "-" : formatDateTimeCell(row.get("timeOut"));

                String workHours = String.valueOf(row.getOrDefault("workHours", ""));
                if (workHours == null || "null".equals(workHours))
                    workHours = "";

                String workHoursDisplay;
                if (isSecondary) {
                    workHoursDisplay = "";
                } else if (isOff) {
                    workHoursDisplay = "-";
                } else if (workHours.isEmpty() || "-".equals(workHours)) {
                    workHoursDisplay = "-";
                } else {
                    workHoursDisplay = workHours;
                }

                if (!isOff && !isSecondary && workHours.contains(":")) {
                    try {
                        String[] hm = workHours.split(":");
                        totalWorkMinutes += Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);
                    } catch (Exception ignored) {
                    }
                }

                double dSalary = parseDouble(row.get("todaySalary"));
                double fCharge = parseDouble(row.get("foodCharge"));
                double nSalary = parseDouble(row.get("netSalary"));

                if (!isSecondary) {
                    totalDaySalary += dSalary;
                    totalFoodCharge += fCharge;
                    totalNetSalary += (dSalary - fCharge);
                }

                String daySalaryDisplay;
                if (isSecondary) {
                    daySalaryDisplay = "";
                } else if (isOff && row.get("todaySalary") == null) {
                    daySalaryDisplay = "-";
                } else if (row.get("todaySalary") != null && !row.get("todaySalary").toString().isEmpty()) {
                    daySalaryDisplay = formatCurrency(dSalary);
                } else {
                    daySalaryDisplay = "-";
                }

                String foodChargeDisplay;
                if (isSecondary) {
                    foodChargeDisplay = "";
                } else if (isOff && row.get("foodCharge") == null) {
                    foodChargeDisplay = "-";
                } else if (row.get("foodCharge") != null && !row.get("foodCharge").toString().isEmpty()) {
                    foodChargeDisplay = formatCurrency(fCharge);
                } else {
                    foodChargeDisplay = "-";
                }

                String netSalaryDisplay;
                if (isSecondary) {
                    netSalaryDisplay = "";
                } else if (isOff && row.get("netSalary") == null) {
                    netSalaryDisplay = "-";
                } else if (row.get("netSalary") != null && !row.get("netSalary").toString().isEmpty()) {
                    netSalaryDisplay = formatCurrency(nSalary);
                } else {
                    netSalaryDisplay = "-";
                }

                String statusDisplay;
                if (isSecondary) {
                    statusDisplay = "";
                } else if (status.isEmpty() || "null".equalsIgnoreCase(status)) {
                    statusDisplay = "-";
                } else {
                    statusDisplay = "<span style='color: " + getStatusColor(status) + "; font-weight: bold;'>"
                            + escapeXml(status) + "</span>";
                }

                sb.append("<tr>");
                sb.append("<td>").append(escapeXml(dayDisplay)).append("</td>");
                sb.append("<td>").append(timeInHtml).append("</td>");
                sb.append("<td>").append(timeOutHtml).append("</td>");
                sb.append("<td>").append(escapeXml(workHoursDisplay)).append("</td>");
                sb.append("<td>").append(daySalaryDisplay).append("</td>");
                sb.append("<td>").append(foodChargeDisplay).append("</td>");
                sb.append("<td>").append(netSalaryDisplay).append("</td>");
                sb.append("<td>").append(statusDisplay).append("</td>");
                sb.append("</tr>");
            }

            int hrs = totalWorkMinutes / 60;
            int mins = totalWorkMinutes % 60;
            String totalWorkHoursStr = hrs + " hr " + mins + " min";

            // Total row
            sb.append("<tr class='total-row'>");
            sb.append("<td class='text-center'>Total</td>");
            sb.append("<td class='text-center'>-</td>");
            sb.append("<td class='text-center'>-</td>");
            sb.append("<td class='text-center'>").append(totalWorkHoursStr).append("</td>");
            sb.append("<td class='text-center'>₹").append(integerFormat.format(Math.round(totalDaySalary)))
                    .append("</td>");
            sb.append("<td class='text-center'>₹").append(integerFormat.format(Math.round(totalFoodCharge)))
                    .append("</td>");
            sb.append("<td class='text-center'>₹").append(integerFormat.format(Math.round(totalNetSalary)))
                    .append("</td>");
            sb.append("<td class='text-center'>-</td>");
            sb.append("</tr>");
            sb.append("</tbody></table>");

            // Allowances, Deductions, Net Salary cards
            List<DeductionsDto> allowances = (List<DeductionsDto>) user.get("allowances");
            if (allowances == null) {
                allowances = (List<DeductionsDto>) user.get("userAllowances");
            }
            List<DeductionsDto> deductions = (List<DeductionsDto>) user.get("deductions");
            if (deductions == null) {
                deductions = (List<DeductionsDto>) user.get("userDeductions");
            }

            double totalAllowances = allowances != null
                    ? Math.round(
                            allowances.stream().mapToDouble(a -> a.getAmount() != null ? a.getAmount() : 0.0).sum())
                    : 0;
            double userDeductions = deductions != null
                    ? Math.round(
                            deductions.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0).sum())
                    : 0;
            double otAmount = Math.round(parseDouble(user.get("totalOtAmount")));
            if (otAmount == 0) {
                otAmount = Math.round(parseDouble(user.get("otAmount")));
            }
            double totalDeductions = userDeductions + Math.round(totalFoodCharge);
            double finalNetSalary = Math.round(totalDaySalary + otAmount + totalAllowances - totalDeductions);

            sb.append("<div class='pdf-footer-section'>");
            sb.append("<table class='cards-table'><tr>");

            // Card 1: Allowances
            sb.append("<td class='card-cell'><div class='card-box'>");
            sb.append("<div class='card-header'>ALLOWANCES</div>");
            sb.append("<div class='card-body'>");
            sb.append("<table class='inner-table'><tbody>");
            if (allowances != null && !allowances.isEmpty()) {
                for (DeductionsDto a : allowances) {
                    double amt = a.getAmount() != null ? a.getAmount() : 0.0;
                    sb.append("<tr><td class='text-left' style='color:#374151;'>").append(escapeXml(a.getLabel()))
                            .append("</td><td class='text-right font-bold' style='color:#111827;'>₹")
                            .append(integerFormat.format(Math.round(amt))).append("</td></tr>");
                }
            } else {
                sb.append(
                        "<tr><td colspan='2' class='text-center' style='color:#6b7280; font-style:italic; padding-top:14px;'>No allowances</td></tr>");
            }
            sb.append("</tbody></table></div>");
            sb.append("<div class='card-footer'>");
            sb.append("<table style='width:100%; border-collapse:collapse;'><tr>");
            sb.append("<td class='text-left' style='padding:0;'>Total Allowance:</td>");
            sb.append("<td class='text-right' style='padding:0;'>₹")
                    .append(integerFormat.format(Math.round(totalAllowances))).append("</td>");
            sb.append("</tr></table></div>");
            sb.append("</div></td>");

            // Card 2: Deductions
            sb.append("<td class='card-cell'><div class='card-box'>");
            sb.append("<div class='card-header'>DEDUCTIONS</div>");
            sb.append("<div class='card-body'>");
            sb.append("<table class='inner-table'><tbody>");
            if (totalFoodCharge > 0) {
                sb.append(
                        "<tr><td class='text-left' style='color:#374151;'>Food Charge</td><td class='text-right font-bold' style='color:#111827;'>₹")
                        .append(integerFormat.format(Math.round(totalFoodCharge))).append("</td></tr>");
            }
            if (deductions != null && !deductions.isEmpty()) {
                for (DeductionsDto d : deductions) {
                    double amt = d.getAmount() != null ? d.getAmount() : 0.0;
                    sb.append("<tr><td class='text-left' style='color:#374151;'>").append(escapeXml(d.getLabel()))
                            .append("</td><td class='text-right font-bold' style='color:#111827;'>₹")
                            .append(integerFormat.format(Math.round(amt))).append("</td></tr>");
                }
            }
            if (totalFoodCharge == 0 && (deductions == null || deductions.isEmpty())) {
                sb.append(
                        "<tr><td colspan='2' class='text-center' style='color:#6b7280; font-style:italic; padding-top:14px;'>No deductions</td></tr>");
            }
            sb.append("</tbody></table></div>");
            sb.append("<div class='card-footer'>");
            sb.append("<table style='width:100%; border-collapse:collapse;'><tr>");
            sb.append("<td class='text-left' style='padding:0;'>Total Deduction:</td>");
            sb.append("<td class='text-right' style='padding:0;'>₹")
                    .append(integerFormat.format(Math.round(totalDeductions))).append("</td>");
            sb.append("</tr></table></div>");
            sb.append("</div></td>");

            // Card 3: Salary Calculation
            sb.append("<td class='card-cell'><div class='card-box'>");
            sb.append("<div class='card-header'>SALARY CALCULATION</div>");
            sb.append("<div class='card-body'>");
            sb.append("<table class='inner-table'><tbody>");
            sb.append(
                    "<tr><td class='text-left' style='color:#374151;'>Total Salary</td><td class='text-right font-bold' style='color:#111827;'>₹")
                    .append(integerFormat.format(Math.round(totalDaySalary))).append("</td></tr>");
            sb.append(
                    "<tr><td class='text-left' style='color:#374151;'>OT Amount</td><td class='text-right' style='color:#374151;'>+ ₹")
                    .append(integerFormat.format(Math.round(otAmount))).append("</td></tr>");
            sb.append(
                    "<tr><td class='text-left' style='color:#374151;'>Total Allowances</td><td class='text-right' style='color:#374151;'>+ ₹")
                    .append(integerFormat.format(Math.round(totalAllowances))).append("</td></tr>");
            sb.append(
                    "<tr><td class='text-left' style='color:#374151;'>Total Deductions</td><td class='text-right' style='color:#374151;'>- ₹")
                    .append(integerFormat.format(Math.round(totalDeductions))).append("</td></tr>");
            sb.append("</tbody></table></div>");
            sb.append("<div class='card-footer'>");
            sb.append("<table style='width:100%; border-collapse:collapse;'><tr>");
            sb.append("<td class='text-left' style='padding:0; font-weight:bold;'>Net Salary:</td>");
            sb.append("<td class='text-right' style='padding:0; font-size:12.5px; font-weight:800; color:#111827;'>₹")
                    .append(integerFormat.format(Math.round(finalNetSalary))).append("</td>");
            sb.append("</tr></table></div>");
            sb.append("</div></td>");

            sb.append("</tr></table></div>");
            sb.append("</div></div>"); // closes pdf-card, then pdf-page
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private String formatDay(Object raw) {
        if (raw == null)
            return "-";
        String s = raw.toString().trim();
        if (s.isEmpty())
            return "-";

        try {
            String datePart = s.contains(",") ? s.split(",")[0].trim() : s;
            if (datePart.contains("/")) {
                String[] parts = datePart.split("/");
                if (parts.length >= 3) {
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    LocalDate ld = LocalDate.of(year, month, day);
                    return ld.format(DateTimeFormatter.ofPattern("MMM d, EEE", Locale.ENGLISH));
                }
            } else if (datePart.contains("-")) {
                LocalDate ld = LocalDate.parse(datePart);
                return ld.format(DateTimeFormatter.ofPattern("MMM d, EEE", Locale.ENGLISH));
            }
        } catch (Exception ignored) {
        }
        return s;
    }

    private String formatDateTimeCell(Object raw) {
        if (raw == null)
            return "-";
        String s = raw.toString().trim();
        if (s.isEmpty() || "-".equals(s) || "null".equalsIgnoreCase(s))
            return "-";

        if (s.contains(",")) {
            String[] parts = s.split(",");
            String dateStr = parts[0].trim();
            String timeStr = parts.length > 1 ? parts[1].trim() : "";
            // Remove seconds if present e.g. 09:29:00 AM -> 09:29 AM
            timeStr = timeStr.replaceAll("(:\\d{2}):\\d{2}\\s*(AM|PM|am|pm)", "$1 $2");
            return "<div class='pdf-time-cell'><div class='pdf-date-line'>" + escapeXml(dateStr)
                    + "</div><div class='pdf-time-line'>" + escapeXml(timeStr) + "</div></div>";
        }
        return "<div class='pdf-time-cell'><div class='pdf-time-line'>" + escapeXml(s) + "</div></div>";
    }

    private String formatCurrency(double amount) {
        if (amount == 0.0) {
            return "₹0";
        }
        if (amount % 1 == 0) {
            return "₹" + integerFormat.format(Math.round(amount));
        } else {
            return "₹" + currencyFormat.format(amount);
        }
    }

    private String getStatusColor(String status) {
        if ("P".equalsIgnoreCase(status))
            return "#15803d";
        if ("A".equalsIgnoreCase(status))
            return "#b91c1c";
        if ("W".equalsIgnoreCase(status))
            return "#2563eb";
        if ("H".equalsIgnoreCase(status))
            return "#c2410c";
        if ("PW".equalsIgnoreCase(status))
            return "#4338ca";
        return "#374151";
    }

    private String escapeXml(Object raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toString();
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private double parseDouble(Object obj) {
        if (obj == null)
            return 0.0;
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
