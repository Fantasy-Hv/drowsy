package com.github.tickstudio.drowsy.server.domain;

import com.github.tickstudio.drowsy.Drowsy;
import com.github.tickstudio.drowsy.server.domain.block.AlpacaBedBlock;
import com.github.tickstudio.drowsy.server.domain.block.AlpacaBedBlockEntity;
import com.github.tickstudio.drowsy.server.domain.block.StrawMatBlock;
import com.github.tickstudio.drowsy.server.domain.block.StrawMatBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

import static com.github.tickstudio.drowsy.Drowsy.MODID;

public class DomainRegistry {

    //方块注册表
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Drowsy.MODID);
    //注册 草席方块（noOcclusion 防止面剔除吃掉下方方块顶面）
    public static final DeferredBlock<StrawMatBlock> STRAW_MAT_BLOCK = BLOCKS.register("straw_mat",
            () -> new StrawMatBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.4F)
                    .sound(SoundType.GRASS)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
            ));

    // 注册 羊驼床方块
    public static final DeferredBlock<AlpacaBedBlock> ALPACA_BED_BLOCK = BLOCKS.register("alpaca_bed",
            () -> new AlpacaBedBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.6F)
                    .sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY)
            ));

    // 注册 羊驼毛方块
    public static final DeferredBlock<Block> ALPACA_WOOL_BLOCK = BLOCKS.register("alpaca_wool",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(0.8F)
                    .sound(SoundType.WOOL)
            ));

    // 方块实体注册表
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Drowsy.MODID);
    // 注册 草席方块实体
    public static final Supplier<BlockEntityType<StrawMatBlockEntity>> STRAW_MAT_ENTITY =
            BLOCK_ENTITIES.register("straw_mat",
                    () -> new BlockEntityType<>(
                            StrawMatBlockEntity::new,
                            Set.of(STRAW_MAT_BLOCK.get()) ,
                            null
                    )
            );

    // 注册 羊驼床方块实体
    public static final Supplier<BlockEntityType<AlpacaBedBlockEntity>> ALPACA_BED_ENTITY =
            BLOCK_ENTITIES.register("alpaca_bed",
                    () -> new BlockEntityType<>(
                            AlpacaBedBlockEntity::new,
                            Set.of(ALPACA_BED_BLOCK.get()),
                            null
                    )
            );

    // 物品注册表
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // 注册 草席物品
    public static final DeferredItem<BlockItem> STRAW_MAT_ITEM =
            ITEMS.registerSimpleBlockItem(STRAW_MAT_BLOCK);
    // 注册 羊驼床物品（手动注册以指定 stacksTo(1)，每格最多1个）
    public static final DeferredItem<BlockItem> ALPACA_BED_ITEM =
            ITEMS.register("alpaca_bed",
                    () -> new BlockItem(ALPACA_BED_BLOCK.get(), new Item.Properties().stacksTo(1)));
    // 注册 羊驼毛物品
    public static final DeferredItem<BlockItem> ALPACA_WOOL_ITEM =
            ITEMS.registerSimpleBlockItem(ALPACA_WOOL_BLOCK);
    // 注册 草束物品
    public static final DeferredItem<Item> BUNDLE_GRASS = ITEMS.registerSimpleItem(
            "bundle_grass",
            new Item.Properties().stacksTo(64)
    );
    // 注册 羊驼毛皮物品
    public static final DeferredItem<Item> ALPACA_FUR_ITEM = ITEMS.registerSimpleItem(
            "alpaca_fur",
            new Item.Properties().stacksTo(64)
    );



    // 创造模式标签页 注册表
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> BEDS = CREATIVE_MODE_TABS.register(
            "drowsy_bed",
            ()->CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.drowsy"))
                    .icon(() -> new ItemStack(STRAW_MAT_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        //用 accept 把 物品 添加到标签页里
                        output.accept(STRAW_MAT_ITEM.get());
                        output.accept(ALPACA_BED_ITEM.get());
                        output.accept(ALPACA_WOOL_ITEM.get());
                        output.accept(BUNDLE_GRASS.get());
                        output.accept(ALPACA_FUR_ITEM.get());
                    })

                    .build()
    );
    public static void registerDomain(IEventBus modEventBus){
        BLOCK_ENTITIES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
