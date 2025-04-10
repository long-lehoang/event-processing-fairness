package com.event.processing.dlq_service.service;

import com.event.processing.dlq_service.domain.entity.DeadLetterEvent;
import com.event.processing.dlq_service.repository.DeadLetterEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service for handling batch database operations.
 * This service provides methods for efficiently processing entities in batches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchOperationService {

  private final DeadLetterEventRepository repository;

  @Value("${dlq.batch.size:100}")
  private int batchSize;

  /**
   * Processes a list of entities in batches.
   * This method collects entities and saves them in batches for better performance.
   *
   * @param entities The entities to save
   * @return The saved entities
   */
  @Transactional
  public List<DeadLetterEvent> saveInBatches(List<DeadLetterEvent> entities) {
    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }

    log.info("Saving {} entities in batches of {}", entities.size(), batchSize);

    List<DeadLetterEvent> savedEntities = new ArrayList<>();
    List<DeadLetterEvent> batch = new ArrayList<>(batchSize);

    for (DeadLetterEvent entity : entities) {
      batch.add(entity);

      if (batch.size() >= batchSize) {
        savedEntities.addAll(repository.saveAll(batch));
        batch.clear();
      }
    }

    // Save any remaining entities
    if (!batch.isEmpty()) {
      savedEntities.addAll(repository.saveAll(batch));
    }

    log.info("Successfully saved {} entities in batches", savedEntities.size());
    return savedEntities;
  }

  /**
   * Processes entities with a consumer function and then saves them in batches.
   * This method is useful when entities need to be modified before saving.
   *
   * @param entities  The entities to process and save
   * @param processor The function to process each entity
   * @return The saved entities
   */
  @Transactional
  public List<DeadLetterEvent> processAndSaveInBatches(
      List<DeadLetterEvent> entities,
      Consumer<DeadLetterEvent> processor) {

    if (entities == null || entities.isEmpty()) {
      return new ArrayList<>();
    }

    log.info("Processing and saving {} entities in batches", entities.size());

    // Process all entities
    for (DeadLetterEvent entity : entities) {
      processor.accept(entity);
    }

    // Save in batches
    return saveInBatches(entities);
  }
}
