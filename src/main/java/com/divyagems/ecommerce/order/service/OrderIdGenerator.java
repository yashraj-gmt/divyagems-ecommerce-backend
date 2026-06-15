package com.divyagems.ecommerce.order.service;

import com.divyagems.ecommerce.entity.OrderMonthlyCounter;
import com.divyagems.ecommerce.order.repository.OrderMonthlyCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Thread-safe order ID generator producing human-readable IDs in the format:
 *   DG-YYYYMM-XXXXX   (e.g. DG-202601-00043)
 *
 * Thread-safety strategy:
 * - Runs in its own REQUIRES_NEW transaction to serialize concurrent requests.
 * - Uses a PESSIMISTIC_WRITE lock on the monthly counter row.
 * - PostgreSQL row-level locking ensures only one thread can increment at a time.
 * - REQUIRES_NEW prevents the outer order transaction from interfering.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIdGenerator {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    private final OrderMonthlyCounterRepository counterRepository;

    /**
     * Generate the next order ID for the current calendar month.
     * This method always runs in a fresh, independent transaction.
     *
     * @return unique order ID string e.g. "DG-202601-00043"
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateNextOrderId() {
        String yearMonthKey = LocalDate.now().format(MONTH_FMT);  // e.g. "202601"

        OrderMonthlyCounter counter = counterRepository
                .findByYearMonthKeyWithLock(yearMonthKey)
                .orElseGet(() -> {
                    // First order of the month — create counter row
                    OrderMonthlyCounter newCounter = OrderMonthlyCounter.builder()
                            .yearMonthKey(yearMonthKey)
                            .counter(0L)
                            .build();
                    return counterRepository.save(newCounter);
                });

        long nextCount = counter.getCounter() + 1;
        counter.setCounter(nextCount);
        counterRepository.save(counter);

        String orderId = String.format("DG-%s-%05d", yearMonthKey, nextCount);
        log.debug("Generated orderId: {}", orderId);
        return orderId;
    }
}
