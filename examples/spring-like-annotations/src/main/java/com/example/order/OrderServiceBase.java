package com.example.order;

import com.aimp.annotations.AIImplemented;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example abstract contract that demonstrates annotation propagation.
 */
@AIImplemented
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
     * Places an order request without calling external systems.
     * The generated implementation should use {@code request.id()} as the order
     * identifier and return
     * {@code new OrderResult("reserved:" + request.id())}.
     *
     * @param request order request to place
     * @return order placement result
     */
    @Transactional
    public abstract OrderResult place(@Valid OrderRequest request);
}
