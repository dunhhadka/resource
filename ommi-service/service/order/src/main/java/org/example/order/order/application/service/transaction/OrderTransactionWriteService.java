package org.example.order.order.application.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.AdminClient;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.application.service.order.OrderCreatedAppEvent;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.PaymentMethodInfo;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.domain.transaction.model.PaymentInfo;
import org.example.order.order.domain.transaction.persistence.TransactionRepository;
import org.example.paymentmethod.PaymentMethod;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionWriteService {

    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;

    private final AdminClient adminClient;

    @EventListener(OrderCreatedAppEvent.class)
    public void handleOrderTransactionFromCheckout(OrderCreatedAppEvent event) {
        log.debug("handle order transaction from checkout: {}", event);

        var paymentResult = event.getPaymentResult();
        var isFromCheckout = paymentResult.isFromCheckout();
        var paymentIds = paymentResult.getPaymentIds();

        if (!isFromCheckout || CollectionUtils.isEmpty(paymentIds)) {
            return;
        }

        var storeId = event.getStoreId();
        var orderId = event.getOrderId();

        var order = this.orderRepository.findById(orderId);

        var transactions = this.transactionRepository.findByPaymentIds(storeId, paymentIds);

        transactions.forEach(transaction -> {
            transaction.resolveOrder(order);
            transactionRepository.save(transaction);
        });
    }


    @EventListener(OrderCreatedAppEvent.class)
    private void handleOrderTransactionAdded(OrderCreatedAppEvent event) {
        log.debug("resolve order transaction from checkout: {}", event);

        var paymentResult = event.getPaymentResult();
        var isFromCheckout = paymentResult.isFromCheckout();
        var paymentIds = paymentResult.getPaymentIds();

        if (isFromCheckout || CollectionUtils.isNotEmpty(paymentIds)) {
            return;
        }

        var storeId = event.getStoreId();
        var orderId = event.getOrderId();
        var transactionRequests = event.getTransactions();

        if (CollectionUtils.isEmpty(transactionRequests)) {
            return;
        }

        var order = this.orderRepository.findById(orderId);

        var orderGateway = getOrderGateway(order);

        var paymentMethodIds = transactionRequests.stream()
                .map(OrderTransactionCreateRequest::getPaymentInfo)
                .filter(Objects::nonNull)
                .map(PaymentInfo::getPaymentMethodId)
                .toList();

        var requestedGateways = transactionRequests.stream()
                .map(OrderTransactionCreateRequest::getGateway)
                .filter(StringUtils::isNotBlank)
                .toList();

        var paymentMethodMap = verifyPaymentMethods(storeId, paymentMethodIds, requestedGateways);
    }

    private Map<Integer, PaymentMethod> verifyPaymentMethods(int storeId, List<Integer> paymentMethodIds, List<String> requestedGateways) {
        List<PaymentMethod> paymentMethods = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(paymentMethodIds)) {

        }

        return Map.of();
    }

    private String getOrderGateway(Order order) {
        return Optional.ofNullable(order.getPaymentMethodInfo())
                .map(PaymentMethodInfo::getGateway)
                .flatMap(o -> Arrays.stream(o.split(",")).findFirst())
                .orElse(null);
    }
}
