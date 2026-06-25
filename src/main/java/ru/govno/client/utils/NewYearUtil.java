package ru.govno.client.utils;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.govno.client.clickgui.ClickGuiScreen;
import ru.govno.client.module.Module;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class NewYearUtil {
   private static boolean active;
   private static final String prefixRes = "vegaline/system/newyear/";
   private static final String imageFormat = ".png";
   private static ResourceLocation PANEL_CLICKGUI_OVERLAY;
   private static ResourceLocation LOGO_MAINMENU;

   public static void setup(boolean setActive) {
      active = setActive;
   }

   public static void init() {
      if (active) {
         try {
            PANEL_CLICKGUI_OVERLAY = new ResourceLocation("vegaline/system/newyear/newyear_panel.png");
            LOGO_MAINMENU = new ResourceLocation("vegaline/system/newyear/newyear_mainmenulogohat.png");
            Minecraft.getMinecraft().getTextureManager().bindTexture(PANEL_CLICKGUI_OVERLAY);
            Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO_MAINMENU);
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }
   }

   public static void insertRenderPanelClickGui(float x, float y, float x2, float y2, float alphaPC, Module.Category category) {
      if (active) {
         int col = ClickGuiScreen.getColor(-324, category);
         col = Color.HSBtoRGB(
            (float)ColorUtils.getHueFromColor(col) / 360.0F,
            Math.min(ColorUtils.getSaturateFromColor(col) * 1.2F, 1.0F),
            Math.min(ColorUtils.getBrightnessFromColor(col) * 5.0F, 1.0F)
         );
         col = ColorUtils.swapAlpha(col, (float)ColorUtils.getAlphaFromColor(col) * alphaPC);
         drawImage(PANEL_CLICKGUI_OVERLAY, x, y, x2, y2, col, false, true);
      }
   }

   public static void insertRenderMainMenuLogo(float cx, float cy) {
      if (active) {
         int col = -1;
         float size = 32.0F;
         drawImage(
            LOGO_MAINMENU,
            cx - size / 2.0F + 1.0F,
            cy - size / 2.0F + 1.0F,
            cx + size / 2.0F + 1.0F,
            cy + size / 2.0F + 1.0F,
            ColorUtils.swapAlpha(ColorUtils.toDark(col, 0.1F), (float)ColorUtils.getAlphaFromColor(col) / 2.0F),
            false,
            true
         );
         drawImage(LOGO_MAINMENU, cx - size / 2.0F, cy - size / 2.0F, cx + size / 2.0F, cy + size / 2.0F, col, false, true);
      }
   }

   private static void drawImage(ResourceLocation image, float x, float y, float x2, float y2, int color, boolean bloom, boolean blur) {
      if (image != null) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferbuilder = tessellator.getBuffer();
         bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         bufferbuilder.pos((double)x, (double)y2).tex(0.0, 1.0).color(color).endVertex();
         bufferbuilder.pos((double)x2, (double)y2).tex(1.0, 1.0).color(color).endVertex();
         bufferbuilder.pos((double)x2, (double)y).tex(1.0, 0.0).color(color).endVertex();
         bufferbuilder.pos((double)x, (double)y).tex(0.0, 0.0).color(color).endVertex();
         GL11.glEnable(3042);
         GL11.glDepthMask(false);
         GL11.glDisable(3008);
         Minecraft.getMinecraft().getTextureManager().bindTexture(image);
         RenderUtils.glColor(-1);
         GL11.glBlendFunc(770, bloom ? 1 : 771);
         tessellator.draw();
         if (bloom) {
            GL11.glBlendFunc(770, 771);
         }

         GlStateManager.resetColor();
         GL11.glEnable(3008);
         GL11.glDepthMask(true);
      }
   }
}
