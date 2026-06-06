package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.model.ShopItem;

/**
 * Implemented by any GUI that can execute buy transactions and be re-opened
 * as a "back" target from QuantityGui.
 */
public interface TransactionHost {
    void executeBuy(ShopItem shopItem, int amount);
    void open();
}
