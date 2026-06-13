package com.github.tickstudio.drowsy.client.render;

import com.github.tickstudio.drowsy.server.domain.structure.block.StrawMatBlock;
import com.github.tickstudio.drowsy.server.domain.structure.blockentity.StrawMatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.SortedSet;

/**
 * 草席方块的方块实体渲染器（Block Entity Renderer）。
 * <p>
 * 草席是双格（1×2）方块，模型文件是一整块跨两格的几何体。
 * 渲染策略：只在 FOOT（脚部）方块实体上画一次完整模型，HEAD（头部）
 * 方块实体不画任何东西，只用于让裂缝系统有模型可读。
 * <p>
 * 裂缝（破坏进度动画）：
 * - 破坏 FOOT 时：LevelRenderer 自动检测并叠加裂缝，无需额外处理
 * - 破坏 HEAD 时：LevelRenderer 的自动包裹只作用于 HEAD 自己的 BER（但 HEAD BER
 *   直接 return 了），所以这里通过反射读取 LevelRenderer 内部的破坏进度数据，
 *   用 SheetedDecalTextureGenerator 手动把裂缝纹理叠加到 FOOT 的模型上，
 *   保证无论破坏哪半边都能在两个半块上看到裂缝动画。
 *
 * @see StrawMatBlock 对应的方块类
 * @see StrawMatBlockEntity 对应的方块实体类
 */
@OnlyIn(Dist.CLIENT)
public class StrawMatRenderer implements BlockEntityRenderer<StrawMatBlockEntity> {

    /**
     * LevelRenderer 内部储存破坏进度的 Map。
     * 原字段为 private，通过反射突破访问限制。
     * <p>
     * 结构：{@code Long2ObjectMap<SortedSet<BlockDestructionProgress>>}
     * - key: BlockPos 的 long 编码（{@code pos.asLong()}）
     * - value: 该位置的所有破坏进度（同一个方块可被多个玩家同时破坏，
     *   用 SortedSet 存放，取进度最高的那个）
     */
    private static final Field DESTRUCTION_PROGRESS;

    static {
        Field f = null;
        try {
            f = LevelRenderer.class.getDeclaredField("destructionProgress");
            f.setAccessible(true);
        } catch (Exception ignored) {
            // 如果反射失败（极少情况），DESTRUCTION_PROGRESS 保持 null，
            // getDestructionStage 会直接返回 -1，裂缝功能退化为仅 FOOT 可用
        }
        DESTRUCTION_PROGRESS = f;
    }

    public StrawMatRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * 返回此方块实体的渲染包围盒，用于视锥体裁剪判断。
     * <p>
     * 草席模型是跨两格的完整几何体，只在 FOOT 位置渲染一次。
     * 默认包围盒只覆盖当前 BlockPos 一格，导致 FOOT 离开视野时
     * 即使 HEAD 在视野内，整个模型也会被裁剪。
     * <p>
     * 这里将包围盒扩展到同时覆盖 FOOT 和 HEAD 两格，
     * 保证只要任意一半在视野内，渲染器就会被调用。
     */
    @Override
    public @NotNull AABB getRenderBoundingBox(StrawMatBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        BedPart part = state.getValue(StrawMatBlock.PART);
        Direction facing = state.getValue(StrawMatBlock.FACING);
        // 计算配对方块的方向：foot→facing，head→facing.getOpposite()
        Direction neighborDir = (part == BedPart.FOOT) ? facing : facing.getOpposite();
        BlockPos neighborPos = blockEntity.getBlockPos().relative(neighborDir);
        // minmax 返回能同时容纳当前方块和配对方块的最小 AABB
        return new AABB(blockEntity.getBlockPos()).minmax(new AABB(neighborPos));
    }

