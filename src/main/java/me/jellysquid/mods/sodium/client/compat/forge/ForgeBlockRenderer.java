package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.ForgeConfig;

import java.util.List;
import java.util.Random;

/**
 * Utility class for BlockRenderer, that delegates to the Forge lighting pipeline.
 */
public class ForgeBlockRenderer {
    private static boolean useForgeLightingPipeline = false;
    private static BlockModelRenderer forgeRenderer;

    public static void init() {
        useForgeLightingPipeline = ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
        forgeRenderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();
    }

    public static boolean useForgeLightingPipeline() {
        return useForgeLightingPipeline;
    }

    private boolean markQuads(ChunkRenderData.Builder renderData, List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return true;
        }
        for (int i = 0; i < quads.size(); i++) {
            ModelQuadView src = (ModelQuadView)quads.get(i);
            Sprite sprite = src.rubidium$getSprite();

            if (sprite != null) {
                renderData.addSprite(sprite);
            }
        }
        return false;
    }

    public boolean renderBlock(LightMode mode, BlockState state, BlockPos pos, BlockRenderView world, BakedModel model, MatrixStack stack,
                               VertexConsumer buffer, Random random, long seed, IModelData data, boolean checkSides, BlockOcclusionCache sideCache,
                               ChunkRenderData.Builder renderData) {
        if (mode == LightMode.FLAT) {
            forgeRenderer.renderModelFlat(world, model, state, pos, stack, buffer, checkSides, random, seed, OverlayTexture.DEFAULT_UV, data);
        } else {
            forgeRenderer.renderModelSmooth(world, model, state, pos, stack, buffer, checkSides, random, seed, OverlayTexture.DEFAULT_UV, data);
        }

        // Process the quads a second time for marking animated sprites and detecting emptiness
        boolean empty;

        random.setSeed(seed);
        empty = markQuads(renderData, model.getQuads(state, null, random, data));

        for(Direction side : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);
            empty = markQuads(renderData, model.getQuads(state, side, random, data));
        }

        return !empty;
    }
}
