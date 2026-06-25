package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.cfg.GuiConfig;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.DisplayCheck;
import ru.govno.client.utils.HoverUtils;
import ru.govno.client.utils.RadioPlayerUtil;
import ru.govno.client.utils.RadioStationsBase;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;
import ru.govno.client.utils.Render.Vec2fColored;

public class RadioPlayer extends Module {
   private static final AnimationUtils alphaPC = new AnimationUtils(0.0F, 0.0F, 0.125F);
   public static RadioPlayer get;
   public FloatSettings WX;
   public FloatSettings WY;
   public BoolSettings Paused;
   public BoolSettings UseWidget;
   public BoolSettings WidgetOnlyInChat;
   public BoolSettings StopInClickGui;
   public BoolSettings FragEffectsSync;
   public BoolSettings MulleWhileHideGame;
   public BoolSettings AutoOrderOnFailLoad;
   public BoolSettings Visualizer;
   public BoolSettings SyncStationsListFromNet;
   public BoolSettings EnhanceBass;
   public BoolSettings EnhanceVoice;
   public BoolSettings EnhanceClarity;
   public ModeSettings VisualizerType;
   public ModeSettings RadioStation;
   public FloatSettings RadioVolume;
   public RadioPlayerUtil radioPlayer;
   private RadioPlayer.FloatArrayAnimator visualizerAnimator;
   private boolean radioStated;
   private final TimerHelper delayUpdatePlayer = TimerHelper.TimerHelperReseted();
   static int mouseX;
   static int mouseY;
   static int mouseButton;
   static boolean clicked = false;
   private final AnimationUtils setPrevAnim = new AnimationUtils(0.0F, 0.0F, 0.03333333F);
   private final AnimationUtils setNextAnim = new AnimationUtils(0.0F, 0.0F, 0.03333333F);
   private final AnimationUtils loadingRadioStreamAnim = new AnimationUtils(0.0F, 0.0F, 0.03F);
   private final TimerHelper timeOfLoading = TimerHelper.TimerHelperReseted();

