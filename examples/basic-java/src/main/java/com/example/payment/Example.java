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
    @AIImplemented("Say hello 10 times an put the counter sum in the status")
    PaymentResult m1();
}
