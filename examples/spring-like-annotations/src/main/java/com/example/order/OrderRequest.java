package com.example.order;

/**
 * Order input used by the propagation example.
 *
 * @param id caller-supplied order identifier
 */
public record OrderRequest(String id) {
}
