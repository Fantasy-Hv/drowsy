package com.github.tickstudio.drowsy.server.domain.structure.blockentity;

import com.github.tickstudio.drowsy.server.domain.DomainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// 方块实体,用于在一格上渲染两格的草席模型
public class StrawMatBlockEntity extends BlockEntity {
    public StrawMatBlockEntity(BlockPos pos, BlockState blockState) {
        super(DomainRegistry.STRAW_MAT_ENTITY.get(), pos, blockState);
    }
}
