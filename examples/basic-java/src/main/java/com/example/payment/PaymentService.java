package com.example.payment;

import com.aimp.annotations.AIImplemented;

/**
 * Example contract implemented by AIMP during compilation.
 */
@AIImplemented
public interface PaymentService {
    /**
     * Charges a payment request without using external systems.
     * Returns {@code new PaymentResult("rejected:missing-reference")} when
     * {@code request.reference()} is blank.
     * Otherwise returns {@code new PaymentResult("approved:" + request.reference())}.
     *
     * @param request payment request to process
     * @return payment result produced by the generated implementation
     */
    PaymentResult charge(PaymentRequest request);
}
