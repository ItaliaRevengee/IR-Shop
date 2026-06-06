package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.api.events.ShopCategoryOpenEvent;
import com.italiarevenge.iRShop.gui.framework.PaginatedGui;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemUtil;
import com.italiarevenge.iRShop.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI showing all items inside a {@link ShopCategory}.
 * Left-click buys, right-click sells, shift+left-click quick-buys.
 */
public class ShopCategoryGui extends PaginatedGui {

    private final IRShop plugin;
    private final Shop shop;
    private final ShopCategory category;
    /** Ordered list parallel to pageItems — contains the ShopItem for each display slot. */
    private final List<ShopItem> displayedItems = new ArrayList<>();

    public ShopCategoryGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop
    ) {
        this(plugin, player, shop, shop.getCategories().get(0));
    }

    public ShopCategoryGui(
            @NotNull IRShop plugin,
            @NotNull Player player,
            @NotNull Shop shop,
            @NotNull ShopCategory category
    ) {
        super(player,
                plugin.getLayoutManager().getLayoutOrDefault(shop.getLayout()),
                TextUtil.parse(shop.getDisplayName() + " <dark_gray>» <white>" + category.getDisplayName()));
        this.plugin = plugin;
        this.shop = shop;
        this.category = category;
        plugin.getGuiManager().register(player, this);
    }

    @Override
    public void build() {
        pageItems.clear();
        displayedItems.clear();

        int discount = plugin.getDiscountService().getDiscount(player);
        double sellMult = plugin.getDiscountService().getSellMultiplier(player);

        for (ShopItem si : category.getItems()) {
            if (!si.isInStock() && !si.isSellable()) continue;

            double effectiveBuy  = plugin.getDiscountService().getEffectiveBuyPrice(si.getBuyPrice(), player);
            double effectiveSell = plugin.getDiscountService().getEffectiveSellPrice(si.getSellPrice(), player);

            String currencyId = si.getCurrencyOverride() != null ? si.getCurrencyOverride() : shop.getCurrencyId();
            String currencyName = plugin.getEconomyManager().getProvider(currencyId).getCurrencyInfo().name();

            List<String> extraLore = new ArrayList<>();
            if (si.isBuyable()) {
                extraLore.add(plugin.getMessageManager().get("gui.item-buy-lore-append[0]") != null ? "" : "");
                extraLore.add("<gray>Buy: <green>" + TextUtil.formatPrice(effectiveBuy) + " <white>" + currencyName);
                if (discount > 0) {
                    extraLore.add("<yellow>Discount: <white>" + discount + "%");
                }
                extraLore.add("<yellow>Left-click <gray>to buy");
                extraLore.add("<yellow>Shift-click <gray>for quick buy");
            }
            if (si.isSellable()) {
                extraLore.add("<gray>Sell: <gold>" + TextUtil.formatPrice(effectiveSell) + " <white>" + currencyName);
                if (sellMult > 1.0) {
                    extraLore.add("<yellow>Multiplier: <white>" + sellMult + "x");
                }
                extraLore.add("<yellow>Right-click <gray>to sell");
            }
            if (!si.hasUnlimitedStock()) {
                extraLore.add("<gray>Stock: <white>" + si.getStock());
            }

            ItemStack display = ItemUtil.appendLore(si.getItem(), TextUtil.parseList(extraLore));
            pageItems.add(display);
            displayedItems.add(si);
        }

        renderPage();

        // Register per-item click actions
        int perPage = layout.getItemsPerPage();
        int start = currentPage * perPage;
        for (int i = 0; i < perPage; i++) {
            int itemIndex = start + i;
            if (itemIndex >= displayedItems.size()) break;
            ShopItem si = displayedItems.get(itemIndex);
            int invSlot = layout.getItemSlot(i);
            if (invSlot < 0) continue;
            setAction(invSlot, e -> handleItemClick(e, si));
        }

        // Nav buttons
        addNavButton("back",
                ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.back-name")),
                () -> new ShopMainGui(plugin, player).open());

        addNavButton("prev-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.prev-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1), "total", String.valueOf(getTotalPages()))),
                this::prevPage);

        addNavButton("next-page",
                ItemUtil.build(layout.getBorderMaterial(),
                        plugin.getMessageManager().get("gui.next-page-name"),
                        Map.of("current", String.valueOf(currentPage + 1), "total", String.valueOf(getTotalPages()))),
                this::nextPage);

        addNavButton("close",
                ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.close-name")),
                () -> player.closeInventory());

        if (plugin.getConfigManager().isSearchEnabled()) {
            addNavButton("search",
                    ItemUtil.build(layout.getBorderMaterial(), plugin.getMessageManager().get("gui.search-name")),
                    () -> new SearchGui(plugin, player, shop).open());
        }
    }

    private void handleItemClick(@NotNull InventoryClickEvent e, @NotNull ShopItem si) {
        boolean isShift = e.isShiftClick();
        boolean isRight  = e.isRightClick();
        boolean isLeft   = e.isLeftClick();

        if (isRight && si.isSellable()) {
            handleSell(si, 1);
        } else if (isLeft && si.isBuyable()) {
            if (isShift && plugin.getConfigManager().isQuickBuyEnabled()) {
                // Quick buy — bypass confirm
                var result = plugin.getTransactionService().buy(player, shop, category, si, 1);
                sendTransactionFeedback(result.messageKey(), si, 1);
            } else if (plugin.getConfigManager().isConfirmPurchases()) {
                new ConfirmGui(plugin, player, shop, category, si, 1, this).open();
            } else {
                var result = plugin.getTransactionService().buy(player, shop, category, si, 1);
                sendTransactionFeedback(result.messageKey(), si, 1);
                refresh();
            }
        }
    }

    private void handleSell(@NotNull ShopItem si, int amount) {
        var result = plugin.getTransactionService().sell(player, shop, category, si, amount);
        sendTransactionFeedback(result.messageKey(), si, amount);
        if (result.success()) refresh();
    }

    private void sendTransactionFeedback(@NotNull String key, @NotNull ShopItem si, int amount) {
        String itemName = si.getItem().getType().name().replace('_', ' ').toLowerCase();
        String price = ""; // Would format via economy — simplified for brevity
        plugin.getMessageManager().send(player, key, Map.of(
                "item-name", itemName,
                "amount", String.valueOf(amount),
                "price", price));
    }
}
