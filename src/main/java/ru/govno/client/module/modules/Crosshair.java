package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.Vec2fColored;

public class Crosshair extends Module {
   public static Crosshair get;
   public ModeSettings CrossType;
   public ModeSettings ScaleIndex;
   public ModeSettings ColorMode;
   public BoolSettings GlowBlooming;
   public BoolSettings MouseMotions;
   public BoolSettings SwingsExpanding;
   public BoolSettings SwingsCenterPoint;
   public BoolSettings SwingsRotate;
   public BoolSettings Spinning;
   public BoolSettings SwingsAnim;
   public ColorSettings PickColor;
   public BoolSettings OnMouseColorChange;
   public ColorSettings OnMouseColor;
   private final ResourceLocation BLOOM_TEX = new ResourceLocation("vegaline/modules/crosshair/bloom.png");
   private final AnimationUtils mousePreX = new AnimationUtils(0.0F, 0.0F, 0.025F);
   private final AnimationUtils mousePreY = new AnimationUtils(0.0F, 0.0F, 0.025F);
   public float[] crossPosMotions = new float[]{0.0F, 0.0F};

   public Crosshair() {
      super("Crosshair", 0, Module.Category.RENDER);
      this.settings.add(this.CrossType = new ModeSettings("CrossType", "Point", this, new String[]{"Point", "TriplePoint", "Circle", "Dash"}));
      this.settings.add(this.GlowBlooming = new BoolSettings("GlowBlooming", false, this));
      this.settings.add(this.ScaleIndex = new ModeSettings("ScaleIndex", "Middle", this, new String[]{"Mini", "Middle", "Big"}));
      this.settings.add(this.MouseMotions = new BoolSettings("MouseMotions", false, this));
      this.settings
         .add(
            this.SwingsExpanding = new BoolSettings(
               "SwingsExpanding",
               true,
               this,
               () -> this.CrossType.getMode().equalsIgnoreCase("TriplePoint") || this.CrossType.getMode().equalsIgnoreCase("Dash")
            )
         );
      this.settings
         .add(
            this.SwingsCenterPoint = new BoolSettings(
               "SwingsCenterPoint",
               true,
               this,
               () -> (this.CrossType.getMode().equalsIgnoreCase("TriplePoint") || this.CrossType.getMode().equalsIgnoreCase("Dash"))
                     && this.SwingsExpanding.getBool()
            )
         );
      this.settings.add(this.SwingsRotate = new BoolSettings("SwingsRotate", false, this, () -> this.CrossType.getMode().equalsIgnoreCase("TriplePoint")));
      this.settings
         .add(
            this.Spinning = new BoolSettings(
               "Spinning", false, this, () -> this.CrossType.getMode().equalsIgnoreCase("TriplePoint") || this.CrossType.getMode().equalsIgnoreCase("Dash")
            )
         );
      this.settings.add(this.SwingsAnim = new BoolSettings("SwingsAnim", false, this, () -> this.CrossType.getMode().equalsIgnoreCase("Circle")));
      this.settings.add(this.ColorMode = new ModeSettings("ColorMode", "BasePreset", this, new String[]{"BasePreset", "Client", "Picker"}));
      this.settings
         .add(
            this.PickColor = new ColorSettings("PickColor", ColorUtils.getColor(255, 0, 240), this, () -> this.ColorMode.getMode().equalsIgnoreCase("Picker"))
         );
      this.settings.add(this.OnMouseColorChange = new BoolSettings("OnMouseColorChange", false, this));
      this.settings.add(this.OnMouseColor = new ColorSettings("OnMouseColor", ColorUtils.getColor(200, 0, 0), this, () -> this.OnMouseColorChange.getBool()));
      mc.getTextureManager().bindTexture(this.BLOOM_TEX);
      mc.getTextureManager().getTexture(this.BLOOM_TEX).setBlurMipmap(true, false);
      this.setDemand(1, 1);
      get = this;
   }

   private int getMainColor(String modeCross) {
      int color;
      color = -1;
      String base = this.ColorMode.getMode();
      label46:
      switch (base) {
         case "BasePreset":
            switch (modeCross) {
               case "Point":
                  color = -1;
                  break label46;
               case "TriplePoint":
                  color = ColorUtils.getColor(255, 160, 255);
                  break label46;
               case "Circle":
                  color = ColorUtils.getColor(255, 185, 90);
                  break label46;
               case "Dash":
                  color = ColorUtils.getColor(120, 120, 255);
               default:
                  break label46;
            }
         case "Client":
            color = ClientColors.getColor1();
            break;
         case "Picker":
            color = this.PickColor.getCol();
      }

      if (this.OnMouseColorChange.getBool()
         && (
            mc.pointedEntity instanceof EntityLivingBase basex && basex.isEntityAlive()
               || HitAura.TARGET != null && HitAura.get.isActived() && HitAura.get.canRotateUpdated
         )) {
         color = this.OnMouseColor.getCol();
      }

      return color;
   }

