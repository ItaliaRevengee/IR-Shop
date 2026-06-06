package com.italiarevenge.iRShop.model;

import org.bukkit.Material;

import java.util.List;

public class ShopItem {

    private final Material material;
    private final double buyPrice;
    private final double sellPrice;
    private final String customName;
    private final List<String> customLore;

    public ShopItem(Material material, double buyPrice, double sellPrice,
                    String customName, List<String> customLore) {
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.customName = customName;
        this.customLore = customLore;
    }

    public Material getMaterial() { return material; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public boolean isBuyable() { return buyPrice > 0; }
    public boolean isSellable() { return sellPrice > 0; }
    public String getCustomName() { return customName; }
    public List<String> getCustomLore() { return customLore; }
}
