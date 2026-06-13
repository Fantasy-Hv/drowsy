package com.github.tickstudio.drowsy.client.render;

import com.github.tickstudio.drowsy.server.domain.block.AlpacaBedBlock;
import com.github.tickstudio.drowsy.server.domain.block.AlpacaBedBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.SortedSet;

/**
 * 羊驼床方块的方块实体渲染器（Block Entity Renderer）。
 * <p>
 * 羊驼床是双格（1×2）方块，模型文件是一整块跨两格的几何体。
 * 渲染策略：只在 FOOT（脚部）方块实体上画一次完整模型，HEAD（头部）
 * 方块实体不画任何东西，只用于让裂缝系统有模型可读。
 * <p>
 * 裂缝（破坏进度动画）：
 * - 破坏 FOOT 时：LevelRenderer 自动检测并叠加裂缝，无需额外处理
 * - 破坏 HEAD 时：通过反射读取 LevelRenderer 内部的破坏进度数据，
 *   用 SheetedDecalTextureGenerator 手动把裂缝纹理叠加到 FOOT 的模型上，
 *   保证无论破坏哪半边都能在两个半块上看到裂缝动画。
 *
 * @see AlpacaBedBlock 对应的方块类
 * @see AlpacaBedBlockEntity 对应的方块实体类
 */
@OnlyIn(Dist.CLIENT)
public class AlpacaBedRenderer implements BlockEntityRenderer<AlpacaBedBlockEntity> {

    private static final Field DESTRUCTION_PROGRESS;

    static {
        Field f = null;
        try {
            f = LevelRenderer.class.getDeclaredField("destructionProgress");
            f.setAccessible(true);
        } catch (Exception ignored) {
        }
        DESTRUCTION_PROGRESS = f;
    }

    public AlpacaBedRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@NotNull AlpacaBedBlockEntity blockEntity, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        BlockState blockState = blockEntity.getBlockState();

        if (blockState.getValue(AlpacaBedBlock.PART) == BedPart.HEAD) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        BlockPos footPos = blockEntity.getBlockPos();
        Direction facing = blockState.getValue(AlpacaBedBlock.FACING);
        BlockPos headPos = footPos.relative(facing);

        BakedModel model = mc.getBlockRenderer().getBlockModel(blockState);

        int headDestruction = getDestructionStage(mc.levelRenderer, headPos);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);  // 移到方块底面中心
        poseStack.mulPose(Axis.YP.rotationDegrees(  // 绕 Y 轴旋转
                -facing.toYRot() - 180));            // 使模型 +X 对齐到方块朝向
        poseStack.translate(-0.5, 0, -0.5);   // 微调 Z 偏移

        for (RenderType renderType : model.getRenderTypes(
                blockState, RandomSource.create(), ModelData.EMPTY)) {

            RenderType entityType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityType);

            if (headDestruction >= 0 && headDestruction < 10) {
                VertexConsumer destructionConsumer = new SheetedDecalTextureGenerator(
                        mc.renderBuffers().crumblingBufferSource()
                                .getBuffer(ModelBakery.DESTROY_TYPES.get(headDestruction)),
                        poseStack.last(),
                        1.0F
                );
                consumer = VertexMultiConsumer.create(destructionConsumer, consumer);
            }

            mc.getBlockRenderer().getModelRenderer()
                    .renderModel(
                            poseStack.last(),
                            consumer,
                            blockState,
                            model,
                            1.0f, 1.0f, 1.0f,
                            packedLight,
                            packedOverlay,
                            ModelData.EMPTY,
                            entityType
                    );
        }

        poseStack.popPose();
    }

    @SuppressWarnings("unchecked")
    private static int getDestructionStage(LevelRenderer renderer, BlockPos pos) {
        if (DESTRUCTION_PROGRESS == null) return -1;
        try {
            Long2ObjectMap<SortedSet<BlockDestructionProgress>> map =
                    (Long2ObjectMap<SortedSet<BlockDestructionProgress>>)
                            DESTRUCTION_PROGRESS.get(renderer);

            SortedSet<BlockDestructionProgress> set = map.get(pos.asLong());
            if (set != null && !set.isEmpty()) {
                return set.last().getProgress();
            }
        } catch (Exception ignored) {
        }
        return -1;
    }
}
