package com.concert.concertreservationapi.exception;

public class ReservationForbiddenException extends RuntimeException {
    public ReservationForbiddenException(String message) {
        super(message);
    }
}
