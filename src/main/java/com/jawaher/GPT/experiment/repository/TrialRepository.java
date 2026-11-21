package com.jawaher.GPT.experiment.repository;

import com.jawaher.GPT.experiment.domain.Trial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TrialRepository extends JpaRepository<Trial, UUID> {

    long countByParticipantId(UUID participantId);

    long countByParticipantIdAndUsableTrue(UUID participantId);

    long countByParticipantIdAndEvaluatedAtIsNotNull(UUID participantId);

    long countByEvaluatedAtIsNotNull();

    @EntityGraph(attributePaths = {"participant"})
    @Query("select t from Trial t order by t.createdAt asc")
    List<Trial> findAllWithParticipant();
}
