package com.invoiceapp.invoice.infrastructure.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberGenerator {

    private final StringRedisTemplate redisTemplate;

    public String generateInvoiceNumber(UUID userId) {
        int currentYear = LocalDate.now().getYear();

        String userCode = getOrCreateUserCode(userId);
        String sequenceKey = String.format("invoice:sequence:%s:%d", userId, currentYear);
        Long sequence = redisTemplate.opsForValue().increment(sequenceKey);

        if (sequence == null) {
            throw new RuntimeException("Failed to generate invoice sequence from Redis");
        }

        if (sequence == 1) {
            LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
            LocalDate expiryDate = endOfYear.plusMonths(1);
            long daysUntilExpiry = Duration.between(
                    LocalDate.now().atStartOfDay(),
                    expiryDate.atStartOfDay()
            ).toDays();

            redisTemplate.expire(sequenceKey, Duration.ofDays(daysUntilExpiry));
            log.info("Created new invoice sequence for user {} year {}", userId, currentYear);
        }

        String invoiceNumber = String.format("%s-%d-%04d", userCode, currentYear, sequence);
        log.debug("Generated invoice number: {}", invoiceNumber);

        return invoiceNumber;
    }


    private String getOrCreateUserCode(UUID userId) {
        String userCodeKey = String.format("user:code:%s", userId);

        String existingCode = redisTemplate.opsForValue().get(userCodeKey);

        if (existingCode != null) {
            return existingCode;
        }

        Long userSequence = redisTemplate.opsForValue().increment("user:code:sequence");

        if (userSequence == null) {
            throw new RuntimeException("Failed to generate user code sequence");
        }

        String newUserCode = String.format("U%04d", userSequence);

        redisTemplate.opsForValue().set(userCodeKey, newUserCode);

        log.info("Created new user code {} for userId {}", newUserCode, userId);

        return newUserCode;
    }
}