package com.github.tickstudio.drowsy.server.domain.structure.block;

import com.github.tickstudio.drowsy.TickUtils;
import com.github.tickstudio.drowsy.server.domain.structure.blockentity.StrawMatBlockEntity;
import com.github.tickstudio.drowsy.server.domain.structure.base.BuffBedTemplate;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StrawMatBlock extends BuffBedTemplate implements EntityBlock  {
    // 草席有虚弱效果
    Holder<MobEffect> effect = MobEffects.WEAKNESS;

    MapCodec<StrawMatBlock> CODEC = simpleCodec(StrawMatBlock::new);
    public StrawMatBlock(Properties properties){
        super(properties);

    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public ImmutableSet<MobEffectInstance> getEffects() {
        return ImmutableSet.of(new MobEffectInstance(effect, TickUtils.sec2tick(60)));
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new StrawMatBlockEntity(pos, state);
    }

    // 碰撞箱体积
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // 无论哪个方向都是高1的平层
        return   box(0, 0, 0, 16, 1, 16);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
