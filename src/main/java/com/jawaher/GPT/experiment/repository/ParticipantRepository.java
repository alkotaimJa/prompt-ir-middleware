package com.jawaher.GPT.experiment.repository;

import com.jawaher.GPT.experiment.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
}
