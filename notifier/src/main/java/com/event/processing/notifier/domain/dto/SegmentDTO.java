package com.event.processing.notifier.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Data Transfer Object for subscriber segments.
 * This class represents a segment or group that a subscriber belongs to,
 * used for categorization and targeting purposes.
 * <p>
 * The DTO includes:
 * - Segment identification
 * - Segment name
 * - Internal subscriber reference
 * <p>
 * Uses Lombok annotations for reducing boilerplate code and Jackson annotations
 * for JSON property mapping.
 *
 * @author LongLe
 * @version 1.0
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class SegmentDTO {
  /**
   * Unique identifier for the segment.
   */
  @JsonProperty("id")
  private String id;

  /**
   * Display name of the segment.
   */
  @JsonProperty("name")
  private String name;

  /**
   * Internal reference to the associated subscriber.
   * Not exposed in JSON serialization.
   */
  @JsonIgnore
  private String subscriberId;
}
