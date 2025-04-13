package org.example.order.order.application.model.draftorder.request;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Getter
@JsonRootName("draft_order")
@Jacksonized
@SuperBuilder(toBuilder = true)
@Setter
public class DraftOrderCreateRequest extends DraftOrderRequest {

    @Builder.Default
    private @Size(max = 100) @NotEmpty List<DraftLineItemRequest> lineItems = new ArrayList<>();
}
