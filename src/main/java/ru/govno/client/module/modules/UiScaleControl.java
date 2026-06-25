package ru.govno.client.module.modules;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class UiScaleControl extends Module {
   public static UiScaleControl get;
   private final BoolSettings SetGuiDefaultScale;
   private final BoolSettings FontScaleAdaptation;
   private final ModeSettings ForceMinecraftScale;
   public static boolean doSetGuiDefaultScale;
   public static boolean doFontScaleAdaptation;
   public static boolean doFontScaleAdaptationPrev;
   @Nullable
   public static Boolean doSetGuiDefaultScalePrev;
   @Nullable
   public static Integer minecraftForceScale;
   @Nullable
   public static Integer oldGuiScalePreSetForce;
   private final AnimationUtils animInterLoadingFonts = new AnimationUtils(0.0F, 0.0F, 0.02F);
   private final AnimationUtils animLinePrepareLoadingFonts = new AnimationUtils(0.0F, 0.0F, 0.08F);
   private final AnimationUtils animLineProcessLoadingFonts = new AnimationUtils(0.0F, 0.0F, 0.08F);

   public UiScaleControl() {
      super("UiScaleControl", 0, Module.Category.MISC, true);
      this.settings.add(this.SetGuiDefaultScale = new BoolSettings("SetGuiDefaultScale", true, this));
      this.settings.add(this.FontScaleAdaptation = new BoolSettings("FontScaleAdaptation", false, this));
      this.settings.add(this.ForceMinecraftScale = new ModeSettings("ForceMinecraftScale", "Default", this, new String[]{"Default", "x0.5", "x1", "x2", "x3"}));
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   private static Integer minecraftForceScaleFromSetting() {
      if (get != null && get.ForceMinecraftScale != null) {
         String var0 = get.ForceMinecraftScale.getMode();
         switch (var0) {
            case "Default":
               return null;
            case "x0.5":
               return 1;
            case "x1":
               return 3;
            case "x2":
               return 4;
            case "x3":
               return 6;
         }
      }

      return null;
   }

   private static void controlScale() {
      if (mc != null && mc.gameSettings != null) {
         Integer setCurrentMinecraftGuiScale = null;
         if (minecraftForceScale != null) {
            if (mc.gameSettings.guiScale != minecraftForceScale) {
               setCurrentMinecraftGuiScale = minecraftForceScale;
            }
         } else if (oldGuiScalePreSetForce != null && mc.gameSettings.guiScale != oldGuiScalePreSetForce) {
            setCurrentMinecraftGuiScale = oldGuiScalePreSetForce;
            oldGuiScalePreSetForce = null;
         }

         if (setCurrentMinecraftGuiScale != null) {
            mc.gameSettings.guiScale = setCurrentMinecraftGuiScale;
            mc.displayGuiScreen(mc.currentScreen);
            if (Client.clickGuiScreen != null && mc.currentScreen == Client.clickGuiScreen) {
               Client.clickGuiScreen.resetPosition();
            }
         }
      }
   }

   public static void updateAll() {
      try {
         if (get != null && get.isActived()) {
            doSetGuiDefaultScale = get.SetGuiDefaultScale.getBool();
            doFontScaleAdaptation = get.FontScaleAdaptation.getBool() && Minecraft.mcResourceManagerAccess && Minecraft.launched;
            if (minecraftForceScale == null) {
               oldGuiScalePreSetForce = mc.gameSettings.guiScale;
            }

            minecraftForceScale = minecraftForceScaleFromSetting();
         } else {
            doSetGuiDefaultScale = false;
            doFontScaleAdaptation = false;
            minecraftForceScale = null;
         }

         if (doSetGuiDefaultScalePrev == null) {
            doSetGuiDefaultScalePrev = doSetGuiDefaultScale;
         } else if (doSetGuiDefaultScalePrev != doSetGuiDefaultScale) {
            doSetGuiDefaultScalePrev = doSetGuiDefaultScale;
         }

         if (doFontScaleAdaptationPrev != doFontScaleAdaptation) {
            doFontScaleAdaptationPrev = doFontScaleAdaptation;
            Fonts.triggerUpdateScale();
         }

         controlScale();
      } catch (Exception var1) {
         var1.printStackTrace();
      }
   }

   @Override
   public void onToggled(boolean actived) {
      try {
         if (!actived) {
            doSetGuiDefaultScale = false;
            doFontScaleAdaptation = false;
            minecraftForceScale = null;
         } else if (get != null) {
            doSetGuiDefaultScale = this.SetGuiDefaultScale.getBool();
            doFontScaleAdaptation = this.FontScaleAdaptation.getBool() && Minecraft.mcResourceManagerAccess && Minecraft.launched;
            if (minecraftForceScale == null) {
               oldGuiScalePreSetForce = mc.gameSettings.guiScale;
            }

            minecraftForceScale = minecraftForceScaleFromSetting();
         }

         controlScale();
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      super.onToggled(actived);
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      this.animInterLoadingFonts.to = Fonts.rescaleInProcess ? 1.0F : 0.0F;
      this.animInterLoadingFonts.getAnim();
      if (this.animInterLoadingFonts.to == 0.0F && this.animInterLoadingFonts.anim > 0.0F) {
         this.animLinePrepareLoadingFonts.to = 0.0F;
         this.animLineProcessLoadingFonts.to = 0.0F;
         this.animLinePrepareLoadingFonts.setAnim(0.0F);
         this.animLineProcessLoadingFonts.setAnim(0.0F);
      }

      if (this.animInterLoadingFonts.anim > 0.0F) {
         float preparePC = Fonts.rescaleInProcess ? (float)Fonts.rescalePrepareCurrent / (float)Fonts.rescalePreparesCount : 1.0F;
         float processPC = Fonts.rescaleInProcess ? (float)Fonts.rescaleProcessCurrent / (float)Fonts.rescaleProcessCount : 1.0F;
         this.animLinePrepareLoadingFonts.to = Math.min(preparePC * 1.05F, 1.0F);
         this.animLineProcessLoadingFonts.to = Math.min(processPC * 1.05F, 1.0F);
         float aPC = this.animInterLoadingFonts.anim * this.animInterLoadingFonts.anim;
         float progressPrepare = this.animLinePrepareLoadingFonts.getAnim();
         float progressProcess = this.animLineProcessLoadingFonts.getAnim();
         if (aPC * 255.0F >= 20.0F) {
            int col = ColorUtils.swapAlpha(-1, 255.0F * aPC);
            int lineBGCol = ColorUtils.toDark(col, 0.1F);
            String main = "Reload fonts: " + (Fonts.rescaleInProcess ? (int)(Math.min(processPC * 1.1F, 1.0F) * 100.0F) + "%" : "done");
            CFontRenderer mainFont = Fonts.noise_24;
            float mainStrW = mainFont.getStringWidth(main);
            float xCenter = (float)sr.getScaledWidth() / 2.0F;
            float y = (float)sr.getScaledHeight() / 3.75F - mainStrW / 2.0F;
            float lineH = 5.0F;
            float yStep = 4.0F;
            float w = 50.0F + 50.0F * aPC;
            float x1 = xCenter - w / 2.0F;
            float x2 = xCenter + w / 2.0F;
            GL11.glDisable(3008);
            mainFont.drawString(
               main, xCenter - mainStrW / 2.0F, y + (this.animInterLoadingFonts.to == 0.0F ? (1.0F - aPC * aPC) * mainFont.getHeight() / 2.0F : 0.0F), col
            );
            GL11.glEnable(3008);
            y += mainFont.getHeight() + yStep;
            if (!(preparePC < 1.0F) && !(progressPrepare < 0.95F)) {
               RenderUtils.drawRect((double)x1, (double)y, (double)x2, (double)(y + lineH), lineBGCol);
               RenderUtils.drawRect((double)x1, (double)y, (double)MathUtils.lerp(x1, x2, progressProcess), (double)(y + lineH), col);
               y += lineH + yStep;
            } else {
               RenderUtils.drawRect((double)x1, (double)y, (double)x2, (double)(y + lineH), lineBGCol);
               RenderUtils.drawRect((double)x1, (double)y, (double)MathUtils.lerp(x1, x2, progressPrepare), (double)(y + lineH), col);
               y += lineH + yStep;
            }
         }
      }
   }
}
