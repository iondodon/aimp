package com.example.payment;

/**
 * Example access point for the generated payment service implementation.
 */
public final class App {
    private App() {
    }

    /**
     * Creates the generated payment service.
     *
     * @return generated payment service implementation
     */
    public static PaymentService service() {
        return new PaymentService_AIGenerated();
    }
}
