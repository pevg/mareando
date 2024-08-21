package com.pevg.www.concurrencia.repositories;

import com.pevg.www.concurrencia.entities.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {
}
