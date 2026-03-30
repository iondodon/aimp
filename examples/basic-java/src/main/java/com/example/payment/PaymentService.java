package com.example.payment;

import com.aimp.annotations.AIImplemented;

/**
 * Example contract implemented by AIMP during compilation.
 */
public interface PaymentService {
    /**
     * Charges a payment request.
     *
     * @param request payment request to process
     * @return payment result produced by the generated implementation
     */
    @AIImplemented("""
        Implement this method without calling external systems.
        Use request.reference() exactly as provided.
        If request.reference() is blank, return exactly new com.example.payment.PaymentResult("rejected:missing-reference").
        Otherwise return exactly new com.example.payment.PaymentResult("approved:" + request.reference()).
        Do not add randomness, networking, databases, or extra dependencies.
        """)
    PaymentResult charge(PaymentRequest request);
}
