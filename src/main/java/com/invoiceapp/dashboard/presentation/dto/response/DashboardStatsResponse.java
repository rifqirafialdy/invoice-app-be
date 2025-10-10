package com.invoiceapp.dashboard.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private BigDecimal totalRevenue;
    private BigDecimal pendingAmount;
    private BigDecimal paidAmount;
    private BigDecimal overdueAmount;
    private Long totalClients;
    private Long totalInvoices;
    private Long pendingInvoicesCount;
    private Long paidInvoicesCount;
    private Long overdueInvoicesCount;
}