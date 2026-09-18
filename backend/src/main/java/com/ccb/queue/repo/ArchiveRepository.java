package com.ccb.queue.repo;

import com.ccb.queue.model.OperationArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArchiveRepository extends JpaRepository<OperationArchive, Long> {
    List<OperationArchive> findAllByOrderByArchivedAtDesc();
    long countByChannelCorrectFalse();
}
