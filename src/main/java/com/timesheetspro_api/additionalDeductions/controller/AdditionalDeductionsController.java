package com.timesheetspro_api.additionalDeductions.controller;

import com.timesheetspro_api.additionalDeductions.service.AdditionalDeductionsService;
import com.timesheetspro_api.common.dto.additionalDeductions.AdditionalDeductionsDto;
import com.timesheetspro_api.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/additionalDeductions")
public class AdditionalDeductionsController {

    @Autowired
    private AdditionalDeductionsService additionalDeductionsService;

    @GetMapping("/get/all/{month}/{id}")
    public ApiResponse<?> findAllByIdAndMonth(@PathVariable String month, @PathVariable Integer id) {
        Map<String, Object> resBody = new HashMap<>();
        try {
            return new ApiResponse<>(HttpStatus.OK.value(), "Fetch details successfully",
                    this.additionalDeductionsService.findByMonthAndUser(id, month));
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Fail to fetch details", resBody);
        }
    }

    @GetMapping("/get/{id}")
    public ApiResponse<?> findById(@PathVariable Integer id) {
        Map<String, Object> resBody = new HashMap<>();
        try {
            return new ApiResponse<>(HttpStatus.OK.value(), "Fetch details successfully",
                    this.additionalDeductionsService.findById(id));
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Fail to fetch details", resBody);
        }
    }

    @PostMapping("/save")
    public ApiResponse<?> save(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody List<AdditionalDeductionsDto> additionalDeductionsDto) {
        Map<String, Object> resBody = new HashMap<>();
        try {
            this.additionalDeductionsService.save(additionalDeductionsDto);
            return new ApiResponse<>(HttpStatus.CREATED.value(), "Additional deductions save successfully", "");
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), resBody);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> delete(@PathVariable Integer id) {
        Map<String, Object> resBody = new HashMap<>();
        try {
            this.additionalDeductionsService.delete(id);
            return new ApiResponse<>(HttpStatus.OK.value(), "Additional deductions deleted successfully", "");
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Fail to delete additional deductions",
                    resBody);
        }
    }
}
