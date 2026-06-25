package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import ru.govno.client.module.modules.UiScaleControl;

public class ScaledResolution {
   private final double scaledWidthD;
   private final double scaledHeightD;
   private int scaledWidth;
   private int scaledHeight;
   private static int scaleFactor;
   public static boolean isPushScale2;

   public ScaledResolution(Minecraft minecraftClient) {
      this.scaledWidth = minecraftClient.displayWidth;
      this.scaledHeight = minecraftClient.displayHeight;
      scaleFactor = 1;
      boolean flag = minecraftClient.isUnicode();
      int i = minecraftClient.gameSettings.guiScale;
      if (i == 0) {
         i = 1000;
      }

      while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240) {
         scaleFactor++;
      }

      if (flag && scaleFactor % 2 != 0 && scaleFactor != 1) {
         scaleFactor--;
      }

      this.scaledWidthD = (double)this.scaledWidth / (double)scaleFactor;
      this.scaledHeightD = (double)this.scaledHeight / (double)scaleFactor;
      this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
      this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
   }

   public ScaledResolution(Minecraft minecraftClient, int guiScale) {
      this.scaledWidth = minecraftClient.displayWidth;
      this.scaledHeight = minecraftClient.displayHeight;
      scaleFactor = 1;
      boolean flag = minecraftClient.isUnicode();
      int i = guiScale;
      if (guiScale == 0) {
         i = 1000;
      }

      while (scaleFactor < i && this.scaledWidth / (scaleFactor + 1) >= 320 && this.scaledHeight / (scaleFactor + 1) >= 240) {
         scaleFactor++;
      }

      if (flag && scaleFactor % 2 != 0 && scaleFactor != 1) {
         scaleFactor--;
      }

      this.scaledWidthD = (double)this.scaledWidth / (double)scaleFactor;
      this.scaledHeightD = (double)this.scaledHeight / (double)scaleFactor;
      this.scaledWidth = MathHelper.ceil(this.scaledWidthD);
      this.scaledHeight = MathHelper.ceil(this.scaledHeightD);
   }

   public int getScaledWidth() {
      return this.scaledWidth;
   }

   public int getScaledHeight() {
      return this.scaledHeight;
   }

   public double getScaledWidth_double() {
      return this.scaledWidthD;
   }

   public double getScaledHeight_double() {
      return this.scaledHeightD;
   }

   public static int getScaleFactor() {
      return scaleFactor;
   }

   public static float lpSCFactor() {
      return (float)scaleFactor / 2.0F;
   }

   private static boolean syncScaleEnabled() {
      return UiScaleControl.doSetGuiDefaultScale;
   }

   public static void setTempStandardScale(Runnable doesSyncCode) {
      int scaleGui = scaleFactor;
      int standardScale = 2;
      if (scaleGui != standardScale && syncScaleEnabled()) {
         try {
            if (Minecraft.getMinecraft().gameSettings.guiScale != standardScale) {
               Minecraft.getMinecraft().gameSettings.guiScale = standardScale;
               Minecraft.getMinecraft().entityRenderer.setupOverlayRendering(standardScale);
               isPushScale2 = true;

               try {
                  doesSyncCode.run();
               } catch (Exception var4) {
                  var4.printStackTrace();
               }

               isPushScale2 = false;
               Minecraft.getMinecraft().gameSettings.guiScale = scaleGui;
               Minecraft.getMinecraft().entityRenderer.setupOverlayRendering(scaleGui);
            } else {
               doesSyncCode.run();
            }
         } catch (Exception var5) {
            doesSyncCode.run();
         }
      } else {
         doesSyncCode.run();
      }
   }

   public static int setTempStandardScaleMouseCoord(int mouseCoord) {
      int scaleGui = scaleFactor;
      return scaleGui != 2 && syncScaleEnabled() ? (int)((float)mouseCoord * lpSCFactor()) : mouseCoord;
   }
}
