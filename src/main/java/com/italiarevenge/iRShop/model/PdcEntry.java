package com.italiarevenge.iRShop.model;

/**
 * Represents a single PersistentDataContainer entry defined in a YAML config.
 * Supported types: STRING, INTEGER, LONG, DOUBLE, FLOAT, BYTE.
 */
public record PdcEntry(String namespace, String key, String type, String rawValue) {}
