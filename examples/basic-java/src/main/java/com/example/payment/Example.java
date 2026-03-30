package com.example.payment;

import com.aimp.annotations.AIImplemented;

/**
 * Small example contract used to demonstrate full-class AIMP generation.
 */
public interface Example {
    /**
     * Produces a payment result for the example scenario.
     *
     * @return synthesized payment result
     */
    @AIImplemented("""
        Build a status string without using external systems.
        Repeat the word "hello" exactly 10 times, separated by a single space.
        Compute the sum of the integers from 1 through 10, which is 55.
        Return exactly new com.example.payment.PaymentResult(greetingText + " | sum=55").
        """)
    PaymentResult m1();
}
