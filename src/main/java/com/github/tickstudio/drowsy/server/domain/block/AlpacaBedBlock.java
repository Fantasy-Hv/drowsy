package com.github.tickstudio.drowsy.server.domain.block;

import com.github.tickstudio.drowsy.TickUtils;
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

/**
 * 羊驼床方块 —— 一种提供再生增益的1×2双层床。
 * <p>
 * 继承自 {@link BuffBedTemplate}（定义了床的放置/睡眠/拆分等通用逻辑），
 * 同时实现 {@link EntityBlock} 以关联 {@link AlpacaBedBlockEntity} 方块实体，
 * 使该方块可以使用实体模型渲染（而非普通方块模型）。
 */
public class AlpacaBedBlock extends BuffBedTemplate implements EntityBlock {

    /**
     * 该床施加的状态效果：再生（Regeneration）。
     * <p>
     * 使用 {@link Holder<MobEffect>} 而非直接引用 {@code MobEffect}，
     * 是因为 Minecraft 的效果注册表采用 Holder 包装，
     * 这样可以支持数据包（datapack）对效果的重定义/替换。
     */
    Holder<MobEffect> effect = MobEffects.REGENERATION;

    /**
     * 方块序列化编解码器。
     * <p>
     * {@link MapCodec} 是 Mojang 数据序列化框架的核心组件，
     * 负责将方块实例与方块状态（BlockState）之间进行序列化/反序列化。
     * {@code simpleCodec} 是父类提供的工厂方法，接受「属性 → 方块实例」的构造函数引用，
     * 生成一个仅保存 Properties 的最小编解码器（因为本方块没有额外的构造参数需要持久化）。
     * <p>
     * 当存档加载时，Minecraft 通过此 CODEC 将 NBT 数据还原为 AlpacaBedBlock 实例。
     */
    MapCodec<AlpacaBedBlock> CODEC = simpleCodec(AlpacaBedBlock::new);

    public AlpacaBedBlock(Properties properties) {
        super(properties);
    }

    /**
     * 向 Minecraft 注册系统提供该方块的编解码器。
     * <p>
     * 这是 {@code HorizontalDirectionalBlock → Block} 继承链中要求的抽象方法，
     * 引擎在序列化/反序列化方块时通过此方法获取对应的 CODEC。
     * 返回类型声明的 {@code ? extends HorizontalDirectionalBlock} 允许
     * 不同子类返回各自的泛型 CODEC。
     */
    @Override
    protected @NotNull MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /**
     * 返回该床提供的增益效果集合。
     * <p>
     * - {@link ImmutableSet} 保证效果集合不可变，防止运行时意外修改；
     * - {@link MobEffectInstance} 描述一个具体的效果实例（效果类型 + 持续时间 + 等级等）；
     * - {@code TickUtils.sec2tick(60)} 将 60 秒转换为游戏刻（1 秒 = 20 tick），即 1200 tick；
     * - 效果等级默认为 0 级（即 I 级，Minecraft 内部从 0 开始计数）。
     * <p>
     * 此方法由 {@link BuffBed} 接口定义，供睡眠逻辑在玩家醒来后施加效果。
     */
    @Override
    public ImmutableSet<MobEffectInstance> getEffects() {
        return ImmutableSet.of(new MobEffectInstance(effect, TickUtils.sec2tick(60)));
    }

    /**
     * 创建与本方块关联的方块实体（BlockEntity）。
     * <p>
     * 实现 {@link EntityBlock} 接口必须覆写此方法。
     * 方块实体用于存储方块状态无法表达的额外数据，
     * 同时也是使用 {@code RenderShape.ENTITYBLOCK_ANIMATED} 渲染方式的前提——
     * 没有方块实体，实体模型渲染器将无法找到渲染目标。
     * <p>
     * Minecraft 在方块被放置到世界中时自动调用此方法，将返回的 BlockEntity
     * 与对应位置绑定。
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AlpacaBedBlockEntity(pos, state);
    }

    /**
     * 定义方块的碰撞/轮廓形状（VoxelShape）。
     * <p>
     * - {@code box(0, 0, 0, 16, 9, 16)} 创建一个从 (0,0,0) 到 (16,9,16) 的长方体，
     *   坐标单位为像素（1 方块 = 16×16×16 像素）；
     * - Y 轴高度为 9 像素（约 9/16 ≈ 0.5625 格），表示这张床只有半格多一点高，
     *   与原版床的高度一致，玩家无法从床面上直接走上去（需要跳跃）；
     * - 此形状同时用于：碰撞检测（实体能否穿过）、轮廓线渲染（玩家准星对准时的黑色边框）
     *   以及寻路计算。
     */
    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return box(0, 0, 0, 16, 9, 16);
    }

    /**
     * 指定方块的渲染方式为「方块实体动画渲染」。
     * <p>
     * {@link RenderShape} 有以下几种取值：
     * <ul>
     *   <li>{@code MODEL} — 使用方块模型 JSON 渲染（默认行为）</li>
     *   <li>{@code ENTITYBLOCK_ANIMATED} — 使用 BlockEntity 关联的实体模型（EntityModel）渲染，
     *       并支持骨骼动画；方块模型的 JSON 会被忽略</li>
     *   <li>其他如 {@code INVISIBLE}/{@code LIQUID} 等</li>
     * </ul>
     * 选择 {@code ENTITYBLOCK_ANIMATED} 意味着：
     * 本方块不使用 blockbench 导出的方块模型 JSON，而是通过
     * {@link AlpacaBedBlockEntity} + 对应的 BlockEntityRenderer
     * 使用实体模型进行渲染，可以实现更复杂的动画效果。
     */
    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
