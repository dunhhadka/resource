package org.example.order.order.domain.fulfillmentorder.persistence;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FulfillmentOrderLineItemRepository extends JpaRepository<FulfillmentOrderLineItem, Integer> {
    @Query(value =
            "SELECT ffoLine FROM fulfillment_order_line_items ffoLine " +
                    "WHERE ffoLine.store_id = :storeId AND ffoLine.fulfillment_order_id IN (:fulfillmentOrderIds)"
            , nativeQuery = true)
    List<FulfillmentOrderLineItem> findByFulfillmentOrderIdIn(int storeId, List<Integer> fulfillmentOrderIds);
}
