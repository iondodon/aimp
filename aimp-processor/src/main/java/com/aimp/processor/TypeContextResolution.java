package com.aimp.processor;

import com.aimp.model.ReferencedTypeModel;
import java.util.List;

record TypeContextResolution(
    List<ReferencedTypeModel> fulfilledTypes,
    List<RejectedContextTypeRequest> rejectedTypeRequests
) {
    TypeContextResolution {
        fulfilledTypes = List.copyOf(fulfilledTypes);
        rejectedTypeRequests = List.copyOf(rejectedTypeRequests);
    }
}
