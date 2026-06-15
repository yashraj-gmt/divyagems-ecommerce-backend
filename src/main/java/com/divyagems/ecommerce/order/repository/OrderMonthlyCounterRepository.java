package com.divyagems.ecommerce.order.repository;

import com.divyagems.ecommerce.entity.OrderMonthlyCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderMonthlyCounterRepository extends JpaRepository<OrderMonthlyCounter, String> {

    /**
     * Fetches the counter row with a PESSIMISTIC_WRITE lock,
     * preventing concurrent transactions from reading stale values.
     * This is the heart of the thread-safe OrderIdGenerator.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM OrderMonthlyCounter c WHERE c.yearMonthKey = :key")
    Optional<OrderMonthlyCounter> findByYearMonthKeyWithLock(@Param("key") String yearMonthKey);
}
