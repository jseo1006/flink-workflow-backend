package com.table_ai.flink.manager.repository;

import com.table_ai.flink.manager.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Optional<Workflow> findByName(String name);
}
