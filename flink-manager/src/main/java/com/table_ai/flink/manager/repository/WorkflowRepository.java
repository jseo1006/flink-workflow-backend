package com.table_ai.flink.manager.repository;

import com.table_ai.flink.manager.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    Optional<Workflow> findByName(String name);

    List<Workflow> findByUserId(String userId);

    Optional<Workflow> findByIdAndUserId(Long id, String userId);

    Optional<Workflow> findByNameAndUserId(String name, String userId);
}
