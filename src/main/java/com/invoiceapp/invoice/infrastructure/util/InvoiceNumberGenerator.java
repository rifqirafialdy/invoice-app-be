package com.invoiceapp.invoice.infrastructure.util;

import com.invoiceapp.common.constants.AppConstants;
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
        Long sequence = getNextSequence(userId, currentYear);

        String invoiceNumber = String.format(AppConstants.INVOICE_NUMBER_FORMAT,
                userCode, currentYear, sequence);

        log.debug("Generated invoice number: {}", invoiceNumber);

        return invoiceNumber;
    }

    private String getOrCreateUserCode(UUID userId) {
        String userCodeKey = AppConstants.REDIS_USER_CODE_PREFIX + userId;
        String existingCode = redisTemplate.opsForValue().get(userCodeKey);

        if (existingCode != null) {
            return existingCode;
        }

        return createNewUserCode(userId, userCodeKey);
    }

    private String createNewUserCode(UUID userId, String userCodeKey) {
        Long userSequence = redisTemplate.opsForValue()
                .increment(AppConstants.REDIS_USER_CODE_SEQUENCE);

        if (userSequence == null) {
            throw new RuntimeException("Failed to generate user code sequence");
        }

        String newUserCode = String.format(AppConstants.USER_CODE_FORMAT, userSequence);
        redisTemplate.opsForValue().set(userCodeKey, newUserCode);

        log.info("Created new user code {} for userId {}", newUserCode, userId);

        return newUserCode;
    }

    private Long getNextSequence(UUID userId, int currentYear) {
        String sequenceKey = String.format("%s%s:%d",
                AppConstants.REDIS_INVOICE_SEQUENCE_PREFIX, userId, currentYear);

        Long sequence = redisTemplate.opsForValue().increment(sequenceKey);

        if (sequence == null) {
            throw new RuntimeException("Failed to generate invoice sequence from Redis");
        }

        if (sequence == 1) {
            setSequenceExpiration(sequenceKey, currentYear);
            log.info("Created new invoice sequence for user {} year {}", userId, currentYear);
        }

        return sequence;
    }

    private void setSequenceExpiration(String sequenceKey, int currentYear) {
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);
        LocalDate expiryDate = endOfYear.plusMonths(1);

        long daysUntilExpiry = Duration.between(
                LocalDate.now().atStartOfDay(),
                expiryDate.atStartOfDay()
        ).toDays();

        redisTemplate.expire(sequenceKey, Duration.ofDays(daysUntilExpiry));
    }
}