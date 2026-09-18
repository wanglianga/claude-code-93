package com.ccb.queue.repo;

import com.ccb.queue.model.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchStatusRepository extends JpaRepository<BranchStatus, Long> {
}
