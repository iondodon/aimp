package com.example.order;

/**
 * Example access point for the generated order service implementation.
 */
public final class App {
    private App() {
    }

    /**
     * Creates the generated order service.
     *
     * @return generated order service implementation
     */
    public static OrderServiceBase service() {
        return new OrderServiceBase_AIGenerated("eu-west");
    }
}
