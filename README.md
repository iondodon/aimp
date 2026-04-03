# aimp

AIMP is a Java annotation processor that turns handwritten interfaces and abstract classes annotated with `@AIImplemented` into generated concrete implementations during compilation.

It does not rewrite handwritten source files. It generates `*_AIGenerated` classes in annotation-processor output, in the same package as the contract.

## Example

Handwritten contract:

```java
package com.example.workflow;

import com.aimp.annotations.AIImplemented;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AIImplemented
@Service
public abstract class OrderWorkflowBase {
    protected final String region;

    protected OrderWorkflowBase(String region) {
        this.region = region;
    }

    /**
     * Orders from priority customers should move faster than everyone else.
     * Very large orders should be held for review before they are confirmed.
     * Everything else can be reserved right away for the current region.
     */
    @Transactional
    public abstract OrderResult place(@Valid OrderRequest request);

    /**
     * Returns from recent purchases can be approved right away.
     * Older returns should be marked for review.
     */
    @Transactional
    public abstract OrderResult reviewReturn(@Valid ReturnRequest request);

    protected boolean isPriorityCustomer(String customerTier) {
        return "gold".equalsIgnoreCase(customerTier)
            || "platinum".equalsIgnoreCase(customerTier);
    }
}
```

Generated shape:

```java
package com.example.workflow;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderWorkflowBase_AIGenerated extends OrderWorkflowBase {
    public OrderWorkflowBase_AIGenerated(String region) {
        super(region);
    }

    @Override
    @Transactional
    public OrderResult place(@Valid OrderRequest request) {
        boolean priority = isPriorityCustomer(request.customerTier());
        boolean largeOrder = request.totalAmount().compareTo(new BigDecimal("1000")) >= 0;

        String status;
        if (priority && largeOrder) {
            status = "priority-review";
        } else if (priority) {
            status = "priority-reserved";
        } else if (largeOrder) {
            status = "manual-review";
        } else if ("online".equalsIgnoreCase(request.salesChannel())) {
            status = "reserved-online";
        } else {
            status = "reserved-store";
        }

        return new OrderResult(status + ":" + request.id() + ":" + this.region);
    }

    @Override
    @Transactional
    public OrderResult reviewReturn(@Valid ReturnRequest request) {
        String status = request.daysSincePurchase() <= 30
            ? "return-approved"
            : "return-review";
        return new OrderResult(status + ":" + request.orderId() + ":" + this.region);
    }
}
```
