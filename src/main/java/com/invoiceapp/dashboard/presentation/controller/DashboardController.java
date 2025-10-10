package com.invoiceapp.dashboard.presentation.controller;

import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.dashboard.application.service.DashboardService;
import com.invoiceapp.dashboard.presentation.dto.response.DashboardStatsResponse;
import com.invoiceapp.dashboard.presentation.dto.response.RecentActivityResponse;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(
            @RequestAttribute("userId") UUID userId) {

        DashboardStatsResponse stats = dashboardService.getDashboardStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/recent-invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getRecentInvoices(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "5") int limit) {

        List<InvoiceResponse> recentInvoices = dashboardService.getRecentInvoices(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(recentInvoices));
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<ApiResponse<List<RecentActivityResponse>>> getRecentActivity(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "10") int limit) {

        List<RecentActivityResponse> activities = dashboardService.getRecentActivity(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }
}