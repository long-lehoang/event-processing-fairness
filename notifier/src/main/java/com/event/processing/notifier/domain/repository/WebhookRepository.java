package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.entity.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, String> {
}
