package com.github.tickstudio.drowsy.server.event.listener;

import com.github.tickstudio.drowsy.server.domain.structure.base.BuffBed;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

@EventBusSubscriber
public class BedSleepListener {
    @SubscribeEvent
    public static void onCanSleep(CanPlayerSleepEvent event){

    }
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event){
        // 添加模组效果
        Player player = event.getEntity();
        player.getSleepingPos().ifPresent(
                pos->{
                    Block block = player.level().getBlockState(pos).getBlock();
                    if (block instanceof BuffBed){
                        ImmutableSet<MobEffectInstance> effects = ((BuffBed) block).getEffects();
                        effects.forEach(player::addEffect);
                    }
                }
        );
    }

    // 玩家睡觉时每个 tick，检查是否应该继续睡
    @SubscribeEvent
    public static void canStillSleep( CanContinueSleepingEvent event){

    }
}
