package org.example.order.order.application.service.order;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.example.customer.Customer;
import org.example.order.order.application.model.order.es.OrderEsModel;
import org.example.order.order.application.service.customer.CustomerService;
import org.example.order.order.application.utils.JsonUtils;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.infrastructure.data.dao.FulfillmentDao;
import org.example.order.order.infrastructure.data.dto.FulfillmentDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderEsWriteService {

    private final FulfillmentDao fulfillmentDao;

    private final CustomerService customerService;

    private final OrderMapper orderMapper;

    private final ElasticsearchClient elasticsearchClient;

    private void indexOrders(List<OrderLog> orderLogs) throws ExecutionException, InterruptedException, IOException {
        if (CollectionUtils.isEmpty(orderLogs)) return;

        var finalLogs = this.getLastOrderLogs(orderLogs);
        var partitionedLogs = finalLogs.stream()
                .collect(Collectors.partitioningBy(log -> log.getEventType() == AppEventType.delete));

        var deleteOrderLogs = partitionedLogs.get(true);
        var addOrUpdateOrderLogs = partitionedLogs.get(false);

        var addOrUpdateEsOrders = mapToEsModels(addOrUpdateOrderLogs);

        var bulkRequest = new BulkRequest.Builder();
        if (CollectionUtils.isNotEmpty(addOrUpdateEsOrders)) {
            for (var esOrder : addOrUpdateEsOrders) {
                processEvent(AppEventType.add, esOrder, bulkRequest);
            }
        }

        if (CollectionUtils.isNotEmpty(deleteOrderLogs)) {
            deleteOrderLogs.forEach(log ->
                    this.processEvent(
                            AppEventType.delete,
                            new OrderEsModel(log.getOrderId(), log.getStoreId()),
                            bulkRequest)
            );
        }

        elasticsearchClient.bulk(bulkRequest.build());
    }

    private void processEvent(AppEventType verb, OrderEsModel esOrder, BulkRequest.Builder bulkRequest) {
        var modelId = String.valueOf(esOrder.getOrderId());
        var routingKey = String.valueOf(esOrder.getStoreId());
        if (verb == AppEventType.delete) {
            bulkRequest.operations(op -> op
                    .delete(d -> d
                            .index("orders")
                            .id(modelId)
                            .routing(routingKey))
            );
        } else {
            bulkRequest.operations(op -> op
                    .index(i -> i
                            .index("orders")
                            .id(modelId)
                            .routing(routingKey)
                            .document(esOrder))
            );
        }
    }

    private List<OrderEsModel> mapToEsModels(List<OrderLog> orderLogs) throws ExecutionException, InterruptedException {
        if (CollectionUtils.isEmpty(orderLogs)) return List.of();

        var orders = orderLogs.stream().map(log -> {
            try {
                return JsonUtils.unmarshal(log.getData(), Order.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        var orderIds = orders.stream().map(order -> order.getId().getId()).toList();
        var storeIds = orders.stream().map(order -> order.getId().getStoreId()).distinct().toList();
        var customerIds = orders.stream()
                .filter(order -> order.getCustomerInfo() != null && NumberUtils.isPositive(order.getCustomerInfo().getCustomerId()))
                .collect(Collectors.groupingBy(order -> order.getId().getStoreId(),
                        Collectors.mapping(order -> order.getCustomerInfo().getCustomerId(), Collectors.toList())));

        // get fulfillment, shipment
        var asyncData = List.of(
                this.fulfillmentDao.getFulfillmentByStoreIdsAndOrderIdsAsync(storeIds, orderIds)
        );
        var data = CompletableFuture.allOf(asyncData.toArray(new CompletableFuture[0]))
                .thenApply(v -> asyncData.stream().map(CompletableFuture::join).toList()).get();

        var fulfillments = (List<FulfillmentDto>) data.get(0);

        var customers = this.getCustomers(customerIds);

        return orders.stream()
                .map(order -> {
                    var orderFulfillments = fulfillments.stream().filter(ff -> ff.getOrderId() == order.getId().getId()).toList();
                    var customerId = Optional.ofNullable(order.getCustomerInfo()).map(CustomerInfo::getCustomerId).orElse(null);
                    var customer = customerId == null ? null : customers.stream().filter(c -> c.getId() == customerId).findFirst().orElse(null);
                    return this.orderMapper.toEsModel(order, customer, orderFulfillments);
                })
                .toList();
    }

    private List<Customer> getCustomers(Map<Integer, List<Integer>> customerIdMap) {
        if (customerIdMap.isEmpty()) return List.of();

        List<Customer> customers = new ArrayList<>();
        customerIdMap.forEach((storeId, customerIds) -> {
            var fetchedCustomers = this.customerService.findByIds(storeId, customerIds);
            if (CollectionUtils.isNotEmpty(fetchedCustomers)) {
                customers.addAll(fetchedCustomers);
            }
        });

        return customers;
    }

    private List<OrderLog> getLastOrderLogs(List<OrderLog> orderLogs) {
        var orderLogMap = orderLogs.stream()
                .collect(Collectors.groupingBy(
                        OrderLog::getOrderId,
                        Collectors.maxBy(Comparator.comparingInt(OrderLog::getId))
                ));
        return orderLogMap.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public void updateShipmentStatus(List<FulfillmentLog> fulfillmentLogs) {
        if (CollectionUtils.isEmpty(fulfillmentLogs)) return;

        Set<Integer> storeIds = new HashSet<>();
        Set<Integer> orderIds = new HashSet<>();
        Map<Integer, Integer> orderStoreMap = new HashMap<>();
        for (var fulfillmentLog : fulfillmentLogs) {
            if (!"update".equals(fulfillmentLog.getVerb())) continue;

            if (fulfillmentLog.getData() == null) continue;

            String shipmentChangeEventText = "\"event_name\":\"change_shipment_status\"";
            boolean hasShipmentEvent = fulfillmentLog.getData().contains(shipmentChangeEventText);
            if (!hasShipmentEvent) continue;

            storeIds.add(fulfillmentLog.getStoreId());
            orderIds.add(fulfillmentLog.getOrderId());
            orderStoreMap.put(fulfillmentLog.getOrderId(), fulfillmentLog.getStoreId());
        }
        if (storeIds.isEmpty() || orderIds.isEmpty()) return;

        var fulfillments = fulfillmentDao.getFulfillmentByStoreIdsAndOrderIds(storeIds, orderIds);
        if (fulfillments.isEmpty()) return;

        var bulkRequest = new BulkRequest.Builder();

        var groupedFulfillments = fulfillments.stream().collect(Collectors.groupingBy(FulfillmentDto::getOrderId));
        // chỉ update lại những fulfillment có status là success
        for (var entrySet : groupedFulfillments.entrySet()) {
            var fulfillmentIndexModels = new ArrayList<OrderEsModel.FulfillmentEsModel>();

            for (var fulfillment : entrySet.getValue()) {
                if (fulfillment.getStatus() != Fulfillment.FulfillmentStatus.success) continue;
                var fulfillmentIndexModel = this.orderMapper.toFulfillmentEsModel(fulfillment);
                fulfillmentIndexModels.add(fulfillmentIndexModel);
            }
            if (fulfillmentIndexModels.isEmpty()) continue;

            var uid = String.valueOf(entrySet.getKey());
            var routingKey = String.valueOf(orderStoreMap.get(entrySet.getKey()));
//            bulkRequest.operations(op ->
//                    op.update(uo ->
//                            uo.index("orders")
//                                    .id(uid)
//                                    .routing(routingKey)
//                                    .build())
//            );
        }
    }

    public record FulfillmentEsWrapper(List<OrderEsModel.FulfillmentEsModel> fulfillments) {
    }

    @Getter
    @Setter
    public static class FulfillmentLog {
        private int id;
        private int storeId;
        private int orderId;
        private int fulfillmentId;

        private String verb;

        private String data;

        private Instant createdOn;

        private String actor;
    }
}
