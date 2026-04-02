package com.example.order;

import com.aimp.annotations.AIImplemented;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example abstract contract that demonstrates annotation propagation.
 */
@Service
public abstract class OrderServiceBase {
    /**
     * Creates the base service with the configured region.
     *
     * @param region deployment region for the service instance
     */
    protected OrderServiceBase(String region) {
    }

    /**
     * Places an order request.
     *
     * @param request order request to place
     * @return order placement result
     */
    @AIImplemented("""
        Implement this method without calling external systems.
        Use request.id() as the order identifier.
        Return exactly new com.example.order.OrderResult("reserved:" + request.id()).
        Do not introduce any extra dependencies or infrastructure.
        """)
    @Transactional
    public abstract OrderResult place(@Valid OrderRequest request);
}
