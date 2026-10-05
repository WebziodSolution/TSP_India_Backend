package com.timesheetspro_api.userInOut.serviceImpl;

import com.timesheetspro_api.common.dto.UserInOut.UserInOutDto;
import com.timesheetspro_api.common.dto.UserInOut.BulkUserInOutDto;
import com.timesheetspro_api.common.dto.companyShiftDto.CompanyShiftDto;
import com.timesheetspro_api.common.dto.holidayTemplateDetails.HolidayTemplateDetailsDto;
import com.timesheetspro_api.common.model.CompanyEmployee.CompanyEmployee;
import com.timesheetspro_api.common.model.UserInOut.UserInOut;
import com.timesheetspro_api.common.model.companyDetails.CompanyDetails;
import com.timesheetspro_api.common.model.companyShift.CompanyShift;
import com.timesheetspro_api.common.model.holidayTemplateDetails.HolidayTemplateDetails;
import com.timesheetspro_api.common.model.holidayTemplates.HolidayTemplates;
import com.timesheetspro_api.common.model.locations.Locations;
import com.timesheetspro_api.common.model.weeklyOff.WeeklyOff;
import com.timesheetspro_api.common.repository.UserInOutRepository;
import com.timesheetspro_api.common.repository.UserRepository;
import com.timesheetspro_api.common.repository.company.*;
import com.timesheetspro_api.common.service.CommonService;
import com.timesheetspro_api.common.specification.UserInOutSpecification;
import com.timesheetspro_api.holidayTemplateDetails.service.HolidayTemplateDetailsService;
import com.timesheetspro_api.userInOut.service.UserInOutService;
import com.timesheetspro_api.common.dto.deductions.DeductionsDto;
import com.timesheetspro_api.common.model.deductions.Deductions;
import com.timesheetspro_api.common.model.overtimeRules.OvertimeRules;
import com.timesheetspro_api.common.repository.DeductionsRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.RegionUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service("userInOutService")
public class UserInOutServiceImpl implements UserInOutService {

    @Autowired
    private UserInOutRepository userInOutRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyEmployeeRepository companyEmployeeRepository;

    @Autowired
    private CompanyShiftRepository companyShiftRepository;

    @Autowired
    private CommonService commonService;

    @Autowired
    private LocationsRepository locationsRepository;

    @Autowired
    private CompanyDetailsRepository companyDetailsRepository;

    @Autowired
    private WeeklyOffRepository weeklyOffRepository;

    @Autowired
    private HolidayTemplatesRepository holidayTemplatesRepository;

    @Autowired
    private HolidayTemplateDetailsService holidayTemplateDetailsService;

    @Autowired
    private DeductionsRepository deductionsRepository;

    private LocalDate parseDateString(String dateStr) {
        if (dateStr == null)
            return null;
        // Remove any trailing time part (e.g., "25/03/2026, 16:19:57")
        if (dateStr.contains(",")) {
            dateStr = dateStr.split(",")[0].trim();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return LocalDate.parse(dateStr, formatter);
    }

    private String getFullName(String firstName, String middleName, String lastName) {
        return Stream.of(firstName, middleName, lastName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" "));
    }

    @Override
    public Map<String, Object> dashboardCounts(int companyId) {
        try {
            Map<String, Object> res = new HashMap<>();
            // Get the current date
            Calendar calendar = Calendar.getInstance();

            // Get Start of the Day (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startOfDay = calendar.getTime();

            // Get End of the Day (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            Date endOfDay = calendar.getTime();

            List<UserInOut> inUsers = this.userInOutRepository.countCheckedInUsers(companyId, startOfDay, endOfDay);
            List<UserInOut> outUsers = this.userInOutRepository.countCheckedOutUsers(companyId, startOfDay, endOfDay);
            List<CompanyEmployee> companyEmployees = this.companyEmployeeRepository.findByCompanyId(companyId).stream()
                    .filter((row) -> row.getIsActive() == 1).toList();
            List<Map<String, Object>> employeesData = new ArrayList<>();
            List<Map<String, Object>> inEmployeesData = new ArrayList<>();
            List<Map<String, Object>> outEmployeesData = new ArrayList<>();

            for (CompanyEmployee companyEmployee : companyEmployees) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("fullname",
                        getFullName(
                                companyEmployee.getFirstName(),
                                companyEmployee.getMiddleName(),
                                companyEmployee.getLastName()));
                employeesData.add(obj);
            }

            for (UserInOut inuser : inUsers) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("fullname",
                        getFullName(
                                inuser.getUser().getFirstName(),
                                inuser.getUser().getMiddleName(),
                                inuser.getUser().getLastName()));
                inEmployeesData.add(obj);
            }
            for (UserInOut inuser : outUsers) {
                Map<String, Object> obj = new HashMap<>();
                obj.put("fullname",
                        getFullName(
                                inuser.getUser().getFirstName(),
                                inuser.getUser().getMiddleName(),
                                inuser.getUser().getLastName()));
                outEmployeesData.add(obj);
            }

