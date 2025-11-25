package net.okakuh.pplshop;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class BlockHighlighter {
    private static List<BlockPos> highlightedBlocks = new ArrayList<>();

    private static int highlightColor = 0xFFFF00FF; // Розовый цвет

    public static void highlightBlocks(List<BlockPos> blocks, DyeColor color) {
        highlightedBlocks = new ArrayList<>(blocks);
        highlightColor = color.getEntityColor() | 0xFF000000; // Полностью непрозрачный розовый
    }

    public static void clearHighlights() {
        highlightedBlocks.clear();
    }

    public static void render(WorldRenderContext context) {
        if (highlightedBlocks.isEmpty()) return;

        MatrixStack stack = context.matrixStack();
        var bufferSource = context.consumers();
        var cameraPos = context.camera().getPos();

        stack.push();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Получаем компоненты цвета
        float red = (highlightColor >> 16 & 255) / 255.0f;
        float green = (highlightColor >> 8 & 255) / 255.0f;
        float blue = (highlightColor & 255) / 255.0f;
        float alpha = (highlightColor >> 24 & 255) / 255.0f;

        Matrix4f matrix = stack.peek().getPositionMatrix();

        // Используем debug quads для заливки граней
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderLayer.getDebugQuads());

        for (BlockPos blockPos : highlightedBlocks) {
            renderBlockFaces(matrix, vertexConsumer, blockPos, red, green, blue, alpha);
        }

        stack.pop();
    }

    private static void renderBlockFaces(Matrix4f matrix, VertexConsumer vertexConsumer, BlockPos pos,
                                         float red, float green, float blue, float alpha) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = pos.getX() + 1;
        double maxY = pos.getY() + 1;
        double maxZ = pos.getZ() + 1;

        // Рисуем все 6 граней куба как залитые квады

        // Нижняя грань (Y-)
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(red, green, blue, alpha);

        // Верхняя грань (Y+)
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(red, green, blue, alpha);

        // Северная грань (Z-)
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(red, green, blue, alpha);

        // Южная грань (Z+)
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);

        // Западная грань (X-)
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) minY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) minX, (float) maxY, (float) minZ).color(red, green, blue, alpha);

        // Восточная грань (X+)
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) minZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) maxY, (float) maxZ).color(red, green, blue, alpha);
        vertexConsumer.vertex(matrix, (float) maxX, (float) minY, (float) maxZ).color(red, green, blue, alpha);
    }
}