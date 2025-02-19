package lordfokas.cartography.feature.mapping.climate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import com.eerussianguy.blazemap.util.RenderHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import lordfokas.cartography.Cartography;
import lordfokas.cartography.utils.Colors;

public class RainfallLegendWidget implements Widget {
    private static NativeImage legend;
    private static RenderType type;

    private static RenderType getLegend() {
        if(type == null) {
            Minecraft mc = Minecraft.getInstance();
            legend = RainfallLayer.getLegend();
            DynamicTexture texture = new DynamicTexture(legend);
            ResourceLocation path = Cartography.resource("dynamic/legend/rainfall");
            mc.getTextureManager().register(path, texture);
            type = RenderType.text(path);
        }

        return type;
    }

    @Override
    public void render(PoseStack stack, int i, int j, float k) {
        if(legend == null) {
            getLegend();
        }

        int height = legend.getHeight();
        stack.translate(-28.0D, (-(height + 8)), 0.0D);
        MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        RenderHelper.fillRect(buffers, stack.last().pose(), 28.0F, (float) (height + 8), -1610612736);
        stack.pushPose();
        stack.translate(16.0D, 4.0D, 0.0D);
        RenderHelper.drawQuad(buffers.getBuffer(getLegend()), stack.last().pose(), 10.0F, (float) height);
        stack.popPose();
        Font font = Minecraft.getInstance().font;
        stack.pushPose();
        stack.translate(0.0D, 2.0D, 0.0D);
        stack.scale(0.5F, 0.5F, 1.0F);

        for(int r = (int) RainfallLayer.MAX_RAINFALL; r >= 0; r -= 100) {
            String label = String.valueOf(r);
            stack.pushPose();
            stack.translate(28 - font.width(label), 0.0D, 0.0D);
            font.drawInBatch(label, 0.0F, 0.0F, Colors.WHITE, false, stack.last().pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
            stack.popPose();
            stack.translate(0.0D, 40D, 0.0D);
        }

        stack.popPose();
        buffers.endBatch();
    }
}
