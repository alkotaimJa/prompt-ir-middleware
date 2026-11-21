package com.jawaher.GPT.experiment.repository;

import com.jawaher.GPT.experiment.domain.Vote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    @EntityGraph(attributePaths = {"trial", "trial.participant"})
    @Query("select v from Vote v order by v.createdAt asc")
    List<Vote> findAllWithTrial();
}
