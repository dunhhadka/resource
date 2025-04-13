package org.example.order.order.application.model.draftorder.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.example.order.order.application.converter.TagConverter;

import java.util.List;

@Getter
@Jacksonized
@SuperBuilder(toBuilder = true)
public class DraftOrderRequest {
    private String currency;

    private @Size(max = 2000) String note;
    private List<@Size(max = 250) String> tags;
    private @Size(max = 128) String email;
    private @Size(max = 50) String phone;

    private Integer locationId;

    private String sourceName;

    private Integer assigneeId;
    private Integer copyOrderId;

    private Boolean taxExempt;

    private DraftOrderAddressRequest billingAddress;
    private DraftOrderAddressRequest shippingAddress;

    private DraftAppliedDiscountRequest appliedDiscount;

    private DraftShippingLineRequest shippingLine;

    private List<@Valid DraftPropertyRequest> noteAttributes;

    private Integer customerId;

    private Integer userId;

    public abstract static class DraftOrderRequestBuilder<C extends DraftOrderRequest, B extends DraftOrderRequestBuilder<C, B>> {
        @JsonSetter
        public <T> B tags(T tagInput) {
            this.tags = TagConverter.convertFromRequest(tagInput);
            return self();
        }
    }
}
