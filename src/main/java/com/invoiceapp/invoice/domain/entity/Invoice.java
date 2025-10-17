package com.invoiceapp.invoice.domain.entity;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.client.domain.entity.Client;
import com.invoiceapp.invoice.domain.enums.InvoiceStatus;
import com.invoiceapp.invoice.domain.enums.RecurringFrequency;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE invoices SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Client client;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,name="status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurring_frequency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private RecurringFrequency recurringFrequency;

    @Column(name = "next_generation_date")
    private LocalDate nextGenerationDate;

    @Column(name = "recurring_series_id")
    private UUID recurringSeriesId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(item -> {
                    if (item.getTotal() == null) {
                        return item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
                    }
                    return item.getTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rate = (this.taxRate != null) ? this.taxRate : BigDecimal.ZERO;
        this.taxAmount = subtotal.multiply(rate).divide(new BigDecimal("100"));
        this.total = subtotal.add(taxAmount);
    }
}