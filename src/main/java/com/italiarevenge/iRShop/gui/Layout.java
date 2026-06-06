package com.italiarevenge.iRShop.gui;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public class Layout {

    public final String id;
    public final int rows;
    public final Material bgMaterial;
    public final String bgName;
    public final Material borderMaterial;
    public final String borderName;
    public final int slotBack;
    public final int slotPrev;
    public final int slotClose;
    public final int slotNext;
    public final int slotSearch;
    public final int slotInfo;
    public final List<Integer> itemSlots;

    public Layout(String id, int rows,
                  Material bgMaterial, String bgName,
                  Material borderMaterial, String borderName,
                  Map<String, Integer> navSlots,
                  List<Integer> itemSlots) {
        this.id = id;
        this.rows = rows;
        this.bgMaterial = bgMaterial;
        this.bgName = bgName;
        this.borderMaterial = borderMaterial;
        this.borderName = borderName;
        this.slotBack   = navSlots.getOrDefault("back", 45);
        this.slotPrev   = navSlots.getOrDefault("prev-page", 48);
        this.slotClose  = navSlots.getOrDefault("close", 49);
        this.slotNext   = navSlots.getOrDefault("next-page", 50);
        this.slotSearch = navSlots.getOrDefault("search", 47);
        this.slotInfo   = navSlots.getOrDefault("info", 53);
        this.itemSlots  = itemSlots;
    }
}
