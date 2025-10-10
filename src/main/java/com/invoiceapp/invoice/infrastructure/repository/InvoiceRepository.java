package com.invoiceapp.invoice.infrastructure.repository;

import com.invoiceapp.invoice.domain.entity.Invoice;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    Optional<Invoice> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByIsRecurringTrueAndNextGenerationDate(LocalDate date);


    List<Invoice> findByIsRecurringTrueAndNextGenerationDateLessThanEqual(LocalDate date);

}