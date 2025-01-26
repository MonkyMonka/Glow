package com.monka.glow.item;

import com.monka.glow.Glow;
import com.monka.glow.block.ModBlocks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Glow.MODID);

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();

        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.insertAfter(Items.GLOWSTONE.getDefaultInstance(), ModBlocks.GLOWSTONE_PRISM.asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }


        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.insertAfter(Items.GLOWSTONE.getDefaultInstance(), ModBlocks.GLOWSTONE_PRISM.asItem().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}