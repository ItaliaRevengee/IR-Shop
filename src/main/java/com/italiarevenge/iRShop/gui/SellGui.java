package com.italiarevenge.iRShop.gui;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.economy.EconomyManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.ItemMatcher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellGui extends BaseGui {

    // Layout: 5 rows (45 slots total)
    //   Rows 0-3  (slots  0-35): sell input zone — interactive
    //   Row  4    (slots 36-44): control row
    private static final int SELL_ROWS  = 4;
    private static final int TOTAL_ROWS = 5;
    private static final int SELL_SLOTS = SELL_ROWS * 9; // 36

    private static final int SLOT_SELL   = SELL_SLOTS;      // 36
    private static final int SLOT_INFO   = SELL_SLOTS + 4;  // 40
    private static final int SLOT_CANCEL = SELL_SLOTS + 8;  // 44

    private final MessageManager msg;
    private final EconomyManager economy;
    private final Map<Material, List<ShopItem>> sellIndex;
    private boolean infoUpdatePending = false;

    public SellGui(Player player) {
        super(player);
        this.msg       = IRShop.get().getMessageManager();
        this.economy   = IRShop.get().getEconomyManager();
        this.sellIndex = buildSellIndex();
    }

    private Map<Material, List<ShopItem>> buildSellIndex() {
        Map<Material, List<ShopItem>> index = new HashMap<>();
        for (Shop shop : IRShop.get().getShopLoader().getShops().values()) {
            for (ShopCategory category : shop.getCategories()) {
                for (ShopItem item : category.getItems()) {
                    if (item.isSellable()) index.computeIfAbsent(item.getMaterial(), k -> new ArrayList<>()).add(item);
                    for (ShopItem variant : item.getVariants()) {
                        if (variant.isSellable()) index.computeIfAbsent(variant.getMaterial(), k -> new ArrayList<>()).add(variant);
                    }
                }
            }
        }
        return index;
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void open() {
        inventory = Bukkit.createInventory(null, TOTAL_ROWS * 9,
                MessageManager.parse(msg.raw().getString("sellgui.title", "<dark_gray>Vendi <gold>Oggetti")));
        renderControls();
        player.openInventory(inventory);
        GuiListener.register(player, this);
    }

    @Override
    public void onClose() {
        returnAllItems();
    }

    // ── rendering ─────────────────────────────────────────────────────────────

    private void renderControls() {
        ItemStack filler = fillerPane();
        for (int i = SELL_SLOTS; i < TOTAL_ROWS * 9; i++) inventory.setItem(i, filler);

        inventory.setItem(SLOT_SELL,   buildButton(Material.LIME_WOOL,
                msg.raw().getString("sellgui.sell-button-name",   "<green><bold>✔ Vendi"),
                msg.raw().getStringList("sellgui.sell-button-lore")));
        inventory.setItem(SLOT_CANCEL, buildButton(Material.RED_WOOL,
                msg.raw().getString("sellgui.cancel-button-name", "<red><bold>✗ Annulla"),
                msg.raw().getStringList("sellgui.cancel-button-lore")));
        updateInfoButton();
    }

    private void updateInfoButton() {
        double total = previewValue();
        List<String> rawLore = msg.raw().getStringList("sellgui.info-button-lore");
        List<Component> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(MessageManager.parse(line.replace("<total>", economy.format(total))));
        }

        ItemStack info = new ItemStack(Material.GOLD_INGOT);
        ItemMeta  meta = info.getItemMeta();
        meta.displayName(MessageManager.parse(
                msg.raw().getString("sellgui.info-button-name", "<gold><bold>Valore Totale")));
        meta.lore(lore);
        info.setItemMeta(meta);
        inventory.setItem(SLOT_INFO, info);
    }

    private void scheduleInfoUpdate() {
        if (infoUpdatePending) return;
        infoUpdatePending = true;
        Bukkit.getScheduler().runTask(IRShop.get(), () -> {
            infoUpdatePending = false;
            updateInfoButton();
        });
    }

    // ── click handling ────────────────────────────────────────────────────────

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        if (slot == SLOT_SELL) {
            executeSellAll();
            return;
        }
        if (slot == SLOT_CANCEL) {
            returnAllItems();
            player.closeInventory();
            return;
        }

        // Rest of control row: stay cancelled (do nothing)
        if (slot >= SELL_SLOTS) return;

        // Sell zone: allow free item interaction
        event.setCancelled(false);
        scheduleInfoUpdate();
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        ClickType click = event.getClick();
        if (click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        moveToSellSlot(event, item);
        scheduleInfoUpdate();
    }

    @Override
    public boolean isInteractiveSlot(int slot) {
        return slot >= 0 && slot < SELL_SLOTS;
    }

    // ── sell logic ────────────────────────────────────────────────────────────

    private void executeSellAll() {
        if (!economy.isAvailable()) {
            player.sendMessage(msg.get("economy.vault-not-found"));
            return;
        }

        double totalEarned = 0;
        int    totalItems  = 0;
        List<ItemStack> unsellable = new ArrayList<>();
        double multiplier = getSellMultiplier();

        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;
            inventory.setItem(i, null);

            ShopItem match = findMatchingShopItem(stack);
            if (match != null) {
                totalEarned += match.getSellPrice() * multiplier * stack.getAmount();
                totalItems  += stack.getAmount();
            } else {
                unsellable.add(stack.clone());
            }
        }

        if (totalItems == 0 && unsellable.isEmpty()) {
            player.sendMessage(msg.get("sellgui.nothing-to-sell"));
            return;
        }

        if (totalItems > 0) {
            economy.deposit(player, totalEarned);
            playSound("sell");
            player.sendMessage(msg.get("sellgui.sold",
                    Placeholder.parsed("amount", String.valueOf(totalItems)),
                    Placeholder.parsed("price",  economy.format(totalEarned))));
        }

        for (ItemStack item : unsellable) {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack dropped : overflow.values())
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
        }

        if (!unsellable.isEmpty())
            player.sendMessage(msg.get("sellgui.unsellable-returned"));

        updateInfoButton();
    }

    private void returnAllItems() {
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;
            inventory.setItem(i, null);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack.clone());
            for (ItemStack dropped : overflow.values())
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void moveToSellSlot(InventoryClickEvent event, ItemStack item) {
        ItemStack remaining = item.clone();

        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack existing = inventory.getItem(i);

            if (existing == null || existing.getType().isAir()) {
                inventory.setItem(i, remaining.clone());
                event.getCurrentItem().setAmount(0);
                return;
            }
            if (existing.isSimilar(remaining)) {
                int space = existing.getMaxStackSize() - existing.getAmount();
                if (space > 0) {
                    int toMove = Math.min(space, remaining.getAmount());
                    existing.setAmount(existing.getAmount() + toMove);
                    remaining.setAmount(remaining.getAmount() - toMove);
                    if (remaining.getAmount() <= 0) {
                        event.getCurrentItem().setAmount(0);
                        return;
                    }
                }
            }
        }
        // Sell zone full: leave what couldn't fit
        event.getCurrentItem().setAmount(remaining.getAmount());
    }

    private ShopItem findMatchingShopItem(ItemStack stack) {
        List<ShopItem> candidates = sellIndex.get(stack.getType());
        if (candidates == null) return null;
        for (ShopItem shopItem : candidates) {
            if (ItemMatcher.matchesStack(stack, shopItem)) return shopItem;
        }
        return null;
    }

    private double previewValue() {
        double total = 0;
        double multiplier = getSellMultiplier();
        for (int i = 0; i < SELL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType().isAir()) continue;
            ShopItem match = findMatchingShopItem(stack);
            if (match != null) total += match.getSellPrice() * multiplier * stack.getAmount();
        }
        return total;
    }

    private double getSellMultiplier() {
        return com.italiarevenge.iRShop.util.ItemBuilder.getSellMultiplier(player);
    }

    private ItemStack fillerPane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = pane.getItemMeta();
        meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack buildButton(Material mat, String nameMM, List<String> loreMM) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.displayName(MessageManager.parse(nameMM));
        List<Component> lore = new ArrayList<>();
        for (String line : loreMM) lore.add(MessageManager.parse(line));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
