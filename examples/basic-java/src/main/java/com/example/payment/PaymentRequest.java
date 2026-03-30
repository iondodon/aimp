package com.example.payment;

/**
 * Payment input used by the example contract.
 *
 * @param reference caller-supplied payment reference
 */
public record PaymentRequest(String reference) {
}
