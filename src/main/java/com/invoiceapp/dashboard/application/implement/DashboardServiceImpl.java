package com.invoiceapp.dashboard.application.implement;

import com.invoiceapp.client.infrastructure.repository.ClientRepository;
import com.invoiceapp.common.specification.BaseSpecification;
import com.invoiceapp.dashboard.application.service.DashboardService;
import com.invoiceapp.dashboard.presentation.dto.response.DashboardStatsResponse;
import com.invoiceapp.dashboard.presentation.dto.response.RecentActivityResponse;
import com.invoiceapp.invoice.application.mapper.InvoiceMapper;
import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    public DashboardStatsResponse getDashboardStats(UUID userId) {
        Specification<Invoice> userSpec = BaseSpecification.withUserId(userId, "user");
        List<Invoice> allInvoices = invoiceRepository.findAll(userSpec);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal overdueAmount = BigDecimal.ZERO;

        long pendingCount = 0;
        long paidCount = 0;
        long overdueCount = 0;

        for (Invoice invoice : allInvoices) {
            BigDecimal total = invoice.getTotal();

            switch (invoice.getStatus()) {
                case PAID:
                    paidAmount = paidAmount.add(total);
                    totalRevenue = totalRevenue.add(total);
                    paidCount++;
                    break;
                case SENT:
                case DUE:
                    pendingAmount = pendingAmount.add(total);
                    pendingCount++;
                    break;
                case OVERDUE:
                    overdueAmount = overdueAmount.add(total);
                    overdueCount++;
                    break;
                default:
                    break;
            }
        }

        long totalClients = clientRepository.count(BaseSpecification.withUserId(userId, "user"));

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .pendingAmount(pendingAmount)
                .paidAmount(paidAmount)
                .overdueAmount(overdueAmount)
                .totalClients(totalClients)
                .totalInvoices((long) allInvoices.size())
                .pendingInvoicesCount(pendingCount)
                .paidInvoicesCount(paidCount)
                .overdueInvoicesCount(overdueCount)
                .build();
    }

    @Override
    public List<InvoiceResponse> getRecentInvoices(UUID userId, int limit) {
        Specification<Invoice> spec = BaseSpecification.withUserId(userId, "user");
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        return invoiceRepository.findAll(spec, pageable)
                .stream()
                .map(invoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecentActivityResponse> getRecentActivity(UUID userId, int limit) {
        List<Invoice> recentInvoices = invoiceRepository.findAll(
                BaseSpecification.withUserId(userId, "user"),
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).getContent();

        List<RecentActivityResponse> activities = new ArrayList<>();

        for (Invoice invoice : recentInvoices) {
            String message = generateActivityMessage(invoice);

            activities.add(RecentActivityResponse.builder()
                    .id(invoice.getId().toString())
                    .type(mapStatusToActivityType(invoice.getStatus()))
                    .message(message)
                    .timestamp(invoice.getUpdatedAt())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .clientName(invoice.getClient().getName())
                    .build());
        }

        return activities;
    }

    private String generateActivityMessage(Invoice invoice) {
        return switch (invoice.getStatus()) {
            case PAID -> String.format("Invoice %s paid by %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
            case SENT -> String.format("Invoice %s sent to %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
            case OVERDUE -> String.format("Invoice %s is overdue for %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
            case CANCELLED -> String.format("Invoice %s cancelled for %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
            default -> String.format("Invoice %s created for %s",
                    invoice.getInvoiceNumber(), invoice.getClient().getName());
        };
    }

    private String mapStatusToActivityType(InvoiceStatus status) {
        return switch (status) {
            case PAID -> "INVOICE_PAID";
            case SENT -> "INVOICE_SENT";
            default -> "INVOICE_CREATED";
        };
    }
}