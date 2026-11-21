package com.jawaher.GPT.experiment.repository;

import com.jawaher.GPT.experiment.domain.LlmResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LlmResponseRepository extends JpaRepository<LlmResponse, UUID> {

    @EntityGraph(attributePaths = {"trial"})
    @Query("select r from LlmResponse r order by r.createdAt asc")
    List<LlmResponse> findAllWithTrial();
}
