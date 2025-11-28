package net.okakuh.pepelandshop.render;

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
                            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                            .build()
            ),
            RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3.0))).build(false)
    );

    private static List<BlockPos> highlightedBlocks = new ArrayList<>();
    private static int highlightColor1 = 0xFFFFFFFF;
    private static int highlightColor2 = 0xFFFFFFFF;

    public static void highlightBlocks(List<BlockPos> blocks, DyeColor color1, DyeColor color2) {
        highlightedBlocks = new ArrayList<>(blocks);
        highlightColor1 = color1.getEntityColor() | 0xFF000000;
        highlightColor2 = color2.getEntityColor() | 0xFF000000;
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

        // Цвета для противоположных вершин:
        // Color1: minX, minY, minZ (нижняя-задняя-левая)
        // Color2: maxX, maxY, maxZ (верхняя-передняя-правая)

        // Нижняя грань (4 линии)
        // minX,minY,minZ -> maxX,minY,minZ (от color1 к смешанному)
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor1);
        buffer.vertex(model, (float) maxX, (float) minY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // maxX,minY,minZ -> maxX,minY,maxZ (от смешанного к color2)
        buffer.vertex(model, (float) maxX, (float) minY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) maxX, (float) minY, (float) maxZ).color(highlightColor2);

        // maxX,minY,maxZ -> minX,minY,maxZ (от color2 к смешанному)
        buffer.vertex(model, (float) maxX, (float) minY, (float) maxZ).color(highlightColor2);
        buffer.vertex(model, (float) minX, (float) minY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // minX,minY,maxZ -> minX,minY,minZ (от смешанного к color1)
        buffer.vertex(model, (float) minX, (float) minY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor1);

        // Верхняя грань (4 линии)
        // minX,maxY,minZ -> maxX,maxY,minZ (от смешанного к смешанному)
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) maxX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // maxX,maxY,minZ -> maxX,maxY,maxZ (от смешанного к color2)
        buffer.vertex(model, (float) maxX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) maxX, (float) maxY, (float) maxZ).color(highlightColor2);

        // maxX,maxY,maxZ -> minX,maxY,maxZ (от color2 к смешанному)
        buffer.vertex(model, (float) maxX, (float) maxY, (float) maxZ).color(highlightColor2);
        buffer.vertex(model, (float) minX, (float) maxY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // minX,maxY,maxZ -> minX,maxY,minZ (от смешанного к смешанному)
        buffer.vertex(model, (float) minX, (float) maxY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // Вертикальные ребра (4 линии)
        // minX,minY,minZ -> minX,maxY,minZ (от color1 к смешанному)
        buffer.vertex(model, (float) minX, (float) minY, (float) minZ).color(highlightColor1);
        buffer.vertex(model, (float) minX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // maxX,minY,minZ -> maxX,maxY,minZ (от смешанного к смешанному)
        buffer.vertex(model, (float) maxX, (float) minY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) maxX, (float) maxY, (float) minZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // maxX,minY,maxZ -> maxX,maxY,maxZ (от color2 к color2)
        buffer.vertex(model, (float) maxX, (float) minY, (float) maxZ).color(highlightColor2);
        buffer.vertex(model, (float) maxX, (float) maxY, (float) maxZ).color(highlightColor2);

        // minX,minY,maxZ -> minX,maxY,maxZ (от смешанного к смешанному)
        buffer.vertex(model, (float) minX, (float) minY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
        buffer.vertex(model, (float) minX, (float) maxY, (float) maxZ).color(interpolateColor(highlightColor1, highlightColor2, 0.5f));
    }

    private static int interpolateColor(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        int a = (int) (a1 + (a2 - a1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}