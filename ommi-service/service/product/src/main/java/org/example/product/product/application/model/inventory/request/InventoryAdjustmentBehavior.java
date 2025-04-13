package org.example.product.product.application.model.inventory.request;

public enum InventoryAdjustmentBehavior {
    decrement_ignoring_policy,
    decrement_obeying_policy,
    decrement_obeying_policy_in_specify_location,
    creating_inventory_level_if_not_exist,
    remove_inventory_level_if_not_exist_in_specify_location;
}
