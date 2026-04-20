package br.com.souza.cqrs_pattern.music_service.database.repository;

import br.com.souza.cqrs_pattern.music_service.database.model.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {}