package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Webhook entities.
 * Provides database operations for webhook configurations and their
 * relationships with events.
 *
 * Key features:
 * - Basic CRUD operations inherited from JpaRepository
 * - Webhook configuration management
 * - Event notification endpoint management
 *
 * @author LongLe
 * @version 1.0
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, String> {
}
