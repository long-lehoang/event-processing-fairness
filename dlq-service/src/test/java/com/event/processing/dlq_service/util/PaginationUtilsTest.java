package com.event.processing.dlq_service.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaginationUtilsTest {

  @Test
  void processPaginated_shouldProcessAllPages() {
    // Arrange
    int pageSize = 2;

    // Create mock page function
    @SuppressWarnings("unchecked")
    Function<Pageable, Page<String>> pageFunction = mock(Function.class);

    // Set up mock to return two pages of data
    Page<String> page1 = new PageImpl<>(Arrays.asList("item1", "item2"));
    Page<String> page2 = new PageImpl<>(Arrays.asList("item3"));
    Page<String> emptyPage = new PageImpl<>(Collections.emptyList());

    when(pageFunction.apply(any(Pageable.class)))
        .thenReturn(page1)
        .thenReturn(page2)
        .thenReturn(emptyPage);

    // Create processor function that converts strings to uppercase
    Function<String, String> processor = String::toUpperCase;

    // Act
    List<String> results = PaginationUtils.processPaginated(pageSize, pageFunction, processor);

    // Assert
    assertEquals(2, results.size());
    assertEquals("ITEM1", results.get(0));
    assertEquals("ITEM2", results.get(1));
    verify(pageFunction, times(1)).apply(any(Pageable.class));
  }

  @Test
  void processPaginated_shouldHandleEmptyPages() {
    // Arrange
    int pageSize = 10;

    // Create mock page function
    @SuppressWarnings("unchecked")
    Function<Pageable, Page<String>> pageFunction = mock(Function.class);

    // Set up mock to return empty page
    Page<String> emptyPage = new PageImpl<>(Collections.emptyList());
    when(pageFunction.apply(any(Pageable.class))).thenReturn(emptyPage);

    // Create processor function
    Function<String, String> processor = String::toUpperCase;

    // Act
    List<String> results = PaginationUtils.processPaginated(pageSize, pageFunction, processor);

    // Assert
    assertTrue(results.isEmpty());
    verify(pageFunction, times(1)).apply(any(Pageable.class));
  }

  @Test
  void processPaginatedWithParam_shouldProcessAllPagesWithParameter() {
    // Arrange
    int pageSize = 2;
    String param = "prefix-";

    // Create mock page function
    @SuppressWarnings("unchecked")
    Function<Pageable, Page<Integer>> pageFunction = mock(Function.class);

    // Set up mock to return two pages of data
    Page<Integer> page1 = new PageImpl<>(Arrays.asList(1, 2));
    Page<Integer> page2 = new PageImpl<>(Arrays.asList(3));
    Page<Integer> emptyPage = new PageImpl<>(Collections.emptyList());

    when(pageFunction.apply(any(Pageable.class)))
        .thenReturn(page1)
        .thenReturn(page2)
        .thenReturn(emptyPage);

    // Act
    List<String> results = PaginationUtils.processPaginatedWithParam(
        pageSize,
        pageFunction,
        (number, prefix) -> prefix + number.toString(),
        param
    );

    // Assert
    assertEquals(2, results.size());
    assertEquals("prefix-1", results.get(0));
    assertEquals("prefix-2", results.get(1));
    verify(pageFunction, times(1)).apply(any(Pageable.class));
  }

  @Test
  void processPaginatedBatch_shouldProcessBatchesOfItems() {
    // Arrange
    int pageSize = 2;

    // Create mock page function
    @SuppressWarnings("unchecked")
    Function<Pageable, Page<String>> pageFunction = mock(Function.class);

    // Set up mock to return two pages of data
    Page<String> page1 = new PageImpl<>(Arrays.asList("item1", "item2"));
    Page<String> page2 = new PageImpl<>(Arrays.asList("item3"));
    Page<String> emptyPage = new PageImpl<>(Collections.emptyList());

    when(pageFunction.apply(any(Pageable.class)))
        .thenReturn(page1)
        .thenReturn(page2)
        .thenReturn(emptyPage);

    // Create batch processor function that converts all strings in the batch to uppercase
    Function<List<String>, List<String>> batchProcessor = batch ->
        batch.stream().map(String::toUpperCase).collect(java.util.stream.Collectors.toList());

    // Act
    List<String> results = PaginationUtils.processPaginatedBatch(pageSize, pageFunction, batchProcessor);

    // Assert
    assertEquals(2, results.size());
    assertEquals("ITEM1", results.get(0));
    assertEquals("ITEM2", results.get(1));
    verify(pageFunction, times(1)).apply(any(Pageable.class));
  }
}
