package com.example.order;

/**
 * Order result returned by the propagation example.
 *
 * @param status order placement outcome
 */
public record OrderResult(String status) {
}
