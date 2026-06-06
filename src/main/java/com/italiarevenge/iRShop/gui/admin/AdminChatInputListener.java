package com.italiarevenge.iRShop.gui.admin;

import com.italiarevenge.iRShop.IRShop;
import com.italiarevenge.iRShop.config.MessageManager;
import com.italiarevenge.iRShop.model.Shop;
import com.italiarevenge.iRShop.model.ShopCategory;
import com.italiarevenge.iRShop.model.ShopItem;
import com.italiarevenge.iRShop.util.CategorySaver;
import com.italiarevenge.iRShop.util.ItemBuilder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminChatInputListener implements Listener {

    public enum Step { ADD_BUY, ADD_SELL, EDIT_BUY, EDIT_SELL }

    public static class Session {
        public Step step;
        public String shopId;
        public String categoryId;
        public int page;
        // add-item fields
        public ItemStack heldItem;
        public double addBuyPrice;
        // edit-price fields
        public int itemIndex;
    }

    private static final Map<UUID, Session> sessions = new HashMap<>();

    // ── session start helpers ────────────────────────────────────────────────

    public static void startAddItem(Player player, ItemStack held,
                                    String shopId, String categoryId, int page) {
        Session s = new Session();
        s.step       = Step.ADD_BUY;
        s.shopId     = shopId;
        s.categoryId = categoryId;
        s.page       = page;
        s.heldItem   = held.clone();
        sessions.put(player.getUniqueId(), s);
        player.sendMessage(MessageManager.parse(
                "<gold>[AdminShop] <yellow>Inserisci il prezzo di <green>acquisto</green> (o <red>cancel</red>):"));
    }

    public static void startEditBuy(Player player, String shopId, String categoryId,
                                    int itemIndex, int page) {
        Session s = new Session();
        s.step       = Step.EDIT_BUY;
        s.shopId     = shopId;
        s.categoryId = categoryId;
        s.itemIndex  = itemIndex;
        s.page       = page;
        sessions.put(player.getUniqueId(), s);
        player.sendMessage(MessageManager.parse(
                "<gold>[AdminShop] <yellow>Nuovo prezzo di <green>acquisto</green> (o <red>cancel</red>):"));
    }

    public static void startEditSell(Player player, String shopId, String categoryId,
                                     int itemIndex, int page) {
        Session s = new Session();
        s.step       = Step.EDIT_SELL;
        s.shopId     = shopId;
        s.categoryId = categoryId;
        s.itemIndex  = itemIndex;
        s.page       = page;
        sessions.put(player.getUniqueId(), s);
        player.sendMessage(MessageManager.parse(
                "<gold>[AdminShop] <yellow>Nuovo prezzo di <gold>vendita</gold> (-1 = non vendibile, o <red>cancel</red>):"));
    }

    public static boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ── event handler ────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (input.equalsIgnoreCase("cancel")) {
            sessions.remove(player.getUniqueId());
            player.sendMessage(MessageManager.parse("<red>Operazione annullata."));
            return;
        }

        double value;
        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageManager.parse("<red>Numero non valido. Riprova o digita <yellow>cancel<red>."));
            return;
        }

        sessions.remove(player.getUniqueId());
        Step step = session.step;
        IRShop plugin = IRShop.get();

        new BukkitRunnable() {
            @Override public void run() {
                switch (step) {
                    case ADD_BUY -> {
                        session.addBuyPrice = value;
                        session.step = Step.ADD_SELL;
                        sessions.put(player.getUniqueId(), session);
                        player.sendMessage(MessageManager.parse(
                                "<gold>[AdminShop] <yellow>Prezzo di <gold>vendita</gold> (-1 = non vendibile, o <red>cancel</red>):"));
                    }
                    case ADD_SELL -> {
                        String entry = buildEntry(session.heldItem, session.addBuyPrice, value);
                        CategorySaver.appendItem(plugin, session.categoryId, entry);
                        plugin.reload();
                        reopenItemList(player, plugin, session.shopId, session.categoryId, session.page);
                        player.sendMessage(MessageManager.parse("<green>Oggetto aggiunto con successo!"));
                    }
                    case EDIT_BUY -> {
                        CategorySaver.updateItemPrice(plugin, session.categoryId, session.itemIndex, true, value);
                        plugin.reload();
                        reopenItemEdit(player, plugin, session.shopId, session.categoryId,
                                session.itemIndex, session.page);
                        player.sendMessage(MessageManager.parse("<green>Prezzo acquisto aggiornato!"));
                    }
                    case EDIT_SELL -> {
                        CategorySaver.updateItemPrice(plugin, session.categoryId, session.itemIndex, false, value);
                        plugin.reload();
                        reopenItemEdit(player, plugin, session.shopId, session.categoryId,
                                session.itemIndex, session.page);
                        player.sendMessage(MessageManager.parse("<green>Prezzo vendita aggiornato!"));
                    }
                }
            }
        }.runTask(plugin);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void reopenItemList(Player player, IRShop plugin,
                                       String shopId, String categoryId, int page) {
        Shop shop = plugin.getShopLoader().getShop(shopId);
        ShopCategory cat = findCategory(plugin, shopId, categoryId);
        if (shop != null && cat != null) new AdminItemListGui(player, shop, cat, page).open();
    }

    private static void reopenItemEdit(Player player, IRShop plugin,
                                       String shopId, String categoryId, int itemIndex, int page) {
        Shop shop = plugin.getShopLoader().getShop(shopId);
        ShopCategory cat = findCategory(plugin, shopId, categoryId);
        if (shop == null || cat == null) return;
        List<ShopItem> items = cat.getItems();
        if (itemIndex >= 0 && itemIndex < items.size()) {
            new AdminItemEditGui(player, shop, cat, items.get(itemIndex), itemIndex, page).open();
        } else {
            new AdminItemListGui(player, shop, cat, page).open();
        }
    }

    static ShopCategory findCategory(IRShop plugin, String shopId, String categoryId) {
        Shop shop = plugin.getShopLoader().getShop(shopId);
        if (shop == null) return null;
        for (ShopCategory c : shop.getCategories()) {
            if (c.getId().equals(categoryId)) return c;
        }
        return null;
    }

    private String buildEntry(ItemStack item, double buy, double sell) {
        if (isPlain(item)) {
            return "  - material: " + item.getType().name() + "\n"
                 + "    buy: "      + buy                    + "\n"
                 + "    sell: "     + sell                   + "\n";
        }
        String b64 = Base64.getEncoder().encodeToString(ItemBuilder.serializeNbt(item));
        return "  - serialized: \"" + b64 + "\"\n"
             + "    buy: "          + buy  + "\n"
             + "    sell: "         + sell + "\n";
    }

    private boolean isPlain(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        return !meta.hasDisplayName()
            && !meta.hasLore()
            && !meta.hasEnchants()
            && !meta.hasCustomModelData()
            && !(meta instanceof LeatherArmorMeta);
    }
}
