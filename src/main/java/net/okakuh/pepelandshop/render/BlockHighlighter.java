package net.okakuh.pepelandshop.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.List;

public class BlockHighlighter {
    private static List<BlockPos> highlightedBlocks = new ArrayList<>();
    private static int highlightColor1 = 0x80FFFFFF;
    private static int highlightColor2 = 0x80FFFFFF;

    public static void highlightBlocks(List<BlockPos> blocks, DyeColor color1, DyeColor color2) {
        highlightedBlocks = new ArrayList<>(blocks);
        int alpha = 0xCC;
        highlightColor1 = (alpha << 24) | (color1.getEntityColor() & 0x00FFFFFF);
        highlightColor2 = (alpha << 24) | (color2.getEntityColor() & 0x00FFFFFF);
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

        // Сохраняем состояние GL
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blendEnabled = GL11.glGetBoolean(GL11.GL_BLEND);

        // Отключаем depth test и запись в Z-буфер
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        var buffer = bufferSource.getBuffer(RenderLayer.getDebugQuads());
        for (BlockPos blockPos : highlightedBlocks) {
            renderFilledCube(model, buffer, blockPos);
        }

        ((VertexConsumerProvider.Immediate) bufferSource).draw();

        // Восстанавливаем состояние GL
        GL11.glDepthFunc(depthFunc);
        GL11.glDepthMask(depthMask);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (!blendEnabled) {
            GL11.glDisable(GL11.GL_BLEND);
        }

        stack.pop();
    }

    private static void renderFilledCube(Matrix4f model, VertexConsumer buf, BlockPos pos) {
        float minX = pos.getX();
        float minY = pos.getY();
        float minZ = pos.getZ();
        float maxX = pos.getX() + 1f;
        float maxY = pos.getY() + 1f;
        float maxZ = pos.getZ() + 1f;

        int c = highlightColor1;
        int a = (c >> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;

        // Фронтальная грань (minZ)
        buf.vertex(model, minX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, minZ).color(r, g, b, a);
        buf.vertex(model, minX, maxY, minZ).color(r, g, b, a);

        // Задняя грань (maxZ)
        buf.vertex(model, minX, minY, maxZ).color(r, g, b, a);
        buf.vertex(model, minX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, maxX, minY, maxZ).color(r, g, b, a);

        // Левый (minX)
        buf.vertex(model, minX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, minX, minY, maxZ).color(r, g, b, a);
        buf.vertex(model, minX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, minX, maxY, minZ).color(r, g, b, a);

        // Правый (maxX)
        buf.vertex(model, maxX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, maxX, minY, maxZ).color(r, g, b, a);

        // Нижняя (minY)
        buf.vertex(model, minX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, minY, minZ).color(r, g, b, a);
        buf.vertex(model, maxX, minY, maxZ).color(r, g, b, a);
        buf.vertex(model, minX, minY, maxZ).color(r, g, b, a);

        // Верхняя (maxY)
        buf.vertex(model, minX, maxY, minZ).color(r, g, b, a);
        buf.vertex(model, minX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, maxZ).color(r, g, b, a);
        buf.vertex(model, maxX, maxY, minZ).color(r, g, b, a);
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