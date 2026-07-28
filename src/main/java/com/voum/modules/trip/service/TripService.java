package com.voum.modules.trip.service;

import com.voum.common.ApiException;
import com.voum.modules.location.entity.UserLocation;
import com.voum.modules.location.repository.UserLocationRepository;
import com.voum.modules.location.service.LocationService;
import com.voum.modules.location.util.HaversineCalculator;
import com.voum.modules.marketplace.entity.RideOffer;
import com.voum.modules.marketplace.entity.RideRequest;
import com.voum.modules.marketplace.events.RideOfferAcceptedEvent;
import com.voum.modules.trip.dto.TripResponse;
import com.voum.modules.trip.dto.TripStatsResponse;
import com.voum.modules.trip.entity.Trip;
import com.voum.modules.trip.events.*;
import com.voum.modules.trip.mapper.TripMapper;
import com.voum.modules.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    private final TripRepository tripRepository;
    private final UserLocationRepository userLocationRepository;
    private final LocationService locationService;
    private final TripMapper tripMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Listens to the marketplace event published when an offer is accepted.
     * Automatically creates a new Trip in CREATED status.
     */
    @EventListener
    @Transactional
    public void handleRideOfferAccepted(RideOfferAcceptedEvent event) {
        RideRequest request = event.getRideRequest();
        RideOffer offer = event.getRideOffer();
        log.info("Creating Trip for accepted request: {} and offer: {}", request.getId(), offer.getId());

        // Calculate distance snapshot
        double distanceKm = 0.0;
        UserLocation driverLoc = userLocationRepository.findByUserId(offer.getMotariId()).orElse(null);
        if (driverLoc != null) {
            distanceKm = HaversineCalculator.calculateDistance(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    driverLoc.getLatitude(), driverLoc.getLongitude()
            );
        }

        Instant now = Instant.now();
        Trip trip = Trip.builder()
                .rideRequestId(request.getId())
                .rideOfferId(offer.getId())
                .passengerId(request.getPassengerId())
                .motariId(offer.getMotariId())
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .pickupAddress(request.getPickupAddress())
                .destinationLatitude(request.getDestinationLatitude())
                .destinationLongitude(request.getDestinationLongitude())
                .destinationAddress(request.getDestinationAddress())
                .agreedPrice(offer.getOfferedPrice())
                .estimatedArrivalMinutes(offer.getEstimatedArrivalMinutes())
                .estimatedDistanceKm(distanceKm)
                .status("CREATED")
                .lastStatusChangeAt(now)
                .build();

        trip = tripRepository.save(trip);

        log.info("Trip created successfully with ID: {} and number: {}", trip.getId(), trip.getTripNumber());

        // Publish events
        eventPublisher.publishEvent(new TripCreatedEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, null, "CREATED"));
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID id, UUID callerId) {
        Trip trip = findAndValidateOwnership(id, callerId);
        return tripMapper.toResponse(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips(UUID userId) {
        return tripRepository.findAllByPassengerIdOrMotariId(userId).stream()
                .map(tripMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TripStatsResponse getTripStats(UUID userId) {
        List<Trip> trips = tripRepository.findAllByPassengerIdOrMotariId(userId);
        ZoneId zone = ZoneId.of("Africa/Kigali");
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime startOf7Days = startOfToday.minusDays(6);
        ZonedDateTime startOf30Days = startOfToday.minusDays(29);

        long todayCount = 0;
        long weeklyCount = 0;
        long monthlyCount = 0;
        long totalCompleted = 0;
        long totalCancelled = 0;

        Map<String, Long> dailyPerformance = new LinkedHashMap<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : days) {
            dailyPerformance.put(d, 0L);
        }

        for (Trip trip : trips) {
            String status = trip.getStatus() != null ? trip.getStatus().toUpperCase() : "";
            if ("COMPLETED".equals(status)) {
                totalCompleted++;
                if (trip.getCreatedAt() != null) {
                    ZonedDateTime tripTime = trip.getCreatedAt().atZone(zone);
                    if (!tripTime.isBefore(startOfToday)) {
                        todayCount++;
                    }
                    if (!tripTime.isBefore(startOf7Days)) {
                        weeklyCount++;
                    }
                    if (!tripTime.isBefore(startOf30Days)) {
                        monthlyCount++;
                    }
                    // Current week Mon-Sun performance
                    int dayOfWeek = tripTime.getDayOfWeek().getValue(); // 1 = Monday .. 7 = Sunday
                    String dayKey = days[dayOfWeek - 1];
                    dailyPerformance.put(dayKey, dailyPerformance.get(dayKey) + 1);
                }
            } else if ("CANCELLED".equals(status)) {
                totalCancelled++;
            }
        }

        return TripStatsResponse.builder()
                .todayCompletedCount(todayCount)
                .weeklyCompletedCount(weeklyCount)
                .monthlyCompletedCount(monthlyCount)
                .totalCompletedCount(totalCompleted)
                .totalCancelledCount(totalCancelled)
                .dailyPerformance(dailyPerformance)
                .build();
    }

    @Transactional
    public TripResponse markEnRoute(UUID id, UUID driverId) {
        log.info("Driver {} marking trip {} as EN_ROUTE", driverId, id);
        Trip trip = findAndValidateOwnership(id, driverId);

        if (!trip.getMotariId().equals(driverId)) {
            throw new ApiException("Access Denied: Only the assigned driver can perform this action.", HttpStatus.FORBIDDEN);
        }

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "MOTARI_EN_ROUTE");

        trip.setStatus("MOTARI_EN_ROUTE");
        trip.setLastStatusChangeAt(Instant.now());
        trip = tripRepository.save(trip);

        eventPublisher.publishEvent(new MotariEnRouteEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "MOTARI_EN_ROUTE"));

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse markArrived(UUID id, UUID driverId) {
        log.info("Driver {} marking trip {} as ARRIVED", driverId, id);
        Trip trip = findAndValidateOwnership(id, driverId);

        if (!trip.getMotariId().equals(driverId)) {
            throw new ApiException("Access Denied: Only the assigned driver can perform this action.", HttpStatus.FORBIDDEN);
        }

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "MOTARI_ARRIVED");

        trip.setStatus("MOTARI_ARRIVED");
        trip.setLastStatusChangeAt(Instant.now());
        trip = tripRepository.save(trip);

        eventPublisher.publishEvent(new MotariArrivedEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "MOTARI_ARRIVED"));

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse markBoarded(UUID id, UUID passengerId) {
        log.info("Passenger {} confirming boarding for trip {}", passengerId, id);
        Trip trip = findAndValidateOwnership(id, passengerId);

        if (!trip.getPassengerId().equals(passengerId)) {
            throw new ApiException("Access Denied: Only the passenger can confirm boarding.", HttpStatus.FORBIDDEN);
        }

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "PASSENGER_ONBOARD");

        trip.setStatus("PASSENGER_ONBOARD");
        trip.setLastStatusChangeAt(Instant.now());
        trip = tripRepository.save(trip);

        eventPublisher.publishEvent(new PassengerBoardedEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "PASSENGER_ONBOARD"));

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse startTrip(UUID id, UUID driverId) {
        log.info("Driver {} starting trip {}", driverId, id);
        Trip trip = findAndValidateOwnership(id, driverId);

        if (!trip.getMotariId().equals(driverId)) {
            throw new ApiException("Access Denied: Only the assigned driver can start the trip.", HttpStatus.FORBIDDEN);
        }

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "IN_PROGRESS");

        Instant now = Instant.now();
        trip.setStatus("IN_PROGRESS");
        trip.setStartedAt(now);
        trip.setLastStatusChangeAt(now);
        trip = tripRepository.save(trip);

        eventPublisher.publishEvent(new TripStartedEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "IN_PROGRESS"));

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse completeTrip(UUID id, UUID driverId) {
        log.info("Driver {} completing trip {}", driverId, id);
        Trip trip = findAndValidateOwnership(id, driverId);

        if (!trip.getMotariId().equals(driverId)) {
            throw new ApiException("Access Denied: Only the assigned driver can complete the trip.", HttpStatus.FORBIDDEN);
        }

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "COMPLETED");

        Instant now = Instant.now();
        trip.setStatus("COMPLETED");
        trip.setCompletedAt(now);
        trip.setLastStatusChangeAt(now);
        trip = tripRepository.save(trip);

        // Reset Motari availability status to ONLINE
        locationService.updateAvailabilityStatus(trip.getMotariId(), "ONLINE");

        eventPublisher.publishEvent(new TripCompletedEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "COMPLETED"));

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse cancelTrip(UUID id, UUID callerId, String reason) {
        log.info("User {} cancelling trip {} with reason: {}", callerId, id, reason);
        Trip trip = findAndValidateOwnership(id, callerId);

        String oldStatus = trip.getStatus();
        validateTransition(oldStatus, "CANCELLED");

        Instant now = Instant.now();
        trip.setStatus("CANCELLED");
        trip.setCancellationReason(reason);
        trip.setCancelledBy(callerId);
        trip.setCancelledAt(now);
        trip.setLastStatusChangeAt(now);
        trip = tripRepository.save(trip);

        // Reset Motari availability status to ONLINE
        locationService.updateAvailabilityStatus(trip.getMotariId(), "ONLINE");

        eventPublisher.publishEvent(new TripCancelledEvent(this, trip));
        eventPublisher.publishEvent(new TripStatusChangedEvent(this, trip, oldStatus, "CANCELLED"));

        return tripMapper.toResponse(trip);
    }

    private Trip findAndValidateOwnership(UUID id, UUID callerId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new ApiException("Trip not found.", HttpStatus.NOT_FOUND));

        if (!trip.getPassengerId().equals(callerId) && !trip.getMotariId().equals(callerId)) {
            throw new ApiException("Access Denied: You are not assigned to this trip.", HttpStatus.FORBIDDEN);
        }

        return trip;
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        if ("CANCELLED".equals(currentStatus) || "COMPLETED".equals(currentStatus)) {
            throw new ApiException("Cannot transition trip state from a terminal state: " + currentStatus, HttpStatus.BAD_REQUEST);
        }

        switch (targetStatus) {
            case "MOTARI_EN_ROUTE":
                if (!"CREATED".equals(currentStatus)) {
                    throw new ApiException("Invalid transition from " + currentStatus + " to MOTARI_EN_ROUTE", HttpStatus.BAD_REQUEST);
                }
                break;
            case "MOTARI_ARRIVED":
                if (!"MOTARI_EN_ROUTE".equals(currentStatus)) {
                    throw new ApiException("Invalid transition from " + currentStatus + " to MOTARI_ARRIVED", HttpStatus.BAD_REQUEST);
                }
                break;
            case "PASSENGER_ONBOARD":
                if (!"MOTARI_ARRIVED".equals(currentStatus)) {
                    throw new ApiException("Invalid transition from " + currentStatus + " to PASSENGER_ONBOARD", HttpStatus.BAD_REQUEST);
                }
                break;
            case "IN_PROGRESS":
                if (!"PASSENGER_ONBOARD".equals(currentStatus)) {
                    throw new ApiException("Invalid transition from " + currentStatus + " to IN_PROGRESS", HttpStatus.BAD_REQUEST);
                }
                break;
            case "COMPLETED":
                if (!"IN_PROGRESS".equals(currentStatus)) {
                    throw new ApiException("Invalid transition from " + currentStatus + " to COMPLETED", HttpStatus.BAD_REQUEST);
                }
                break;
            case "CANCELLED":
                // Cancellation allowed from CREATED, MOTARI_EN_ROUTE, or MOTARI_ARRIVED
                if (!"CREATED".equals(currentStatus) && !"MOTARI_EN_ROUTE".equals(currentStatus) && !"MOTARI_ARRIVED".equals(currentStatus)) {
                    throw new ApiException("Cancellation not allowed in state: " + currentStatus, HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                throw new ApiException("Unknown trip status: " + targetStatus, HttpStatus.BAD_REQUEST);
        }
    }
}
