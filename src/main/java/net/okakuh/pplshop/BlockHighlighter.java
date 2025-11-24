package net.okakuh.pplshop;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static net.minecraft.client.gl.RenderPipelines.POSITION_COLOR_SNIPPET;

public class BlockHighlighter {
    private static final RenderLayer.MultiPhase BLOCK_HIGHLIGHT = RenderLayer.of(
            "block_highlight_no_depth",
            1536,
            RenderPipelines.register(
                    RenderPipeline.builder(POSITION_COLOR_SNIPPET)
                            .withLocation("pipeline/block_highlight")
                            .withVertexShader("core/position_color")
                            .withFragmentShader("core/position_color")
                            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                            .withCull(false)
                            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                            .build()
            ),
            RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.0))).build(false)
    );

    private static List<BlockPos> highlightedBlocks = new ArrayList<>();
    private static int highlightColor = 0xFFFFFFFF; // Белый по умолчанию

    public static void highlightBlocks(List<BlockPos> blocks, DyeColor color) {
        highlightedBlocks = new ArrayList<>(blocks);
        highlightColor = color.getEntityColor() | 0xFF000000;
    }

    public static void clearHighlights() {
        highlightedBlocks.clear();
    }

    public static void render(WorldRenderContext context) {
        if (highlightedBlocks.isEmpty()) return;

        var stack = context.matrixStack();
        var bufferSource = context.consumers();
        var cameraPos = context.camera().getPos();

        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        final Matrix4f model = stack.peek().getPositionMatrix();

        var buffer = bufferSource.getBuffer(BLOCK_HIGHLIGHT);

        for (BlockPos blockPos : highlightedBlocks) {
            renderBlockOutline(model, buffer, blockPos);
        }

        stack.pop();
    }

    private static void renderBlockOutline(Matrix4f model, VertexConsumer buffer, BlockPos pos) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = pos.getX() + 1;
        double maxY = pos.getY() + 1;
        double maxZ = pos.getZ() + 1;

        // Нижняя грань
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) minY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) minY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) minY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor);

        // Вертикальные ребра
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(highlightColor);

        buffer.vertex(model, (float) maxX, (float) minY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) maxY, (float) minZ).color(highlightColor);

        buffer.vertex(model, (float) maxX, (float) minY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) maxY, (float) maxZ).color(highlightColor);

        buffer.vertex(model, (float) minX, (float) minY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) maxY, (float) maxZ).color(highlightColor);

        // Верхняя грань
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) maxY, (float) minZ).color(highlightColor);
        buffer.vertex(model, (float) maxX, (float) maxY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) maxY, (float) maxZ).color(highlightColor);
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(highlightColor);
    }
}