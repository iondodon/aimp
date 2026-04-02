package com.example.payment;

import com.aimp.annotations.AIImplemented;

/**
 * Small example contract used to demonstrate full-class AIMP generation.
 */
@AIImplemented
public interface Example {
    /**
     * Produces a payment result for the example scenario.
     * The generated implementation should repeat the word {@code hello} exactly
     * ten times separated by a single space, compute the sum of the integers
     * from 1 through 10, and return
     * {@code new PaymentResult(greetingText + " | sum=55")}.
     *
     * @return synthesized payment result
     */
    PaymentResult m1();
}
