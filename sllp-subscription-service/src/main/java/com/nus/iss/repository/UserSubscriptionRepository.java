package com.nus.iss.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.SubscriptionStatus;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserSubscription> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SubscriptionStatus status);
}
