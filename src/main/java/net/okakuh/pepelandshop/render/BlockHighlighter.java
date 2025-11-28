package net.okakuh.pepelandshop.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class BlockHighlighter {
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

        // Сохраняем состояние depth test
        boolean depthTestEnabled = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glLineWidth(10.0f);

        var buffer = bufferSource.getBuffer(RenderLayer.getLines());

        for (BlockPos blockPos : highlightedBlocks) {
            renderBlockOutline(model, buffer, blockPos);
        }

        ((VertexConsumerProvider.Immediate) bufferSource).draw();

        GL11.glLineWidth(1.0f);

        // Восстанавливаем depth test
        if (depthTestEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
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

        // Нижняя грань (4 линии)
        addLine(buffer, model, minX, minY, minZ, maxX, minY, minZ, highlightColor1, interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, maxX, minY, minZ, maxX, minY, maxZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), highlightColor2);
        addLine(buffer, model, maxX, minY, maxZ, minX, minY, maxZ, highlightColor2, interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, minX, minY, maxZ, minX, minY, minZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), highlightColor1);

        // Верхняя грань (4 линии)
        addLine(buffer, model, minX, maxY, minZ, maxX, maxY, minZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, maxX, maxY, minZ, maxX, maxY, maxZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), highlightColor2);
        addLine(buffer, model, maxX, maxY, maxZ, minX, maxY, maxZ, highlightColor2, interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, minX, maxY, maxZ, minX, maxY, minZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), interpolateColor(highlightColor1, highlightColor2, 0.5f));

        // Вертикальные ребра (4 линии)
        addLine(buffer, model, minX, minY, minZ, minX, maxY, minZ, highlightColor1, interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, maxX, minY, minZ, maxX, maxY, minZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), interpolateColor(highlightColor1, highlightColor2, 0.5f));
        addLine(buffer, model, maxX, minY, maxZ, maxX, maxY, maxZ, highlightColor2, highlightColor2);
        addLine(buffer, model, minX, minY, maxZ, minX, maxY, maxZ, interpolateColor(highlightColor1, highlightColor2, 0.5f), interpolateColor(highlightColor1, highlightColor2, 0.5f));
    }

    private static void addLine(VertexConsumer buffer, Matrix4f model, double x1, double y1, double z1, double x2, double y2, double z2, int color1, int color2) {
        buffer.vertex(model, (float) x1, (float) y1, (float) z1).normal(0f, 1f, 0f).color(color1);
        buffer.vertex(model, (float) x2, (float) y2, (float) z2).normal(0f, 1f, 0f).color(color2);
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