package com.github.tickstudio.drowsy;

import com.github.tickstudio.drowsy.client.render.AlpacaBedRenderer;
import com.github.tickstudio.drowsy.client.render.StrawMatRenderer;
import com.github.tickstudio.drowsy.server.domain.DomainRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = Drowsy.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = Drowsy.MODID, value = Dist.CLIENT)
public class DrowsyClient {
    public DrowsyClient(ModContainer container) {

        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
    }

    // 注册方块实体渲染器（BER）
    // EntityRenderersEvent.RegisterRenderers 在 mod 事件总线上触发，
    // @EventBusSubscriber 在 1.21.1 默认就是注册到 mod 总线，所以能收到这个事件。
    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                DomainRegistry.STRAW_MAT_ENTITY.get(),
                StrawMatRenderer::new
        );
        event.registerBlockEntityRenderer(
                DomainRegistry.ALPACA_BED_ENTITY.get(),
                AlpacaBedRenderer::new
        );
    }

}
