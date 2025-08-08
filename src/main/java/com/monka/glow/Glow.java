package com.monka.glow;

import com.monka.glow.glowstone.GlowstoneParticle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Glow.MODID)
public class Glow {
    public static final String MODID = "glow";

    public Glow(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        GlowRegistry.register(modEventBus);

        modEventBus.addListener(Glow::addCreative);
    }

    public static void addCreative(final BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tab = event.getTabKey();
        ItemStack glowstone = Items.GLOWSTONE.getDefaultInstance();
        ItemStack glowstonePrism = GlowRegistry.GLOWSTONE_PRISM.get().asItem().getDefaultInstance();
        CreativeModeTab.TabVisibility parentAndSearchTabs = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;

        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.getEntries().putAfter(glowstone,glowstonePrism,parentAndSearchTabs);
        }

        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.getEntries().putAfter(glowstone,glowstonePrism,parentAndSearchTabs);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(GlowRegistry.GLOWSTONE_DUST.get(), sprites
                    -> (simpleParticleType, clientLevel, d, e, f, g, h, i)
                    -> new GlowstoneParticle(clientLevel, d, e, f, g, h, i, sprites));
        }
    }
}