            res.put("countCheckedInUsers", inUsers.size());
            res.put("countCheckedOutUsers", outUsers.size());
            res.put("companyTotalUserCount", companyEmployees.size());
            res.put("inUsersData", inEmployeesData);
            res.put("outUserData", outEmployeesData);
            res.put("totalUserData", employeesData);

            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getAllEntriesGroupByUser(List<Integer> userIds, String startDate, String endDate,
            String timeZone, List<Integer> locationIds, List<Integer> departmentIds, Integer companyId) {
        try {
            ZoneId zone = ZoneId.of(timeZone);
            Instant startInstant, endInstant;
            LocalDate startLocal = null;
            LocalDate endLocal = null;

            if (startDate == null || endDate == null) {
                // Default: current month in UTC
                ZoneId utc = ZoneId.of("UTC");
                LocalDate now = LocalDate.now(utc);
                LocalDate firstOfMonth = now.withDayOfMonth(1);

                // Assign local dates (needed for dateRange later)
                startLocal = firstOfMonth;
                endLocal = now;

                ZonedDateTime startZdt = firstOfMonth.atStartOfDay(utc);
                ZonedDateTime endZdt = now.atTime(LocalTime.MAX).atZone(utc);
                startInstant = startZdt.toInstant();
                endInstant = endZdt.toInstant();
            } else {
                startLocal = parseDateString(startDate);
                endLocal = parseDateString(endDate);

                ZonedDateTime startZdt = startLocal.atStartOfDay(zone);
                ZonedDateTime endZdt = endLocal.atTime(LocalTime.MAX).atZone(zone);

                startInstant = startZdt.toInstant();
                endInstant = endZdt.toInstant();
            }

            Date startUTC = Date.from(startInstant);
            Date endUTC = Date.from(endInstant);

            // --- Build specification (unchanged) ---
            Specification<UserInOut> spec = UserInOutSpecification.createdOnGreaterThanEqual(startUTC);
            if (userIds != null && !userIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.userIdIn(userIds));
            }
            if (locationIds != null && !locationIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.hasLocationId(locationIds));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.hasDepartmentIds(departmentIds));
            }
            if (companyId != null) {
                spec = spec.and(UserInOutSpecification.hasCompany(companyId));
            }
            spec = spec.and(UserInOutSpecification.createdOnLessThanEqual(endUTC));

            // --- Fetch data from repository (unchanged) ---
            // List<UserInOut> userInOutList = this.userInOutRepository.findAll(spec,
            // Sort.by(Sort.Direction.ASC, "id"));
            List<UserInOut> userInOutList = this.userInOutRepository.findAll(spec);

            // --- Group by User entity (unchanged) ---
            Map<CompanyEmployee, List<UserInOut>> groupedByUser = userInOutList.stream()
                    .collect(Collectors.groupingBy(UserInOut::getUser));

            // --- Build the list of all dates in the range (inclusive) ---
            List<LocalDate> dateRange = startLocal.datesUntil(endLocal.plusDays(1)).collect(Collectors.toList());

            // --- Prepare response ---
            List<Map<String, Object>> userGroups = new ArrayList<>();

            for (Map.Entry<CompanyEmployee, List<UserInOut>> entry : groupedByUser.entrySet()) {
                CompanyEmployee user = entry.getKey();
                List<UserInOut> entries = entry.getValue();

                // --- Pre‑fetch shift data for this user (cached per user) ---
                int regularMinutes = 0;
                int breakMinutes = 0;
                CompanyEmployee companyEmployee = this.companyEmployeeRepository.findById(user.getEmployeeId())
                        .orElse(user);
                if (companyEmployee.getCompanyShift() != null) {
                    CompanyShift companyShift = this.companyShiftRepository
                            .findById(companyEmployee.getCompanyShift().getId())
                            .orElse(null);
                    if (companyShift != null) {
                        companyEmployee.setCompanyShift(companyShift);
                        Float regularHours = companyShift.getTotalHours();
                        regularMinutes = regularHours != null ? Math.round(regularHours * 60) : 0;
                    }
                }
                breakMinutes = companyEmployee.getLunchBreak() != null ? companyEmployee.getLunchBreak() : 0;

                String userName = Stream.of(
                        user.getFirstName(),
                        user.getMiddleName(),
                        user.getLastName())
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining(" "));

                // --- Map each entry to its local date (for quick lookup) ---
                Map<LocalDate, List<UserInOut>> entriesByDate = new HashMap<>();
                for (UserInOut uio : entries) {
                    LocalDate date = uio.getCreatedOn().toInstant().atZone(ZoneId.of(timeZone)).toLocalDate();
                    entriesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(uio);
                }

                // Sort entries for each day chronologically by createdOn
                for (List<UserInOut> dayEntries : entriesByDate.values()) {
                    dayEntries.sort(Comparator.comparing(UserInOut::getCreatedOn,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                }

                // --- Pre-fetch Holidays (unchanged) ---
                List<String> holidayDates = new ArrayList<>();
                List<HolidayTemplates> holidayTemplates = this.holidayTemplatesRepository
                        .findByCompanyId(user.getCompanyDetails().getId());

                if (holidayTemplates != null && !holidayTemplates.isEmpty()) {
                    for (HolidayTemplates template : holidayTemplates) {
                        List<HolidayTemplateDetailsDto> dtoList = this.holidayTemplateDetailsService
                                .getAllHolidayTemplateDetailsByTemplateId(template.getId());
                        if (dtoList != null && !dtoList.isEmpty()) {
                            for (HolidayTemplateDetailsDto dto : dtoList) {
                                if (dto.getDate() != null && dto.getDate().length() >= 10) {
                                    holidayDates.add(dto.getDate().substring(0, 10));
                                }
                            }
                        }
                    }
                }

                WeeklyOff weeklyOff = user.getWeeklyOff();

                // --- Initialize counters for P, A, Weekly Off, Holiday ---
                int presentCount = 0; // P (present on normal days)
                int absentCount = 0; // A (absent on normal days)
                int weeklyOffCount = 0; // total weekly off days (whether worked or not)
                int holidayCount = 0; // total holiday days (whether worked or not)

                // --- Build the "data" array for all dates in the range ---
                List<Map<String, Object>> dataList = new ArrayList<>();
                int totalGrossMinutes = 0;
                int totalOvertimeMinutes = 0;
                int rowIndex = 1;
                Map<LocalDate, Long> dailyWorkedMinutes = new HashMap<>();
                Set<LocalDate> actualWorkDays = new HashSet<>();

                for (LocalDate date : dateRange) {
                    List<UserInOut> dayEntries = entriesByDate.get(date);

                    // 1. Determine if today is a Holiday or a Weekly Off
                    boolean isHoliday = false;
                    boolean isWeeklyOff = false;
                    String formattedCurrentDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    // Check if the current date is a holiday
                    if (holidayDates.contains(formattedCurrentDate)) {
                        isHoliday = true;
                    }

                    // Check if the current date falls under the Weekly Off rules (only if not
                    // already a holiday)
                    if (!isHoliday && weeklyOff != null) {
                        DayOfWeek dayOfWeek = date.getDayOfWeek();
                        int weekOfMonth = ((date.getDayOfMonth() - 1) / 7) + 1; // Returns 1, 2, 3, 4, or 5
                        isWeeklyOff = isWeeklyOffDay(dayOfWeek, weekOfMonth, weeklyOff);
                    }

                    // 2. Determine status and update present/absent counters
                    boolean hasValidPresentEntry = false;
                    if (dayEntries != null) {
                        for (UserInOut uio : dayEntries) {
                            if (uio.getTimeIn() != null && uio.getTimeOut() != null) {
                                hasValidPresentEntry = true;
                                break;
                            }
                        }
                    }

                    // Update counters
                    if (isHoliday) {
                        holidayCount++;
                    }
                    if (isWeeklyOff && !hasValidPresentEntry) {
                        weeklyOffCount++;
                    }

                    if (hasValidPresentEntry) {
                        presentCount++;
                    } else {
                        if (!isWeeklyOff && !isHoliday) {
                            absentCount++;
                        }
                    }

                    if (dayEntries == null || dayEntries.isEmpty()) {
                        // Absent or incomplete day (no entries at all)
                        ZonedDateTime zdt = date.atStartOfDay(ZoneId.of(timeZone));
                        Date createdOnDate = Date.from(zdt.toInstant());

                        Map<String, Object> dataItem = new HashMap<>();
                        dataItem.put("id", null);
                        dataItem.put("timeIn", null);
                        dataItem.put("timeOut", null);
                        dataItem.put("createdOn", this.commonService.convertDateToString(createdOnDate, timeZone));
                        dataItem.put("locationId", null);
                        dataItem.put("regular", formatMinutesToHHmm(regularMinutes));
                        dataItem.put("breakTime", formatMinutesToHHmm(breakMinutes));
                        dataItem.put("workHours", "00:00");
                        dataItem.put("overtime", "00:00");
                        dataItem.put("totalHours", "00:00");

                        String status;
                        if (isWeeklyOff) {
                            status = "W"; // Weekly Off (absent)
                        } else if (isHoliday) {
                            status = "H"; // Holiday (absent)
                        } else {
                            status = "A"; // Absent on normal day
                        }
                        dataItem.put("status", status);

                        boolean isHourlyEmp = companyEmployee.getEmployeeType() != null
                                && companyEmployee.getEmployeeType().getId() == 2
                                && companyEmployee.getHourlyRate() != null;
                        double todaySalary = 0.0;
                        double foodCharge = 0.0;
                        if ((isWeeklyOff || isHoliday) && !isHourlyEmp) {
                            double monthlySalary = companyEmployee.getBasicSalary() != null
                                    ? companyEmployee.getBasicSalary()
                                    : 0.0;
                            todaySalary = Math.round((monthlySalary / 30.0) * 100.0) / 100.0;
                            String canteenType = companyEmployee.getCanteenType();
                            if (canteenType != null && !"No Canteen".equalsIgnoreCase(canteenType)) {
                                double monthlyCanteen = companyEmployee.getCanteenAmount() != null
                                        ? companyEmployee.getCanteenAmount()
                                        : 0.0;
                                foodCharge = Math.round((monthlyCanteen / 30.0) * 100.0) / 100.0;
                            }
                        }
                        double netSalary = Math.round((todaySalary - foodCharge) * 100.0) / 100.0;
                        dataItem.put("todaySalary", todaySalary);
                        dataItem.put("foodCharge", foodCharge);
                        dataItem.put("netSalary", netSalary);
                        dataItem.put("otAmount", 0);

                        dataItem.put("userName", userName);
                        dataItem.put("rowId", rowIndex++);
                        dataList.add(dataItem);
                    } else {
                        // 1. Calculate the day's total gross minutes from all valid entries
                        int dayTotalGrossMinutes = 0;
                        for (UserInOut uio : dayEntries) {
                            if (uio.getTimeIn() != null && uio.getTimeOut() != null) {
                                long diffMs = uio.getTimeOut().getTime() - uio.getTimeIn().getTime();
                                int grossMinutes = (int) (diffMs / (60 * 1000));
                                dayTotalGrossMinutes += grossMinutes;
                            }
                        }

                        // 2. Compute the day's overall work hours and overtime
                        int dayNetMinutes = Math.max(0, dayTotalGrossMinutes - breakMinutes);
                        int dayOvertimeMinutes = Math.max(0, dayNetMinutes - regularMinutes);

                        // Accumulate user totals
                        totalGrossMinutes += dayTotalGrossMinutes;
                        totalOvertimeMinutes += dayOvertimeMinutes;

                        if (dayTotalGrossMinutes > 0) {
                            dailyWorkedMinutes.put(date, (long) dayNetMinutes);
                            actualWorkDays.add(date);
                        }

                        double daySalary = calculateTodaySalary(companyEmployee, dayNetMinutes);
                        double dayFoodCharge = calculateFoodCharge(companyEmployee, dayNetMinutes);
                        double dayNetSalary = Math.round((daySalary - dayFoodCharge) * 100.0) / 100.0;
                        int dayOtAmount = calculateDailyOvertimeAmount(companyEmployee, dayNetMinutes);

                        // 3. Output the entries
                        boolean isFirst = true;
                        for (UserInOut uio : dayEntries) {
                            Map<String, Object> dataItem = new HashMap<>();
                            boolean hasValidTimes = (uio.getTimeIn() != null && uio.getTimeOut() != null);

                            if (hasValidTimes) {
                                Date timeIn = uio.getTimeIn();
                                Date timeOut = uio.getTimeOut();

                                dataItem.put("id", uio.getId());
                                dataItem.put("timeIn", this.commonService.convertDateToString(timeIn, timeZone));
                                dataItem.put("timeOut", this.commonService.convertDateToString(timeOut, timeZone));
                                dataItem.put("createdOn",
                                        this.commonService.convertDateToString(uio.getCreatedOn(), timeZone));
                                dataItem.put("locationId",
                                        uio.getLocations() != null ? uio.getLocations().getId() : null);

                                if (isFirst) {
                                    dataItem.put("regular", formatMinutesToHHmm(regularMinutes));
                                    dataItem.put("breakTime", formatMinutesToHHmm(breakMinutes));
                                    dataItem.put("workHours", formatMinutesToHHmm(dayNetMinutes));
                                    dataItem.put("overtime", formatMinutesToHHmm(dayOvertimeMinutes));
                                    dataItem.put("totalHours", formatMinutesToHHmm(dayTotalGrossMinutes));

                                    String status;
                                    if (isHoliday || isWeeklyOff) {
                                        status = "PW"; // Present on Weekly Off/Holiday
                                    } else {
                                        status = "P"; // Present on normal day
                                    }
                                    dataItem.put("status", status);
                                    dataItem.put("todaySalary", daySalary);
                                    dataItem.put("foodCharge", dayFoodCharge);
                                    dataItem.put("netSalary", dayNetSalary);
                                    dataItem.put("otAmount", dayOtAmount);
                                    isFirst = false;
                                } else {
                                    dataItem.put("regular", "");
                                    dataItem.put("breakTime", "");
                                    dataItem.put("workHours", "");
                                    dataItem.put("overtime", "");
                                    dataItem.put("totalHours", "");
                                    dataItem.put("status", "");
                                    dataItem.put("todaySalary", "");
                                    dataItem.put("foodCharge", "");
                                    dataItem.put("netSalary", "");
                                    dataItem.put("otAmount", "");
                                }
                            } else {
                                // Incomplete entry (e.g. clock-in but no clock-out)
                                dataItem.put("id", uio.getId());
                                dataItem.put("timeIn",
                                        uio.getTimeIn() != null
                                                ? this.commonService.convertDateToString(uio.getTimeIn(), timeZone)
                                                : null);
                                dataItem.put("timeOut",
                                        uio.getTimeOut() != null
                                                ? this.commonService.convertDateToString(uio.getTimeOut(), timeZone)
                                                : null);
                                dataItem.put("createdOn",
                                        this.commonService.convertDateToString(uio.getCreatedOn(), timeZone));
                                dataItem.put("locationId",
                                        uio.getLocations() != null ? uio.getLocations().getId() : null);

                                if (isFirst) {
                                    dataItem.put("regular", formatMinutesToHHmm(regularMinutes));
                                    dataItem.put("breakTime", formatMinutesToHHmm(breakMinutes));
                                    dataItem.put("workHours", "00:00");
                                    dataItem.put("overtime", "00:00");
                                    dataItem.put("totalHours", "00:00");

                                    String status;
                                    if (isWeeklyOff) {
                                        status = "W"; // Weekly Off
                                    } else if (isHoliday) {
                                        status = "H"; // Holiday
                                    } else {
                                        status = "A"; // Absent/Incomplete on normal day
                                    }
                                    dataItem.put("status", status);
                                    dataItem.put("todaySalary", 0);
                                    dataItem.put("foodCharge", 0);
                                    dataItem.put("netSalary", 0);
                                    dataItem.put("otAmount", 0);
                                    isFirst = false;
                                } else {
                                    dataItem.put("regular", "");
                                    dataItem.put("breakTime", "");
                                    dataItem.put("workHours", "");
                                    dataItem.put("overtime", "");
                                    dataItem.put("totalHours", "");
                                    dataItem.put("status", "");
                                    dataItem.put("todaySalary", "");
                                    dataItem.put("foodCharge", "");
                                    dataItem.put("netSalary", "");
                                    dataItem.put("otAmount", "");
                                }
                            }

                            dataItem.put("userName", userName);
                            dataItem.put("rowId", rowIndex++);
                            dataList.add(dataItem);
                        }
                    }
                }
                // --- Build user group object with totals and new counters ---
                Map<String, Object> userGroup = new HashMap<>();
                userGroup.put("id", user.getEmployeeId());
                userGroup.put("username", userName);

                Float hourlyRate = companyEmployee.getHourlyRate() != null ? companyEmployee.getHourlyRate()
                        : user.getHourlyRate();
                Integer basicSalary = companyEmployee.getBasicSalary() != null ? companyEmployee.getBasicSalary()
                        : user.getBasicSalary();

                if (hourlyRate != null && hourlyRate > 0) {
                    userGroup.put("hourlyRate", hourlyRate);
                    userGroup.put("hourRate", hourlyRate);
                } else if (basicSalary != null && basicSalary > 0) {
                    BigDecimal daySalary = BigDecimal.valueOf(basicSalary.doubleValue())
                            .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                    userGroup.put("daySalary", daySalary);
                }

                // Add the new counters right after username
                userGroup.put("presentCount", presentCount); // P
                userGroup.put("absentCount", absentCount); // A
                userGroup.put("weeklyOffCount", weeklyOffCount); // Weekly Off days
                userGroup.put("holidayCount", holidayCount); // Holiday days
                userGroup.put("department", user.getDepartment().getDepartmentName());
                userGroup.put("data", dataList);
                userGroup.put("totalHours", formatMinutesToHHmm(totalGrossMinutes));
                userGroup.put("totalOvertime", formatMinutesToHHmm(totalOvertimeMinutes));

                int totalOtAmount = calculateOvertimeAmount(companyEmployee, dailyWorkedMinutes, actualWorkDays);
                List<DeductionsDto> userAllowances = calculateTotalAllowanceAndDeductions(user.getEmployeeId(),
                        "Allowance");
                List<DeductionsDto> userDeductions = calculateTotalAllowanceAndDeductions(user.getEmployeeId(),
                        "Deduction");

                userGroup.put("otAmount", totalOtAmount);
                userGroup.put("totalOtAmount", totalOtAmount);
                userGroup.put("allowances", userAllowances);
                userGroup.put("deductions", userDeductions);

                userGroups.add(userGroup);
            }

            // --- Wrap the array in a Map to satisfy the return type ---
            Map<String, Object> result = new HashMap<>();
            result.put("users", userGroups);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public List<UserInOutDto> getAllEntriesByUserId(List<Integer> userIds, String startDate, String endDate,
            String timeZone, List<Integer> locationIds, List<Integer> departmentIds, Integer companyId) {
        try {
            // // --- Date handling using java.time ---
            ZoneId zone = ZoneId.of(timeZone);
            Instant startInstant, endInstant;

            if (startDate == null || endDate == null) {
                // Default: current month in UTC
                ZoneId utc = ZoneId.of("UTC");
                LocalDate now = LocalDate.now(utc);
                LocalDate firstOfMonth = now.withDayOfMonth(1);
                ZonedDateTime startZdt = firstOfMonth.atStartOfDay(utc);
                ZonedDateTime endZdt = now.atTime(LocalTime.MAX).atZone(utc);
                startInstant = startZdt.toInstant();
                endInstant = endZdt.toInstant();
            } else {
                LocalDate startLocal = parseDateString(startDate);
                LocalDate endLocal = parseDateString(endDate);

                ZonedDateTime startZdt = startLocal.atStartOfDay(zone);
                ZonedDateTime endZdt = endLocal.atTime(LocalTime.MAX).atZone(zone);
                startInstant = startZdt.toInstant();
                endInstant = endZdt.toInstant();
            }

            Date startUTC = Date.from(startInstant);
            Date endUTC = Date.from(endInstant);

            // --- Build specification (unchanged) ---
            Specification<UserInOut> spec = UserInOutSpecification.createdOnGreaterThanEqual(startUTC);
            if (userIds != null && !userIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.userIdIn(userIds));
            }
            if (locationIds != null && !locationIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.hasLocationId(locationIds));
            }
            if (departmentIds != null && !departmentIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.hasDepartmentIds(departmentIds));
            }
            if (companyId != null) {
                spec = spec.and(UserInOutSpecification.hasCompany(companyId));
            }
            spec = spec.and(UserInOutSpecification.createdOnLessThanEqual(endUTC));

            // --- Fetch raw entries (unchanged) ---
            List<UserInOut> userInOutList = this.userInOutRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));

            // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            // dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            // Date start, end;
            // if (startDate == null || endDate == null) {
            // Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            // calendar.set(Calendar.DAY_OF_MONTH, 1);
            // start = calendar.getTime();
            // calendar.add(Calendar.MONTH, 1);
            // calendar.set(Calendar.DAY_OF_MONTH, 0);
            // end = calendar.getTime();
            // }else{
            // start = this.commonService.convertLocalToUtc(startDate, timeZone, false);
            // end = this.commonService.convertLocalToUtc(endDate, timeZone, true);
            // }
            // // --- Build specification ---
            // Specification<UserInOut> spec =
            // UserInOutSpecification.createdOnGreaterThanEqual(start);
            // if (userIds != null && !userIds.isEmpty()) {
            // spec = spec.and(UserInOutSpecification.userIdIn(userIds));
            // }
            // if (companyId != null) {
            // spec = spec.and(UserInOutSpecification.hasCompany(companyId));
            // }
            // spec = spec.and(UserInOutSpecification.createdOnLessThanEqual(end));
            // List<UserInOut> userInOutList = this.userInOutRepository.findAll(spec,
            // Sort.by(Sort.Direction.ASC, "id"));

            // --- Collect distinct user IDs from the entries ---
            Set<Integer> distinctUserIds = userInOutList.stream()
                    .map(uio -> uio.getUser().getEmployeeId())
                    .collect(Collectors.toSet());

            // --- Pre‑fetch all employees (and their shift & lunch break) in one query ---
            Map<Integer, EmployeeData> employeeDataMap = new HashMap<>();
            Map<Integer, CompanyEmployee> employeeMap = new HashMap<>();
            if (!distinctUserIds.isEmpty()) {
                List<CompanyEmployee> employees = companyEmployeeRepository.findAllById(distinctUserIds);
                for (CompanyEmployee emp : employees) {
                    employeeMap.put(emp.getEmployeeId(), emp);
                    int regMinutes = 0;
                    int breakMinutes = emp.getLunchBreak() != null ? emp.getLunchBreak() : 0;
                    if (emp.getCompanyShift() != null) {
                        CompanyShift shift = emp.getCompanyShift();
                        // totalHours is stored as Float (e.g., 8.5 for 8h30m)
                        Float regHours = shift.getTotalHours();
                        regMinutes = regHours != null ? Math.round(regHours * 60) : 0;
                    }
                    employeeDataMap.put(emp.getEmployeeId(), new EmployeeData(regMinutes, breakMinutes));
                }
            }

            // Group by Date (chronologically sorted) and then User ID
            Map<LocalDate, Map<Integer, List<UserInOut>>> groupedByDateAndUser = new TreeMap<>();
            for (UserInOut uio : userInOutList) {
                LocalDate date = uio.getCreatedOn().toInstant().atZone(zone).toLocalDate();
                int empId = uio.getUser().getEmployeeId();

                groupedByDateAndUser
                        .computeIfAbsent(date, k -> new LinkedHashMap<>())
                        .computeIfAbsent(empId, k -> new ArrayList<>())
                        .add(uio);
            }

            // --- Map each grouped entry to DTO with computed fields ---
            List<UserInOutDto> userInOutDtoList = new ArrayList<>();
            int rowIndex = 1;
            Map<Integer, List<DeductionsDto>> userAllowancesMap = new HashMap<>();
            Map<Integer, List<DeductionsDto>> userDeductionsMap = new HashMap<>();

            for (Map.Entry<LocalDate, Map<Integer, List<UserInOut>>> dateEntry : groupedByDateAndUser.entrySet()) {
                Map<Integer, List<UserInOut>> userMap = dateEntry.getValue();

                for (Map.Entry<Integer, List<UserInOut>> userEntry : userMap.entrySet()) {
                    int empId = userEntry.getKey();
                    List<UserInOut> dayRecords = userEntry.getValue();

                    EmployeeData empData = employeeDataMap.get(empId);
                    int regularMinutes = empData != null ? empData.regularMinutes : 0;
                    int breakMinutes = empData != null ? empData.breakMinutes : 0;
                    CompanyEmployee emp = employeeMap.get(empId);

                    // Sort the records for this day chronologically by createdOn
                    dayRecords.sort(Comparator.comparing(UserInOut::getCreatedOn,
                            Comparator.nullsLast(Comparator.naturalOrder())));

                    // Consolidate dayRecords into a single DTO
                    UserInOutDto dto = new UserInOutDto();

                    UserInOut firstRecord = dayRecords.get(0);
                    dto.setId(firstRecord.getId()); // Use first record's ID

                    String userName = Stream.of(
                            firstRecord.getUser().getFirstName(),
                            firstRecord.getUser().getMiddleName(),
                            firstRecord.getUser().getLastName())
                            .filter(Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.joining(" "));
                    dto.setUserName(userName);
                    dto.setRowId(rowIndex++);
                    CompanyEmployee empObj = emp != null ? emp : firstRecord.getUser();
                    if (empObj != null) {
                        dto.setHourlyRate(empObj.getHourlyRate());
                        if (empObj.getBasicSalary() != null && empObj.getBasicSalary() > 0) {
                            BigDecimal daySal = BigDecimal.valueOf(empObj.getBasicSalary().doubleValue())
                                    .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
                            dto.setDaySalary(daySal);
                        }
                    }
                    dto.setFirstName(firstRecord.getUser().getFirstName());
                    dto.setLastName(firstRecord.getUser().getLastName());
                    dto.setCreatedOn(this.commonService.convertDateToString(firstRecord.getCreatedOn(), timeZone));
                    dto.setUserId(empId);
                    if (firstRecord.getLocations() != null) {
                        dto.setLocationId(firstRecord.getLocations().getId());
                    }

                    // Shift DTO
                    CompanyShiftDto companyShiftDto = new CompanyShiftDto();
                    CompanyEmployee companyEmployee = firstRecord.getUser();
                    if (companyEmployee.getCompanyShift() != null) {
                        CompanyShift companyShift = companyEmployee.getCompanyShift();
                        companyShiftDto.setCompanyId(companyShift.getCompanyDetails().getId());
                        BeanUtils.copyProperties(companyShift, companyShiftDto);
                        dto.setCompanyShiftDto(companyShiftDto);
                    }

                    // Consolidated times and calculations
                    Date firstTimeIn = null;
                    Date lastTimeOut = null;
                    int dayTotalGrossMinutes = 0;
                    boolean hasAnyValidTimes = false;

                    for (UserInOut record : dayRecords) {
                        if (record.getTimeIn() != null) {
                            if (firstTimeIn == null || record.getTimeIn().before(firstTimeIn)) {
                                firstTimeIn = record.getTimeIn();
                            }
                        }
                        if (record.getTimeOut() != null) {
                            if (lastTimeOut == null || record.getTimeOut().after(lastTimeOut)) {
                                lastTimeOut = record.getTimeOut();
                            }
                        }
                        if (record.getTimeIn() != null && record.getTimeOut() != null) {
                            hasAnyValidTimes = true;
                            long diffMs = record.getTimeOut().getTime() - record.getTimeIn().getTime();
                            int grossMinutes = (int) (diffMs / (60 * 1000));
                            dayTotalGrossMinutes += grossMinutes;
                        }
                    }

                    if (firstTimeIn != null) {
                        dto.setTimeIn(this.commonService.convertDateToString(firstTimeIn, timeZone));
                    }
                    if (lastTimeOut != null) {
                        dto.setTimeOut(this.commonService.convertDateToString(lastTimeOut, timeZone));
                    }

                    if (hasAnyValidTimes) {
                        int workMinutes = Math.max(0, dayTotalGrossMinutes - breakMinutes);
                        int overtimeMinutes = Math.max(0, workMinutes - regularMinutes);

                        dto.setRegular(formatMinutesToHHmm(regularMinutes));
                        dto.setBreakTime(formatMinutesToHHmm(breakMinutes));
                        dto.setWorkHours(formatMinutesToHHmm(workMinutes));
                        dto.setOvertime(formatMinutesToHHmm(overtimeMinutes));
                        dto.setTotalHours(formatMinutesToHHmm(dayTotalGrossMinutes));
                        dto.setStatus("P");

                        double todaySalary = calculateTodaySalary(emp != null ? emp : companyEmployee, workMinutes);
                        double foodCharge = calculateFoodCharge(emp != null ? emp : companyEmployee, workMinutes);
                        double netSalary = Math.round((todaySalary - foodCharge) * 100.0) / 100.0;
                        dto.setTodaySalary(todaySalary);
                        dto.setFoodCharge(foodCharge);
                        dto.setNetSalary(netSalary);

                        int dayOtAmount = calculateDailyOvertimeAmount(empObj, workMinutes);
                        dto.setOtAmount(dayOtAmount);
                    } else {
                        dto.setRegular(formatMinutesToHHmm(regularMinutes));
                        dto.setBreakTime(formatMinutesToHHmm(breakMinutes));
                        dto.setWorkHours("00:00");
                        dto.setOvertime("00:00");
                        dto.setTotalHours("00:00");
                        dto.setStatus("A");
                        dto.setTodaySalary(0);
                        dto.setFoodCharge(0);
                        dto.setNetSalary(0);
                        dto.setOtAmount(0);
                    }
                    dto.setDepartment(firstRecord.getUser().getDepartment().getDepartmentName());
                    dto.setStatus(firstTimeIn != null ? "P" : "A");

                    List<DeductionsDto> userAllowances = userAllowancesMap.computeIfAbsent(empId,
                            id -> calculateTotalAllowanceAndDeductions(id, "Allowance"));
                    List<DeductionsDto> userDeductions = userDeductionsMap.computeIfAbsent(empId,
                            id -> calculateTotalAllowanceAndDeductions(id, "Deduction"));
                    dto.setAllowances(userAllowances);
                    dto.setDeductions(userDeductions);

                    userInOutDtoList.add(dto);
                }
            }

            return userInOutDtoList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // Helper inner class to hold pre‑fetched data
    private static class EmployeeData {
        int regularMinutes;
        int breakMinutes;

        EmployeeData(int regularMinutes, int breakMinutes) {
            this.regularMinutes = regularMinutes;
            this.breakMinutes = breakMinutes;
        }
    }

    // Helper method (already used in groupBy)
    private String formatMinutesToHHmm(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    private double calculateTodaySalary(CompanyEmployee employee, int workMinutes) {
        if (employee == null)
            return 0.0;
        boolean isHourly = employee.getEmployeeType() != null
                && employee.getEmployeeType().getId() == 2
                && employee.getHourlyRate() != null;
        if (isHourly) {
            long hrs = workMinutes / 60;
            long mins = workMinutes % 60;
            double hourlyRate = employee.getHourlyRate();
            double payForWorked = (hrs * hourlyRate) + (mins * hourlyRate / 60.0);
            return Math.round(payForWorked * 100.0) / 100.0;
        } else {
            double monthlySalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : 0.0;
            double dailyRate = monthlySalary / 30.0;
            return Math.round(dailyRate * 100.0) / 100.0;
        }
    }

    private double calculateFoodCharge(CompanyEmployee employee, int workMinutes) {
        if (employee == null)
            return 0.0;
        String canteenType = employee.getCanteenType();
        if (canteenType == null || "No Canteen".equalsIgnoreCase(canteenType)) {
            return 0.0;
        }

        int canteenAmount = employee.getCanteenAmount() != null ? employee.getCanteenAmount() : 0;
        if (canteenAmount <= 0) {
            return 0.0;
        }

        boolean isHourly = employee.getEmployeeType() != null
                && employee.getEmployeeType().getId() == 2
                && employee.getHourlyRate() != null;

        if (isHourly) {
            if ("Labour Type".equals(canteenType)) {
                long threshold = 13 * 60; // 13 hours (780 minutes)
                if (workMinutes > threshold) {
                    return (double) canteenAmount;
                } else {
                    return (double) (canteenAmount * 2);
                }
            } else if ("Office Type".equals(canteenType)) {
                return (double) canteenAmount;
            } else {
                return 0.0;
            }
        } else {
            // For Salaried employee, canteen amount is monthly, pass getCanteenAmount / 30
            return Math.round((canteenAmount / 30.0) * 100.0) / 100.0;
        }
    }

    // Helper method to calculate overtime amount for a date range (matches
    // buildEmployeeSalaryStatement)
    private int calculateOvertimeAmount(CompanyEmployee employee, Map<LocalDate, Long> dailyWorkedMinutes,
            Set<LocalDate> actualWorkDays) {
        if (employee == null || employee.getOvertimeRules() == null) {
            return 0;
        }

        if (employee.getEmployeeType() != null && employee.getEmployeeType().getId() == 2) {
            return 0;
        }

        OvertimeRules rule = employee.getOvertimeRules();
        Float otPayPerSlab = rule.getOtAmount() != null ? rule.getOtAmount() : 0f;
        int dailySalary = 0;

        if (employee.getEmployeeType() != null && employee.getEmployeeType().getId() == 2
                && employee.getHourlyRate() != null) {
            dailySalary = (int) (employee.getCompanyShift() != null
                    && employee.getCompanyShift().getTotalHours() != null
                            ? employee.getCompanyShift().getTotalHours() * employee.getHourlyRate()
                            : 0);
        } else {
            dailySalary = employee.getBasicSalary() != null ? employee.getBasicSalary() / 30 : 0;
        }

        float employeeShiftHours = employee.getCompanyShift() != null
                && employee.getCompanyShift().getTotalHours() != null
                        ? employee.getCompanyShift().getTotalHours()
                        : 0f;
        float shiftMinutes = employeeShiftHours * 60f;
        String otType = rule.getOtType() != null ? rule.getOtType().trim().toLowerCase() : "";

        int totalOtAmount = 0;
        for (LocalDate date : actualWorkDays) {
            long workedMin = dailyWorkedMinutes.getOrDefault(date, 0L);
            long dailyOtMinutes = Math.max(0, workedMin - (long) shiftMinutes);
            if (dailyOtMinutes <= 0) {
                continue;
            }

            switch (otType) {
                case "fixed amount":
                case "fixed amount per hour":
                    long otHours = (long) Math.ceil(dailyOtMinutes / 60.0);
                    totalOtAmount += (int) (otHours * otPayPerSlab);
                    break;
                case "1 day salary":
                    totalOtAmount += dailySalary;
                    break;
                case "1.5 day salary":
                    totalOtAmount += (int) Math.round(dailySalary * 1.5);
                    break;
                case "2 day salary":
                    totalOtAmount += dailySalary * 2;
                    break;
                case "2.5 day salary":
                    totalOtAmount += (int) Math.round(dailySalary * 2.5);
                    break;
                case "3 day salary":
                    totalOtAmount += dailySalary * 3;
                    break;
            }
        }
        return totalOtAmount;
    }

    // Helper method to calculate overtime amount for a single day
    private int calculateDailyOvertimeAmount(CompanyEmployee employee, int workMinutes) {
        if (employee == null || employee.getOvertimeRules() == null) {
            return 0;
        }

        if (employee.getEmployeeType() != null && employee.getEmployeeType().getId() == 2) {
            return 0;
        }

        float employeeShiftHours = employee.getCompanyShift() != null
                && employee.getCompanyShift().getTotalHours() != null
                        ? employee.getCompanyShift().getTotalHours()
                        : 0f;
        float shiftMinutes = employeeShiftHours * 60f;
        long dailyOtMinutes = Math.max(0, workMinutes - (long) shiftMinutes);
        if (dailyOtMinutes <= 0) {
            return 0;
        }

        OvertimeRules rule = employee.getOvertimeRules();
        Float otPayPerSlab = rule.getOtAmount() != null ? rule.getOtAmount() : 0f;
        int dailySalary = 0;
        if (employee.getEmployeeType() != null && employee.getEmployeeType().getId() == 2
                && employee.getHourlyRate() != null) {
            dailySalary = (int) (employee.getCompanyShift() != null
                    && employee.getCompanyShift().getTotalHours() != null
                            ? employee.getCompanyShift().getTotalHours() * employee.getHourlyRate()
                            : 0);
        } else {
            dailySalary = employee.getBasicSalary() != null ? employee.getBasicSalary() / 30 : 0;
        }

        String otType = rule.getOtType() != null ? rule.getOtType().trim().toLowerCase() : "";
        switch (otType) {
            case "fixed amount":
            case "fixed amount per hour":
                long otHours = (long) Math.ceil(dailyOtMinutes / 60.0);
                return (int) (otHours * otPayPerSlab);
            case "1 day salary":
                return dailySalary;
            case "1.5 day salary":
                return (int) Math.round(dailySalary * 1.5);
            case "2 day salary":
                return dailySalary * 2;
            case "2.5 day salary":
                return (int) Math.round(dailySalary * 2.5);
            case "3 day salary":
                return dailySalary * 3;
            default:
                return 0;
        }
    }

    // Helper method to calculate allowance/deductions for a user
    private List<DeductionsDto> calculateTotalAllowanceAndDeductions(Integer userId, String type) {
        List<DeductionsDto> deductionsDtoList = new ArrayList<>();
        if (userId == null) {
            return deductionsDtoList;
        }
        List<Deductions> deductionsList = this.deductionsRepository.findByEmployeeIdAndType(userId, type);
        if (deductionsList != null) {
            for (Deductions deductions : deductionsList) {
                DeductionsDto deductionsDto = new DeductionsDto();
                deductionsDto.setId(deductions.getId());
                deductionsDto.setEmployeeId(userId);
                deductionsDto.setLabel(deductions.getLabel());
                deductionsDto.setName(deductions.getLabel());
                deductionsDto.setAmount(deductions.getAmount());
                deductionsDto.setType(deductions.getType());
                deductionsDtoList.add(deductionsDto);
            }
        }
        return deductionsDtoList;
    }

    private int hhDotMmToMinutes(Object value) {
        if (value == null)
            return 0;

        try {
            double val = Double.parseDouble(value.toString());
            int hours = (int) val;
            int minutes = (int) Math.round((val - hours) * 100);

            if (minutes < 0 || minutes > 59) {
                return hours * 60;
            }
            return (hours * 60) + minutes;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public UserInOutDto getUserLastInOut(int id) {
        try {
            UserInOut userInOut = this.userInOutRepository.getLastRecord(id);
            UserInOutDto userInOutDto = new UserInOutDto();
            if (userInOut != null) {
                userInOutDto.setId(userInOut.getId());
                userInOutDto.setUserId(userInOut.getUser().getEmployeeId());

                userInOutDto.setTimeIn(this.commonService.convertDateToString(userInOut.getTimeIn(), "Asia/Calcutta"));
                if (userInOut.getLocations() != null) {
                    userInOutDto.setLocationId(userInOut.getLocations().getId());
                }
                userInOutDto
                        .setTimeOut(this.commonService.convertDateToString(userInOut.getTimeOut(), "Asia/Calcutta"));

                return userInOutDto;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UserInOutDto getUserInOut(Long id) {
        try {
            UserInOut userInOut = this.userInOutRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("UserInOut not found"));
            UserInOutDto userInOutDto = new UserInOutDto();
            userInOutDto.setId(userInOut.getId());
            userInOutDto.setUserId(userInOut.getUser().getEmployeeId());
            userInOutDto.setTimeIn(this.commonService.convertDateToString(userInOut.getTimeIn(), "Asia/Calcutta")); // Defaulting
            // to
            // IST
            // or
            // pass
            // TZ?
            if (userInOut.getTimeOut() != null) {
                userInOutDto
                        .setTimeOut(this.commonService.convertDateToString(userInOut.getTimeOut(), "Asia/Calcutta"));
            }
            if (userInOut.getLocations() != null) {
                userInOutDto.setLocationId(userInOut.getLocations().getId());
            }
            return userInOutDto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public UserInOutDto createUserInOut(int userId, Integer locationId, Integer companyId, Date timeIn) {
        try {
            UserInOut userInOut = new UserInOut();
            CompanyEmployee companyEmployee = this.companyEmployeeRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            userInOut.setUser(companyEmployee);
            CompanyDetails companyDetails = this.companyDetailsRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            userInOut.setTimeIn(timeIn);
            userInOut.setCreatedOn(timeIn); // or new Date()? Use same timeIn for consistency
            userInOut.setCompanyDetails(companyDetails);
            userInOut.setIsSalaryGenerate(0);
            if (locationId != null && locationId > 0) {
                Locations locations = this.locationsRepository.findById(locationId)
                        .orElseThrow(() -> new RuntimeException("Location not found"));
                userInOut.setLocations(locations);
            }
            this.userInOutRepository.save(userInOut);
            UserInOutDto res = new UserInOutDto();
            res.setId(userInOut.getId());
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    // Keep original for backward compatibility
    public UserInOutDto createUserInOut(int userId, Integer locationId, Integer companyId) {
        return createUserInOut(userId, locationId, companyId, new Date());
    }

    public boolean updateUserInOut(Long id, int userId) {
        try {
            UserInOut userInOut = this.userInOutRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("UserInOut record not found"));
            CompanyEmployee employee = this.companyEmployeeRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Use current time as the intended timeOut
            Date now = new Date();

            // Delegate to common handler
            boolean updated = handleTimeOutUpdate(employee, userInOut, now,
                    userInOut.getLocations() != null ? userInOut.getLocations().getId() : null,
                    employee.getCompanyDetails().getId());

            return updated;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public UserInOutDto updateUserInOut(UserInOutDto dto) {
        try {
            UserInOut userInOut = this.userInOutRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("UserInOut record not found"));
            CompanyEmployee employee = this.companyEmployeeRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Parse timeOut from DTO
            if (dto.getTimeOut() == null || dto.getTimeOut().isEmpty()) {
                throw new RuntimeException("TimeOut is required for update");
            }
            OffsetDateTime odt = OffsetDateTime.parse(dto.getTimeOut());
            Date timeOut = Date.from(odt.toInstant());

            // Delegate to common handler
            boolean updated = handleTimeOutUpdate(employee, userInOut, timeOut,
                    dto.getLocationId(),
                    employee.getCompanyDetails().getId());

            if (!updated) {
                // New record created, return original DTO (or maybe indicate?)
                return dto;
            }

            // If updated, we might also want to reflect other changes? Currently only
            // timeOut is set.
            // The original code also set user (already set) but nothing else.
            return dto;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String clickInOut(int userId, Integer locationId, Integer companyId) {
        try {
            CompanyEmployee employee = companyEmployeeRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            // Get current open record (timeOut null)
            UserInOut existing = userInOutRepository.getCurrentUserRecord(userId);

            if (existing != null) {
                // There is an open record → attempt to clock out
                boolean updated = updateUserInOut(existing.getId(), userId);
                if (updated) {
                    return "updated:" + employee.getFirstName() + " " + employee.getLastName();
                } else {
                    return "created:" + employee.getFirstName() + " " + employee.getLastName();
                }
            } else {
                // No open record → clock in (create new with current time)
                createUserInOut(userId, locationId, companyId, new Date());
                return "created:" + employee.getFirstName() + " " + employee.getLastName();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteUserInOut(Long id) {
        try {
            UserInOut userInOut = this.userInOutRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("UserInOut record not found"));
            this.userInOutRepository.delete(userInOut);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UserInOutDto addClockInOut(UserInOutDto userInOutDto) {
        try {
            if (userInOutDto.getId() != null) {
                // Update existing record
                UserInOut userInOut = this.userInOutRepository.findById(userInOutDto.getId())
                        .orElseThrow(() -> new RuntimeException("Clock in out not found"));
                CompanyEmployee employee = this.companyEmployeeRepository.findById(userInOutDto.getUserId())
                        .orElseThrow(() -> new RuntimeException("Employee not found"));

                // If timeOut is provided, apply gap logic
                if (userInOutDto.getTimeOut() != null) {
                    Date timeOut = parseAnyDate(userInOutDto.getTimeOut());
                    boolean updated = handleTimeOutUpdate(employee, userInOut, timeOut,
                            userInOutDto.getLocationId(),
                            userInOutDto.getCompanyId());
                    if (!updated) {
                        // New record created, return DTO as is
                        return userInOutDto;
                    }
                }

                // Also update timeIn if provided? (original code does this, but we keep it)
                if (userInOutDto.getTimeIn() != null) {
                    userInOut.setTimeIn(parseAnyDate(userInOutDto.getTimeIn()));
                }
                // Note: createdOn might also be updated in original, but we skip for brevity
                userInOutRepository.save(userInOut);
                return userInOutDto;
            } else {
                // Create new record (no gap check for creation)
                UserInOut userInOut = new UserInOut();
                CompanyEmployee companyEmployee = this.companyEmployeeRepository.findById(userInOutDto.getUserId())
                        .orElseThrow(() -> new RuntimeException("Employee not found"));
                CompanyDetails companyDetails = this.companyDetailsRepository.findById(userInOutDto.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Company not found"));

                userInOut.setIsSalaryGenerate(0);
                userInOut.setUser(companyEmployee);
                userInOut.setCompanyDetails(companyDetails);

                if (userInOutDto.getCreatedOn() == null) {
                    userInOut.setCreatedOn(new Date());
                } else {
                    userInOut.setCreatedOn(parseAnyDate(userInOutDto.getCreatedOn()));
                }

                if (userInOutDto.getTimeIn() != null) {
                    userInOut.setTimeIn(parseAnyDate(userInOutDto.getTimeIn()));
                } else {
                    throw new RuntimeException("Clock In is required");
                }

                if (userInOutDto.getTimeOut() != null) {
                    userInOut.setTimeOut(parseAnyDate(userInOutDto.getTimeOut()));
                } else {
                    userInOut.setTimeOut(null);
                }

                this.userInOutRepository.save(userInOut);
                return userInOutDto;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<UserInOutDto> getTodayEntriesByUserId(int userId) {
        // Get the current date
        Calendar calendar = Calendar.getInstance();

        // Get Start of the Day (00:00:00)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        // Get End of the Day (23:59:59)
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endOfDay = calendar.getTime();

        List<UserInOut> entries = userInOutRepository.findByUserIdAndToday(userId, startOfDay, endOfDay);

        return entries.stream().map(entry -> {
            UserInOutDto dto = new UserInOutDto();
            dto.setId(entry.getId());
            dto.setTimeIn(this.commonService.convertDateToString(entry.getTimeIn(), "Asia/Calcutta"));
            dto.setTimeOut(this.commonService.convertDateToString(entry.getTimeOut(), "Asia/Calcutta"));
            dto.setUserId(entry.getUser().getEmployeeId());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTimeInOutReport(List<Integer> userIds, String startDate, String endDate,
            String timeZone, Integer companyId) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date start, end;
            if (startDate == null || endDate == null) {
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                start = calendar.getTime();
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 0);
                end = calendar.getTime();
            } else {
                start = this.commonService.convertLocalToUtc(startDate, timeZone, false);
                end = this.commonService.convertLocalToUtc(endDate, timeZone, true);
            }

            // Fetch only the users specified by userIds
            List<CompanyEmployee> users = new ArrayList<>();
            if (userIds != null && !userIds.isEmpty()) { // Check for null first!
                users = this.companyEmployeeRepository.findAllById(userIds);
            } else {
                users = this.companyEmployeeRepository.findAll(); // if no userIds provided get all users.
            }
            Map<Integer, String> userMap = users.stream()
                    .collect(Collectors.toMap(CompanyEmployee::getEmployeeId,
                            user -> Stream.of(
                                    user.getFirstName(),
                                    user.getMiddleName(),
                                    user.getLastName())
                                    .filter(Objects::nonNull)
                                    .filter(s -> !s.isBlank())
                                    .collect(Collectors.joining(" "))));

            Specification<UserInOut> spec = UserInOutSpecification.createdOnGreaterThanEqual(start)
                    .and(UserInOutSpecification.createdOnLessThanEqual(end));

            // Add a specification to filter by userIds
            if (userIds != null && !userIds.isEmpty()) {
                spec = spec.and(UserInOutSpecification.userIdIn(userIds));
            }
            if (companyId != null) {
                spec = spec.and(UserInOutSpecification.hasCompany(companyId));
            }

            List<UserInOut> userInOutRecords = userInOutRepository.findAll(spec);
            Map<Integer, List<UserInOut>> userInOutMap = userInOutRecords.stream()
                    .collect(Collectors.groupingBy(record -> record.getUser().getEmployeeId()));

            List<Map<String, Object>> responseList = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : userMap.entrySet()) {
                int currentUserId = entry.getKey();
                String userName = entry.getValue();
                List<UserInOut> records = userInOutMap.getOrDefault(currentUserId, new ArrayList<>());

                Map<Integer, List<Map<String, Object>>> monthlyRecords = new HashMap<>();
                for (UserInOut record : records) {
                    Calendar recordCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    recordCal.setTime(record.getCreatedOn());
                    int month = recordCal.get(Calendar.MONTH) + 1;

                    Map<String, Object> dayRecord = Map.of(
                            "records", List.of(
                                    Map.of(
                                            "timeIn",
                                            this.commonService.convertDateToString(record.getTimeIn(), timeZone),
                                            "timeOut",
                                            record.getTimeOut() != null
                                                    ? this.commonService.convertDateToString(record.getTimeOut(),
                                                            timeZone)
                                                    : "")));

                    monthlyRecords.computeIfAbsent(month, m -> new ArrayList<>()).add(dayRecord);
                }

                List<Map<String, Object>> monthData = monthlyRecords.entrySet().stream()
                        .map(entrySet -> Map.of("month", entrySet.getKey(), "data", entrySet.getValue()))
                        .collect(Collectors.toList());

                responseList.add(Map.of("username", userName, "records", monthData));
            }

            return Map.of("data", responseList);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Workbook generateExcelReport(Map<String, Object> data, String startDateStr, String endDateStr,
            String timeZone) {
        try {
            Workbook workbook = new XSSFWorkbook();
            SimpleDateFormat jsonDateFormat = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a");
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy"); // Include year
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

            Map<String, Sheet> monthSheets = new LinkedHashMap<>();

            Date startDate = null;
            Date endDate = null;

            try {
                if (startDateStr != null && !startDateStr.isEmpty()) {
                    startDate = dateFormat.parse(startDateStr);
                }
                if (endDateStr != null && !endDateStr.isEmpty()) {
                    endDate = dateFormat.parse(endDateStr);
                }
            } catch (Exception e) {
                System.err.println("Invalid start or end date format. Defaulting to current month.");
            }

            if (startDate == null || endDate == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                startDate = calendar.getTime();

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                endDate = calendar.getTime();
            }

            Set<String> monthKeys = generateMonthKeys(startDate, endDate, monthFormat);

            for (String monthKey : monthKeys) {
                Sheet sheet = createSheet(workbook, monthKey, startDate, endDate); // Pass startDate and endDate
                monthSheets.put(monthKey, sheet);
            }
            List<Map<String, Object>> userData = (List<Map<String, Object>>) data.get("data");

            for (Map<String, Object> user : userData) {
                String userName = (String) user.get("username");
                List<Map<String, Object>> records = (List<Map<String, Object>>) user.get("records");

                if (records == null || records.isEmpty()) {
                    for (String monthKey : monthSheets.keySet()) {
                        Sheet sheet = monthSheets.get(monthKey);
                        writeUserRecord(sheet, userName, Collections.emptyList(), timeZone);
                    }
                    continue;
                }

                for (String monthKey : monthSheets.keySet()) {
                    Sheet sheet = monthSheets.get(monthKey);
                    List<Map<String, Object>> filteredRecords = new ArrayList<>();
                    try {
                        Date currentMonthDate = monthFormat.parse(monthKey);
                        Calendar currentMonthCalendar = Calendar.getInstance();
                        currentMonthCalendar.setTime(currentMonthDate);
                        int currentMonth = currentMonthCalendar.get(Calendar.MONTH) + 1; // 1-based index

                        for (Map<String, Object> record : records) {
                            Integer recordMonth = (Integer) record.get("month");
                            if (recordMonth != null && recordMonth == currentMonth) {
                                List<Map<String, Object>> dataList = (List<Map<String, Object>>) record.get("data");
                                if (dataList != null) {
                                    for (Map<String, Object> dataEntry : dataList) {
                                        List<Map<String, Object>> timeRecords = (List<Map<String, Object>>) dataEntry
                                                .get("records");
                                        if (timeRecords != null) {
                                            filteredRecords.addAll(timeRecords);
                                        }
                                    }
                                }
                            }
                        }
                        writeUserRecord(sheet, userName, filteredRecords, timeZone);
                    } catch (ParseException e) {
                        System.err.println("Error parsing month key: " + monthKey);
                        e.printStackTrace();
                    }
                }
            }

            return workbook;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<String> generateMonthKeys(Date startDate, Date endDate, SimpleDateFormat monthFormat) {
        Set<String> monthKeys = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String key1, String key2) {
                try {
                    Date date1 = monthFormat.parse(key1);
                    Date date2 = monthFormat.parse(key2);
                    return date1.compareTo(date2); // Sort chronologically
                } catch (Exception e) {
                    throw new RuntimeException("Error parsing month keys: " + e.getMessage());
                }
            }
        });

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        int direction = startDate.before(endDate) ? 1 : -1; // Determine the direction of iteration

        while (true) {
            String monthKey = monthFormat.format(calendar.getTime());
            monthKeys.add(monthKey);

            if (calendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == endCalendar.get(Calendar.MONTH)) {
                break; // Stop when the end month is reached
            }

            calendar.add(Calendar.MONTH, direction); // Move to the next or previous month
        }

        return monthKeys;
    }

    private Sheet createSheet(Workbook workbook, String sheetName, Date startDate, Date endDate) {
        Sheet sheet = workbook.createSheet(sheetName.replace("/", "-"));

        // Calculate the start and end days for the sheet
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy"); // Corrected format
            Date sheetMonthDate = monthFormat.parse(sheetName);
            calendar.setTime(sheetMonthDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return sheet; // Return if parsing fails
        }

        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date sheetStartDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date sheetEndDate = calendar.getTime();

        int startDay = 1;
        int endDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (startDate != null && sheetStartDate.getMonth() == startDate.getMonth()
                && sheetStartDate.getYear() == startDate.getYear()) {
            startDay = startDate.getDate();
        }

        if (endDate != null && sheetEndDate.getMonth() == endDate.getMonth()
                && sheetEndDate.getYear() == endDate.getYear()) {
            endDay = endDate.getDate();
        }
        int totalColumns = endDay - startDay + 2; // +2 for "User Name" and "Total Hours"

        // Create title row
        Row titleRow = sheet.createRow(0);
        CellRangeAddress titleRegion = new CellRangeAddress(0, 0, 0, totalColumns);
        sheet.addMergedRegion(titleRegion); // Adjusted colspan
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("In-Out Report");

        RegionUtil.setBorderTop(BorderStyle.THIN, titleRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, titleRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, titleRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, titleRegion, sheet);
        titleCell.setCellStyle(createCellStyle(workbook, true, true, true, true, 14));

        // Create month row
        Row monthRow = sheet.createRow(1);
        CellRangeAddress monthRegion = new CellRangeAddress(1, 1, 0, totalColumns);
        sheet.addMergedRegion(monthRegion);
        Cell monthCell = monthRow.createCell(0);
        monthCell.setCellValue(sheetName);

        RegionUtil.setBorderTop(BorderStyle.THIN, monthRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, monthRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, monthRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, monthRegion, sheet);
        monthCell.setCellStyle(createCellStyle(workbook, true, true, true, true, 14));

        // Create header row with day numbers
        Row headerRow = sheet.createRow(2);
        Cell userNameCell = headerRow.createCell(0);
        userNameCell.setCellValue("User Name");
        userNameCell.setCellStyle(createCellStyle(workbook, true, true, true, true, 12));

        // Add day numbers (startDay to endDay)
        for (int i = startDay; i <= endDay; i++) {
            Cell cell = headerRow.createCell(i - startDay + 1);
            cell.setCellValue(i);
            cell.setCellStyle(createCellStyle(workbook, true, true, true, true, 12));
        }

        // Add "Total Hours" in the last column
        Cell totalHoursCell = headerRow.createCell(endDay - startDay + 2);
        totalHoursCell.setCellValue("Total Hours");
        totalHoursCell.setCellStyle(createCellStyle(workbook, true, true, true, true, 12));

        return sheet;
    }

    private void writeUserRecord(Sheet sheet, String userName, List<Map<String, Object>> records, String timeZone) {
        int rowNum = findOrCreateUserRow(sheet, userName);
        Row row = sheet.getRow(rowNum);
        if (row == null) {
            row = sheet.createRow(rowNum);
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy, hh:mm:ss a");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        long totalMinutes = 0;
        int startDay = sheet.getRow(2).getCell(1).getNumericCellValue() != 1
                ? (int) sheet.getRow(2).getCell(1).getNumericCellValue()
                : 1;
        int endDay = (int) sheet.getRow(2).getCell(sheet.getRow(2).getLastCellNum() - 2).getNumericCellValue();

        Map<Integer, StringBuilder> dayEntries = new HashMap<>();
        for (Map<String, Object> record : records) {
            try {
                if (record.get("timeIn") == null || record.get("timeOut") == null) {
                    continue;
                }

                Date timeIn = inputFormat
                        .parse(this.commonService.convertUtcToLocal(record.get("timeIn").toString(), timeZone));
                Date timeOut = inputFormat
                        .parse(this.commonService.convertUtcToLocal(record.get("timeOut").toString(), timeZone));

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(timeIn);
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

                long durationMinutes = Math.round((timeOut.getTime() - timeIn.getTime()) / (60.0 * 1000.0));

                if (durationMinutes < 0) {
                    durationMinutes += 24 * 60; // Handle overnight shifts
                }

                totalMinutes += durationMinutes;

                if (dayOfMonth >= startDay && dayOfMonth <= endDay) {
                    dayEntries.putIfAbsent(dayOfMonth, new StringBuilder());
                    dayEntries.get(dayOfMonth)
                            .append(timeFormat.format(timeIn))
                            .append(" - ")
                            .append(timeFormat.format(timeOut))
                            .append("\n");
                }
            } catch (Exception e) {
                System.err.println("Error parsing timeIn or timeOut: " + e.getMessage());
            }
        }

        for (int day = startDay; day <= endDay; day++) {
            Cell cell = row.getCell(day - startDay + 1);
            if (cell == null) {
                cell = row.createCell(day - startDay + 1);
            }
            cell.setCellStyle(createCellStyle(sheet.getWorkbook(), false, true, false, true, 11));

            String timeRanges = dayEntries.containsKey(day) ? dayEntries.get(day).toString().trim() : "-";
            cell.setCellValue(timeRanges);
        }

        Cell totalCell = row.getCell(endDay - startDay + 2);
        if (totalCell == null) {
            totalCell = row.createCell(endDay - startDay + 2);
        }
        totalCell.setCellStyle(createCellStyle(sheet.getWorkbook(), false, true, true, true, 11));
        totalCell.setCellValue(formatTotalTime(totalMinutes)); // Pass totalMinutes to formatTotalTime
    }

    private int findOrCreateUserRow(Sheet sheet, String userName) {
        int lastRow = sheet.getLastRowNum();
        for (int i = 3; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null && row.getCell(0).getStringCellValue().equals(userName)) {
                return i;
            }
        }
        Row newRow = sheet.createRow(lastRow + 1);
        Cell cell = newRow.createCell(0);
        cell.setCellValue(userName);
        cell.setCellStyle(createCellStyle(sheet.getWorkbook(), true, true, true, true, 11)); // Bold, no borders, no
        // centered, size 11
        return lastRow + 1;
    }

    private CellStyle createCellStyle(Workbook workbook, boolean isBold, boolean hasBorders, boolean isCentered,
            boolean isVerticallyCentered, int fontSize) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        if (isBold) {
            font.setBold(true);
        }

        font.setFontHeightInPoints((short) fontSize);
        style.setFont(font);

        if (isCentered) {
            style.setAlignment(HorizontalAlignment.CENTER);
        }
        if (isVerticallyCentered) {
            style.setVerticalAlignment(VerticalAlignment.CENTER); // Add vertical alignment
        }
        if (hasBorders) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }

        style.setWrapText(true);
        return style;
    }

    private String formatTotalTime(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%d hr %02d min", hours, minutes);
    }

    public Date parseAnyDate(String s) {
        try {
            if (s == null || s.trim().isEmpty())
                return null;
            s = s.trim();

            // 1) ISO with time (Z or offset) - e.g. 2026-01-31T08:34:45.622Z
            if (s.contains("T")) {
                return Date.from(Instant.parse(s));
            }

            // 2) dd/MM/yyyy - e.g. 31/01/2026
            if (s.matches("\\d{2}/\\d{2}/\\d{4}")) {
                SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                f.setLenient(false);
                // choose timezone: if this represents a "date only", usually treat it as local
                // start-of-day
                // If you want it as UTC midnight, keep UTC.
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(s);
            }

            // 3) yyyy-MM-dd - e.g. 2026-01-31
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                f.setLenient(false);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(s);
            }

            // 4) dd/MM/yyyy, hh:mm:ss a
            SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy, hh:mm:ss a", Locale.ENGLISH);
            f.setLenient(false);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(s);

        } catch (Exception e) {
            throw new RuntimeException("Invalid date format: " + s, e);
        }
    }

    private boolean handleTimeOutUpdate(CompanyEmployee employee, UserInOut existingRecord,
            Date timeOut, Integer locationId, Integer companyId) {

        String autoTimeInAfter = employee.getCompanyDetails().getAutoTimeInAfterHours();

        // 1️⃣ If no limit is defined, just update the existing record
        if (autoTimeInAfter == null || autoTimeInAfter.isEmpty()) {
            existingRecord.setTimeOut(timeOut);
            userInOutRepository.save(existingRecord);
            return true;
        }

        // 2️⃣ Convert UTC timeIn and current timeOut to IST (Asia/Kolkata)
        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        // timeIn from DB (UTC) to IST
        ZonedDateTime timeInIst = existingRecord.getTimeIn().toInstant()
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(istZone);

        // current timeOut (passed as param) to IST
        ZonedDateTime timeOutIst = timeOut.toInstant()
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(istZone);

        // 3️⃣ Parse allowed gap duration (format "HH:mm")
        String[] parts = autoTimeInAfter.split(":");
        int limitHours = Integer.parseInt(parts[0]);
        int limitMinutes = Integer.parseInt(parts[1]);
        Duration allowedLimit = Duration.ofHours(limitHours).plusMinutes(limitMinutes);

        // 4️⃣ Calculate current session duration
        Duration sessionDuration = Duration.between(timeInIst, timeOutIst);

        // 5️⃣ Check if session duration exceeds the limit
        if (!sessionDuration.isNegative() && sessionDuration.compareTo(allowedLimit) > 0) {
            // Gap exceeded → create new record for next day (timeOut + 24 hours)
            // Instant nextDayInstant = timeOut.toInstant().plus(Duration.ofDays(1));
            // Date nextDayTimeIn = Date.from(nextDayInstant);
            Date nextDayTimeIn = Date.from(timeOut.toInstant());
            createUserInOut(employee.getEmployeeId(), locationId, companyId, nextDayTimeIn);
            return false;
        } else {
            // Within limit → update existing record
            existingRecord.setTimeOut(timeOut);
            userInOutRepository.save(existingRecord);
            return true;
        }
    }

    private boolean isWeeklyOffDay(DayOfWeek dayOfWeek, int weekOfMonth, WeeklyOff weeklyOff) {
        if (weeklyOff == null)
            return false;

        switch (dayOfWeek) {
            case SUNDAY:
                if (weeklyOff.isSundayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isSunday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isSunday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isSunday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isSunday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isSunday5th());
            case MONDAY:
                if (weeklyOff.isMondayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isMonday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isMonday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isMonday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isMonday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isMonday5th());
            case TUESDAY:
                if (weeklyOff.isTuesdayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isTuesday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isTuesday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isTuesday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isTuesday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isTuesday5th());
            case WEDNESDAY:
                if (weeklyOff.isWednesdayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isWednesday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isWednesday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isWednesday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isWednesday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isWednesday5th());
            case THURSDAY:
                if (weeklyOff.isThursdayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isThursday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isThursday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isThursday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isThursday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isThursday5th());
            case FRIDAY:
                if (weeklyOff.isFridayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isFriday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isFriday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isFriday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isFriday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isFriday5th());
            case SATURDAY:
                if (weeklyOff.isSaturdayAll())
                    return true;
                return (weekOfMonth == 1 && weeklyOff.isSaturday1st()) ||
                        (weekOfMonth == 2 && weeklyOff.isSaturday2nd()) ||
                        (weekOfMonth == 3 && weeklyOff.isSaturday3rd()) ||
                        (weekOfMonth == 4 && weeklyOff.isSaturday4th()) ||
                        (weekOfMonth == 5 && weeklyOff.isSaturday5th());
            default:
                return false;
        }
    }

    @Override
    @Transactional
    public void addBulkClockInOut(BulkUserInOutDto bulkDto) {
        try {
            if (bulkDto == null || bulkDto.getUserId() == null || bulkDto.getUserId().isEmpty()) {
                throw new RuntimeException("User ID list cannot be empty");
            }
            if (bulkDto.getStartDate() == null || bulkDto.getStartDate().isEmpty() ||
                    bulkDto.getEndDate() == null || bulkDto.getEndDate().isEmpty()) {
                throw new RuntimeException("Start date and End date are required");
            }

            Date start = parseAnyDate(bulkDto.getStartDate());
            Date end = parseAnyDate(bulkDto.getEndDate());

            if (start == null || end == null) {
                throw new RuntimeException("Start date and End date are invalid");
            }

            ZoneId utcZone = ZoneId.of("UTC");
            LocalDate startLocal = start.toInstant().atZone(utcZone).toLocalDate();
            LocalDate endLocal = end.toInstant().atZone(utcZone).toLocalDate();

            if (startLocal.isAfter(endLocal)) {
                throw new RuntimeException("Start date cannot be after End date");
            }

            Date timeInDate = parseAnyDate(bulkDto.getTimeIn());
            Date timeOutDate = (bulkDto.getTimeOut() != null && !bulkDto.getTimeOut().isEmpty())
                    ? parseAnyDate(bulkDto.getTimeOut())
                    : null;

            if (timeInDate == null) {
                throw new RuntimeException("Time In is required");
            }

            ZonedDateTime baseTimeIn = timeInDate.toInstant().atZone(utcZone);
            ZonedDateTime baseTimeOut = timeOutDate != null ? timeOutDate.toInstant().atZone(utcZone) : null;
            Duration duration = baseTimeOut != null ? Duration.between(baseTimeIn, baseTimeOut) : null;

            CompanyDetails companyDetails = this.companyDetailsRepository.findById(bulkDto.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));

            for (Integer userId : bulkDto.getUserId()) {
                CompanyEmployee companyEmployee = this.companyEmployeeRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Employee not found for id: " + userId));

                WeeklyOff weeklyOff = null;
                if (companyEmployee.getWeeklyOff() != null) {
                    weeklyOff = this.weeklyOffRepository.findById(companyEmployee.getWeeklyOff().getId())
                            .orElse(null);
                }

                List<String> holidayDates = new ArrayList<>();
                if (companyEmployee.getHolidayTemplates() != null) {
                    List<HolidayTemplateDetailsDto> dtoList = this.holidayTemplateDetailsService
                            .getAllHolidayTemplateDetailsByTemplateId(companyEmployee.getHolidayTemplates().getId());
                    if (dtoList != null && !dtoList.isEmpty()) {
                        for (HolidayTemplateDetailsDto dto : dtoList) {
                            if (dto.getDate() != null && dto.getDate().length() >= 10) {
                                holidayDates.add(dto.getDate().substring(0, 10));
                            }
                        }
                    }
                }

                for (LocalDate date = startLocal; !date.isAfter(endLocal); date = date.plusDays(1)) {
                    String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    if (!holidayDates.isEmpty() && holidayDates.contains(formattedDate)) {
                        continue;
                    }

                    if (weeklyOff != null) {
                        DayOfWeek dayOfWeek = date.getDayOfWeek();
                        int weekOfMonth = ((date.getDayOfMonth() - 1) / 7) + 1;
                        if (isWeeklyOffDay(dayOfWeek, weekOfMonth, weeklyOff)) {
                            continue;
                        }
                    }

                    UserInOut userInOut = new UserInOut();
                    Date startDate = Date.from(date.atStartOfDay(utcZone).toInstant());
                    Date endDate = Date.from(date.atTime(LocalTime.MAX).atZone(utcZone).toInstant());
                    List<UserInOut> existingRecords = this.userInOutRepository
                            .findByUserIdAndDateRange(companyEmployee.getEmployeeId(), startDate, endDate);

                    userInOut.setUser(companyEmployee);
                    userInOut.setCompanyDetails(companyDetails);
                    userInOut.setIsSalaryGenerate(0);

                    ZonedDateTime newTimeIn = baseTimeIn.withYear(date.getYear())
                            .withMonth(date.getMonthValue())
                            .withDayOfMonth(date.getDayOfMonth());

                    ZonedDateTime newTimeOut = null;
                    if (baseTimeOut != null) {
                        newTimeOut = newTimeIn.plus(duration);
                    }

                    userInOut.setTimeIn(Date.from(newTimeIn.toInstant()));
                    if (newTimeOut != null) {
                        userInOut.setTimeOut(Date.from(newTimeOut.toInstant()));
                    } else {
                        userInOut.setTimeOut(null);
                    }

                    userInOut.setCreatedOn(Date.from(newTimeIn.toInstant()));
                    if (existingRecords.isEmpty()) {
                        this.userInOutRepository.save(userInOut);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}