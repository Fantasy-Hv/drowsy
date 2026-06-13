package com.github.tickstudio.drowsy.server.domain.block;

import com.github.tickstudio.drowsy.server.domain.DomainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlpacaBedBlockEntity extends BlockEntity {
    public AlpacaBedBlockEntity(BlockPos pos, BlockState blockState) {
        super(DomainRegistry.ALPACA_BED_ENTITY.get(), pos, blockState);
    }
}