    /**
     * 渲染草席方块实体。
     * <p>
     * 只在 FOOT 部分执行渲染：加载 BakedModel，按朝向旋转姿态，
     * 遍历模型的所有 RenderType 层逐层画出来。如果 HEAD 正在被破坏，
     * 额外叠加裂缝纹理。
     *
     * @param blockEntity   方块实体（FOOT 或 HEAD）
     * @param partialTick   本帧的插值进度（0.0 ~ 1.0），用于平滑动画
     * @param poseStack     姿态栈，记录当前渲染的平移/旋转/缩放
     * @param bufferSource  顶点缓冲源，从这里按 RenderType 获取 VertexConsumer
     * @param packedLight   亮度值（天空光 + 方块光的打包 int，用 {@code LightTexture.pack(sky, block)} 得到）
     * @param packedOverlay 覆盖纹理 UV（BER 中始终为 {@code OverlayTexture.NO_OVERLAY}，
     *                      裂缝不通过此参数传递）
     */
    @Override
    public void render(@NotNull StrawMatBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState blockState = blockEntity.getBlockState();

        // HEAD 半块不画任何东西——它的 BlockEntity 只用来让裂缝系统
        // 能找到这个位置有几何体可读（RenderShape 为 ENTITYBLOCK_ANIMATED）
        if (blockState.getValue(StrawMatBlock.PART) == BedPart.HEAD) {
            return;
        }

        // ---- FOOT 半块：渲染完整模型 ----
        Minecraft mc = Minecraft.getInstance();
        BlockPos footPos = blockEntity.getBlockPos();
        Direction facing = blockState.getValue(StrawMatBlock.FACING);
        BlockPos headPos = footPos.relative(facing); // HEAD 在 FOOT 前方（FACING 方向）一格

        // 从 blockstate JSON 获取烘焙后的模型（包含了旋转、UV、面片等预计算数据）
        BakedModel model = mc.getBlockRenderer().getBlockModel(blockState);

        // 检查 HEAD 半块是否正在被破坏（FOOT 的破坏由 LevelRenderer 自动处理）
        int headDestruction = getDestructionStage(mc.levelRenderer, headPos);

        // ---- 姿态变换 ----
        // 模型坐标系：foot 在原点，head 在 +X 方向（旋转前）
        // 需要把模型旋转到方块实际朝向，并平移到正确位置
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);                         // 移到方块底面中心
        poseStack.mulPose(Axis.YP.rotationDegrees(                  // 绕 Y 轴旋转
                -facing.toYRot() - 90));                           // 使模型 +X 对齐到方块朝向
        poseStack.translate(0, 0, -0.5);                           // 微调 Z 偏移

        // ---- 逐层渲染 ----
        // 一个方块模型可能属于多个 RenderType（如 solid + cutout），需要分别画
        for (RenderType renderType : model.getRenderTypes(
                blockState, RandomSource.create(), ModelData.EMPTY)) {

            // 将方块模型的 RenderType 转为 BER 场景下等价的 entity 版本
            // （如 RenderType.solid() → RenderType.entitySolid()），
            // 保证深度测试、alpha 测试等 GPU 状态与 BER 管线兼容
            RenderType entityType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityType);

            // HEAD 被破坏时，手动叠加裂缝纹理
            if (headDestruction >= 0 && headDestruction < 10) {
                // SheetedDecalTextureGenerator 是一个顶点包装器，
                // 会把裂缝贴图以世界坐标对齐的方式"贴"在模型面上
                VertexConsumer destructionConsumer = new SheetedDecalTextureGenerator(
                        mc.renderBuffers().crumblingBufferSource()
                                .getBuffer(ModelBakery.DESTROY_TYPES.get(headDestruction)),
                        poseStack.last(),   // 提供当前变换矩阵，用于计算世界空间 UV
                        1.0F                // 不调暗
                );
                // VertexMultiConsumer 把每个顶点同时写入两个 buffer：
                //   - consumer（正常渲染）
                //   - destructionConsumer（裂缝纹理叠加）
                consumer = VertexMultiConsumer.create(destructionConsumer, consumer);
            }

            // 把模型的所有面片遍历一遍，计算顶点位置/UV/光照，写入 consumer
            mc.getBlockRenderer().getModelRenderer()
                    .renderModel(
                            poseStack.last(),   // 当前变换矩阵（旋转后的姿态）
                            consumer,           // 顶点写入目标（可能已叠加裂缝）
                            blockState,         // 用于判断面片遮挡剔除
                            model,              // 烘焙好的 BakedModel
                            1.0f, 1.0f, 1.0f,  // RGB 染色（全白 = 不染色）
                            packedLight,        // 亮度值
                            packedOverlay,      // 覆盖效果（无）
                            ModelData.EMPTY,    // 不传额外模型数据
                            entityType          // 当前渲染层类型，用于选择面片
                    );
        }

        poseStack.popPose();
    }

    /**
     * 查询某个方块位置当前的破坏进度（0-9）。
     * <p>
     * 通过反射访问 LevelRenderer 内部的 {@code destructionProgress} 字段，
     * 取出该位置所有破坏者中进度最高的值。
     *
     * @param renderer LevelRenderer 实例（通过 {@code Minecraft.getInstance().levelRenderer} 获取）
     * @param pos      要查询的方块坐标
     * @return 破坏阶段（0=刚开打，9=即将破坏），未在破坏中返回 -1
     */
    @SuppressWarnings("unchecked")
    private static int getDestructionStage(LevelRenderer renderer, BlockPos pos) {
        if (DESTRUCTION_PROGRESS == null) return -1;
        try {
            // destructionProgress 的类型：
            // Long2ObjectMap<SortedSet<BlockDestructionProgress>>
            // key 是 BlockPos 的 long 编码，value 是破坏进度集合
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> map =
                    (Long2ObjectMap<SortedSet<BlockDestructionProgress>>)
                            DESTRUCTION_PROGRESS.get(renderer);

            SortedSet<BlockDestructionProgress> set = map.get(pos.asLong());
            if (set != null && !set.isEmpty()) {
                // 多个玩家同时挖同一个方块时取进度最高的
                return set.last().getProgress();
            }
        } catch (Exception ignored) {
            // 反射失败或其他异常，安全回退
        }
        return -1;
    }
}