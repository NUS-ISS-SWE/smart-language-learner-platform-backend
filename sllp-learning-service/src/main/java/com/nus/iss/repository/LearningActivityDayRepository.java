package com.nus.iss.repository;

import com.nus.iss.entity.LearningActivityDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface LearningActivityDayRepository extends JpaRepository<LearningActivityDay, Long> {
    boolean existsByUserIdAndActivityDate(Long userId, LocalDate activityDate);
    @Query("select d.activityDate from LearningActivityDay d where d.userId = :userId order by d.activityDate desc")
    List<LocalDate> findActivityDates(Long userId);
}
