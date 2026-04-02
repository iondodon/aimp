# aimp

AIMP is a Java annotation processor that turns handwritten interfaces and abstract classes annotated with `@AIImplemented` into generated concrete implementations during compilation.

It does not rewrite handwritten source files. It generates `*_AIGenerated` classes in annotation-processor output, in the same package as the contract.

## Example

Handwritten contract:

```java
package com.example.payment;

import com.aimp.annotations.AIImplemented;

public interface PaymentService {
    @AIImplemented("Charge a payment and return the result")
    PaymentResult charge(PaymentRequest request);
}
```

Generated shape:

```java
package com.example.payment;

public class PaymentService_AIGenerated implements PaymentService {
    @Override
    public PaymentResult charge(PaymentRequest request) {
        return new PaymentResult("approved");
    }
}
```
