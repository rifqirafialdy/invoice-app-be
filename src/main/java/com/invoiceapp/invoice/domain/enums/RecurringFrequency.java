package com.invoiceapp.invoice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public enum RecurringFrequency {
    DAILY("Daily", 1) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusDays(1);
        }
    },
    WEEKLY("Weekly", 7) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusWeeks(1);
        }
    },
    BIWEEKLY("Bi-Weekly", 14) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusWeeks(2);
        }
    },
    MONTHLY("Monthly", 30) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusMonths(1);
        }
    },
    QUARTERLY("Quarterly", 90) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusMonths(3);
        }
    },
    YEARLY("Yearly", 365) {
        @Override
        public LocalDate calculateNextDate(LocalDate from) {
            return from.plusYears(1);
        }
    };

    private final String displayName;
    private final int approximateDays;

    public abstract LocalDate calculateNextDate(LocalDate from);

    public static RecurringFrequency fromString(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return null;
        }

        try {
            return RecurringFrequency.valueOf(frequency.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}