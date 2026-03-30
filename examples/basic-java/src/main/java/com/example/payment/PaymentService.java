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
    @AIImplemented("Charge a payment and return the result")
    PaymentResult charge(PaymentRequest request);
}
