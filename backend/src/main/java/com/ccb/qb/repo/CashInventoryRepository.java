package com.ccb.qb.repo;

import com.ccb.qb.entity.CashInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CashInventoryRepository extends JpaRepository<CashInventory, Long> {
    Optional<CashInventory> findByRecordDate(LocalDate date);
}
