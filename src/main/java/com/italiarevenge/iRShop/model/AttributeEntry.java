package com.italiarevenge.iRShop.model;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Represents an attribute modifier entry defined in a YAML config.
 *
 * YAML format:
 * attribute-modifiers:
 *   - attribute: GENERIC_ATTACK_DAMAGE
 *     amount: 5.0
 *     operation: ADD_NUMBER   # ADD_NUMBER | ADD_SCALAR | MULTIPLY_SCALAR_1
 *     slot: HAND              # ANY | HAND | OFF_HAND | HEAD | CHEST | LEGS | FEET | ARMOR
 */
public record AttributeEntry(
        Attribute attribute,
        double amount,
        AttributeModifier.Operation operation,
        EquipmentSlotGroup slotGroup
) {}
