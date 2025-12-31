package com.plott.soul_cage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.plott.soul_cage.items.SoulCageReusableItem;
import com.plott.soul_cage.items.SoulCageSingleUseItem;
import com.plott.soul_cage.items.SoulCageEventHandler;

import java.util.ArrayList;
import java.util.List;

public class SoulCageMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("soul_cage");

    // Declare items without instantiation
    public static Item REUSABLE_SOUL_CAGE;
    public static Item ONE_TIME_SOUL_CAGE;

    // Declare the custom item group as a RegistryKey
    public static final RegistryKey<ItemGroup> SOUL_CAGE_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP,
            Identifier.of("soul_cage", "general"));

    // Configuration variables
    public static boolean enableWhitelist = false;
    public static boolean enableBlacklist = true;
    public static final List<String> whitelist = new ArrayList<>();
    public static final List<String> blacklist = new ArrayList<>();

    @Override
    public void onInitialize() {
        // Initialize configuration
        initConfig();

        // Instantiate items here
        REUSABLE_SOUL_CAGE = new SoulCageReusableItem(new Item.Settings().maxCount(1));
        ONE_TIME_SOUL_CAGE = new SoulCageSingleUseItem(new Item.Settings().maxCount(1));

        // Register items
        Registry.register(Registries.ITEM, Identifier.of("soul_cage", "reusable_soul_cage"), REUSABLE_SOUL_CAGE);
        Registry.register(Registries.ITEM, Identifier.of("soul_cage", "one_time_soul_cage"), ONE_TIME_SOUL_CAGE);

        LOGGER.info("SoulCage items registered: reusable_soul_cage, one_time_soul_cage");

        // Register the item group
        Registry.register(Registries.ITEM_GROUP, SOUL_CAGE_GROUP, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.soul_cage.general"))
                .icon(() -> new ItemStack(REUSABLE_SOUL_CAGE))
                .build());

        // Add items to the custom item group
        ItemGroupEvents.modifyEntriesEvent(SOUL_CAGE_GROUP).register(entries -> {
            entries.add(REUSABLE_SOUL_CAGE);
            entries.add(ONE_TIME_SOUL_CAGE);
        });

        // Register event listener
        SoulCageEventHandler.registerEntityInteractionListener();

        LOGGER.info("Soul Cage Mod initialized.");
    }

    private void initConfig() {
        // Initialize whitelist and blacklist here
        whitelist.add("minecraft:cow");
        blacklist.add("minecraft:wither");
        blacklist.add("minecraft:player");

    }
}
