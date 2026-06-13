package com.github.tickstudio.drowsy.server.domain.structure.base;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.NotNull;

/**
 * 睡觉后可获得buff的床
 */
public interface BuffBed extends  IBlockExtension {
    ImmutableSet<MobEffectInstance> getEffects();

    @Override
    default boolean isBed(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull LivingEntity sleeper) {
        return true;
    }
}
