package com.italiarevenge.iRShop.model;

import org.bukkit.Material;

import java.util.List;

public class Shop {

    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final String currency;
    private final String layout;
    private final List<ShopCategory> categories;

    public Shop(String id, String displayName, String description,
                Material icon, String currency, String layout,
                List<ShopCategory> categories) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.currency = currency;
        this.layout = layout;
        this.categories = categories;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Material getIcon() { return icon; }
    public String getCurrency() { return currency; }
    public String getLayout() { return layout; }
    public List<ShopCategory> getCategories() { return categories; }
}
