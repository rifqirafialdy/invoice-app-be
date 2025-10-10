package com.invoiceapp.dashboard.application.service;

import com.invoiceapp.dashboard.presentation.dto.response.DashboardStatsResponse;
import com.invoiceapp.dashboard.presentation.dto.response.RecentActivityResponse;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;

import java.util.List;
import java.util.UUID;

public interface DashboardService {
    DashboardStatsResponse getDashboardStats(UUID userId);
    List<InvoiceResponse> getRecentInvoices(UUID userId, int limit);
    List<RecentActivityResponse> getRecentActivity(UUID userId, int limit);
}