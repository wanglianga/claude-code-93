package com.ccb.qb.repo;

import com.ccb.qb.entity.BranchAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchAlertRepository extends JpaRepository<BranchAlert, Long> {
    List<BranchAlert> findByActiveTrueOrderByCreatedAtDesc();
}
