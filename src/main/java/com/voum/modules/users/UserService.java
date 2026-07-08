package com.voum.modules.users;

import com.voum.common.ApiException;
import com.voum.modules.users.dto.UserProfileResponse;
import com.voum.modules.users.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final MotariRepository motariRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole().name())
                .rating(user.getRating())
                .completedTrips(user.getCompletedTrips())
                .isOnline(user.getIsOnline())
                .isVerified(user.getIsVerified())
                .subscriptionPlan(user.getSubscriptionPlan())
                .createdAt(user.getCreatedAt());

        if (user.getRole() == Role.PASSENGER) {
            passengerRepository.findById(userId).ifPresent(p -> 
                builder.passenger(UserProfileResponse.PassengerDetails.builder()
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .profileImage(p.getProfileImage())
                        .status(p.getStatus())
                        .build())
            );
        } else if (user.getRole() == Role.MOTARI) {
            motariRepository.findById(userId).ifPresent(m -> 
                builder.motari(UserProfileResponse.MotariDetails.builder()
                        .firstName(m.getFirstName())
                        .lastName(m.getLastName())
                        .nationalId(m.getNationalId())
                        .motoPlateNumber(m.getMotoPlateNumber())
                        .profileImage(m.getProfileImage())
                        .verificationStatus(m.getVerificationStatus())
                        .status(m.getStatus())
                        .build())
            );
        }

        return builder.build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (req.getEmail() != null && !req.getEmail().trim().isEmpty() && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new ApiException("Email is already registered.", HttpStatus.CONFLICT);
            }
            user.setEmail(req.getEmail());
        }

        if (user.getRole() == Role.PASSENGER) {
            Passenger passenger = passengerRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("Passenger profile not found.", HttpStatus.NOT_FOUND));

            if (req.getFirstName() != null) passenger.setFirstName(req.getFirstName());
            if (req.getLastName() != null) passenger.setLastName(req.getLastName());
            if (req.getProfileImage() != null) passenger.setProfileImage(req.getProfileImage());

            user.setName(passenger.getFirstName() + " " + passenger.getLastName());
            passengerRepository.save(passenger);

        } else if (user.getRole() == Role.MOTARI) {
            Motari motari = motariRepository.findById(userId)
                    .orElseThrow(() -> new ApiException("Motari profile not found.", HttpStatus.NOT_FOUND));

            if (req.getFirstName() != null) motari.setFirstName(req.getFirstName());
            if (req.getLastName() != null) motari.setLastName(req.getLastName());
            if (req.getProfileImage() != null) motari.setProfileImage(req.getProfileImage());
            
            if (req.getMotoPlateNumber() != null && !req.getMotoPlateNumber().equals(motari.getMotoPlateNumber())) {
                if (motariRepository.existsByMotoPlateNumber(req.getMotoPlateNumber())) {
                    throw new ApiException("Plate number already registered.", HttpStatus.CONFLICT);
                }
                motari.setMotoPlateNumber(req.getMotoPlateNumber());
            }

            user.setName(motari.getFirstName() + " " + motari.getLastName());
            motariRepository.save(motari);
        }

        userRepository.save(user);
        return getProfile(userId);
    }

    @Transactional
    public void saveDeviceToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public void updateLanguage(UUID userId, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));
        user.setPreferredLanguage(language);
        userRepository.save(user);
    }
}
