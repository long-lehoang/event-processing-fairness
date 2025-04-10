package com.event.processing.dlq_service.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class for pagination operations.
 * This class provides common methods for handling paginated data.
 */
public final class PaginationUtils {

  private PaginationUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Processes paginated data using the provided function.
   *
   * @param pageSize     The size of each page
   * @param pageFunction The function to retrieve a page of data
   * @param processor    The function to process each item in the page
   * @param <T>          The type of data being processed
   * @param <R>          The type of result
   * @return A list of results from processing the data
   */
  public static <T, R> List<R> processPaginated(
      int pageSize,
      Function<Pageable, Page<T>> pageFunction,
      Function<T, R> processor) {

    List<R> results = Collections.synchronizedList(new ArrayList<>());
    int pageNumber = 0;
    boolean hasMorePages = true;

    while (hasMorePages) {
      Pageable pageable = PageRequest.of(pageNumber, pageSize);
      Page<T> page = pageFunction.apply(pageable);

      List<T> content = page.getContent();
      if (content.isEmpty()) {
        hasMorePages = false;
        continue;
      }

      for (T item : content) {
        R result = processor.apply(item);
        if (result != null) {
          results.add(result);
        }
      }

      pageNumber++;
      hasMorePages = page.hasNext();
    }

    return results;
  }

  /**
   * Processes paginated data using the provided function with an additional parameter.
   *
   * @param pageSize     The size of each page
   * @param pageFunction The function to retrieve a page of data
   * @param processor    The function to process each item in the page
   * @param param        Additional parameter for the processor function
   * @param <T>          The type of data being processed
   * @param <P>          The type of the additional parameter
   * @param <R>          The type of result
   * @return A list of results from processing the data
   */
  public static <T, P, R> List<R> processPaginatedWithParam(
      int pageSize,
      Function<Pageable, Page<T>> pageFunction,
      BiFunction<T, P, R> processor,
      P param) {

    List<R> results = Collections.synchronizedList(new ArrayList<>());
    int pageNumber = 0;
    boolean hasMorePages = true;

    while (hasMorePages) {
      Pageable pageable = PageRequest.of(pageNumber, pageSize);
      Page<T> page = pageFunction.apply(pageable);

      List<T> content = page.getContent();
      if (content.isEmpty()) {
        hasMorePages = false;
        continue;
      }

      for (T item : content) {
        R result = processor.apply(item, param);
        if (result != null) {
          results.add(result);
        }
      }

      pageNumber++;
      hasMorePages = page.hasNext();
    }

    return results;
  }

  /**
   * Processes paginated data using the provided batch processor function.
   * This method is useful when you want to process an entire batch of items at once.
   *
   * @param pageSize       The size of each page
   * @param pageFunction   The function to retrieve a page of data
   * @param batchProcessor The function to process each batch of items
   * @param <T>            The type of data being processed
   * @param <R>            The type of result
   * @return A list of results from processing the data
   */
  public static <T, R> List<R> processPaginatedBatch(
      int pageSize,
      Function<Pageable, Page<T>> pageFunction,
      Function<List<T>, List<R>> batchProcessor) {

    List<R> results = Collections.synchronizedList(new ArrayList<>());
    int pageNumber = 0;
    boolean hasMorePages = true;

    while (hasMorePages) {
      Pageable pageable = PageRequest.of(pageNumber, pageSize);
      Page<T> page = pageFunction.apply(pageable);

      List<T> content = page.getContent();
      if (content.isEmpty()) {
        hasMorePages = false;
        continue;
      }

      // Process the entire batch at once
      List<R> batchResults = batchProcessor.apply(content);
      if (batchResults != null && !batchResults.isEmpty()) {
        results.addAll(batchResults);
      }

      pageNumber++;
      hasMorePages = page.hasNext();
    }

    return results;
  }
}
