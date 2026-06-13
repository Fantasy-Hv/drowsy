package com.github.tickstudio.drowsy.server.event.listener;

import com.github.tickstudio.drowsy.server.domain.DomainRegistry;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber
public class LlamaDropListener {

    @SubscribeEvent
    public static void onLlamaDeath(LivingDropsEvent event) {
        if (event.getEntity() instanceof Llama) {
            if (event.getEntity().getRandom().nextFloat() < 0.9f) {
                ItemEntity itemEntity = new ItemEntity(
                        event.getEntity().level(),
                        event.getEntity().getX(),
                        event.getEntity().getY(),
                        event.getEntity().getZ(),
                        new ItemStack(DomainRegistry.ALPACA_FUR_ITEM.get(), 1)
                );
                event.getDrops().add(itemEntity);
            }
        }
    }
}