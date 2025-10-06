package com.invoiceapp.invoice.application.implement;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.client.domain.entity.Client;
import com.invoiceapp.client.infrastructure.repository.ClientRepository;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.common.specification.BaseSpecification;
import com.invoiceapp.invoice.application.service.InvoiceService;
import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.entity.InvoiceItem;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.invoice.infrastructure.util.InvoiceNumberGenerator;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceItemRequest;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceRequest;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceItemResponse;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import com.invoiceapp.product.domain.entity.Product;
import com.invoiceapp.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private LocalDate calculateNextGenerationDate(LocalDate issueDate, String frequency) {
        if (frequency == null) return null;

        return switch (frequency) {
            case "WEEKLY" -> issueDate.plusWeeks(1);
            case "MONTHLY" -> issueDate.plusMonths(1);
            case "YEARLY" -> issueDate.plusYears(1);
            default -> issueDate.plusMonths(1);
        };
    }

    @Override
    public InvoiceResponse createInvoice(InvoiceRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = clientRepository.findByIdAndUserId(request.getClientId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        String invoiceNumber = invoiceNumberGenerator.generateInvoiceNumber(userId);

        Invoice invoice = Invoice.builder()
                .user(user)
                .client(client)
                .invoiceNumber(invoiceNumber)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .status(request.getStatus())
                .taxRate(request.getTaxRate())
                .notes(request.getNotes())
                .isRecurring(Boolean.TRUE.equals(request.getIsRecurring()))
                .recurringFrequency(request.getRecurringFrequency())
                .nextGenerationDate(
                        Boolean.TRUE.equals(request.getIsRecurring()) ?
                                calculateNextGenerationDate(request.getIssueDate(), request.getRecurringFrequency()) :
                                null
                )
                .build();

        for (InvoiceItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndUserId(itemRequest.getProductId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            InvoiceItem item = InvoiceItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            invoice.addItem(item);
        }

        invoice.calculateTotals();
        invoice = invoiceRepository.save(invoice);

        return mapToResponse(invoice);
    }

    @Override
    public InvoiceResponse updateInvoice(UUID invoiceId, InvoiceRequest request, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        Client client = clientRepository.findByIdAndUserId(request.getClientId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        invoice.setClient(client);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(request.getStatus());
        invoice.setTaxRate(request.getTaxRate());
        invoice.setNotes(request.getNotes());
        invoice.getItems().clear();

        for (InvoiceItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndUserId(itemRequest.getProductId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            InvoiceItem item = InvoiceItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            invoice.addItem(item);
        }

        invoice.calculateTotals();
        invoice = invoiceRepository.save(invoice);

        return mapToResponse(invoice);
    }

    @Override
    public void deleteInvoice(UUID invoiceId, UUID userId) {
        if (!invoiceRepository.existsByIdAndUserId(invoiceId, userId)) {
            throw new ResourceNotFoundException("Invoice not found");
        }
        invoiceRepository.deleteById(invoiceId);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return mapToResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getAllInvoices(UUID userId, int page, int size, String sortBy, String sortDir,
                                                String search, InvoiceStatus status, LocalDate startDate, LocalDate endDate) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Invoice> spec = Specification.allOf(
                BaseSpecification.<Invoice>withUserId(userId, "user"),
                BaseSpecification.<Invoice>withSearch(search, "invoiceNumber"),
                BaseSpecification.<Invoice, InvoiceStatus>withEquals("status", status),
                BaseSpecification.<Invoice, LocalDate>withDateRange("issueDate", startDate, endDate)
        );

        Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
        return invoices.map(this::mapToResponse);
    }



    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems().stream()
                .map(item -> InvoiceItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .total(item.getTotal())
                        .build())
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .clientId(invoice.getClient().getId())
                .clientName(invoice.getClient().getName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .displayStatus(invoice.getStatus().name())
                .items(itemResponses)
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .total(invoice.getTotal())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
