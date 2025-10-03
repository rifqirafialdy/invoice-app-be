package com.invoiceapp.invoice.infrastructure.repository;

import com.invoiceapp.invoice.domain.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    Optional<Invoice> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}