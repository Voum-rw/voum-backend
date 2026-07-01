package com.voum.modules.review.repository;

import com.voum.modules.review.entity.TripReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripReviewRepository extends JpaRepository<TripReview, UUID> {

    boolean existsByTripIdAndReviewerId(UUID tripId, UUID reviewerId);

    // Get all reviews where the reviewed user is motariId
    @Query("SELECT r FROM TripReview r WHERE r.reviewedUserId = :motariId ORDER BY r.createdAt DESC")
    Page<TripReview> findByReviewedUserId(@Param("motariId") UUID motariId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM TripReview r WHERE r.reviewedUserId = :userId")
    Double getAverageRatingForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(r) FROM TripReview r WHERE r.reviewedUserId = :userId")
    Integer countReviewsForUser(@Param("userId") UUID userId);

    @Query("SELECT r FROM TripReview r WHERE " +
           "(:phone IS NULL OR " +
           " r.reviewerId IN (SELECT u.id FROM User u WHERE u.phone LIKE %:phone%) OR " +
           " r.reviewedUserId IN (SELECT u.id FROM User u WHERE u.phone LIKE %:phone%)) ORDER BY r.createdAt DESC")
    Page<TripReview> findAllFiltered(@Param("phone") String phone, Pageable pageable);
}

