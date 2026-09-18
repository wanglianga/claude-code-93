package com.ccb.queue.repo;

import com.ccb.queue.model.CashInventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashInventoryRepository extends JpaRepository<CashInventory, Long> {
}
