package com.monka.glow;

import com.monka.glow.glowstone.GlowstoneParticle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(Glow.MODID)
public class Glow {
    public static final String MODID = "glow";
    public Glow(IEventBus modEventBus, ModContainer modContainer) {
        GlowRegistry.register(modEventBus);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(final BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        ItemStack glowstone = Items.GLOWSTONE.getDefaultInstance();
        ItemStack glowstonePrism = GlowRegistry.GLOWSTONE_PRISM.asItem().getDefaultInstance();
        CreativeModeTab.TabVisibility parentAndSearchTabs = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.insertAfter(glowstone, glowstonePrism, parentAndSearchTabs);
        }

        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.insertAfter(glowstone, glowstonePrism, parentAndSearchTabs);
        }
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(GlowRegistry.GLOWSTONE_DUST.get(), sprites
                    -> (simpleParticleType, clientLevel, d, e, f, g, h, i)
                    -> new GlowstoneParticle(clientLevel, d, e, f, g, h, i, sprites));
        }
    }
}

