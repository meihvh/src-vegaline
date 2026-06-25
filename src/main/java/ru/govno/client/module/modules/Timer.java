package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec2f;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.TPSDetect;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class Timer extends Module {
   public static Timer get;
   public FloatSettings TX;
   public FloatSettings TY;
   public FloatSettings Increase;
   public FloatSettings Randomize;
   public FloatSettings TimeOutMS;
   public FloatSettings BoundUp;
   public BoolSettings TimeOut;
   public BoolSettings DisablePostExpire;
   public BoolSettings Stamina;
   public BoolSettings NCPBypass;
   public BoolSettings MatrixBypass;
   public BoolSettings PhantomDash;
   public BoolSettings SmoothWastage;
   public BoolSettings DrawSmart;
   public ModeSettings StaminaMode;
   public ModeSettings Render;
   public ModeSettings TimerSFX;
   private final boolean sneakSpoof = false;
   private static final TimerHelper afkWait = new TimerHelper();
   private static final TimerHelper timeOutWait = new TimerHelper();
   private boolean afk = true;
   private boolean phantomIsRegening;
   private float yaw;
   private float pitch;
   private static float forceTimer = 1.0F;
   private boolean smartGo;
   private boolean critical;
   private boolean panicRegen;
   public static double percent = 1.0;
   public static AnimationUtils percentSmooth = new AnimationUtils(1.0F, 1.0F, 0.12F);
   public static AnimationUtils smoothInt9 = new AnimationUtils(9.0F, 9.0F, 0.06F);
   private static final AnimationUtils toShowPC = new AnimationUtils(0.0F, 0.0F, 0.15F);
   private final AnimationUtils maxTriggerAnim = new AnimationUtils(0.0F, 0.0F, 0.03F);
   private final AnimationUtils minTriggerAnim = new AnimationUtils(0.0F, 0.0F, 0.03F);
   public static boolean isRegening = false;
   public static boolean isCharging = false;
   public static float lastChargingFinishValue;
   public static boolean forceWastage = false;
   private final TimerHelper sfxDelay = new TimerHelper();
   private final int sneakTicks = 0;
   public static boolean cancel = false;
   public static float x;
   public static float y;
   protected static final ResourceLocation BATTARY_BASE = new ResourceLocation("vegaline/modules/timer/battary_base.png");
   protected static final ResourceLocation BATTARY_OVERLAY = new ResourceLocation("vegaline/modules/timer/battary_overlay.png");
   protected static final ResourceLocation WAIST_BASE = new ResourceLocation("vegaline/modules/timer/waist_base.png");
   protected static final ResourceLocation WAIST_OVERLAY = new ResourceLocation("vegaline/modules/timer/waist_overlay.png");
   protected final Tessellator tessellator = Tessellator.getInstance();
   protected final BufferBuilder buffer = this.tessellator.getBuffer();

   public Timer() {
      super("Timer", 0, Module.Category.MOVEMENT);
      this.settings.add(this.TX = new FloatSettings("TX", 0.5F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.TY = new FloatSettings("TY", 0.8F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.Increase = new FloatSettings("Increase", 2.0F, 4.0F, 0.1F, this, () -> !this.NCPBypass.getBool() || this.Stamina.getBool()));
      this.settings.add(this.Randomize = new FloatSettings("Randomize", 0.7F, 3.0F, 0.0F, this, () -> !this.NCPBypass.getBool() || this.Stamina.getBool()));
      this.settings.add(this.TimeOut = new BoolSettings("TimeOut", false, this));
      this.settings.add(this.TimeOutMS = new FloatSettings("TimeOutMS", 220.0F, 1000.0F, 1.0F, this, () -> this.TimeOut.getBool()));
      this.settings.add(this.Stamina = new BoolSettings("Stamina", true, this));
      this.settings.add(this.BoundUp = new FloatSettings("BoundUp", 0.05F, 0.9F, 0.0F, this, () -> this.Stamina.getBool()));
      this.settings.add(this.NCPBypass = new BoolSettings("NCPBypass", false, this, () -> !this.Stamina.getBool()));
      this.settings
         .add(
            this.StaminaMode = new ModeSettings(
               "StaminaMode", "Matrix", this, new String[]{"Matrix", "NCP", "Other", "Vulcan", "MatrixLow&Ncp"}, () -> this.Stamina.getBool()
            )
         );
      this.settings
         .add(this.MatrixBypass = new BoolSettings("MatrixBypass", false, this, () -> this.Stamina.getBool() && this.StaminaMode.getMode().contains("Matrix")));
      this.settings.add(this.DisablePostExpire = new BoolSettings("DisablePostExpire", false, this, () -> this.Stamina.getBool()));
      this.settings.add(this.SmoothWastage = new BoolSettings("SmoothWastage", false, this, () -> this.Stamina.getBool()));
      this.settings.add(this.PhantomDash = new BoolSettings("PhantomDash", true, this, () -> this.Stamina.getBool()));
      this.settings.add(this.DrawSmart = new BoolSettings("DrawSmart", true, this, () -> this.Stamina.getBool()));
      this.settings
         .add(
            this.Render = new ModeSettings(
               "Render",
               "SmoothNine",
               this,
               new String[]{"Line", "Plate", "Circle", "SmoothNine", "LongLine"},
               () -> this.Stamina.getBool() && this.DrawSmart.getBool()
            )
         );
      this.settings.add(this.TimerSFX = new ModeSettings("TimerSFX", "SF", this, new String[]{"None", "Dev", "SF"}, () -> this.Stamina.getBool()));
      this.setDemand(2, 2);
      get = this;
   }

   private float getPhantomSneakSlowing() {
      return 0.5F;
   }

   private boolean canPhantomSlowing() {
      return Minecraft.player.onGround
         && Minecraft.player.isSneaking()
         && !Minecraft.player.isJumping()
         && percent < 1.0
         && !this.afk
         && !this.isMatrixBypass();
   }

   private static void forceWastage() {
      forceWastage = true;
   }

   public void setTempCancel() {
      cancel = true;
   }

   public static void forceTimer(float value) {
      if (get.Stamina.getBool()) {
         afkWait.reset();
         get.afk = false;
         forceTimer = value;
         forceWastage();
      }
   }

   public static boolean canDrawTimer() {
      return get != null && get.Stamina.getBool() && get.DrawSmart.getBool();
   }

   public static float getWidth() {
      return get.Render.currentMode.equalsIgnoreCase("LongLine")
         ? 100.0F
         : (
            get.Render.currentMode.equalsIgnoreCase("SmoothNine")
               ? 18.0F
               : (get.Render.currentMode.equalsIgnoreCase("Plate") ? 28.0F : (get.Render.currentMode.equalsIgnoreCase("Line") ? 40.0F : 19.0F))
         );
   }

   public static float getHeight() {
      float ext = mc.currentScreen instanceof GuiChat ? 6.0F : 0.0F;
      return get.Render.currentMode.equalsIgnoreCase("LongLine")
         ? 9.0F + ext
         : (
            get.Render.currentMode.equalsIgnoreCase("SmoothNine")
               ? 18.0F
               : (
                  get.Render.currentMode.equalsIgnoreCase("Plate")
                     ? 40.0F
                     : (get.Render.currentMode.equalsIgnoreCase("Line") ? 1.5F + toShowPC.getAnim() * 3.0F + ext : 19.0F)
               )
         );
   }

   public static float[] getCoordsSettings() {
      return new float[]{get.TX.getFloat(), get.TY.getFloat()};
   }

   public static float getX(ScaledResolution sr) {
      return (float)sr.getScaledWidth() * getCoordsSettings()[0] - getWidth() / 2.0F;
   }

   public static float getY(ScaledResolution sr) {
      return (float)sr.getScaledHeight() * getCoordsSettings()[1] - getHeight() / 2.0F;
   }

   public static void setSetsX(float set) {
      ((FloatSettings)get.settings.get(0)).setFloat(set);
   }

   public static void setSetsY(float set) {
      ((FloatSettings)get.settings.get(1)).setFloat(set);
   }

   public static boolean isHoveredToTimer(int mouseX, int mouseY, ScaledResolution sr) {
      return canDrawTimer() && RenderUtils.isHovered((float)mouseX, (float)mouseY, getX(sr), getY(sr), getWidth(), getHeight());
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      float alphaPC = this.Stamina.getAnimation() * this.DrawSmart.getAnimation();
      if (alphaPC != 0.0F) {
         float lpSCFactor = ScaledResolution.lpSCFactor();
         String mode = this.Render.currentMode;
         float x = getX(sr);
         float y = getY(sr);
         float w = getWidth();
         float h = getHeight();
         if (this.Render.getMode().equalsIgnoreCase("LongLine")) {
            float ext = mc.currentScreen instanceof GuiChat ? 6.0F : 0.0F;
            y += ext / 2.0F;
            h -= ext;
         }

         float dx = (float)sr.getScaledWidth() / 2.0F - (x + w / 2.0F);
         float dy = (float)sr.getScaledHeight() / 2.0F - (y + h / 2.0F);
         boolean middle = Math.sqrt((double)(dx * dx + dy * dy)) < 2.0 && !mode.equalsIgnoreCase("Plate");
         if (middle) {
            if (mode.equalsIgnoreCase("Circle")) {
               h /= 1.25F;
               w /= 1.25F;
            }

            x = (float)sr.getScaledWidth() / 2.0F - w / 2.0F;
            y = (float)sr.getScaledHeight() / 2.0F - h / 2.0F - 0.25F;
            x += Crosshair.get.crossPosMotions[0];
            y += Crosshair.get.crossPosMotions[1];
         }

         float x2 = x + w;
         float y2 = y + h;
         float pc = percentSmooth.getAnim();
         switch (mode) {
            case "Line":
               int colStep = (int)(150.0F * pc);
               int c1 = ClientColors.getColor1(0, pc * alphaPC);
               int c2 = ClientColors.getColor1(colStep, (0.5F + pc * 0.5F) * alphaPC);
               int c3 = ClientColors.getColor1(colStep, (0.75F + pc * 0.25F) * alphaPC);
               int c4 = ClientColors.getColor1(colStep * 3, alphaPC);
               float extX = 0.0F;
               float extY = this.maxTriggerAnim.getAnim() * this.maxTriggerAnim.anim * 1.5F;
               RenderUtils.drawLightContureRect(
                  (double)(x - extX), (double)(y - extY), (double)(x2 + extX), (double)(y2 + extY), ColorUtils.swapAlpha(Integer.MIN_VALUE, 190.0F * alphaPC)
               );
               RenderUtils.drawWaveGradient(x - extX, y - extY, x + w * pc + extX, y2 + extY, 1.0F, c1, c2, c3, c4, true, false);
               c1 = ColorUtils.swapAlpha(c1, (float)ColorUtils.getAlphaFromColor(c1) / 10.0F);
               c2 = ColorUtils.swapAlpha(c2, (float)ColorUtils.getAlphaFromColor(c2) / 10.0F);
               c3 = ColorUtils.swapAlpha(c3, (float)ColorUtils.getAlphaFromColor(c3) / 10.0F);
               c4 = ColorUtils.swapAlpha(c4, (float)ColorUtils.getAlphaFromColor(c4) / 10.0F);
               RenderUtils.drawWaveGradient(x - extX, y - extY, x + w + extX, y2 + extY, 0.6F, c1, c2, c3, c4, true, false);
               float showPC = toShowPC.getAnim();
               boolean show = (double)showPC > 0.05;
               String str = "Timer";
               CFontRenderer fontx = Fonts.mntsb_10;
               float strW = fontx.getStringWidth(str);
               float texX = x + w / 2.0F - strW / 2.0F;
               float texY = y + 4.0F - extY;
               int texCol = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
               if (mc.currentScreen instanceof GuiChat) {
                  fontx.drawStringWithShadow(str, texX, texY, texCol);
               } else if (show) {
                  str = percent != 0.0 && percent != 1.0
                     ? ((double)percentSmooth.getAnim() > percent ? "-" : "") + (int)(percent * 100.0) + ((double)percentSmooth.getAnim() < percent ? "+" : "")
                     : (int)(percent * 100.0) + "";
                  strW = fontx.getStringWidth(str);
                  texX = x + (w - strW) * pc;
                  texY = y - 1.5F - showPC * 2.5F;
                  float texAlpha = 255.0F * showPC * (0.5F + showPC * 0.5F) * (0.75F + pc * 0.25F);
                  texCol = ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), texAlpha);
                  if (texAlpha > 32.0F) {
                     fontx.drawStringWithShadow(str, texX, texY, texCol);
                  }
               }
               break;
            case "Circle":
               RenderUtils.resetBlender();
               float extS = MathUtils.clamp(
                  (middle ? toShowPC.getAnim() : 1.0F) + (this.minTriggerAnim.getAnim() + this.maxTriggerAnim.getAnim()) / 5.0F, 0.0F, 1.0F
               );
               boolean crit = this.critical;
               if (!middle || mc.currentScreen instanceof GuiChat) {
                  RenderUtils.drawSmoothCircle((double)(x + w / 2.0F), (double)(y + h / 2.0F), w / 2.0F + 1.0F, ColorUtils.getColor(0, 0, 0, 60.0F * alphaPC));
               }

               GL11.glEnable(3042);
               if (middle
                  && mc.currentScreen instanceof GuiChat
                  && Mouse.isButtonDown(0)
                  && (float)MathUtils.getDifferenceOf(sr.getScaledWidth(), Mouse.getX()) < w
                  && (float)MathUtils.getDifferenceOf(sr.getScaledHeight(), Mouse.getY()) < h) {
                  int texColx = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
                  if (ColorUtils.getAlphaFromColor(texColx) > 32) {
                     Fonts.comfortaaBold_12
                        .drawStringWithOutline(
                           "Timer indicator has centered",
                           x + w / 2.0F - Fonts.comfortaaBold_12.getStringWidth("Timer indicator has centered") / 2.0F,
                           y - 11.0F,
                           texColx
                        );
                  }
               }

               if ((double)percentSmooth.getAnim() >= 0.01 || this.minTriggerAnim.getAnim() > 0.0F) {
                  if (middle && extS > 0.03F && mc.gameSettings.thirdPersonView == 0) {
                     RenderUtils.drawCircledTHud(
                        x + w / 2.0F,
                        (double)(y + h / 2.0F),
                        w / 2.25F * extS * (middle ? 0.6F + 0.4F * pc : 1.0F),
                        1.0F,
                        Integer.MIN_VALUE,
                        extS * extS * 195.0F * (0.5F + 0.5F * pc) * extS * alphaPC,
                        2.0F * extS + 0.05F
                     );
                  }

                  if (mc.gameSettings.thirdPersonView == 0 || !middle) {
                     RenderUtils.drawClientCircle(
                        x + w / 2.0F,
                        (double)(y + h / 2.0F),
                        w / 2.25F * extS * (middle ? 0.6F + 0.4F * pc : 1.0F),
                        percentSmooth.getAnim() * 359.0F,
                        (middle ? 3.0F : 3.5F + 3.0F * pc) * lpSCFactor,
                        extS * extS * (0.5F + 0.5F * pc) * alphaPC
                     );
                  }
               }

               if (!middle && extS > 0.03F) {
                  RenderUtils.drawSmoothCircle((double)(x + w / 2.0F), (double)(y + h / 2.0F), w / 2.5F + 1.0F, ColorUtils.getColor(0, 0, 0, 150.0F * alphaPC));
               }

               if (this.minTriggerAnim.getAnim() != 0.0F || this.maxTriggerAnim.getAnim() != 0.0F) {
                  float aPCT = MathUtils.clamp(this.minTriggerAnim.getAnim() + this.maxTriggerAnim.getAnim(), 0.0F, 1.0F);
                  float tR = w / 2.25F - (middle ? 4.0F : 3.0F) + 4.0F * toShowPC.getAnim();
                  tR += aPCT * 2.0F;
                  RenderUtils.drawClientCircleWithOverallToColor(
                     x + w / 2.0F,
                     (double)(y + h / 2.0F),
                     tR,
                     359.0F,
                     aPCT * 5.0F * lpSCFactor,
                     aPCT,
                     ColorUtils.getOverallColorFrom(
                        ColorUtils.swapAlpha(-1, 255.0F * alphaPC), ColorUtils.getColor(255, 0, 0, 255.0F * alphaPC), this.minTriggerAnim.getAnim()
                     ),
                     aPCT
                  );
               }

               String pppc = (crit ? "*-*" : (int)(percent * 100.0)) + "";
               float strW2 = Fonts.mntsb_10.getStringWidth(pppc);
               if (!middle) {
                  if (pppc.equalsIgnoreCase("100")) {
                     GL11.glPushMatrix();
                     float timePC = (float)(System.currentTimeMillis() % 1200L) / 1200.0F;
                     float timePC2 = ((double)timePC > 0.5 ? 1.0F - timePC : timePC) * 2.0F;
                     RenderUtils.customRotatedObject2D(x, y, w, h, (double)(timePC * 360.0F + 90.0F));
                     RenderUtils.drawCircledTHud(x + w / 2.0F, (double)(y + h / 2.0F), 3.0F, timePC2, -1, 145.0F * alphaPC, 0.75F * lpSCFactor);
                     RenderUtils.drawCircledTHud(x + w / 2.0F, (double)(y + h / 2.0F), 4.5F, 1.0F - timePC2, -1, 115.0F * alphaPC, 1.1F * lpSCFactor);
                     float time2PC = (float)((System.currentTimeMillis() + 600L) % 1000L) / 1000.0F;
                     float time2PC2 = ((double)time2PC > 0.5 ? 1.0F - time2PC : time2PC) * 2.0F;
                     RenderUtils.drawCircledTHud(
                        x + w / 2.0F,
                        (double)(y + h / 2.0F),
                        4.0F + 2.5F * time2PC,
                        1.0F,
                        -1,
                        85.0F * time2PC2 * alphaPC,
                        (0.1F + time2PC2 * 3.5F) * lpSCFactor
                     );
                     GL11.glPopMatrix();
                  } else {
                     int c = crit ? ClientColors.getColor1() : ColorUtils.getOverallColorFrom(Integer.MAX_VALUE, -1, (float)percent);
                     c = ColorUtils.swapAlpha(c, (float)ColorUtils.getAlphaFromColor(c) * alphaPC);
                     if (ColorUtils.getAlphaFromColor(c) > 32) {
                        Fonts.mntsb_10.drawString(pppc, x + w / 2.0F - strW2 / 2.0F, y + 9.5F, c);
                     }
                  }
               }

               RenderUtils.resetBlender();
               break;
            case "Plate":
               int col1x = ClientColors.getColorQ(1, alphaPC);
               int col2x = ClientColors.getColorQ(2, alphaPC);
               int col3x = ClientColors.getColorQ(3, alphaPC);
               int col4x = ClientColors.getColorQ(4, alphaPC);
               int whitex = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
               int red = ColorUtils.getColor(255, 0, 0, 255.0F * alphaPC);
               if (this.maxTriggerAnim.getAnim() > 0.0F) {
                  col1x = ColorUtils.getOverallColorFrom(col1x, whitex, this.maxTriggerAnim.anim);
                  col2x = ColorUtils.getOverallColorFrom(col2x, whitex, this.maxTriggerAnim.anim);
                  col3x = ColorUtils.getOverallColorFrom(col3x, whitex, this.maxTriggerAnim.anim);
                  col4x = ColorUtils.getOverallColorFrom(col4x, whitex, this.maxTriggerAnim.anim);
               }

               if (this.minTriggerAnim.getAnim() > 0.0F) {
                  col1x = ColorUtils.getOverallColorFrom(col1x, red, this.minTriggerAnim.anim);
                  col2x = ColorUtils.getOverallColorFrom(col2x, red, this.minTriggerAnim.anim);
                  col3x = ColorUtils.getOverallColorFrom(col3x, red, this.minTriggerAnim.anim);
                  col4x = ColorUtils.getOverallColorFrom(col4x, red, this.minTriggerAnim.anim);
               }

               GL11.glDisable(3008);
               GlStateManager.depthMask(false);
               GlStateManager.enableBlend();
               GlStateManager.enableTexture2D();
               GlStateManager.shadeModel(7425);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               GL11.glTranslated(0.0, (double)(-(-this.minTriggerAnim.anim + this.maxTriggerAnim.anim) * 2.0F), 0.0);
               mc.getTextureManager().bindTexture(BATTARY_BASE);
               this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(col1x).endVertex();
               this.buffer.pos((double)x, (double)y2).tex(0.0, 1.0).color(col2x).endVertex();
               this.buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).color(col3x).endVertex();
               this.buffer.pos((double)x2, (double)y).tex(1.0, 0.0).color(col4x).endVertex();
               this.tessellator.draw();
               mc.getTextureManager().bindTexture(BATTARY_OVERLAY);
               StencilUtil.initStencilToWrite();
               if ((double)pc > 0.99) {
                  RenderUtils.drawAlphedRect((double)x, (double)(y + 2.5F + 34.0F * (1.0F - pc)), (double)x2, (double)(y2 - 2.0F), whitex);
               } else {
                  float Y1 = y + 2.5F + 34.0F * (1.0F - pc);
                  float Y2 = y2 - 2.0F;
                  float X1 = x + 2.0F;
                  float X2 = x2 - 2.0F;
                  float waveDelay = 2000.0F;
                  float waveStep = 0.15F;
                  float waveHeight = 12.0F * (1.0F - pc) * pc;
                  int vertexXStep = 2;
                  GlStateManager.disableTexture2D();
                  List<Vec2f> vectors = new ArrayList<>();
                  vectors.add(new Vec2f(X1, Y2));
                  int vecIndex = 0;

                  for (float waveX = X1; waveX <= X2; waveX += (float)vertexXStep) {
                     float timePC = (float)(
                           (System.currentTimeMillis() + (long)((float)vecIndex * waveStep * waveDelay / (float)vertexXStep)) % (long)((int)waveDelay)
                        )
                        / waveDelay;
                     float waveY = Y1 - waveHeight / 2.0F + (float)MathUtils.easeInOutQuadWave((double)timePC) * waveHeight;
                     vectors.add(new Vec2f(waveX, waveY));
                     vecIndex++;
                  }

                  vectors.add(new Vec2f(X2, Y2));
                  RenderUtils.drawSome(vectors, whitex, 9);
               }

               GlStateManager.enableTexture2D();
               StencilUtil.readStencilBuffer(1);
               this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(col1x).endVertex();
               this.buffer.pos((double)x, (double)y2).tex(0.0, 1.0).color(col2x).endVertex();
               this.buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).color(col3x).endVertex();
               this.buffer.pos((double)x2, (double)y).tex(1.0, 0.0).color(col4x).endVertex();
               this.tessellator.draw();
               StencilUtil.uninitStencilBuffer();
               GL11.glTranslated(0.0, (double)((-this.minTriggerAnim.getAnim() + this.maxTriggerAnim.getAnim()) * 2.0F), 0.0);
               GlStateManager.shadeModel(7424);
               GlStateManager.depthMask(true);
               GL11.glEnable(3008);
               GlStateManager.resetColor();
               break;
            case "Waist":
               int col1 = ClientColors.getColor1(0, alphaPC);
               int col2 = ClientColors.getColor2(-324, alphaPC);
               int col3 = ClientColors.getColor2(0, alphaPC);
               int col4 = ClientColors.getColor1(972, alphaPC);
               int black = ColorUtils.getColor(0, 0, 0, 140.0F * alphaPC);
               GL11.glDisable(3008);
               GlStateManager.depthMask(false);
               GlStateManager.enableBlend();
               GlStateManager.shadeModel(7425);
               GlStateManager.tryBlendFuncSeparate(
                  GlStateManager.SourceFactor.SRC_ALPHA,
                  GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                  GlStateManager.SourceFactor.ONE,
                  GlStateManager.DestFactor.ZERO
               );
               mc.getTextureManager().bindTexture(WAIST_BASE);
               this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(black).endVertex();
               this.buffer.pos((double)x, (double)y2).tex(0.0, 1.0).color(black).endVertex();
               this.buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).color(black).endVertex();
               this.buffer.pos((double)x2, (double)y).tex(1.0, 0.0).color(black).endVertex();
               this.tessellator.draw();
               mc.getTextureManager().bindTexture(WAIST_OVERLAY);
               this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.buffer.pos((double)x, (double)y).tex(0.0, 0.0).color(col1).endVertex();
               this.buffer.pos((double)x, (double)y2).tex(0.0, 1.0).color(col2).endVertex();
               this.buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).color(col3).endVertex();
               this.buffer.pos((double)x2, (double)y).tex(1.0, 0.0).color(col4).endVertex();
               StencilUtil.initStencilToWrite();
               float overlayX1 = x + 22.0F;
               float overlayX2 = overlayX1 + 45.0F * MathUtils.clamp(pc * 1.005F, 0.0F, 1.0F);
               int white = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
               RenderUtils.drawRect((double)overlayX1, (double)y, (double)overlayX2, (double)y2, white);
               StencilUtil.readStencilBuffer(1);
               this.tessellator.draw();
               StencilUtil.uninitStencilBuffer();
               GlStateManager.shadeModel(7424);
               GlStateManager.depthMask(true);
               GL11.glEnable(3008);
               GlStateManager.resetColor();
               CFontRenderer font = Fonts.comfortaaBold_14;
               if (ColorUtils.getAlphaFromColor(white) > 32) {
                  font.drawString((int)(percent * 100.0) + "%", x + 3.0F, y + 5.5F, white);
               }
               break;
            case "SmoothNine":
               int bgCol = ColorUtils.getColor(18, 18, 18, 255.0F * alphaPC);
               int bgCol2 = ColorUtils.getColor(36, 36, 36, 255.0F * alphaPC);
               smoothInt9.to = (float)((int)MathUtils.clamp(percent * 9.0 + 0.5, 0.0, 9.0));
               smoothInt9.speed = 0.1F;
               float smooth9 = MathUtils.lerp(pc * 9.0F, smoothInt9.getAnim(), 0.75F);
               int ecoC = 30;
               int startRad = ecoC;
               int endRad = (int)((float)ecoC + (360.0F - (float)ecoC * 2.0F) * pc);
               int endRadBG = (int)((float)ecoC + (360.0F - (float)ecoC * 2.0F));
               float circleW = 3.0F;
               float circleRange = h - 6.0F - circleW / 2.0F;
               circleRange /= 2.0F;
               float trAPC = this.maxTriggerAnim.getAnim() + this.minTriggerAnim.getAnim();
               trAPC = trAPC > 1.0F ? 1.0F : (trAPC < 0.0F ? 0.0F : trAPC);
               if (trAPC > 0.0F) {
                  int trCol = ColorUtils.getOverallColorFrom(
                     ColorUtils.getColor(255, (int)MathUtils.clamp(255.0F * trAPC * alphaPC, 0.0F, 255.0F)),
                     ColorUtils.getColor(255, 0, 0, MathUtils.clamp(255.0F * trAPC * alphaPC, 0.0F, 255.0F)),
                     (double)this.minTriggerAnim.anim > 0.03 ? 1.0F : 0.0F
                  );
                  RenderUtils.drawSmoothCircle((double)(x + w / 2.0F), (double)(y + h / 2.0F), circleRange + 5.0F, trCol);
               }

               RenderUtils.drawSmoothCircle((double)(x + w / 2.0F), (double)(y + h / 2.0F), circleRange + 5.0F - 2.0F * trAPC, bgCol);
               RenderUtils.enableGL2D();
               RenderUtils.glColor(-1);
               GL11.glDisable(2852);
               GL11.glLineWidth((circleW + 1.0F) * lpSCFactor);
               StencilUtil.initStencilToWrite();
               GL11.glBegin(3);

               for (int rad = endRadBG; rad > startRad; rad -= 6) {
                  float sinX = x + w / 2.0F - MathHelper.sin(MathHelper.toRadians((float)rad)) * (circleRange + circleW / 2.0F);
                  float cosY = y + h / 2.0F + MathHelper.cos(MathHelper.toRadians((float)rad)) * (circleRange + circleW / 2.0F);
                  GL11.glVertex2d((double)sinX, (double)cosY);
               }

               GL11.glEnd();
               StencilUtil.readStencilBuffer(1);
               GL11.glPointSize(circleW * lpSCFactor);
               int var116 = endRadBG;
               RenderUtils.glColor(bgCol2);
               GL11.glBegin(0);

               while (var116 > endRad) {
                  float sinX = x + w / 2.0F - MathHelper.sin(MathHelper.toRadians((float)var116)) * (circleRange + circleW / 2.0F);
                  float cosY = y + h / 2.0F + MathHelper.cos(MathHelper.toRadians((float)var116)) * (circleRange + circleW / 2.0F);
                  GL11.glVertex2d((double)sinX, (double)cosY);
                  var116 -= 6;
               }

               var116 = endRad;
               GL11.glEnd();
               GL11.glBegin(0);

               while (var116 > startRad) {
                  int cccc = ClientColors.getColor1(4800 - var116 * 6, (1.0F - trAPC / 3.0F) / 2.0F * alphaPC);
                  RenderUtils.glColor(cccc);
                  float sinX = x + w / 2.0F - MathHelper.sin(MathHelper.toRadians((float)var116)) * (circleRange + circleW / 2.0F);
                  float cosY = y + h / 2.0F + MathHelper.cos(MathHelper.toRadians((float)var116)) * (circleRange + circleW / 2.0F);
                  GL11.glVertex2d((double)sinX, (double)cosY);
                  var116 -= 6;
               }

               GL11.glEnd();
               GL11.glPointSize(1.0F);
               StencilUtil.uninitStencilBuffer();
               GL11.glLineWidth(1.0F);
               RenderUtils.disableGL2D();
               GL11.glEnable(3042);
               GL11.glEnable(3553);
               smoothInt9.speed = 0.12F;
               if ((double)MathUtils.getDifferenceOf(smoothInt9.to, smoothInt9.getAnim()) < 0.1) {
                  smoothInt9.setAnim(smoothInt9.to);
               }

               int col1xx = ClientColors.getColor1(0, (1.0F - trAPC / 3.0F) * alphaPC);
               int col2xx = ClientColors.getColor2(0, (1.0F - trAPC / 3.0F) * alphaPC);
               CFontRenderer fontxx = Fonts.mntsb_14;
               StencilUtil.initStencilToWrite();
               RenderUtils.drawSmoothCircle((double)(x + w / 2.0F), (double)(y + h / 2.0F), 3.5F, -1);
               StencilUtil.readStencilBuffer(1);
               if (smooth9 < 1.75F) {
                  col1xx = ColorUtils.getOverallColorFrom(bgCol2, col1xx, smooth9 / 1.75F);
                  col2xx = ColorUtils.getOverallColorFrom(bgCol2, col2xx, smooth9 / 1.75F);
               }

               for (int i = 10; i > 0; i--) {
                  float aPCT = MathUtils.clamp(
                        1.0F - MathUtils.getDifferenceOf(y + h / 2.0F + (float)(i - 1) * 7.0F - smooth9 * 7.0F, y + h / 2.0F) / 4.0F / 2.0F, 0.0F, 1.0F
                     )
                     * alphaPC;
                  if (aPCT > 0.3F && (float)ColorUtils.getAlphaFromColor(col1xx) * aPCT >= 33.0F) {
                     float tx = x + w / 2.0F - fontxx.getStringWidth(String.valueOf((int)((float)i - 0.5F))) / 2.0F;
                     float ty = y + h / 2.0F - 1.5F + (float)(i - 1) * 7.0F - smooth9 * 7.0F;
                     fontxx.drawVGradientString(
                        String.valueOf((int)((float)i - 0.5F)),
                        tx,
                        ty,
                        ColorUtils.swapAlpha(col2xx, (float)ColorUtils.getAlphaFromColor(col2xx) * aPCT),
                        ColorUtils.swapAlpha(col1xx, (float)ColorUtils.getAlphaFromColor(col1xx) * aPCT)
                     );
                  }
               }

               StencilUtil.uninitStencilBuffer();
               break;
            case "LongLine":
               float showPCx = toShowPC.getAnim();
               alphaPC *= showPCx;
               alphaPC *= 6.0F;
               alphaPC = Math.min(alphaPC, 1.0F);
               if (alphaPC * 255.0F >= 3.0F) {
                  float rW = MathUtils.lerp(w / 2.0F, w, showPCx);
                  int colFill = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
                  int colFillLight = ColorUtils.swapAlpha(colFill, (float)ColorUtils.getAlphaFromColor(colFill) * 0.2F);
                  int colShLine = ColorUtils.swapAlpha(0, 55.0F * alphaPC);
                  if (this.maxTriggerAnim.getAnim() > 0.0F) {
                     int green = ColorUtils.getColor(0, 255, 0, 255.0F * alphaPC);
                     colFill = ColorUtils.getOverallColorFrom(colFill, green, this.maxTriggerAnim.anim);
                     colFillLight = ColorUtils.getOverallColorFrom(colFillLight, colFill, this.minTriggerAnim.anim);
                  }

                  if (this.minTriggerAnim.getAnim() > 0.0F) {
                     int redx = ColorUtils.getColor(255, 0, 0, 255.0F * alphaPC);
                     colFill = ColorUtils.getOverallColorFrom(colFill, redx, this.minTriggerAnim.anim);
                     colFillLight = ColorUtils.getOverallColorFrom(colFillLight, colFill, this.minTriggerAnim.anim);
                  }

                  x += (w - rW) / 2.0F;
                  x2 -= (w - rW) / 2.0F;
                  float extIn = 3.0F;
                  float minBoundPC01 = this.BoundUp.getFloat();
                  if (minBoundPC01 != 0.0F && !isCharging && !isRegening && percent != 1.0) {
                     float xEndMinBound = MathUtils.lerp(x + extIn, x2 - extIn, minBoundPC01);
                     RenderUtils.drawAlphedRect(
                        (double)(x + extIn),
                        (double)(y + extIn),
                        (double)(xEndMinBound - 0.5F),
                        (double)(y2 - extIn),
                        ColorUtils.toDark(colFill, MathUtils.lerp(1.0F, 0.75F, Math.min((1.0F - pc) / (1.0F - minBoundPC01), 1.0F)))
                     );
                     float xEndTimer = MathUtils.lerp(x + extIn, x2 - extIn, pc);
                     RenderUtils.drawAlphedRect(
                        (double)Math.min(xEndTimer, xEndMinBound), (double)(y + extIn), (double)xEndTimer, (double)(y2 - extIn), colFill
                     );
                  } else {
                     RenderUtils.drawAlphedRect(
                        (double)(x + extIn), (double)(y + extIn), (double)MathUtils.lerp(x + extIn, x2 - extIn, pc), (double)(y2 - extIn), colFill
                     );
                  }

                  RenderUtils.drawAlphedRect(
                     (double)Math.min(MathUtils.lerp(x + extIn, x2 - extIn, pc) + 1.0F, x2 - extIn - 1.0F),
                     (double)(y + extIn),
                     (double)(x2 - extIn),
                     (double)(y2 - extIn),
                     colFillLight
                  );
                  if (isCharging && pc < lastChargingFinishValue) {
                     float cxCharging = MathUtils.lerp(x + extIn, x2 - extIn, lastChargingFinishValue);
                     RenderUtils.drawAlphedRect((double)(cxCharging - 0.5F), (double)(y + extIn), (double)cxCharging, (double)(y2 - extIn), colFill);
                  }

                  RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                     x + extIn,
                     y + extIn,
                     MathUtils.lerp(x + extIn, x2 - extIn, pc),
                     y2 - extIn,
                     0.0F,
                     extIn,
                     colShLine,
                     colShLine,
                     colShLine,
                     colShLine,
                     false,
                     false,
                     true
                  );
                  float outLW = 1.0F;
                  RenderUtils.drawAlphedRect((double)(x + outLW), (double)y, (double)(x2 - outLW), (double)(y + outLW), colFill);
                  RenderUtils.drawAlphedRect((double)(x + outLW), (double)(y2 - outLW), (double)(x2 - outLW), (double)y2, colFill);
                  RenderUtils.drawAlphedRect((double)x, (double)y, (double)(x + outLW), (double)y2, colFill);
                  RenderUtils.drawAlphedRect((double)(x2 - outLW), (double)y, (double)x2, (double)y2, colFill);
                  RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                     x, y, x2, y2, 0.0F, h / 2.0F, colShLine, colShLine, colShLine, colShLine, false, false, true
                  );
                  if (mc.currentScreen instanceof GuiChat) {
                     float xExtSide = 5.5F;
                     RenderUtils.drawSmoothCircle((double)(x - xExtSide), (double)(y + h / 2.0F), h / 4.0F + 1.0F, colShLine);
                     RenderUtils.drawSmoothCircle((double)(x2 + xExtSide), (double)(y + h / 2.0F), h / 4.0F + 1.0F, colShLine);
                     RenderUtils.drawSmoothCircle((double)(x - xExtSide), (double)(y + h / 2.0F), h / 4.0F, colFill);
                     RenderUtils.drawSmoothCircle((double)(x2 + xExtSide), (double)(y + h / 2.0F), h / 4.0F, colFill);
                  }
               }
         }
      }
   }

   @EventTarget
   public void onReceive(EventReceivePacket event) {
      if ((this.actived || forceWastage) && this.smartGo && event.getPacket() instanceof SPacketPlayerPosLook TP) {
         if (Minecraft.player.getDistance(TP.getX(), TP.getY(), TP.getZ()) > 20.0 || this.isNcpTimerDisabler() || this.isMatrixBypass()) {
            return;
         }

         this.panicRegen = true;
         this.smartGo = false;
         percent /= 1.5;
         this.critical = true;
      }
   }

   private double[] timerArgs(String mode, boolean flaged, double tpsPC20, double timerSpeed) {
      double chargeSP = 1.0;
      double dropSP = 0.0;
      double regenSP = 0.0;
      double chargeMul = this.phantomIsRegening ? (double)this.getPhantomSneakSlowing() : (this.MatrixBypass.getBool() ? 0.1F : 1.0);
      switch (mode) {
         case "Matrix":
            chargeSP = 0.07 / tpsPC20 * chargeMul;
            dropSP = 0.021 * timerSpeed * tpsPC20;
            regenSP = 0.33333 / tpsPC20 * chargeMul;
            break;
         case "NCP":
            chargeSP = 0.05 / tpsPC20 * chargeMul;
            dropSP = 0.046 * timerSpeed * tpsPC20;
            regenSP = 0.75 / tpsPC20 * chargeMul;
            break;
         case "Other":
            chargeSP = 0.08 * tpsPC20 * chargeMul;
            dropSP = 0.046 * timerSpeed * tpsPC20;
            regenSP = 0.85 / tpsPC20 * chargeMul;
            break;
         case "Vulcan":
            chargeSP = 0.1 / tpsPC20 * chargeMul;
            dropSP = 0.11 * timerSpeed * tpsPC20;
            regenSP = tpsPC20 * chargeMul;
            break;
         case "MatrixLow&Ncp":
            chargeSP = 0.079 / tpsPC20 * chargeMul;
            dropSP = 0.018 * timerSpeed * tpsPC20;
            regenSP = 0.9F * tpsPC20 * chargeMul;
      }

      if (this.isMatrixBypass()) {
         dropSP *= 0.05F;
      }

      if (flaged) {
         chargeSP /= 1.425;
         regenSP /= 3.5;
      }

      return new double[]{chargeSP, dropSP, regenSP};
   }

   private boolean updateAfkStatus(TimerHelper timer) {
      if (!timer.hasReached(100.0)) {
         this.yaw = Minecraft.player.lastReportedYaw;
         this.pitch = EntityPlayerSP.lastReportedPitch;
      }

      double player3DSpeed = Math.sqrt(Entity.Getmotionx * Entity.Getmotionx + Entity.Getmotiony * Entity.Getmotiony + Entity.Getmotionz * Entity.Getmotionz);
      boolean FORCE_RECHARGE = Minecraft.player.ticksExisted == 1 || Minecraft.player.isDead;
      if (!FORCE_RECHARGE) {
         FORCE_RECHARGE = FreeCam.get != null && FreeCam.get.actived;
      }

      if (FORCE_RECHARGE
         || this.yaw == Minecraft.player.lastReportedYaw
            && this.pitch == EntityPlayerSP.lastReportedPitch
            && (player3DSpeed == 0.0784000015258789 || player3DSpeed == 0.0 || player3DSpeed == 0.02)
            && !forceWastage) {
         if (timer.hasReached(150.0)) {
            timer.reset();
            this.afk = true;
         }
      } else {
         this.afk = false;
         timer.reset();
      }

      if (Minecraft.player.ticksExisted == 1 || Minecraft.player.isDead) {
         this.afk = true;
         AnimationUtils var10000 = percentSmooth;
         percent = 1.0;
         var10000.to = (float)1.0;
         percentSmooth.setAnim(MathUtils.lerp(percentSmooth.anim, percentSmooth.to, 0.66666F));
         this.critical = false;
      }

      return this.afk;
   }

   private double updateTimerPercent(double[] args, boolean isAfk, float boundUp) {
      boolean phantomRegen = this.phantomIsRegening && !this.actived;
      isRegening = false;
      isCharging = false;
      if (percent < 1.0 && isAfk != phantomRegen) {
         percent = percent + args[0] / (double)(1.0F - boundUp);
         isRegening = true;
         if (!phantomRegen) {
            this.critical = false;
         }
      } else if (!isAfk && percent < 1.0 && !this.actived && mc.timer.speed <= 1.0) {
         double upped = (double)((float)(args[0] * 0.2F) / 5.0F);
         if (upped + percent - 0.02F - (double)boundUp <= args[2] / (double)(1.0F - boundUp) && !forceWastage) {
            percent += upped;
            isCharging = true;
            lastChargingFinishValue = Math.min((float)args[2] / (1.0F - boundUp) + boundUp + 0.02F, 1.0F);
         }

         this.critical = false;
      }

      if (this.panicRegen && percent == 1.0) {
         this.panicRegen = false;
         if (this.critical) {
            this.critical = false;
         }
      }

      if (!isAfk && percent > (double)boundUp && (this.smartGo || forceWastage)) {
         percent = Math.max(percent - args[1], (double)boundUp);
      }

      return percent = MathUtils.clamp(percent, 0.0, 1.0);
   }

   private boolean canDisableByTimeOut(boolean timeOutEnabled, int timeOutMS) {
      return this.actived && timeOutEnabled && timeOutWait.hasReached((double)timeOutMS);
   }

   private boolean canAbuseTimerSpeed(boolean isSmart) {
      boolean FORCE_STOP = false;
      if (ElytraBoost.get.actived && ElytraBoost.canElytra()) {
         String ebMode = ElytraBoost.get.Mode.currentMode;
         if (ebMode.equalsIgnoreCase("MatrixFly2") && !ElytraBoost.get.NoTimerDefunction.getBool() || ebMode.equalsIgnoreCase("MatrixFly3")) {
            FORCE_STOP = true;
         }
      }

      return forceWastage && percent > (double)this.BoundUp.getFloat()
         || this.actived && ((this.smartGo || this.isMatrixBypass()) && !this.critical || !isSmart) && !FORCE_STOP;
   }

   private double getTimerBoostSpeed(boolean can, boolean smart, float boundUp) {
      double speed = 1.0;
      if (can) {
         float timer = this.NCPBypass.getBool() && !this.Stamina.getBool()
            ? (Minecraft.player.ticksExisted % 30 > 1 && Minecraft.player.ticksExisted % 30 < 16 ? 5.25F : 2.0F)
            : this.Increase.getFloat();
         speed = smart && this.isMatrixBypass()
            ? (double)(MoveMeHelp.getSpeed() < 0.12F ? 4.5F : 1.0F)
            : (
               smart && timer > 1.0F && this.SmoothWastage.getBool()
                  ? (double)(1.0F + (timer - 1.0F) / 6.0F) + (double)((timer - 1.0F) / 1.1666666F) * (percent - (double)boundUp)
                  : (double)timer
            );
         float randomVal = this.Randomize.getFloat();
         double randomize = (double)(randomVal * (float)(-1 + 2 * (Minecraft.player.ticksExisted % 2)));
         if (randomize != 0.0) {
            speed += randomize > 0.0 ? randomize : randomize / 2.0;
         }

         if (forceWastage) {
            speed = (double)forceTimer;
         }

         forceTimer = 1.0F;
      }

      return can ? MathUtils.clamp(speed, 0.025, 100.0) : 1.0;
   }

   private boolean isNcpTimerDisabler() {
      return this.NCPBypass.getBool() && !this.Stamina.getBool();
   }

   public boolean isMatrixBypass() {
      return this.MatrixBypass.getBool()
         && this.Stamina.getBool()
         && this.StaminaMode.getMode().contains("Matrix")
         && mc.world != null
         && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.1F)).isEmpty()
         && this.isActived()
         && this.smartGo;
   }

   private int timerSFXSleepMS() {
      return this.smartGo ? 40 : 50;
   }

   @Override
   public void alwaysUpdate() {
      if (afkWait != null && Minecraft.player != null) {
         if (cancel) {
            cancel = false;
         } else {
            boolean smartTimer = this.Stamina.getBool();
            float boundUp = this.BoundUp.getFloat();
            boolean canABB = this.canAbuseTimerSpeed(smartTimer);
            String sfxMode = this.TimerSFX.currentMode;
            boolean doSfx = !sfxMode.equalsIgnoreCase("None");
            double speed = this.getTimerBoostSpeed(canABB, smartTimer, boundUp);
            if (smartTimer) {
               double prevPercent = percent;
               double[] ARGS = this.timerArgs(
                  this.StaminaMode.currentMode,
                  this.panicRegen,
                  (double)(TPSDetect.getTPSServer() / 20.0F),
                  mc.timer.speed * GameSyncTPS.getGameConpense(1.0, GameSyncTPS.instance.SyncPercent.getFloat()) - 1.0
               );
               if (this.PhantomDash.getBool()) {
                  this.phantomIsRegening = this.canPhantomSlowing();
                  speed *= this.phantomIsRegening ? (double)this.getPhantomSneakSlowing() : 1.0;
               } else if (this.phantomIsRegening) {
                  this.phantomIsRegening = false;
               }

               this.smartGo = this.updateTimerPercent(ARGS, this.updateAfkStatus(afkWait), boundUp) > (double)boundUp
                  && !this.afk
                  && !this.critical
                  && (
                     !this.Stamina.getBool()
                        || !this.MatrixBypass.getBool()
                        || !this.StaminaMode.getMode().contains("Matrix")
                        || !Minecraft.player.isJumping() && Minecraft.player.posY == Minecraft.player.lastTickPosY
                  );
               if (this.smartGo && this.isMatrixBypass()) {
                  float mul = 0.0726F;
                  if (!MoveMeHelp.w() || MoveMeHelp.s() || !MoveMeHelp.a() && !MoveMeHelp.d()) {
                     if (MoveMeHelp.s() && !MoveMeHelp.w()) {
                        if (Minecraft.player.isSprinting()) {
                           if (MoveMeHelp.a() || MoveMeHelp.d()) {
                              mul = 0.0699F;
                           }
                        } else if (!MoveMeHelp.a() && !MoveMeHelp.d()) {
                           mul = 0.102F;
                        } else {
                           mul = 0.1F;
                        }
                     }
                  } else {
                     mul = 0.0699F;
                  }

                  MoveMeHelp.setTempSpeedPost((double)mul);
               }

               percentSmooth.to = (float)percent;
               boolean isRenderLongLine = this.DrawSmart.getBool() && this.Render.getMode().equalsIgnoreCase("LongLine");
               toShowPC.to = (!(percent > (double)boundUp) || !(percent < 1.0))
                     && (!(this.minTriggerAnim.getAnim() > 0.0F) && !(this.maxTriggerAnim.getAnim() > 0.0F) || isRenderLongLine)
                  ? (isRenderLongLine && mc.currentScreen instanceof GuiChat ? 0.5F : 0.0F)
                  : 1.0F;
               toShowPC.speed = 0.1F;
               if (doSfx && prevPercent != percent && (percent == (double)boundUp || percent == 1.0)) {
                  MusicHelper.playSound(
                     (percent < prevPercent ? "timerlow" : "timermax") + sfxMode.toLowerCase() + ".wav", sfxMode.equalsIgnoreCase("Dev") ? 0.8F : 0.3F
                  );
                  this.sfxDelay.reset();
                  if (percent < prevPercent) {
                     this.minTriggerAnim.to = 1.01F;
                  } else {
                     this.maxTriggerAnim.to = 1.01F;
                  }
               }

               if (doSfx && this.sfxDelay.hasReached((double)this.timerSFXSleepMS()) && (int)(prevPercent * 100.0) != (int)(percent * 100.0)) {
                  if (prevPercent > percent && percent != 0.0 && this.smartGo) {
                     MusicHelper.playSound(this.smartGo ? "timertickdrop.wav" : "timertickcharge.wav", 0.165F);
                     this.sfxDelay.reset();
                  }

                  if (prevPercent < percent && percent != 1.0) {
                     MusicHelper.playSound("timertickcharge.wav", 0.165F);
                     this.sfxDelay.reset();
                  }
               } else if (this.isActived() && this.DisablePostExpire.getBool() && percent == (double)boundUp && this.sfxDelay.hasReached(35.0)) {
                  this.toggle(false);
               }

               if (this.maxTriggerAnim.getAnim() > 1.0F) {
                  this.maxTriggerAnim.setAnim(1.0F);
                  this.maxTriggerAnim.to = 0.0F;
               }

               if (this.minTriggerAnim.getAnim() > 1.0F) {
                  this.minTriggerAnim.setAnim(1.0F);
                  this.minTriggerAnim.to = 0.0F;
               }

               if (this.maxTriggerAnim.to == 0.0F && (double)this.maxTriggerAnim.getAnim() < 0.03) {
                  this.maxTriggerAnim.setAnim(0.0F);
               }

               if (this.minTriggerAnim.to == 0.0F && (double)this.minTriggerAnim.getAnim() < 0.03) {
                  this.minTriggerAnim.setAnim(0.0F);
               }

               this.minTriggerAnim.speed = 0.1F;
               this.maxTriggerAnim.speed = 0.075F;
            } else {
               if (this.actived && (double)Minecraft.player.ticksExisted % ((mc.timer.speed - 1.0) * 30.0) == 0.0 && this.isNcpTimerDisabler()) {
                  Minecraft.player
                     .connection
                     .sendPacket(
                        new CPacketPlayer.PositionRotation(
                           Minecraft.player.posX,
                           Minecraft.player.posY - (Minecraft.player.onGround ? 0.1 : 1.1),
                           Minecraft.player.posZ,
                           Minecraft.player.rotationYaw,
                           Minecraft.player.rotationPitch,
                           Minecraft.player.onGround
                        )
                     );
               }

               if (percent != 1.0) {
                  this.smartGo = false;
                  percentSmooth.to = 1.0F;
                  percent = 1.0;
                  toShowPC.to = 0.0F;
               }
            }

            if (this.canDisableByTimeOut(this.TimeOut.getBool(), this.TimeOutMS.getInt())) {
               this.toggle(false);
            } else {
               mc.timer.speed = speed;
               forceWastage = false;
            }
         }
      }
   }

   @Override
   public String getDisplayName() {
      return this.Stamina.getBool()
         ? this.getName()
            + this.getSuff()
            + (!this.panicRegen && !this.critical ? "Smart" + (percent >= (double)this.BoundUp.getFloat() ? "-Max" : (int)(percent * 100.0) + "%") : "Flagged")
         : this.getDisplayByDouble((double)this.Increase.getFloat());
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         timeOutWait.reset();
      } else {
         mc.timer.speed = 1.0;
      }

      super.onToggled(actived);
   }
}
