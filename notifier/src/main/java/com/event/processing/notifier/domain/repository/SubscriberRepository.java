package com.event.processing.notifier.domain.repository;

import com.event.processing.notifier.domain.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, String> {
}