   public RadioPlayer() {
      super("RadioPlayer", 0, Module.Category.MISC);
      this.settings.add(this.WX = new FloatSettings("WX", 0.01F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.WY = new FloatSettings("WY", 0.3F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.Paused = new BoolSettings("Paused", false, this, () -> false));
      this.settings.add(this.UseWidget = new BoolSettings("UseWidget", true, this));
      this.settings.add(this.WidgetOnlyInChat = new BoolSettings("WidgetOnlyInChat", false, this, () -> this.UseWidget.getBool()));
      this.settings.add(this.Visualizer = new BoolSettings("Visualizer", true, this, () -> this.UseWidget.getBool()));
      this.settings
         .add(
            this.VisualizerType = new ModeSettings(
               "VisualizerType",
               "Type-1",
               this,
               new String[]{"Type-1", "Type-2", "Type-3", "Type-4"},
               () -> this.UseWidget.getBool() && this.Visualizer.getBool()
            )
         );
      this.settings.add(this.RadioVolume = new FloatSettings("RadioVolume", 50.0F, 100.0F, 1.0F, this, () -> !this.UseWidget.getBool()).setAnimSpeed(0.125F));
      this.settings.add(this.StopInClickGui = new BoolSettings("StopInClickGui", true, this));
      this.settings.add(this.FragEffectsSync = new BoolSettings("FragEffectsSync", true, this));
      this.settings.add(this.MulleWhileHideGame = new BoolSettings("MulleWhileHideGame", true, this));
      this.settings.add(this.AutoOrderOnFailLoad = new BoolSettings("AutoOrderOnFailLoad", true, this));
      String[] stationsOFRadio = RadioStationsBase.getRadioModesForSetting();
      this.settings.add(this.RadioStation = new ModeSettings("RadioStation", stationsOFRadio[0], this, stationsOFRadio));
      this.settings.add(this.SyncStationsListFromNet = new BoolSettings("SyncStationsFromNet", false, this));
      this.settings.add(this.EnhanceBass = new BoolSettings("EnhanceBass", false, this, () -> !this.UseWidget.getBool()));
      this.settings.add(this.EnhanceVoice = new BoolSettings("EnhanceVoice", false, this, () -> !this.UseWidget.getBool()));
      this.settings.add(this.EnhanceClarity = new BoolSettings("EnhanceClarity", false, this, () -> !this.UseWidget.getBool()));
      this.setDemand(3, 3);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   private boolean isLowPassForRadio() {
      if (mc.isGamePaused()) {
         return true;
      } else {
         if (this.MulleWhileHideGame.getBool()) {
            boolean hideWindow = false;

            try {
               hideWindow = !DisplayCheck.isVisible();
            } catch (Exception var3) {
               var3.printStackTrace();
            }

            if (hideWindow) {
               return true;
            }
         }

         return mc.currentScreen != null
            && !(mc.currentScreen instanceof GuiChat)
            && !(mc.currentScreen instanceof GuiContainer)
            && !(mc.currentScreen instanceof GuiMainMenu)
            && mc.currentScreen != Client.clickGuiScreen
            && !(mc.currentScreen instanceof GuiConfig);
      }
   }

   public static void updateAll() {
      if (mc.world != null || get.radioStated) {
         if (get != null) {
            if (get.delayUpdatePlayer.hasReached(5.0)) {
               get.radioStated = true;
               if (get.radioPlayer != null) {
                  if (Panic.stop) {
                     get.radioPlayer.instantStopPlaying();
                  } else if (get.isActived()) {
                     RadioStationsBase.setUsingNET(get.SyncStationsListFromNet.getBool());
                     if (Minecraft.player != null && Minecraft.player.ticksExisted % 20 == 5) {
                        get.RadioStation.syncAsyncToNewStringArray(RadioStationsBase.getRadioModesForSetting());
                     }

                     if (!get.UseWidget.getBool() && get.Paused.getBool()) {
                        get.Paused.setBool(false);
                     }

                     get.radioPlayer.setRadioURLSmooth(RadioStationsBase.getUrlFromStationName(get.RadioStation.getMode()));
                     float vol01 = get.RadioVolume.getAnimation() / 100.0F;
                     if (mc.world == null && mc.currentScreen instanceof GuiMainMenu) {
                        vol01 *= 0.333333F;
                     }

                     vol01 *= ClientTune.getVolumeMuteMultiplierAlways();
                     get.radioPlayer
                        .setPlayingStatus(
                           !get.Paused.getBool() && !GuiMainMenu.qClicked && (!get.StopInClickGui.getBool() || mc.currentScreen != Client.clickGuiScreen),
                           vol01,
                           true
                        );
                     get.radioPlayer
                        .setEnhanceSound(get.EnhanceBass.getBool(), get.EnhanceVoice.getBool(), get.EnhanceClarity.getBool(), get.isLowPassForRadio());
                     if (get.FragEffectsSync.getBool()
                        && FragEffects.get.isActived()
                        && (FragEffects.get.strikeAnimation.anim > 0.1F || FragEffects.get.totemAnimation.anim > 0.1F)) {
                        get.radioPlayer.setInstantLowPass();
                     }

                     get.radioPlayer.updatePlayingOnTicks();
                  } else {
                     get.radioPlayer.stopPlayingUpdate(false);
                  }
               } else if (Minecraft.player != null && Minecraft.player.ticksExisted > 20) {
                  get.radioPlayer = new RadioPlayerUtil();
                  get.Paused.setBool(true);
               }

               get.delayUpdatePlayer.reset();
            }
         }
      }
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      updateAll();
      alphaPC.to = this.isActived() && (!this.WidgetOnlyInChat.getBool() || mc.currentScreen instanceof GuiChat) && this.UseWidget.getBool() ? 1.0F : 0.0F;
      float aPC = getAlphaPC();
      if (canDrawWindow() || this.UseWidget.getBool()) {
         GL11.glDepthMask(false);
         this.drawWindow(aPC);
         GL11.glDepthMask(true);
      }
   }

   public static float getAlphaPC() {
      return get == null ? 0.0F : alphaPC.getAnim();
   }

   public static boolean canDrawWindow() {
      return getAlphaPC() * 255.0F > 1.0F;
   }

   public static float[] getWindowCoord() {
      float x = 0.0F;
      float y = 0.0F;
      if (get != null) {
         ScaledResolution sr = new ScaledResolution(mc);
         x = get.currentFloatValue("WX") * (float)sr.getScaledWidth();
         x = (float)((int)(x * 2.0F)) / 2.0F;
         y = get.currentFloatValue("WY") * (float)sr.getScaledHeight();
         y = (float)((int)(y * 2.0F)) / 2.0F;
      }

      return new float[]{x, y};
   }

   public static float getWindowWidth() {
      return 90.0F;
   }

   public static float getWindowHeight() {
      return 40.0F;
   }

   public static boolean isHoveredToPanel(boolean onlyMove) {
      return canDrawWindow()
         && RenderUtils.isHovered(
            (float)mouseX, (float)mouseY, getWindowCoord()[0], getWindowCoord()[1], getWindowWidth(), onlyMove ? 24.0F : getWindowHeight()
         );
   }

   public static void updateMousePos(int mX, int mY) {
      mouseX = mX;
      mouseY = mY;
   }

   public static void callClick(int mX, int mY, int mB) {
      mouseX = mX;
      mouseY = mY;
      mouseButton = mB;
      if (isHoveredToPanel(false) && !isHoveredToPanel(true)) {
         clicked = true;
         onClickMouse();
      }
   }

   private void drawWindow(float alphaPC) {
      if (this.radioPlayer != null && get != null) {
         boolean manyGlows = Hud.get.ManyGlows.getBool();
         GL11.glPushMatrix();
         float scale = Math.min(alphaPC * 1.1F, 1.0F);
         scale = 0.9F + 0.1F * (float)MathUtils.easeInOutQuad((double)scale);
         alphaPC *= alphaPC;
         alphaPC *= MathUtils.lerp(1.0F, 0.3F, this.radioPlayer.getLowPassPC01());
         float lpSCFactor = ScaledResolution.lpSCFactor();
         boolean canDraws = alphaPC * 255.0F >= 1.0F;
         boolean canDrawText = alphaPC * 255.0F >= 33.0F;
         int bgCol = ColorUtils.toDark(ClientColors.getColor1(45, 0.55F * alphaPC), 0.125F);
         int white = ColorUtils.getColor(255, 255, 255, 235.0F * alphaPC);
         float x = getWindowCoord()[0];
         float y = getWindowCoord()[1];
         float w = getWindowWidth();
         float h = getWindowHeight();
         boolean isPlaying = this.radioPlayer.isPlaying();
         boolean loadingFail = this.timeOfLoading.hasReached(3000.0) && !isPlaying;
         boolean canNextTrackOnFail = this.AutoOrderOnFailLoad.getBool() && this.timeOfLoading.hasReached(5000.0) && !isPlaying;
         if (canNextTrackOnFail) {
            this.RadioStation.setNextModeOrder();
            this.timeOfLoading.reset();
         }

         GL11.glPushMatrix();
         RenderUtils.customScaledObject2D(x, y, w, h, scale);
         float changeAnim = MathUtils.valWave01(Math.min((this.setPrevAnim.getAnim() + this.setNextAnim.getAnim()) / 0.8F, 1.0F));
         changeAnim *= changeAnim;
         if (canDraws) {
            RenderUtils.hudRectWithString(x, y, x + w, y + h + 2.0F, "", Hud.get.HudRectMode.getMode(), alphaPC, manyGlows, 11);
         }

         float labelExtY = 0.0F;
         if (canDraws) {
            float stringRenderPC01 = 1.0F - changeAnim;
            String label = MathUtils.getStringPercent(this.RadioStation.getMode() + ":", stringRenderPC01);
            float mulAlphaStr = (this.radioPlayer.isWantToChangeUrl() || this.radioPlayer.waitLoadingUrlOrError())
                  && this.timeOfLoading.getTime() % 200L < 100L
               ? 0.2F
               : (!this.radioPlayer.isPlaying() ? 0.7F : 1.0F);
            String info = Hud.get.HudRectMode.getMode();
            switch (info) {
               case "Glow":
                  labelExtY = 7.0F;
                  break;
               case "Window":
               case "Plain":
               case "Stipple":
                  labelExtY = 4.0F;
                  break;
               case "Rail":
                  labelExtY = 4.5F;
            }

            if (canDrawText && (float)ColorUtils.getAlphaFromColor(white) * mulAlphaStr >= 33.0F) {
               if (manyGlows) {
                  Fonts.comfortaaRegular_15
                     .drawStringWithBloomAndShadow(
                        label,
                        x + w / 2.0F - Fonts.comfortaaRegular_15.getStringWidth(label) / 2.0F,
                        y + labelExtY,
                        ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * mulAlphaStr),
                        0.35F * alphaPC * mulAlphaStr,
                        8
                     );
               } else {
                  Fonts.comfortaaRegular_15
                     .drawStringWithShadow(
                        label,
                        x + w / 2.0F - Fonts.comfortaaRegular_15.getStringWidth(label) / 2.0F,
                        y + labelExtY,
                        ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * mulAlphaStr)
                     );
               }
            }

            if (canDrawText) {
               if (!loadingFail) {
                  if (!this.radioPlayer.waitLoadingUrlOrError() && !this.radioPlayer.isCurrentActiveURL(this.RadioStation.getMode())) {
                     info = MathUtils.getStringPercent(RadioStationsBase.getInfoFromStationName(this.RadioStation.getMode()), stringRenderPC01);
                  } else {
                     String append = "";
                     float timePC = (float)(System.currentTimeMillis() % 600L) / 600.0F;
                     if (timePC > 0.75F) {
                        append = "...";
                     } else if (timePC > 0.5F) {
                        append = "..";
                     } else if (timePC > 0.25F) {
                        append = ".";
                     } else {
                        append = "";
                     }

                     info = "Загрузка" + append;
                  }
               } else {
                  info = this.timeOfLoading.hasReached(4500.0) && this.AutoOrderOnFailLoad.getBool() ? "Переключение вперёд..." : "Неудалось загрузить :(";
               }

               float infoX = x + w / 2.0F - Fonts.comfortaaRegular_12.getStringWidth(info) / 2.0F;
               if (Fonts.comfortaaRegular_12.getStringWidth(info) - (w - 4.0F) > 0.0F) {
                  float pct = (float)(System.currentTimeMillis() % 6000L) / 6000.0F;
                  if (pct < 0.5F) {
                     pct *= 1.75F;
                     pct = pct > 0.5F ? 0.5F : pct;
                  }

                  pct = pct > 0.5F
                     ? (float)MathUtils.easeInOutElastic((double)((1.0F - pct) * (1.0F - pct) * 2.0F))
                     : (float)MathUtils.easeInOutQuad((double)(pct * 2.0F));
                  float stencilXOff = 1.0F;
                  String buttonX2 = Hud.get.HudRectMode.getMode();
                  switch (buttonX2) {
                     case "Glow":
                        stencilXOff = 1.0F;
                        break;
                     case "Window":
                        stencilXOff = 1.5F;
                        break;
                     case "Plain":
                        stencilXOff = 0.5F;
                        break;
                     case "Stipple":
                        stencilXOff = 0.5F;
                        break;
                     case "Rail":
                        stencilXOff = 3.0F;
                  }

                  infoX = MathUtils.lerp(x + stencilXOff + 1.0F, x - (Fonts.comfortaaRegular_12.getStringWidth(info) - w) - (stencilXOff + 1.0F), pct);
               }

               float infoAPC = 1.0F - 0.65F * this.Paused.getAnimation();
               if ((float)ColorUtils.getAlphaFromColor(white) * infoAPC >= 33.0F) {
                  StencilUtil.initStencilToWrite();
                  float stencilXOff = 1.0F;
                  String var98 = Hud.get.HudRectMode.getMode();
                  switch (var98) {
                     case "Glow":
                        stencilXOff = 1.0F;
                        break;
                     case "Window":
                        stencilXOff = 1.5F;
                        break;
                     case "Plain":
                        stencilXOff = 0.5F;
                        break;
                     case "Stipple":
                        stencilXOff = 0.5F;
                        break;
                     case "Rail":
                        stencilXOff = 3.0F;
                  }

                  RenderUtils.drawRect((double)(x + stencilXOff), (double)y, (double)(x + w - stencilXOff), (double)(y + h), -1);
                  StencilUtil.readStencilBuffer(1);
                  if (manyGlows) {
                     Fonts.comfortaaRegular_12
                        .drawStringWithBloom(
                           info,
                           infoX,
                           y + 16.5F,
                           ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * infoAPC),
                           0.7F * alphaPC * infoAPC,
                           5
                        );
                  } else {
                     Fonts.comfortaaRegular_12
                        .drawString(info, infoX, y + 16.5F, ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * infoAPC));
                  }

                  StencilUtil.uninitStencilBuffer();
               }
            }
         }

         float vizAPC = isPlaying ? this.Visualizer.getAnimation() * (1.0F - this.loadingRadioStreamAnim.getAnim()) : 0.0F;
         if (canDraws && vizAPC * 255.0F >= 1.0F) {
            try {
               StencilUtil.initStencilToWrite();
               float stencilXOff = 0.0F;
               String var69 = Hud.get.HudRectMode.getMode();
               switch (var69) {
                  case "Glow":
                     labelExtY = 3.5F;
                     break;
                  case "Window":
                     labelExtY = 12.5F;
                     break;
                  case "Plain":
                     labelExtY = 12.5F;
                     break;
                  case "Stipple":
                     labelExtY = 0.5F;
                     break;
                  case "Rail":
                     labelExtY = 0.0F;
               }

               RenderUtils.drawRect((double)(x + 1.0F), (double)(y + labelExtY), (double)(x + w - 1.0F), (double)(y + h), -1);
               StencilUtil.readStencilBuffer(1);
               List<Vec2fColored> lines = new ArrayList<>();
               float[] samples = this.radioPlayer.getTemporaryPlayerSamples(15.0F * vizAPC * this.radioPlayer.getVolume01(), true);
               if (this.visualizerAnimator == null) {
                  this.visualizerAnimator = new RadioPlayer.FloatArrayAnimator(0.45F);
               }

               this.visualizerAnimator.setSpeed(Math.min(0.125F / vizAPC, 1.0F));
               samples = this.visualizerAnimator.animatedArray(samples);
               float xExt = 5.0F;
               float yExt = 2.0F;
               float yMove = 4.0F;
               float visW = w - xExt * 2.0F;
               float visLineW = visW / (float)samples.length;
               String buttonX4 = this.VisualizerType.getMode();
               switch (buttonX4) {
                  case "Type-1":
                     float extDown = 0.0F;
                     String var122 = Hud.get.HudRectMode.getMode();
                     switch (var122) {
                        case "Glow":
                        case "Stipple":
                           extDown = 0.0F;
                           break;
                        case "Window":
                           extDown = 5.0F;
                           break;
                        case "Plain":
                           extDown = 4.0F;
                           break;
                        case "Rail":
                           extDown = 5.0F;
                     }

                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float xSample = MathUtils.lerp(x + xExt, x + w - xExt, ciclePC01);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.2F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.8F)
                              * vizAPC
                        );
                        float heightLineFromCenter = sampleValue * ((h - yExt * 2.0F - yMove) / 2.0F);
                        lines.add(
                           new Vec2fColored(
                              xSample,
                              y + h / 2.0F + extDown + yMove - yExt - heightLineFromCenter,
                              ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(lineCol))
                           )
                        );
                        lines.add(new Vec2fColored(xSample, y + h / 2.0F + extDown + yMove - yExt + heightLineFromCenter, lineCol));
                     }
                     break;
                  case "Type-2":
                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float xSample = MathUtils.lerp(x + xExt, x + w - xExt, ciclePC01);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.35F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.65F)
                              * vizAPC
                        );
                        float heightLine = sampleValue * (h / 1.3333334F - yExt * 2.0F - yMove);
                        lines.add(
                           new Vec2fColored(
                              xSample, y + h - yExt - heightLine, ColorUtils.swapAlpha(lineCol, (float)ColorUtils.getAlphaFromColor(lineCol) * 0.5F)
                           )
                        );
                        lines.add(new Vec2fColored(xSample, y + h - yExt, ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(lineCol) * 0.2F)));
                     }
                     break;
                  case "Type-3":
                     float extDownx = 0.0F;
                     String var119 = Hud.get.HudRectMode.getMode();
                     switch (var119) {
                        case "Glow":
                        case "Stipple":
                           extDownx = 0.0F;
                           break;
                        case "Window":
                           extDownx = 5.0F;
                           break;
                        case "Plain":
                           extDownx = 4.0F;
                           break;
                        case "Rail":
                           extDownx = 2.5F;
                     }

                     float xSampleC = x + w / 2.0F;
                     float ySampleC = y + h / 2.0F + yMove + extDownx;
                     float sampleRangeMin = h / 4.0F;
                     float sampleRangeMax = h / 2.0F;
                     float radExt = 5.5F;

                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.2F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.8F)
                              * vizAPC
                        );
                        float rangeSample = sampleValue * (sampleRangeMax - sampleRangeMin);
                        float radYaw = MathHelper.toRadians(ciclePC01 * (180.0F - radExt * 2.0F) - 180.0F + radExt);
                        float sinPlus = -MathHelper.sin(radYaw);
                        float cosPlus = MathHelper.cos(radYaw);
                        float sampleX1 = xSampleC + sinPlus * sampleRangeMin;
                        float sampleX2 = xSampleC + sinPlus * (sampleRangeMin + rangeSample);
                        float sampleY1 = ySampleC + cosPlus * sampleRangeMin;
                        float sampleY2 = ySampleC + cosPlus * (sampleRangeMin + rangeSample);
                        lines.add(new Vec2fColored(sampleX1, sampleY1, ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(lineCol) * 0.4F)));
                        lines.add(new Vec2fColored(sampleX2, sampleY2, lineCol));
                     }

                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.2F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.8F)
                              * vizAPC
                        );
                        float rangeSample = sampleValue * (sampleRangeMax - sampleRangeMin);
                        float radYaw = MathHelper.toRadians(180.0F - ciclePC01 * (180.0F - radExt * 2.0F) - radExt);
                        float sinPlus = -MathHelper.sin(radYaw);
                        float cosPlus = MathHelper.cos(radYaw);
                        float sampleX1 = xSampleC + sinPlus * sampleRangeMin;
                        float sampleX2 = xSampleC + sinPlus * (sampleRangeMin + rangeSample);
                        float sampleY1 = ySampleC + cosPlus * sampleRangeMin;
                        float sampleY2 = ySampleC + cosPlus * (sampleRangeMin + rangeSample);
                        lines.add(new Vec2fColored(sampleX1, sampleY1, ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(lineCol) * 0.4F)));
                        lines.add(new Vec2fColored(sampleX2, sampleY2, lineCol));
                     }
                     break;
                  case "Type-4":
                     float extDownxx = 0.0F;
                     String buttonX7 = Hud.get.HudRectMode.getMode();
                     switch (buttonX7) {
                        case "Glow":
                        case "Stipple":
                           extDownxx = 0.0F;
                           break;
                        case "Window":
                           extDownxx = 5.0F;
                           break;
                        case "Plain":
                           extDownxx = 4.0F;
                           break;
                        case "Rail":
                           extDownxx = 4.0F;
                     }

                     float xSampleCx = x + w / 2.0F;
                     float ySampleCx = y + h / 2.0F + yMove + extDownxx;
                     float sampleRangeMinx = h / 4.0F;
                     float sampleRangeMaxx = h / 2.0F;

                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.2F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.8F)
                              * vizAPC
                        );
                        float rangeSample = sampleValue * (sampleRangeMaxx - sampleRangeMinx);
                        float radYaw = MathHelper.toRadians(ciclePC01 * 180.0F - 180.0F);
                        float sinPlus = -MathHelper.sin(radYaw);
                        float cosPlus = MathHelper.cos(radYaw);
                        float sampleX2 = xSampleCx + sinPlus * (sampleRangeMinx + rangeSample);
                        float sampleY2 = ySampleCx + cosPlus * (sampleRangeMinx + rangeSample);
                        lines.add(0, new Vec2fColored(sampleX2, sampleY2, ColorUtils.swapAlpha(lineCol, (float)ColorUtils.getAlphaFromColor(lineCol))));
                     }

                     for (int i = 0; i < samples.length; i++) {
                        float ciclePC01 = Math.min((float)i / ((float)samples.length - 1.0F), 1.0F);
                        float sampleValue = (float)Math.pow((double)samples[i], 0.2F);
                        int lineCol = ClientColors.getColor1(
                           i * 6,
                           MathUtils.lerp(alphaPC * 0.1F, alphaPC, 0.2F + (float)MathUtils.easeInOutExpo((double)Math.min(sampleValue * 1.25F, 1.0F)) * 0.8F)
                              * vizAPC
                        );
                        float rangeSample = sampleValue * (sampleRangeMaxx - sampleRangeMinx);
                        float radYaw = MathHelper.toRadians(180.0F - ciclePC01 * 180.0F);
                        float sinPlus = -MathHelper.sin(radYaw);
                        float cosPlus = MathHelper.cos(radYaw);
                        float sampleX2 = xSampleCx + sinPlus * (sampleRangeMinx + rangeSample);
                        float sampleY2 = ySampleCx + cosPlus * (sampleRangeMinx + rangeSample);
                        lines.add(new Vec2fColored(sampleX2, sampleY2, ColorUtils.swapAlpha(lineCol, (float)ColorUtils.getAlphaFromColor(lineCol))));
                     }

                     lines.add(0, new Vec2fColored(xSampleCx, ySampleCx, 0));
               }

               RenderUtils.anialisON(true, true, true);
               GL11.glBlendFunc(770, 1);
               buttonX4 = this.VisualizerType.getMode();
               switch (buttonX4) {
                  case "Type-1":
                     GL11.glLineWidth(lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 5, 0.5F);
                     GL11.glLineWidth(0.25F * lpSCFactor);
                     RenderUtils.drawVec2ColoredNoSmooth(lines, 1, 0.25F);
                     GL11.glLineWidth(1.0F);
                     GL11.glPointSize(0.25F * lpSCFactor);
                     RenderUtils.drawVec2ColoredNoSmooth(lines, 0, 1.5F);
                     GL11.glPointSize(1.0F);
                     break;
                  case "Type-2":
                     GL11.glLineWidth(visLineW * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 1);
                     GL11.glLineWidth(1.0F);
                     GL11.glPointSize(visLineW / 1.75F * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 0, 0.66666F);
                     GL11.glPointSize(1.0F);
                     break;
                  case "Type-3":
                     GL11.glPointSize(0.25F * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 0);
                     GL11.glPointSize(1.0F);
                     RenderUtils.drawVec2Colored(lines, 7);
                     break;
                  case "Type-4":
                     RenderUtils.drawVec2Colored(lines, 6, 0.5F);
                     if (lines.get(0) != null) {
                        lines.remove(0);
                     }

                     GL11.glLineWidth(3.25F * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 3, 0.6F);
                     GL11.glLineWidth(0.25F * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 3);
                     GL11.glLineWidth(0.6F * lpSCFactor);
                     RenderUtils.drawVec2Colored(lines, 3);
                     GL11.glLineWidth(1.0F);
               }

               GL11.glBlendFunc(770, 771);
               RenderUtils.anialisOFF(true, true, false);
               StencilUtil.uninitStencilBuffer();
            } catch (Exception var59) {
               var59.printStackTrace();
            }
         }

         this.loadingRadioStreamAnim.speed = 0.05F;
         float loadingAPC;
         if (!this.radioPlayer.waitLoadingUrlOrError() && !this.radioPlayer.isWantToChangeUrl()) {
            loadingAPC = this.loadingRadioStreamAnim.getAnim();
            if (this.loadingRadioStreamAnim.anim > 0.9F && this.timeOfLoading.hasReached(1000.0)) {
               this.loadingRadioStreamAnim.to = 0.0F;
               this.timeOfLoading.reset();
            }

            if (this.timeOfLoading.hasReached(1000.0)) {
               this.timeOfLoading.reset();
            }
         } else {
            if (this.loadingRadioStreamAnim.anim < 0.5F) {
               this.loadingRadioStreamAnim.setAnim(0.5F);
            }

            this.loadingRadioStreamAnim.to = 1.0F;
            loadingAPC = this.loadingRadioStreamAnim.getAnim();
         }

         if (canDraws) {
            loadingAPC *= 1.0F - Math.min(this.Paused.getAnimation() + changeAnim, 1.0F);
            if (loadingAPC * alphaPC * 255.0F >= 1.0F) {
               float loadingCX = x + w / 2.0F;
               float loadingCY = y + 3.0F + h / 2.0F;
               float loadingCircleRad = (h - 3.0F) / (2.75F + (1.0F - loadingAPC));
               int colBg = ColorUtils.getColor(0, 0, 0, 80.0F * alphaPC * loadingAPC);
               int colPoints = ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * loadingAPC);
               RenderUtils.drawSmoothCircle((double)loadingCX, (double)loadingCY, loadingCircleRad, colBg);
               int points = 12;
               float pointSize = 3.5F;
               float pointSizeGlow = pointSize + 5.0F;
               float cicleAnimTime = (float)(System.currentTimeMillis() % 1000L) / 1000.0F;
               float pointsCircleRad = loadingCircleRad - pointSize / 2.0F - 2.0F;
               cicleAnimTime = ((float)MathUtils.easeInOutExpo((double)cicleAnimTime) + (float)(System.currentTimeMillis() % 3000L) / 3000.0F) % 1.0F;
               List<Vec2fColored> pointsVecs = new ArrayList<>();

               for (float yaw = 0.0F; yaw < 360.0F; yaw += 360.0F / (float)points) {
                  float ciclePC01 = yaw / 360.0F;
                  float radianYawAnim = MathHelper.toRadians((yaw + cicleAnimTime * 360.0F) % 360.0F);
                  int pointColor = ColorUtils.swapAlpha(colPoints, (float)ColorUtils.getAlphaFromColor(colPoints) * MathUtils.lerp(0.08F, 1.0F, ciclePC01));
                  pointsVecs.add(
                     new Vec2fColored(
                        loadingCX - MathHelper.sin(radianYawAnim) * pointsCircleRad, loadingCY + MathHelper.cos(radianYawAnim) * pointsCircleRad, pointColor
                     )
                  );
               }

               RenderUtils.anialisON(false, false, true);
               GL11.glPointSize(pointSize * lpSCFactor);
               RenderUtils.drawVec2ColoredNoSmooth(pointsVecs, 0);
               GL11.glPointSize(pointSizeGlow * lpSCFactor);
               RenderUtils.drawVec2ColoredNoSmooth(pointsVecs, 0, 0.2F);
               GL11.glPointSize(1.0F);
            }
         }

         loadingAPC = 16.0F;
         float buttonEqW = 3.0F;
         float buttonVolW = 22.0F;
         float buttonsXStep = 1.0F;
         float buttonsExt = 2.5F;
         float buttonX1 = x + buttonsExt;
         float buttonX2 = buttonX1 + loadingAPC + buttonsXStep;
         float buttonX3 = buttonX2 + loadingAPC + buttonsXStep;
         float buttonX4 = buttonX3 + loadingAPC + buttonsXStep;
         float buttonX5 = buttonX4 + buttonEqW + buttonsXStep;
         float buttonX6 = buttonX5 + buttonEqW + buttonsXStep;
         float buttonX7 = buttonX6 + buttonEqW + buttonsXStep;
         float buttonY1 = y + h - loadingAPC - buttonsExt + 2.0F;
         boolean hover1 = HoverUtils.isHovered(buttonX1, buttonY1, buttonX1 + loadingAPC, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover2 = HoverUtils.isHovered(buttonX2, buttonY1, buttonX2 + loadingAPC, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover3 = HoverUtils.isHovered(buttonX3, buttonY1, buttonX3 + loadingAPC, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover4 = HoverUtils.isHovered(buttonX4 - 1.0F, buttonY1, buttonX4 + buttonEqW, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover5 = HoverUtils.isHovered(buttonX5 - 1.0F, buttonY1, buttonX5 + buttonEqW, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover6 = HoverUtils.isHovered(buttonX6 - 1.0F, buttonY1, buttonX6 + buttonEqW, buttonY1 + loadingAPC, mouseX, mouseY);
         boolean hover7 = HoverUtils.isHovered(buttonX7, buttonY1, buttonX7 + buttonVolW, buttonY1 + loadingAPC, mouseX, mouseY);
         if (canDraws) {
            RenderUtils.drawAlphedRect((double)buttonX1, (double)buttonY1, (double)(buttonX1 + loadingAPC), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX2, (double)buttonY1, (double)(buttonX2 + loadingAPC), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX3, (double)buttonY1, (double)(buttonX3 + loadingAPC), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX4, (double)buttonY1, (double)(buttonX4 + buttonEqW), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX5, (double)buttonY1, (double)(buttonX5 + buttonEqW), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX6, (double)buttonY1, (double)(buttonX6 + buttonEqW), (double)(buttonY1 + loadingAPC), bgCol);
            RenderUtils.drawAlphedRect((double)buttonX7, (double)buttonY1, (double)(buttonX7 + buttonVolW), (double)(buttonY1 + loadingAPC), bgCol);
            int hoverCol = ColorUtils.getOverallColorFrom(
               ColorUtils.getColor(30, 30, 30, 120.0F * alphaPC), ColorUtils.getColor(0, 0, 0, 255.0F * alphaPC), 0.4F
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX1 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX1 + loadingAPC - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover1 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX2 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX2 + loadingAPC - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover2 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX3 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX3 + loadingAPC - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover3 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX4 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX4 + buttonEqW - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover4 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX5 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX5 + buttonEqW - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover5 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX6 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX6 + buttonEqW - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover6 ? hoverCol : bgCol
            );
            RenderUtils.drawLightContureRectSmooth(
               (double)(buttonX7 + 0.5F),
               (double)(buttonY1 + 0.5F),
               (double)(buttonX7 + buttonVolW - 0.5F),
               (double)(buttonY1 + loadingAPC - 0.5F),
               hover7 ? hoverCol : bgCol
            );
         }

         float epX = 7.0F;
         float epY = 5.5F;
         float lW = 2.0F;
         float moveX = 0.0F;
         float duplicateExt = 2.5F;
         float anim = this.setPrevAnim.getAnim();
         anim = MathUtils.lerp(anim, 1.0F, anim);
         if (anim > 0.85F) {
            get.setPrevAnim.setAnim(0.0F);
            get.setPrevAnim.to = 0.0F;
            get.RadioStation.setPrevModeOrder();
            anim = 1.0F;
         }

         if (canDraws) {
            ArrayList<Vec2fColored> verts = new ArrayList<>();
            if (anim != 0.0F) {
               float extRR = 1.0F - anim;
               int colFill = ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * extRR * 0.5F);
               RenderUtils.drawRoundedFullGradientInsideShadow(
                  buttonX1, buttonY1, buttonX1 + loadingAPC, buttonY1 + loadingAPC, loadingAPC * extRR, colFill, colFill, colFill, colFill, true
               );
               RenderUtils.drawRoundedFullGradientShadow(
                  buttonX1, buttonY1, buttonX1 + loadingAPC, buttonY1 + loadingAPC, loadingAPC * extRR / 2.0F, 0.5F, colFill, colFill, colFill, colFill, true
               );
            }

            float wave = MathUtils.valWave01(anim);
            moveX -= wave * 1.5F;
            verts.add(new Vec2fColored(buttonX1 + moveX + loadingAPC - epX, buttonY1 + epY, white));
            verts.add(new Vec2fColored(buttonX1 + moveX + epX, buttonY1 + loadingAPC / 2.0F, white));
            verts.add(new Vec2fColored(buttonX1 + moveX + loadingAPC - epX, buttonY1 + loadingAPC - epY, white));
            RenderUtils.anialisON(true, false, true);
            GL11.glLineWidth(lW * lpSCFactor);
            GL11.glPointSize(Math.max(lW - 0.25F, 0.1F) * lpSCFactor);
            RenderUtils.drawVec2ColoredNoSmooth(verts, 3);
            RenderUtils.drawVec2ColoredNoSmooth(verts, 0);
            if (wave > 0.03F) {
               GL11.glPushMatrix();
               GL11.glTranslated((double)(wave * duplicateExt), 0.0, 0.0);
               RenderUtils.drawVec2ColoredNoSmooth(verts, 3, wave * 0.5F);
               GL11.glTranslated((double)(-wave * duplicateExt * 2.0F), 0.0, 0.0);
               RenderUtils.drawVec2ColoredNoSmooth(verts, 3, wave * 0.5F);
               GL11.glPopMatrix();
            }

            GL11.glLineWidth(1.0F);
            GL11.glPointSize(1.0F);
            RenderUtils.anialisOFF(true, false, false);
         }

         boolean buttonToProgress1 = this.Paused.getBool();
         this.Paused.setAnimSpeed(0.025F);
         epY = this.Paused.getAnimation();
         if (canDraws) {
            lW = (float)MathUtils.easeInOutExpo((double)(1.0F - epY));
            boolean drawAsPause = lW > 0.5F;
            if (lW != 0.0F && lW != 1.0F) {
               duplicateExt = buttonToProgress1 ? Math.min(epY * 2.0F, 1.0F) : Math.max(Math.min((1.0F - epY) * 2.0F, 1.0F), 0.0F);
               duplicateExt = 1.0F - duplicateExt;
               if (duplicateExt != 0.0F) {
                  int colFill = ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * duplicateExt * 0.5F);
                  RenderUtils.drawRoundedFullGradientInsideShadow(
                     buttonX2, buttonY1, buttonX2 + loadingAPC, buttonY1 + loadingAPC, loadingAPC * duplicateExt, colFill, colFill, colFill, colFill, true
                  );
                  RenderUtils.drawRoundedFullGradientShadow(
                     buttonX2,
                     buttonY1,
                     buttonX2 + loadingAPC,
                     buttonY1 + loadingAPC,
                     loadingAPC * duplicateExt / 2.0F,
                     0.5F,
                     colFill,
                     colFill,
                     colFill,
                     colFill,
                     true
                  );
               }

               GL11.glPushMatrix();
               RenderUtils.customRotatedObject2D(
                  buttonX2, buttonY1, loadingAPC, loadingAPC, (double)((lW * 90.0F + (drawAsPause ? -90.0F : 0.0F)) * (float)(buttonToProgress1 ? -1 : 1))
               );
               RenderUtils.customScaledObject2D(buttonX2, buttonY1, loadingAPC, loadingAPC, 1.0F - MathUtils.valWave01(lW) * 0.75F);
            }

            if (drawAsPause) {
               duplicateExt = 6.0F;
               anim = 5.0F;
               float wStick = 1.5F;
               RenderUtils.anialisON(false, true, false);
               RenderUtils.drawRect(
                  (double)(buttonX2 + duplicateExt),
                  (double)(buttonY1 + anim),
                  (double)(buttonX2 + duplicateExt + wStick),
                  (double)(buttonY1 + loadingAPC - anim),
                  white
               );
               RenderUtils.drawRect(
                  (double)(buttonX2 + loadingAPC - duplicateExt - wStick),
                  (double)(buttonY1 + anim),
                  (double)(buttonX2 + loadingAPC - duplicateExt),
                  (double)(buttonY1 + loadingAPC - anim),
                  white
               );
               RenderUtils.anialisOFF(false, true, false);
            } else {
               duplicateExt = 5.5F;
               anim = 5.0F;
               ArrayList<Vec2fColored> vertsx = new ArrayList<>();
               vertsx.add(new Vec2fColored(buttonX2 + duplicateExt, buttonY1 + anim, white));
               vertsx.add(new Vec2fColored(buttonX2 + loadingAPC - duplicateExt, buttonY1 + loadingAPC / 2.0F, white));
               vertsx.add(new Vec2fColored(buttonX2 + duplicateExt, buttonY1 + loadingAPC - anim, white));
               RenderUtils.anialisON(false, true, false);
               RenderUtils.drawVec2ColoredNoSmooth(vertsx, 5);
               RenderUtils.anialisOFF(false, true, false);
            }

            if (lW != 0.0F && lW != 1.0F) {
               GL11.glPopMatrix();
            }
         }

         epX = 7.0F;
         epY = 5.5F;
         lW = 2.0F;
         moveX = 0.0F;
         duplicateExt = 2.5F;
         anim = this.setNextAnim.getAnim();
         anim = MathUtils.lerp(anim, 1.0F, anim);
         if (anim > 0.85F) {
            get.setNextAnim.setAnim(0.0F);
            get.setNextAnim.to = 0.0F;
            get.RadioStation.setNextModeOrder();
            anim = 1.0F;
         }

         if (canDraws) {
            ArrayList<Vec2fColored> vertsx = new ArrayList<>();
            if (anim != 0.0F) {
               float extRR = 1.0F - anim;
               int colFill = ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * extRR * 0.5F);
               RenderUtils.drawRoundedFullGradientInsideShadow(
                  buttonX3, buttonY1, buttonX3 + loadingAPC, buttonY1 + loadingAPC, loadingAPC * extRR, colFill, colFill, colFill, colFill, true
               );
               RenderUtils.drawRoundedFullGradientShadow(
                  buttonX3, buttonY1, buttonX3 + loadingAPC, buttonY1 + loadingAPC, loadingAPC * extRR / 2.0F, 0.5F, colFill, colFill, colFill, colFill, true
               );
            }

            float wave = MathUtils.valWave01(anim);
            moveX += wave * 1.5F;
            vertsx.add(new Vec2fColored(buttonX3 + moveX + epX, buttonY1 + epY, white));
            vertsx.add(new Vec2fColored(buttonX3 + moveX + loadingAPC - epX, buttonY1 + loadingAPC / 2.0F, white));
            vertsx.add(new Vec2fColored(buttonX3 + moveX + epX, buttonY1 + loadingAPC - epY, white));
            RenderUtils.anialisON(true, false, true);
            GL11.glLineWidth(lW * lpSCFactor);
            GL11.glPointSize(Math.max(lW - 0.25F, 0.1F) * lpSCFactor);
            RenderUtils.drawVec2ColoredNoSmooth(vertsx, 3);
            RenderUtils.drawVec2ColoredNoSmooth(vertsx, 0);
            if (wave > 0.03F) {
               GL11.glPushMatrix();
               GL11.glTranslated((double)(wave * duplicateExt), 0.0, 0.0);
               RenderUtils.drawVec2ColoredNoSmooth(vertsx, 3, wave * 0.5F);
               GL11.glTranslated((double)(-wave * duplicateExt * 2.0F), 0.0, 0.0);
               RenderUtils.drawVec2ColoredNoSmooth(vertsx, 3, wave * 0.5F);
               GL11.glPopMatrix();
            }

            GL11.glLineWidth(1.0F);
            GL11.glPointSize(1.0F);
            RenderUtils.anialisOFF(true, false, false);
         }

         epX = 0.0F;
         epY = 0.0F;
         if (canDraws) {
            epX = this.EnhanceBass.getAnimation();
            epY = this.EnhanceVoice.getAnimation();
            if (epX > 0.1F) {
               lW = buttonY1 + 0.5F + loadingAPC / 2.0F * (1.0F - epX);
               moveX = buttonY1 - 0.5F + loadingAPC / 2.0F * (1.0F + epX);
               int bassCol = ColorUtils.getColor(255, 90, 70, 255.0F * alphaPC * epX);
               RenderUtils.drawAlphedRect((double)(buttonX4 + 0.5F), (double)lW, (double)(buttonX4 + buttonEqW - 0.5F), (double)moveX, bassCol);
               RenderUtils.drawAlphedRect(
                  (double)(buttonX4 + 1.0F),
                  (double)(lW + 0.5F),
                  (double)(buttonX4 + buttonEqW - 1.0F),
                  (double)(moveX - 0.5F),
                  ColorUtils.toDark(bassCol, 0.3F)
               );
            }
         }

         lW = 0.0F;
         if (canDraws) {
            lW = this.EnhanceClarity.getAnimation();
            if (epY == 0.0F) {
               epY = this.EnhanceVoice.getAnimation();
            }

            if (epY > 0.1F) {
               moveX = buttonY1 + 0.5F + loadingAPC / 2.0F * (1.0F - epY);
               duplicateExt = buttonY1 - 0.5F + loadingAPC / 2.0F * (1.0F + epY);
               int voiceCol = ColorUtils.getColor(155, 110, 255, 255.0F * alphaPC * epY);
               RenderUtils.drawAlphedRect((double)(buttonX5 + 0.5F), (double)moveX, (double)(buttonX5 + buttonEqW - 0.5F), (double)duplicateExt, voiceCol);
               RenderUtils.drawAlphedRect(
                  (double)(buttonX5 + 1.0F),
                  (double)(moveX + 0.5F),
                  (double)(buttonX5 + buttonEqW - 1.0F),
                  (double)(duplicateExt - 0.5F),
                  ColorUtils.toDark(voiceCol, 0.3F)
               );
            }
         }

         if (canDraws) {
            if (lW == 0.0F) {
               lW = this.EnhanceClarity.getAnimation();
            }

            if (lW > 0.1F) {
               moveX = buttonY1 + 0.5F + loadingAPC / 2.0F * (1.0F - lW);
               duplicateExt = buttonY1 - 0.5F + loadingAPC / 2.0F * (1.0F + lW);
               int clarityCol = ColorUtils.getColor(140, 255, 255, 255.0F * alphaPC * lW);
               RenderUtils.drawAlphedRect((double)(buttonX6 + 0.5F), (double)moveX, (double)(buttonX6 + buttonEqW - 0.5F), (double)duplicateExt, clarityCol);
               RenderUtils.drawAlphedRect(
                  (double)(buttonX6 + 1.0F),
                  (double)(moveX + 0.5F),
                  (double)(buttonX6 + buttonEqW - 1.0F),
                  (double)(duplicateExt - 0.5F),
                  ColorUtils.toDark(clarityCol, 0.3F)
               );
            }
         }

         if (canDraws) {
            if (epX > 0.99F && epY > 0.99F) {
               RenderUtils.drawAlphedVGradient(
                  (double)(buttonX4 + buttonEqW - 0.5F),
                  (double)(buttonY1 + loadingAPC / 2.0F - 0.5F),
                  (double)(buttonX5 + 0.5F),
                  (double)(buttonY1 + loadingAPC / 2.0F + 0.5F),
                  ColorUtils.getColor(255, 90, 70, 255.0F * alphaPC),
                  ColorUtils.getColor(155, 110, 255, 255.0F * alphaPC)
               );
            }

            if (epY > 0.99F && lW > 0.99F) {
               RenderUtils.drawAlphedVGradient(
                  (double)(buttonX5 + buttonEqW - 0.5F),
                  (double)(buttonY1 + loadingAPC / 2.0F - 0.5F),
                  (double)(buttonX6 + 0.5F),
                  (double)(buttonY1 + loadingAPC / 2.0F + 0.5F),
                  ColorUtils.getColor(155, 110, 255, 255.0F * alphaPC),
                  ColorUtils.getColor(140, 255, 255, 255.0F * alphaPC)
               );
            }
         }

         if (canDraws) {
            moveX = Math.max(Math.min(this.RadioVolume.getAnimation() / 100.0F * 1.001F, 1.0F), 0.0F);
            duplicateExt = 2.0F;
            anim = 2.0F;
            float dialYExt = 6.0F;
            float lWx = 0.5F;
            int bgVolCol = ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC);
            int textCol = ColorUtils.swapAlpha(white, (float)ColorUtils.getAlphaFromColor(white) * 0.5F);
            ArrayList<Vec2fColored> vertsxx = new ArrayList<>();
            RenderUtils.anialisON(true, true, false);
            vertsxx.add(new Vec2fColored(buttonX7 + duplicateExt, buttonY1 + loadingAPC - anim, bgVolCol));
            vertsxx.add(new Vec2fColored(buttonX7 + buttonVolW - duplicateExt, buttonY1 + loadingAPC - anim, bgVolCol));
            vertsxx.add(new Vec2fColored(buttonX7 + buttonVolW - duplicateExt, buttonY1 + anim + dialYExt, bgVolCol));
            GL11.glLineWidth(lWx * lpSCFactor);
            RenderUtils.drawVec2Colored(vertsxx, 2, 0.3F);
            RenderUtils.drawVec2Colored(vertsxx, 5, 0.08F);
            GL11.glLineWidth(1.0F);
            vertsxx.clear();
            float exponentVol01 = MathUtils.lerp(moveX, 1.0F, moveX / 2.5F);
            vertsxx.add(new Vec2fColored(buttonX7 + duplicateExt, buttonY1 + loadingAPC - anim, white));
            vertsxx.add(
               new Vec2fColored(buttonX7 + MathUtils.lerp(duplicateExt, buttonVolW - duplicateExt, exponentVol01), buttonY1 + loadingAPC - anim, white)
            );
            vertsxx.add(
               new Vec2fColored(
                  buttonX7 + MathUtils.lerp(duplicateExt, buttonVolW - duplicateExt, exponentVol01),
                  buttonY1 + MathUtils.lerp(loadingAPC - anim, anim + dialYExt, exponentVol01),
                  white
               )
            );
            GL11.glLineWidth(lpSCFactor);
            RenderUtils.drawVec2Colored(vertsxx, 2);
            GL11.glLineWidth(1.0F);
            RenderUtils.drawVec2Colored(vertsxx, 5);
            RenderUtils.anialisOFF(true, true, false);
            if ((float)ColorUtils.getAlphaFromColor(textCol) >= 33.0F) {
               Fonts.mntsb_10.drawString("VOL", buttonX7 + duplicateExt, buttonY1 + anim + 2.0F, textCol);
               String volPC100 = (int)(moveX * 100.0F) + "%";
               Fonts.mntsb_7.drawString(volPC100, buttonX7 + duplicateExt, buttonY1 + anim + 7.0F, textCol);
            }
         }

         if (canDraws
            && HoverUtils.isHovered(buttonX7, buttonY1, buttonX7 + buttonVolW, buttonY1 + loadingAPC, mouseX, mouseY)
            && Mouse.isButtonDown(0)
            && mc.currentScreen instanceof GuiChat) {
            moveX = 1.5F;
            buttonVolW -= moveX;
            duplicateExt = MathUtils.clamp(((float)mouseX - moveX - buttonX7) / buttonVolW * (1.0F + 1.5F / buttonVolW) * 100.0F, 1.0F, 100.0F);
            buttonVolW += moveX;
            get.RadioVolume.setFloat(duplicateExt);
         }

         GL11.glPopMatrix();
         GL11.glPopMatrix();
      }
   }

   public static void onClickMouse() {
      if (get != null) {
         if (clicked) {
            float x = getWindowCoord()[0];
            float y = getWindowCoord()[1];
            float w = getWindowWidth();
            float h = getWindowHeight();
            if (mouseButton == 0) {
               float buttonVH = 16.0F;
               float buttonEqW = 3.0F;
               float buttonVolW = 22.0F;
               float buttonsXStep = 1.0F;
               float buttonsExt = 2.5F;
               float buttonX1 = x + buttonsExt;
               float buttonX2 = buttonX1 + buttonVH + buttonsXStep;
               float buttonX3 = buttonX2 + buttonVH + buttonsXStep;
               float buttonX4 = buttonX3 + buttonVH + buttonsXStep;
               float buttonX5 = buttonX4 + buttonEqW + buttonsXStep;
               float buttonX6 = buttonX5 + buttonEqW + buttonsXStep;
               float buttonX7 = buttonX6 + buttonEqW + buttonsXStep;
               float buttonY1 = y + h - buttonVH - buttonsExt + 2.0F;
               if (HoverUtils.isHovered(buttonX1, buttonY1, buttonX1 + buttonVH, buttonY1 + buttonVH, mouseX, mouseY)
                  && MathUtils.getDifferenceOf(get.Paused.getAnimation(), 0.5F) > 0.4F) {
                  get.setPrevAnim.setAnim(0.0F);
                  get.setPrevAnim.to = 1.0F;
                  get.Paused.setBool(false);
               }

               if (HoverUtils.isHovered(buttonX2, buttonY1, buttonX2 + buttonVH, buttonY1 + buttonVH, mouseX, mouseY)
                  && MathUtils.getDifferenceOf(get.Paused.getAnimation(), 0.5F) > 0.4F) {
                  get.Paused.setBool(!get.Paused.getBool());
               }

               if (HoverUtils.isHovered(buttonX3, buttonY1, buttonX3 + buttonVH, buttonY1 + buttonVH, mouseX, mouseY)
                  && MathUtils.getDifferenceOf(get.Paused.getAnimation(), 0.5F) > 0.4F) {
                  get.setNextAnim.setAnim(0.0F);
                  get.setNextAnim.to = 1.0F;
                  get.Paused.setBool(false);
               }

               if (HoverUtils.isHovered(buttonX4 - 1.0F, buttonY1, buttonX4 + buttonEqW, buttonY1 + buttonVH, mouseX, mouseY)) {
                  get.EnhanceBass.setBool(!get.EnhanceBass.getBool());
               }

               if (HoverUtils.isHovered(buttonX5 - 1.0F, buttonY1, buttonX5 + buttonEqW, buttonY1 + buttonVH, mouseX, mouseY)) {
                  get.EnhanceVoice.setBool(!get.EnhanceVoice.getBool());
               }

               if (HoverUtils.isHovered(buttonX6 - 1.0F, buttonY1, buttonX6 + buttonEqW, buttonY1 + buttonVH, mouseX, mouseY)) {
                  get.EnhanceClarity.setBool(!get.EnhanceClarity.getBool());
               }

               if (HoverUtils.isHovered(buttonX7, buttonY1, buttonX7 + buttonVolW, buttonY1 + buttonVH, mouseX, mouseY)) {
                  float set = MathUtils.clamp(((float)mouseX - buttonX7) / buttonVolW * (1.0F + 1.5F / buttonVolW) * 100.0F, 1.0F, 100.0F);
                  get.RadioVolume.setFloat(set);
               }
            }
         }

         clicked = false;
      }
   }

   public static void callWhell(boolean plus) {
      if (isHoveredToPanel(false)) {
         float x = getWindowCoord()[0];
         float y = getWindowCoord()[1];
         float w = getWindowWidth();
         float h = getWindowHeight();
         float buttonVH = 16.0F;
         float buttonEqW = 3.0F;
         float buttonVolW = 22.0F;
         float buttonsXStep = 1.0F;
         float buttonsExt = 2.5F;
         float buttonX1 = x + buttonsExt;
         float buttonX2 = buttonX1 + buttonVH + buttonsXStep;
         float buttonX3 = buttonX2 + buttonVH + buttonsXStep;
         float buttonX4 = buttonX3 + buttonVH + buttonsXStep;
         float buttonX5 = buttonX4 + buttonEqW + buttonsXStep;
         float buttonX6 = buttonX5 + buttonEqW + buttonsXStep;
         float buttonX7 = buttonX6 + buttonEqW + buttonsXStep;
         float buttonY1 = y + h - buttonVH - buttonsExt + 2.0F;
         if (HoverUtils.isHovered(buttonX7, buttonY1, buttonX7 + buttonVolW, buttonY1 + buttonVH, mouseX, mouseY)) {
            float set = MathUtils.clamp(get.RadioVolume.getFloat() + 0.5F + (plus ? -5.0F : 5.0F), 1.0F, 100.0F);
            if ((int)set % 5 != 0) {
               set--;
            }

            if (set < 1.0F) {
               set = 1.0F;
            }

            get.RadioVolume.setFloat(set);
         }
      }
   }

   private class FloatArrayAnimator {
      private final List<AnimationUtils> animations = new ArrayList<>();
      private float speed;

      public FloatArrayAnimator(float speed) {
         this.speed = speed;
      }

      public void setSpeed(float speed) {
         this.speed = speed;
      }

      public float[] animatedArray(float[] arrayIn) {
         if (arrayIn != null && arrayIn.length != 0) {
            while (this.animations.size() < arrayIn.length) {
               this.animations.add(new AnimationUtils(0.0F, 0.0F, this.speed));
            }

            this.animations.removeIf(animationx -> this.animations.size() > arrayIn.length);
            int index = 0;

            for (AnimationUtils animation : this.animations) {
               animation.speed = this.speed;
               float valueTo = arrayIn[index];
               animation.to = valueTo;
               if (animation.to == 0.0F && animation.anim == 0.0F && valueTo != 0.0F) {
                  animation.setAnim(valueTo);
                  arrayIn[index] = valueTo;
               } else {
                  arrayIn[index] = animation.getAnim();
               }

               index++;
            }

            return arrayIn;
         } else {
            return arrayIn;
         }
      }
   }
}
