package com.voum.modules.review.events;

import com.voum.modules.review.entity.TripReview;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MotariReviewedPassengerEvent extends ApplicationEvent {

    private final TripReview review;

    public MotariReviewedPassengerEvent(Object source, TripReview review) {
        super(source);
        this.review = review;
    }
}
