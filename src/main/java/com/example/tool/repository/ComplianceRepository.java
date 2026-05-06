package com.example.tool.repository;

import com.example.tool.entity.Compliance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ComplianceRepository extends JpaRepository<Compliance, Long> {

    List<Compliance> findByStatus(String status);

    long countByStatus(String status);

    @Query("SELECT c FROM Compliance c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Compliance> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    List<Compliance> findByDueDateBetween(LocalDate start, LocalDate end);

    /**
     * Returns items whose dueDate is strictly before {@code date}
     * and whose status is NOT equal to {@code status}.
     * Used by the daily overdue reminder to find past-due, incomplete items.
     */
    List<Compliance> findByDueDateBeforeAndStatusNot(LocalDate date, String status);

    /**
     * Returns items whose dueDate falls within [{@code start}, {@code end}]
     * and whose status is NOT equal to {@code status}.
     * Used by the 7-day deadline alert to find upcoming, incomplete items.
     */
    List<Compliance> findByDueDateBetweenAndStatusNot(LocalDate start, LocalDate end, String status);
}
