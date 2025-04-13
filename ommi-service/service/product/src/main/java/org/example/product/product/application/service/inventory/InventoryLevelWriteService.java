package org.example.product.product.application.service.inventory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.product.product.application.model.inventory.request.*;
import org.example.product.product.domain.inventory.model.InventoryItem;
import org.example.product.product.domain.inventory.model.InventoryLevel;
import org.example.product.product.domain.inventory.repository.InventoryItemRepository;
import org.example.product.product.domain.inventory.repository.InventoryLevelRepository;
import org.example.product.product.domain.inventory.repository.InventoryTrackingRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLevelWriteService {

    private static final String inventoryAdjustmentPreProcessTopic = "";

    private final InventoryTrackingRepository inventoryTrackingRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final TransactionTemplate transactionTemplate;

    private final MessageSource messageSource;


    public void commitInventory(int storeId, InventoryRequest request) {
        if (this.inventoryTrackingRepository.existsByStoreIdAndIdempotencyKey(storeId, request.getIdempotencyKey())) {
            return;
        }

        var inventoryItemIds = request.getAdjustments().stream()
                .flatMap(i -> i.getLineItems().stream())
                .map(InventoryTransactionLineItemRequest::getInventoryItemId)
                .filter(id -> id > 0)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(inventoryItemIds)) {
            throw new IllegalArgumentException();
        }

        var inventoryLevels = this.inventoryLevelRepository.getByStoreIdAndInventoryItemIdIn(storeId, inventoryItemIds);

        removeInventoryLevelsIfInventoryItemIdNotExist(request, inventoryItemIds, inventoryLevels);
        if (CollectionUtils.isEmpty(request.getAdjustments())) {
            return;
        }

        var errorTracerBuilder = ErrorTracer.builder();

        validateInventoryRequest(request, errorTracerBuilder);

        checkExistsInventoryLevelInLocation(request, inventoryLevels, errorTracerBuilder);

        if (!errorTracerBuilder.isEmpty()) {
            throw new IllegalArgumentException();
        }

        var inventoryAdjustmentRequest = new InventoryAdjustmentRequest();
        inventoryAdjustmentRequest.setIdempotencyKey(request.getIdempotencyKey());

        var transactions = new ArrayList<InventoryAdjustmentTransactionRequest>();

        for (var adjustment : request.getAdjustments()) {
            for (var lineItem : adjustment.getLineItems()) {
                var issuedAt = adjustment.getIssuedAt() != null ? adjustment.getIssuedAt() : Instant.now();
                var transactionBuilder = InventoryAdjustmentTransactionRequest.builder()
                        .locationId(adjustment.getLocationId())
                        .inventoryItemId(lineItem.getInventoryItemId());

                List<InventoryAdjustmentTransactionChangeRequest> changes = lineItem.getChanges().stream()
                        .map(inventoryChange ->
                                InventoryAdjustmentTransactionChangeRequest.builder()
                                        .value(inventoryChange.getValue())
                                        .changeType(inventoryChange.getChangeType())
                                        .valueType(inventoryChange.getValueType())
                                        .build())
                        .toList();
                transactionBuilder
                        .changes(changes);

                transactionBuilder
                        .reason(adjustment.getReason())
                        .issuedAt(issuedAt);

                transactions.add(transactionBuilder.build());
            }
        }

        inventoryAdjustmentRequest.setTransactions(transactions);

        adjustForTransactions(storeId, inventoryAdjustmentRequest);
    }

    private void adjustForTransactions(int storeId, InventoryAdjustmentRequest adjustmentRequest) {
        if (CollectionUtils.isEmpty(adjustmentRequest.getTransactions())
                && this.inventoryTrackingRepository.existsByStoreIdAndIdempotencyKey(storeId, adjustmentRequest.getIdempotencyKey())) {
            return;
        }
        var locationIds = adjustmentRequest.getTransactions().stream()
                .map(InventoryAdjustmentTransactionRequest::getLocationId)
                .filter(locationId -> locationId > 0)
                .distinct().toList();
        var inventoryItemIds = adjustmentRequest.getTransactions().stream()
                .map(InventoryAdjustmentTransactionRequest::getInventoryItemId)
                .filter(id -> id > 0)
                .distinct().toList();
        var inventoryLevelFetched = this.inventoryLevelRepository.getByStoreIdAndInventoryItemIdIn(storeId, inventoryItemIds);
        if (CollectionUtils.isEmpty(inventoryLevelFetched)) {
            return;
        }

        var inventoryItems = this.inventoryItemRepository.getByStoreIdAndIdIn(storeId, inventoryItemIds)
                .stream().collect(Collectors.toMap(InventoryItem::getId, Function.identity()));

        var inventoryOutboxKafkaMessages = new ArrayList<InventoryOutboxMessage>();
        List<InventoryLevel> inventoryLevels = new ArrayList<>();
        for (var transactionRequest : adjustmentRequest.getTransactions()) {
            var inventoryItemOptional = inventoryItems.get(transactionRequest.getInventoryItemId());
            if (inventoryItemOptional == null || !inventoryItemOptional.isTracked()) {
                continue;
            }
            enrichInventoryLevelAndInventoryOutboxKafkaMessage(
                    storeId,
                    adjustmentRequest.getIdempotencyKey(),
                    inventoryLevelFetched,
                    inventoryLevels,
                    inventoryOutboxKafkaMessages,
                    transactionRequest
            );
        }

        store(inventoryLevels, adjustmentRequest.getIdempotencyKey(), inventoryOutboxKafkaMessages);
    }

    private void store(
            List<InventoryLevel> inventoryLevels,
            String idempotencyKey,
            ArrayList<InventoryOutboxMessage> inventoryOutboxKafkaMessages
    ) {
        this.transactionTemplate.executeWithoutResult((status) -> {
            if (!inventoryLevels.isEmpty()) {

            }
        });
    }

    private void enrichInventoryLevelAndInventoryOutboxKafkaMessage(
            int storeId,
            String idempotencyKey,
            List<InventoryLevel> inventoryLevelFetched,
            List<InventoryLevel> inventoryLevels,
            ArrayList<InventoryOutboxMessage> inventoryOutboxKafkaMessages,
            InventoryAdjustmentTransactionRequest transactionRequest
    ) {
        var inventoryLevelOptional = inventoryLevelFetched.stream()
                .filter(level -> level.getInventoryItemId() == transactionRequest.getInventoryItemId())
                .findFirst()
                .orElse(null);
        if (inventoryLevelOptional == null) {
            return;
        }

        boolean updateStock = false;
        boolean updateCostPrice = false;

        if (transactionRequest.getChanges().stream().anyMatch(change -> change.getValueType() == InventoryAdjustmentTransactionChangeRequest.ValueType.fix)
                && transactionRequest.getChanges().stream().anyMatch(change -> change.getChangeType() == InventoryAdjustmentChangeType.available)) {
            var availableChange = new InventoryAdjustmentTransactionChangeRequest();
            availableChange.setChangeType(InventoryAdjustmentChangeType.available);
            availableChange.setBeforeValue(inventoryLevelOptional.getAvailable());
            availableChange.setValueType(InventoryAdjustmentTransactionChangeRequest.ValueType.fix);
            // update available
            transactionRequest.getChanges().add(availableChange);
        }

        for (var change : transactionRequest.getChanges()) {
            if (change.getValueType() == InventoryAdjustmentTransactionChangeRequest.ValueType.delta) {
                change.setBeforeValue(inventoryLevelOptional.getOnHand());
                inventoryLevelOptional.setOnHand(inventoryLevelOptional.getOnHand().add(change.getValue()));
                updateStock = true;
            } else if (change.getChangeType() == InventoryAdjustmentChangeType.available) {
                change.setBeforeValue(inventoryLevelOptional.getAvailable());
                inventoryLevelOptional.setCommitted(inventoryLevelOptional.getCommitted().add(change.getValue()));
                updateStock = true;
            } else {

            }
        }

        if (updateStock) {
            inventoryLevels.add(inventoryLevelOptional);
        }

        if (updateStock || updateCostPrice) {
            var outboxKafkaMessage = new InventoryOutboxMessage();
            outboxKafkaMessage.setStoreId(storeId);
            outboxKafkaMessage.setTopicName(inventoryAdjustmentPreProcessTopic);
            outboxKafkaMessage.setMessageKey(String.valueOf(storeId));
//            outboxKafkaMessage.setMessageValue();
            inventoryOutboxKafkaMessages.add(outboxKafkaMessage);
        }
    }

    private void checkExistsInventoryLevelInLocation(InventoryRequest request, List<InventoryLevel> inventoryLevels, ErrorTracer.ErrorTracerBuilder errorTracerBuilder) {
        var inventoryLevelMap = inventoryLevels.stream()
                .collect(Collectors.groupingBy(InventoryLevel::getInventoryItemId));

        for (int i = 0; i < request.getAdjustments().size(); i++) {
            var adjustment = request.getAdjustments().get(i);

            for (int lineIndex = 0; lineIndex < adjustment.getLineItems().size(); i++) {
                var lineItem = adjustment.getLineItems().get(lineIndex);

                var inventoryLevelsOfItem = inventoryLevelMap.getOrDefault(lineItem.getInventoryItemId(), List.of());

                boolean isNotInLocation = inventoryLevelsOfItem.stream()
                        .anyMatch(level -> level.getLocationId() != adjustment.getLocationId());

                if (isNotInLocation) {
                    errorTracerBuilder.addError(UserError.builder()
                            .code("not_exists")
                            .message(messageSource.getMessage("adjustment.line_item.inventory_level.not_exists", null, LocaleContextHolder.getLocale()))
                            .fields(List.of("adjustments", String.valueOf(i), "line_items", String.valueOf(lineIndex), "inventory_item"))
                            .build());
                }
            }
        }
    }

    private void validateInventoryRequest(InventoryRequest request, ErrorTracer.ErrorTracerBuilder errorTracerBuilder) {
        for (int adjustmentIndex = 0; adjustmentIndex < request.getAdjustments().size(); adjustmentIndex++) {
            var adjustment = request.getAdjustments().get(adjustmentIndex);
            if (adjustment.getIssuedAt() != null && adjustment.getIssuedAt().isBefore(Instant.now().minus(1, ChronoUnit.DAYS))) {
                errorTracerBuilder.addError(UserError.builder()
                        .code("issued_at")
                        .message(messageSource.getMessage("inventory.adjustment.issued_at.exceed_on_day", null, LocaleContextHolder.getLocale()))
                        .fields(List.of("adjustments", String.valueOf(adjustmentIndex), "issued_at"))
                        .build());
            }
            if (adjustment.getIssuedAt() != null && adjustment.getIssuedAt().isAfter(Instant.now())) {
                errorTracerBuilder.addError(UserError.builder()
                        .code("issued_at")
                        .message(messageSource.getMessage("inventory.adjustment.issued_at.not_be_future", null, LocaleContextHolder.getLocale()))
                        .fields(List.of("adjustments", String.valueOf(adjustmentIndex), "issued_at"))
                        .build());
            }
            for (int index = 0; index < adjustment.getLineItems().size(); index++) {
                var lineItem = adjustment.getLineItems().get(index);
                if (lineItem.getChanges().stream().anyMatch(change -> change.getChangeType() == InventoryAdjustmentChangeType.reserved)) {
                    errorTracerBuilder.addError(UserError.builder()
                            .code("change_type")
                            .message(messageSource.getMessage("inventory.adjustment.change_type.reserved_un_support", null, LocaleContextHolder.getLocale()))
                            .fields(List.of("adjustments", String.valueOf(adjustmentIndex), "line_items", String.valueOf(index), "change_type"))
                            .build());
                }
                if (lineItem.getChanges().stream().anyMatch(change -> change.getValue().signum() == 0 && change.getValueType() == InventoryAdjustmentTransactionChangeRequest.ValueType.delta)) {
                    errorTracerBuilder.addError(UserError.builder()
                            .code("value_type")
                            .message(messageSource.getMessage("inventory.adjustment.value_type.invalid", null, LocaleContextHolder.getLocale()))
                            .fields(List.of("adjustments", String.valueOf(adjustmentIndex), "line_items", String.valueOf(index), "value_type"))
                            .build());
                }
            }
        }
    }

    private void removeInventoryLevelsIfInventoryItemIdNotExist(InventoryRequest request, List<Integer> inventoryItemIds, List<InventoryLevel> inventoryLevels) {
        // get list inventory_item_ids not in inventoryLevels
        var inventoryItemIdsNotInInventoryLevels = inventoryItemIds.stream()
                .filter(inventoryItemId -> inventoryLevels.stream().noneMatch(level -> level.getInventoryItemId() == inventoryItemId))
                .toList();
        if (CollectionUtils.isEmpty(inventoryItemIdsNotInInventoryLevels)) {
            return;
        }

        // create a modifiable copy of adjustments
        List<AdjustmentRequest> modifiableAdjustments = new ArrayList<>(request.getAdjustments());
        modifiableAdjustments.forEach(adjustment -> {
            //Create a modifiable copy of line items
            List<InventoryTransactionLineItemRequest> modifiableLineItems = new ArrayList<>(adjustment.getLineItems());

            modifiableLineItems.removeIf(line -> inventoryItemIdsNotInInventoryLevels.contains(line.getInventoryItemId()));

            if (request.getBehavior() == InventoryAdjustmentBehavior.remove_inventory_level_if_not_exist_in_specify_location) {
                var inventoryLevelsInLocation = inventoryLevels.stream()
                        .filter(inventoryLevel -> inventoryLevel.getLocationId() == adjustment.getLocationId())
                        .toList();
                var inventoryItemIdsExistInSpecifyLocation = inventoryLevelsInLocation.stream()
                        .map(InventoryLevel::getInventoryItemId)
                        .toList();
                modifiableLineItems.removeIf(line -> !inventoryItemIdsExistInSpecifyLocation.contains(line.getInventoryItemId()));
            }

            adjustment.setLineItems(modifiableLineItems);
        });

        modifiableAdjustments.removeIf(adjustment -> adjustment.getLineItems().isEmpty());

        // re-set adjustments request
        request.setAdjustments(modifiableAdjustments);
    }
}
