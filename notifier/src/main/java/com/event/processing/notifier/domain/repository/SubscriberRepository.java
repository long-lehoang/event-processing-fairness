package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Subscriber entities.
 * Provides database operations for subscriber information and their
 * relationships with segments and events.
 * <p>
 * Key features:
 * - Basic CRUD operations inherited from JpaRepository
 * - Subscriber profile management
 * - Subscription status tracking
 * - Custom fields support
 *
 * @author LongLe
 * @version 1.0
 */
@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, String> {
}