   private boolean isMotionsMouse() {
      return !mc.gameSettings.hideGUI && !mc.gameSettings.showDebugInfo ? this.MouseMotions.getBool() : false;
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByMode(this.CrossType.getMode());
   }

   @Override
   public void onUpdateLimitedDelay() {
      if (!this.isMotionsMouse()) {
         this.updateMotions(true);
      }
   }

   @Override
   public void onRender2D(ScaledResolution sr) {
      this.updateMotions(!this.isMotionsMouse());
      if (mc.gameSettings.thirdPersonView == 0 && !mc.gameSettings.showDebugInfo) {
         float[] crossPos = this.crossPos(sr);
         String modeCross = this.CrossType.getMode();
         int scaleIndex = this.ScaleIndex.getModeIndex();
         this.drawCross(modeCross, crossPos[0], crossPos[1], this.getMainColor(modeCross), 1.0F, this.GlowBlooming.getAnimation(), scaleIndex);
      }
   }

   private void drawCross(String modeName, float x, float y, int mainColor, float alphaPC, float bloomingAPC, int scaleIndex02) {
      if (!(alphaPC * 255.0F < 1.0F)) {
         float sizeMulBasedPix = ScaledResolution.lpSCFactor();
         x = (float)((int)(x * sizeMulBasedPix * 2.0F)) / (sizeMulBasedPix * 2.0F);
         y = (float)((int)(y * sizeMulBasedPix * 2.0F)) / (sizeMulBasedPix * 2.0F);
         int scaleIndex13 = scaleIndex02 + 1;
         mainColor = ColorUtils.swapAlpha(mainColor, 255.0F * alphaPC);
         int mainColorZero = ColorUtils.swapAlpha(mainColor, 0.0F);
         int black = ColorUtils.getColor(0, 0, 0, 255.0F * alphaPC);
         float swingPercent = Minecraft.player == null ? 0.0F : Minecraft.player.getSwingProgress(mc.getRenderPartialTicks());
         float swingWave = (float)MathUtils.easeInOutQuadWave((double)swingPercent);
         float swingExpo = (float)MathUtils.easeInOutExpo((double)swingPercent);
         List<Vec2fColored> vecs = new ArrayList<>();
         List<Vec2fColored> glowPositions = new ArrayList<>();
         switch (modeName) {
            case "Point":
               GL11.glEnable(2832);
               switch (scaleIndex02) {
                  case 0:
                     GL11.glPointSize(2.75F * sizeMulBasedPix);
                     break;
                  case 1:
                     GL11.glPointSize(4.5F * sizeMulBasedPix);
                     break;
                  case 2:
                     GL11.glPointSize(6.5F * sizeMulBasedPix);
               }

               Vec2fColored blackVec = new Vec2fColored(x, y, black);
               vecs.add(blackVec);
               vecs.add(blackVec);
               RenderUtils.drawVec2Colored(vecs, 0);
               vecs.clear();
               switch (scaleIndex02) {
                  case 0:
                     GL11.glPointSize(1.75F * sizeMulBasedPix);
                     break;
                  case 1:
                     GL11.glPointSize(3.0F * sizeMulBasedPix);
                     break;
                  case 2:
                     GL11.glPointSize(4.25F * sizeMulBasedPix);
               }

               Vec2fColored mainColorVec = new Vec2fColored(x, y, mainColor);
               vecs.add(mainColorVec);
               vecs.add(mainColorVec);
               RenderUtils.drawVec2Colored(vecs, 0);
               GL11.glPointSize(1.0F);
               glowPositions.addAll(vecs);
               break;
            case "TriplePoint":
               boolean expand = this.SwingsExpanding.getBool();
               boolean rotates = this.SwingsRotate.getBool();
               int steps = 3;
               int rotateAdd360 = (int)(
                  (float)(rotates ? (int)(swingExpo * 360.0F / (float)steps) : 0)
                     + (this.Spinning.getBool() ? (float)((double)System.nanoTime() / 1000000.0 % 900.0 / 900.0) * 360.0F : 0.0F)
               );
               float radius = 0.25F + (1.25F + (expand ? 2.5F * swingWave : 0.0F)) * (float)scaleIndex13;
               float pointSize = (0.75F + 1.5F * (float)scaleIndex13) * sizeMulBasedPix;

               for (int rad = rotateAdd360; rad < 360 + rotateAdd360; rad += 360 / steps) {
                  float radian = MathHelper.toRadians((float)rad);
                  float vertX = x - MathHelper.sin(radian) * radius;
                  float vertY = y + MathHelper.cos(radian) * radius;
                  Vec2fColored mainColorVecx = new Vec2fColored(vertX, vertY, mainColor);
                  vecs.add(mainColorVecx);
               }

               boolean center = expand && swingPercent > 0.0F && this.SwingsCenterPoint.getBool();
               if (center) {
                  Vec2fColored centerColVec = new Vec2fColored(
                     x, y, ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * swingWave)
                  );
                  vecs.add(centerColVec);
               }

               GL11.glEnable(2832);
               GL11.glPointSize(pointSize);
               RenderUtils.drawVec2Colored(vecs, 0);
               GL11.glPointSize(1.0F);
               glowPositions.addAll(vecs);
               break;
            case "Circle":
               float swingWaveAnim = this.SwingsAnim.getBool() ? swingWave : 0.0F;
               float range = 1.0F + (1.0F - swingWaveAnim) * 1.5F * (float)scaleIndex13;
               float width = 0.5F + 0.5F * (float)scaleIndex13 + swingWaveAnim * 2.5F * (float)scaleIndex13;
               float var52 = (float)((int)(y * 2.0F)) / 2.0F;
               GL11.glPushMatrix();
               RenderUtils.customRotatedObject2D(x, var52, 0.0F, 0.0F, (double)(-180.0F * (1.0F - swingWaveAnim) - 90.0F));
               RenderUtils.drawCircledTHudWithOverallColor(
                  x,
                  (double)var52,
                  range / 2.0F,
                  1.0F - swingWaveAnim,
                  mainColor,
                  (float)ColorUtils.getAlphaFromColor(mainColor) / 2.0F,
                  width * sizeMulBasedPix,
                  0,
                  0.0F
               );
               GL11.glPopMatrix();
               glowPositions.add(new Vec2fColored(x, y, mainColor));
               break;
            case "Dash":
               boolean spinning = this.Spinning.getBool();
               if (spinning) {
                  GL11.glPushMatrix();
                  RenderUtils.customRotatedObject2D(x, y, 0.0F, 0.5F, (double)((float)((double)System.nanoTime() / 1000000.0 % 1300.0 / 1300.0) * 360.0F));
                  GL11.glEnable(2881);
               }

               boolean expandSwing = this.SwingsExpanding.getBool();
               float baseWidth = 4.0F * (float)scaleIndex13 / 2.0F;
               float baseWidthNear = baseWidth / 1.35F;
               float expandXParts = expandSwing ? swingWave * 2.0F * (float)scaleIndex13 : 0.0F;
               float xC1 = x - expandXParts;
               float xC2 = x + expandXParts;
               float x1Base = x - baseWidth - expandXParts;
               float x2Base = x + baseWidth + expandXParts;
               float x1BaseNear = x - baseWidthNear - expandXParts;
               float x2BaseNear = x + baseWidthNear + expandXParts;
               RenderUtils.drawAlphedSideways((double)x1Base, (double)y, (double)xC1, (double)(y + 0.5F), mainColorZero, mainColor);
               RenderUtils.drawAlphedSideways((double)xC2, (double)y, (double)x2Base, (double)(y + 0.5F), mainColor, mainColorZero);
               int mainColorDawn = ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * (0.1F + (float)scaleIndex13 * 0.1F));
               RenderUtils.drawAlphedSideways((double)x1BaseNear, (double)(y - 0.5F), (double)xC1, (double)y, mainColorZero, mainColorDawn);
               RenderUtils.drawAlphedSideways((double)xC2, (double)(y - 0.5F), (double)x2BaseNear, (double)y, mainColorDawn, mainColorZero);
               RenderUtils.drawAlphedSideways((double)x1BaseNear, (double)(y + 0.5F), (double)xC1, (double)(y + 1.0F), mainColorZero, mainColorDawn);
               RenderUtils.drawAlphedSideways((double)xC2, (double)(y + 0.5F), (double)x2BaseNear, (double)(y + 1.0F), mainColorDawn, mainColorZero);
               if (expandXParts >= 1.0F) {
                  int partitionsCol = ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * swingWave);
                  float extYFromCen = 0.5F + expandXParts / 1.5F;
                  RenderUtils.drawAlphedGradient((double)xC1, (double)(y - extYFromCen), (double)(xC1 + 0.5F), (double)(y + 0.5F), mainColorZero, partitionsCol);
                  RenderUtils.drawAlphedGradient(
                     (double)xC1, (double)(y + 0.5F), (double)(xC1 + 0.5F), (double)(y + 0.5F + extYFromCen), partitionsCol, mainColorZero
                  );
                  RenderUtils.drawAlphedGradient((double)(xC2 - 0.5F), (double)(y - extYFromCen), (double)xC2, (double)(y + 0.5F), mainColorZero, partitionsCol);
                  RenderUtils.drawAlphedGradient(
                     (double)(xC2 - 0.5F), (double)(y + 0.5F), (double)xC2, (double)(y + 0.5F + extYFromCen), partitionsCol, mainColorZero
                  );
                  boolean centerPoint = this.SwingsCenterPoint.getBool();
                  if (centerPoint) {
                     int pointColor = ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * swingWave);
                     float pointSizex = expandXParts * sizeMulBasedPix;
                     Vec2fColored centerPointVec = new Vec2fColored(x, y + 0.25F, pointColor);
                     vecs.add(centerPointVec);
                     GL11.glEnable(2832);
                     GL11.glPointSize(pointSizex);
                     RenderUtils.drawVec2Colored(vecs, 0);
                     GL11.glPointSize(1.0F);
                     glowPositions.add(centerPointVec);
                  }
               }

               if (spinning) {
                  GL11.glDisable(2881);
                  GL11.glPopMatrix();
               }
         }

         if (bloomingAPC * 255.0F >= 1.0F) {
            float glowRadiusSwitch = 200.0F;
            float glowAPCSwitch = 0.2F;
            switch (modeName) {
               case "Point":
                  glowRadiusSwitch = 3.5F;
                  glowAPCSwitch = 0.05F;
                  break;
               case "TriplePoint":
                  glowRadiusSwitch = 3.5F;
                  glowAPCSwitch = 0.075F;
                  break;
               case "Circle":
                  glowRadiusSwitch = 5.5F;
                  glowAPCSwitch = 0.1F;
                  break;
               case "Dash":
                  glowRadiusSwitch = 2.5F;
                  glowAPCSwitch = 0.2F;
            }

            if (glowRadiusSwitch != 0.0F && !glowPositions.isEmpty()) {
               float glowRadius = glowRadiusSwitch * (float)scaleIndex13;
               float glowAPC = glowAPCSwitch * bloomingAPC;
               RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               glowPositions.forEach(glowPos -> {
                  float xGlow = glowPos.getX();
                  float yGlow = glowPos.getY();
                  int colorGlow = ColorUtils.toDark(glowPos.getColor(), glowAPC);
                  RenderUtils.buffer.pos((double)(xGlow - glowRadius), (double)(yGlow - glowRadius)).tex(0.0, 0.0).color(colorGlow).endVertex();
                  RenderUtils.buffer.pos((double)(xGlow + glowRadius), (double)(yGlow - glowRadius)).tex(1.0, 0.0).color(colorGlow).endVertex();
                  RenderUtils.buffer.pos((double)(xGlow + glowRadius), (double)(yGlow + glowRadius)).tex(1.0, 1.0).color(colorGlow).endVertex();
                  RenderUtils.buffer.pos((double)(xGlow - glowRadius), (double)(yGlow + glowRadius)).tex(0.0, 1.0).color(colorGlow).endVertex();
               });
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 1);
               GL11.glEnable(3553);
               GL11.glDisable(2896);
               GL11.glDisable(2929);
               GL11.glDisable(2884);
               mc.getTextureManager().bindTexture(this.BLOOM_TEX);
               RenderUtils.tessellator.draw();
               GL11.glEnable(2884);
               GL11.glEnable(2929);
               GL11.glBlendFunc(770, 771);
            }
         }
      }
   }

   private float[] crossPos(ScaledResolution sr) {
      return new float[]{(float)(sr.getScaledWidth() / 2) - this.getMouseMotion()[0], (float)(sr.getScaledHeight() / 2) - this.getMouseMotion()[1]};
   }

   private void updateMotions(boolean reset) {
      if (reset) {
         this.mousePreX.to = 0.0F;
         this.mousePreY.to = 0.0F;
         this.mousePreX.setAnim(0.0F);
         this.mousePreY.setAnim(0.0F);
         this.crossPosMotions[0] = 0.0F;
         this.crossPosMotions[1] = 0.0F;
      } else {
         int vantuz = Minecraft.player.ticksExisted < 10 ? 0 : 1;
         if (Minecraft.player.isRiding()) {
            this.mousePreX.to = 0.0F;
            this.mousePreY.to = 0.0F;
         } else {
            this.mousePreX.to = (Minecraft.player.lastReportedPreYaw - Minecraft.player.rotationYaw) * 3.5F * (float)vantuz;
            this.mousePreY.to = (EntityPlayerSP.lastReportedPrePitch - Minecraft.player.rotationPitch) * 5.0F * (float)vantuz;
         }

         this.crossPosMotions[0] = -this.mousePreX.getAnim();
         this.crossPosMotions[1] = -this.mousePreY.getAnim();
      }
   }

   public float[] getMouseMotion() {
      return new float[]{this.mousePreX.anim, this.mousePreY.anim};
   }

   public void draw(float x, float y, int color) {
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );
      RenderUtils.drawPolygonPartsGlowBackSAlpha((double)x, (double)y, 4.0F, 1, color, 0, ColorUtils.getGLAlphaFromColor(color), true);
   }
}
