package com.invoiceapp.invoice.application.implement;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.client.domain.entity.Client;
import com.invoiceapp.client.infrastructure.repository.ClientRepository;
import com.invoiceapp.common.dto.PageDTO;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.common.specification.BaseSpecification;
import com.invoiceapp.invoice.application.helper.RecurringInvoiceHelper;
import com.invoiceapp.invoice.application.mapper.InvoiceMapper;
import com.invoiceapp.invoice.application.service.InvoiceEmailService;
import com.invoiceapp.invoice.application.service.InvoiceService;
import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.entity.InvoiceItem;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.infrastructure.repository.InvoiceRepository;
import com.invoiceapp.invoice.infrastructure.util.InvoiceNumberGenerator;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceItemRequest;
import com.invoiceapp.invoice.presentation.dto.request.InvoiceRequest;
import com.invoiceapp.invoice.presentation.dto.response.InvoiceResponse;
import com.invoiceapp.product.domain.entity.Product;
import com.invoiceapp.product.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final InvoiceEmailService invoiceEmailService;
    private final InvoiceMapper invoiceMapper;
    private final RecurringInvoiceHelper recurringInvoiceHelper;

    @Override
    @CacheEvict(value = "invoices", key = "#userId.toString()")
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
                                recurringInvoiceHelper.calculateNextGenerationDate(
                                        request.getIssueDate(),
                                        request.getRecurringFrequency()
                                ) : null
                )
                .build();

        for (InvoiceItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndUserId(itemRequest.getProductId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            InvoiceItem item = InvoiceItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productDescription(product.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            invoice.addItem(item);
        }

        invoice.calculateTotals();
        invoice = invoiceRepository.save(invoice);

        if (invoice.getStatus() == InvoiceStatus.SENT) {
            invoiceEmailService.sendInvoiceActionEmail(invoice);
        }

        log.info("Invoice created: {} for user: {}", invoice.getInvoiceNumber(), userId);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @CacheEvict(value = "invoices", key = "#userId.toString()")
    public InvoiceResponse updateInvoice(UUID invoiceId, InvoiceRequest request, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        InvoiceStatus oldStatus = invoice.getStatus();

        Client client = clientRepository.findByIdAndUserId(request.getClientId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        invoice.setClient(client);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setStatus(request.getStatus());
        invoice.setTaxRate(request.getTaxRate());
        invoice.setNotes(request.getNotes());
        invoice.getItems().clear();
        if (request.getStatus() == InvoiceStatus.CANCELLED &&
                Boolean.TRUE.equals(invoice.getIsRecurring())) {
            invoice.setIsRecurring(false);
            log.info("Stopped recurring for invoice {} - status changed to CANCELLED",
                    invoice.getInvoiceNumber());
        }

        for (InvoiceItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndUserId(itemRequest.getProductId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            InvoiceItem item = InvoiceItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productDescription(product.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();

            invoice.addItem(item);
        }

        invoice.calculateTotals();
        invoice = invoiceRepository.save(invoice);

        if (oldStatus != request.getStatus()) {
            if (request.getStatus() == InvoiceStatus.SENT) {
                invoiceEmailService.sendInvoiceActionEmail(invoice);
            } else if (request.getStatus() == InvoiceStatus.PAID) {
                invoiceEmailService.sendPaymentConfirmationEmail(invoice);
            }
        }

        log.info("Invoice updated: {} for user: {}", invoice.getInvoiceNumber(), userId);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @CacheEvict(value = "invoices", key = "#userId.toString()")
    public void deleteInvoice(UUID invoiceId, UUID userId) {
        if (!invoiceRepository.existsByIdAndUserId(invoiceId, userId)) {
            throw new ResourceNotFoundException("Invoice not found");
        }
        invoiceRepository.deleteById(invoiceId);
        log.info("Invoice deleted: {} for user: {}", invoiceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Cacheable(
            cacheNames = "invoices",
            key = "#userId.toString()",
            condition = "#search == null && #status == null && #startDate == null && #endDate == null && #isRecurring == null"
    )
    @Transactional(readOnly = true)
    public PageDTO<InvoiceResponse> getAllInvoices(
            UUID userId,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String search,
            InvoiceStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isRecurring) {

        log.info("Cache miss - Fetching invoices from database for user: {}", userId);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Invoice> spec = Specification.allOf(
                BaseSpecification.<Invoice>withUserId(userId, "user"),
                BaseSpecification.<Invoice>withSearch(search, "invoiceNumber"),
                BaseSpecification.<Invoice, InvoiceStatus>withEquals("status", status),
                BaseSpecification.<Invoice, LocalDate>withDateRange("issueDate", startDate, endDate),
                BaseSpecification.<Invoice, Boolean>withEquals("isRecurring", isRecurring)
        );

        Page<Invoice> invoices = invoiceRepository.findAll(spec, pageable);
        Page<InvoiceResponse> invoiceResponsePage = invoices.map(invoiceMapper::toResponse);

        return new PageDTO<>(
                invoiceResponsePage.getContent(),
                invoiceResponsePage.getTotalPages(),
                invoiceResponsePage.getTotalElements(),
                invoiceResponsePage.getNumber(),
                invoiceResponsePage.getSize()
        );
    }
    @Override
    @CacheEvict(value = "invoices", key = "#userId.toString()")
    @Transactional
    public InvoiceResponse stopRecurring(UUID invoiceId, UUID userId) {
        Invoice invoice = invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!Boolean.TRUE.equals(invoice.getIsRecurring())) {
            throw new IllegalStateException("Invoice is not recurring");
        }

        invoice.setIsRecurring(false);
        invoice = invoiceRepository.save(invoice);
        log.info("Stopped recurring invoice: {} for user: {}", invoice.getInvoiceNumber(), userId);
        return invoiceMapper.toResponse(invoice);
    }
}
