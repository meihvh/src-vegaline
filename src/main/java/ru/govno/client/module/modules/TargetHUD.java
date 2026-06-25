package ru.govno.client.module.modules;

import com.google.common.collect.Lists;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import javax.vecmath.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventRender2D;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.CrystalField;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.GaussianBlur;
import ru.govno.client.utils.Render.HeadSkinUVLivingBase;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;
import ru.govno.client.utils.Render.StringIntsAnimator;
import ru.govno.client.utils.URender.other.NoiseAnimation;
import ru.govno.client.utils.Voronoi.VoronoiOfCopyRenderTemp;
import ru.govno.client.utils.Voronoi.VoronoiOfQuad;

public class TargetHUD extends Module {
   public static TargetHUD get;
   public static EntityLivingBase curTarget = null;
   public static EntityLivingBase soundTarget = null;
   static ArrayList<TargetHUD.particle> particles = new ArrayList<>();
   public static float xPosHud;
   public static float yPosHud;
   public static float widthHud;
   public static float heightHud;
   AnimationUtils Scale = new AnimationUtils(1.0F, 1.0F, 0.07F);
   public int framesShowingForce;
   private float armorHealth;
   public ModeSettings Mode;
   public FloatSettings THudX;
   public FloatSettings THudY;
   public BoolSettings PreRangedTarget;
   public BoolSettings RaycastTarget;
   public BoolSettings CastPosition;
   public BoolSettings TargettingSFX;
   float hpRectAnim = 0.0F;
   String targetName = "";
   AnimationUtils alphaHp = new AnimationUtils(0.0F, 0.0F, 0.075F);
   final AnimationUtils hpAnim = new AnimationUtils(1.0F, 1.0F, 0.05F);
   final AnimationUtils absorbAnim = new AnimationUtils(0.0F, 0.0F, 0.05F);
   final AnimationUtils hurtHpAnim = new AnimationUtils(1.0F, 1.0F, 0.05F);
   private float prevHp;
   private float prevAbsb;
   private final List<TargetHUD.VoronoiOfCopyRenderTempEvent> voronoiRenderTempEventList = Lists.newArrayList();
   private EntityLivingBase prevFrameTargetEntity;
   private boolean hasLastDispatchTick = false;
   private final StringIntsAnimator hpStringAnim = new StringIntsAnimator();
   private final int[] lastUpdatedResColors = new int[4];
   public static ResourceLocation skin = null;
   public static ResourceLocation OldSkin = null;
   NoiseAnimation noiser = new NoiseAnimation();
   int targetHurt;

   float Scale() {
      return this.Scale.getAnim();
   }

   float Scaleclamp() {
      return MathUtils.clamp(this.Scale(), 0.0F, 1.0F);
   }

   public TargetHUD() {
      super("TargetHUD", 0, Module.Category.COMBAT);
      this.settings
         .add(this.Mode = new ModeSettings("Mode", "Light", this, new String[]{"Light", "WetWorn", "Neomoin", "Modern", "Bushy", "Subtle", "Entire", "Oasis"}));
      this.settings.add(this.THudX = new FloatSettings("T-Hud X", 0.45F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.THudY = new FloatSettings("T-Hud Y", 0.6F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.PreRangedTarget = new BoolSettings("PreRangedTarget", true, this));
      this.settings.add(this.RaycastTarget = new BoolSettings("RaycastTarget", true, this));
      this.settings.add(this.CastPosition = new BoolSettings("CastPosition", false, this));
      this.settings.add(this.TargettingSFX = new BoolSettings("TargettingSFX", true, this));
      this.setDemand(3, 2);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   private Vector3d project2D(int scaleFactor, double x, double y, double z) {
      GL11.glGetFloat(2982, RenderUtils.modelview);
      GL11.glGetFloat(2983, RenderUtils.projection);
      GL11.glGetInteger(2978, RenderUtils.viewport);
      return GLU.gluProject((float)x, (float)y, (float)z, RenderUtils.modelview, RenderUtils.projection, RenderUtils.viewport, RenderUtils.vector)
         ? new Vector3d(
            (double)(RenderUtils.vector.get(0) / (float)scaleFactor),
            (double)(((float)Display.getHeight() - RenderUtils.vector.get(1)) / (float)scaleFactor),
            (double)RenderUtils.vector.get(2)
         )
         : null;
   }

   float[] castPosition(EventRender2D event) {
      float xn = -1.0F;
      float yn = -1.0F;
      EntityLivingBase entity = curTarget;
      if (entity != null && entity != Minecraft.player) {
         ScaledResolution scaledResolution = event.getResolution();
         int scaleFactor = ScaledResolution.getScaleFactor();
         double x = RenderUtils.interpolate(entity.posX, entity.prevPosX, (double)event.getPartialTicks());
         double y = RenderUtils.interpolate(entity.posY, entity.prevPosY, (double)event.getPartialTicks());
         double z = RenderUtils.interpolate(entity.posZ, entity.prevPosZ, (double)event.getPartialTicks());
         double height = (double)entity.getEyeHeight() / (entity.isChild() ? 1.80042 : 1.0);
         AxisAlignedBB aabb = new AxisAlignedBB(x, y, z, x, y + height, z);
         Vector3d[] vectors = new Vector3d[]{
            new Vector3d(aabb.minX, aabb.minY, aabb.minZ),
            new Vector3d(aabb.minX, aabb.maxY, aabb.minZ),
            new Vector3d(aabb.maxX, aabb.minY, aabb.minZ),
            new Vector3d(aabb.maxX, aabb.maxY, aabb.minZ),
            new Vector3d(aabb.minX, aabb.minY, aabb.maxZ),
            new Vector3d(aabb.minX, aabb.maxY, aabb.maxZ),
            new Vector3d(aabb.maxX, aabb.minY, aabb.maxZ),
            new Vector3d(aabb.maxX, aabb.maxY, aabb.maxZ)
         };
         mc.entityRenderer.setupCameraTransformCompactCalcMatrix(event.getPartialTicks());
         Vector4d position = null;
         Vector3d[] vecList = vectors;
         int vecLength = vectors.length;

         for (int l = 0; l < vecLength; l++) {
            Vector3d vector = this.project2D(
               scaleFactor, vecList[l].x - RenderManager.viewerPosX, vecList[l].y - RenderManager.viewerPosY, vecList[l].z - RenderManager.viewerPosZ
            );
            if (vector != null && vector.z >= 0.0 && vector.z < 1.0) {
               if (position == null) {
                  position = new Vector4d(vector.x, vector.y, vector.z, 0.0);
               }

               position.x = Math.min(vector.x, position.x);
               position.y = Math.min(vector.y, position.y);
               position.z = Math.max(vector.x, position.z);
               position.w = Math.max(vector.y, position.w);
            }
         }

         mc.entityRenderer.setupOverlayRendering();
         if (position != null) {
            double posX = position.x;
            double posY = position.y;
            double endPosX = position.z;
            double endPosY = position.w;
            xn = (float)(posX + (endPosX - posX) / 2.0) - widthHud / 2.0F;
            yn = (float)posY + heightHud / 2.0F;
         }
      }

      return new float[]{xn, yn};
   }

   private boolean castPosIsValid(float[] position) {
      return position[0] != -1.0F && position[1] != -1.0F;
   }

   void updatePosition(EventRender2D event) {
      float animSpeed = (float)Minecraft.frameTime * 0.035F;
      float animSpeedCast = (float)Minecraft.frameTime * 0.0075F;
      float[] castedPosition = new float[]{-1.0F, -1.0F};
      if (this.CastPosition.getBool()) {
         castedPosition = this.castPosition(event);
      }

      if (this.CastPosition.getBool()
         && castedPosition[0] > 0.0F
         && castedPosition[0] < (float)event.getResolution().getScaledWidth() - widthHud
         && castedPosition[1] > 0.0F
         && castedPosition[1] < (float)event.getResolution().getScaledHeight() - heightHud
         && !(mc.currentScreen instanceof GuiChat)
         && getTarget() != null
         && this.castPosIsValid(castedPosition)) {
         xPosHud = MathUtils.harp(xPosHud, castedPosition[0], animSpeedCast);
         yPosHud = MathUtils.harp(yPosHud, castedPosition[1], animSpeedCast);
      } else {
         xPosHud = MathUtils.harp(xPosHud, this.THudX.getFloat() * (float)event.getResolution().getScaledWidth() - widthHud / 2.0F, animSpeed);
         yPosHud = MathUtils.harp(yPosHud, this.THudY.getFloat() * (float)event.getResolution().getScaledHeight() - heightHud / 2.0F, animSpeed / 2.0F);
      }
   }

   void renderLight(EntityLivingBase target) {
      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      String hp = target.getHealth() == 0.0F ? "" : String.format("%.1f", this.hpRectAnim * target.getMaxHealth() + target.getAbsorptionAmount()) + "hp";
      float w = (float)((int)widthHud);
      float h = (float)((int)heightHud);
      CFontRenderer namefont = Fonts.mntsb_16;
      float x = xPosHud;
      float y = yPosHud;
      float texX = x + h;
      widthHud = MathUtils.clamp(texX - x + namefont.getStringWidth(this.targetName) + 4.0F, 100.0F, 900.0F);
      heightHud = 38.0F;
      this.hpRectAnim = MathUtils.lerp(
         this.hpRectAnim, MathUtils.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F), 0.015F * (float)Minecraft.frameTime
      );
      float percScaled = MathUtils.clamp(this.Scale() * this.Scale(), 0.0F, 1.0F);
      int bgC = ColorUtils.swapAlpha(-1, 240.0F * percScaled);
      int roundC = ColorUtils.swapAlpha(-1, 125.0F * percScaled * percScaled);
      int bgSH = ColorUtils.swapAlpha(-1, 55.0F * percScaled);
      int hpSH = ColorUtils.swapAlpha(0, 35.0F * percScaled * percScaled);
      int hpbgC = ColorUtils.swapAlpha(0, 55.0F * percScaled);
      int hpSC = ColorUtils.getColor(0, 0, 0, this.alphaHp.getAnim() * percScaled * percScaled);
      int texC = ColorUtils.swapAlpha(ColorUtils.getColor(20, 20, 20), 255.0F * percScaled);
      int itC = ColorUtils.swapAlpha(ColorUtils.getColor(0, 0, 0), 255.0F * percScaled);
      int hpC = ColorUtils.swapAlpha(ClientColors.getColor1(0), 255.0F * percScaled);
      int hpC2 = ColorUtils.getOverallColorFrom(hpC, ColorUtils.swapAlpha(ClientColors.getColor2(30), 255.0F * percScaled), this.hpRectAnim);
      int hpbgC2 = ColorUtils.swapAlpha(hpC, 85.0F * percScaled);
      int hpbgC3 = ColorUtils.swapAlpha(hpC2, 85.0F * percScaled);
      GL11.glPushMatrix();
      GL11.glTranslated(0.0, (double)(((this.Scale() > 1.0F ? this.Scale() * this.Scale() : this.Scale()) - 1.0F) * -8.0F), 0.0);
      StencilUtil.initStencilToWrite();
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         2.5F, 2.5F, h - 2.5F, h - 2.5F, 5.0F, 0.0F, -1, -1, -1, -1, false, true, false
      );
      StencilUtil.readStencilBuffer(0);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 5.0F, 1.25F, bgC, bgC, bgC, bgC, true, true, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 5.0F, 18.0F, bgSH, bgSH, bgSH, bgSH, true, false, true
      );
      StencilUtil.uninitStencilBuffer();
      GlStateManager.enableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.tryBlendFuncSeparate(
         GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
      );

      for (int i = 0; i < particles.size(); i++) {
         TargetHUD.particle particle = particles.get(i);
         float timePC = (float)particle.getTime() / 700.0F;
         if (timePC >= 1.0F) {
            particles.remove(particle);
         } else {
            int pc = ColorUtils.getOverallColorFrom(hpC, hpC2, timePC);
            int pColor = ColorUtils.swapAlpha(pc, 255.0F * (1.0F - timePC) * percScaled);
            particle.update(pColor);
         }
      }

      if (target.hurtTime > 8) {
         for (int count = 0; count < 2; count++) {
            particles.add(new TargetHUD.particle(x + h / 2.0F, y + h / 2.0F));
         }
      }

      if (res != null) {
         StencilUtil.initStencilToWrite();
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x + 2.5F, y + 2.5F, x + h - 2.5F, y + h - 2.5F, 5.0F, 0.0F, -1, -1, -1, -1, false, true, false
         );
         StencilUtil.readStencilBuffer(1);
         HeadSkinUVLivingBase.of(target)
            .setWhite(percScaled * (0.5F + percScaled / 2.0F), true)
            .bindTex(res)
            .tessellateDefaultQuad(x + 2.5F, y + 2.5F, x + h - 2.5F, y + h - 2.5F);
         StencilUtil.uninitStencilBuffer();
         RenderUtils.roundedFullRoundedOutline(x + 2.5F, y + 2.5F, x + h - 2.5F, y + h - 2.5F, 5.1F, 5.25F, 5.25F, 5.25F, roundC);
      }

      if (ColorUtils.getAlphaFromColor(texC) >= 28) {
         namefont.drawStringWithShadow(this.targetName, texX, y + 4.5F, texC);
      }

      float hpX1 = x + h + 1.0F;
      float hpX2 = MathUtils.clamp(x + h + 1.0F + (w - h - 5.0F) * this.hpRectAnim, hpX1 + 4.0F, x + h + 1.0F + (w - h - 5.0F) * this.hpRectAnim);
      float hpX3 = x + h + 1.0F + (w - h - 5.0F);
      this.alphaHp.to = MathUtils.getDifferenceOf(hpX2, hpX3) > 9.0F ? 255.0F : 0.0F;
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         hpX1, y + h - 8.5F, hpX3, y + h - 4.0F, 2.0F, 1.0F, hpbgC, hpbgC, hpbgC, hpbgC, false, true, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         hpX1, y + h - 8.5F, hpX3, y + h - 4.0F, 2.0F, 2.0F, hpSH, hpSH, hpSH, hpSH, false, false, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         hpX1, y + h - 8.5F, hpX2, y + h - 4.0F, 2.0F, 3.0F, hpbgC2, hpbgC3, hpbgC3, hpbgC2, false, true, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         hpX1, y + h - 8.5F, hpX2, y + h - 4.0F, 2.0F, 0.5F, hpC, hpC2, hpC2, hpC, false, true, true
      );
      StencilUtil.initStencilToWrite();
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         hpX1, y + h - 8.5F, hpX3, y + h - 3.5F, 2.0F, 0.5F, -1, -1, -1, -1, false, true, false
      );
      StencilUtil.readStencilBuffer(1);
      if (ColorUtils.getAlphaFromColor(hpSC) >= 28) {
         Fonts.mntsb_12.drawString(hp, hpX2 + 1.0F, y + h - 7.0F, hpSC);
      }

      StencilUtil.uninitStencilBuffer();
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;

      for (ItemStack ix : stuff) {
         if (ix != null) {
            ix.getItem();
            stacks.add(ix);
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      if (stacksCount != 0) {
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x + h + 2.0F, y + h - 23.0F, x + h + 4.0F + (float)stacksCount * 8.6F, y + h - 13.0F, 4.0F, 0.5F, itC, itC, itC, itC, false, true, true
         );
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x + h + 2.0F, y + h - 23.0F, x + h + 4.0F + (float)stacksCount * 8.6F, y + h - 13.0F, 4.0F, 5.5F, hpbgC, hpbgC, hpbgC, hpbgC, false, false, true
         );
      }

      RenderItem itemRender = mc.getRenderItem();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.enableDepth();
      float xn = h - 5.0F;
      float yn = 16.0F;
      GL11.glTranslated((double)x, (double)y, 0.0);
      xn *= 2.0F;
      yn *= 2.0F;
      GL11.glScaled(0.5, 0.5, 0.5);

      for (ItemStack stackx : stacks) {
         if (!stackx.isEmpty()) {
            xn += 17.0F;
         }

         GL11.glTranslated((double)xn, (double)yn, 0.0);
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)(percScaled * percScaled), (double)(percScaled * percScaled), 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         itemRender.zLevel = 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
         if (255.0F * percScaled >= 26.0F) {
            itemRender.renderItemOverlayIntoGUIWithTextColor(
               Fonts.minecraftia_16,
               stackx,
               0,
               0,
               stackx.getCount(),
               ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), MathUtils.clamp(255.0F * percScaled, 30.0F, 255.0F))
            );
         }

         RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, percScaled, 1.0F);
         itemRender.zLevel = 0.0F;
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)(1.0F / (percScaled * percScaled)), (double)(1.0F / (percScaled * percScaled)), 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
      }

      GL11.glScaled(2.0, 2.0, 2.0);
      xn /= 2.0F;
      yn /= 2.0F;
      GL11.glTranslated((double)(-x), (double)(-y), 0.0);
      RenderHelper.disableStandardItemLighting();
      GL11.glPopMatrix();
   }

   void renderWetWorn(EntityLivingBase target) {
      new ScaledResolution(mc);
      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      float aPC = this.Scaleclamp();
      aPC *= aPC;
      float Scale = this.Scale();
      String hp = target.getHealth() == 0.0F ? "" : String.format("%.1f", this.hpRectAnim * target.getMaxHealth() + target.getAbsorptionAmount()) + "hp";
      float w = widthHud;
      float h = heightHud;
      CFontRenderer namefont = Fonts.mntsb_15;
      float x = xPosHud;
      float y = yPosHud;
      float texX = x + h;
      widthHud = MathUtils.clamp(texX - x + namefont.getStringWidth(this.targetName) + 4.0F, 100.0F, 900.0F);
      heightHud = 36.0F;
      this.hpRectAnim = MathUtils.lerp(
         this.hpRectAnim, MathUtils.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F), 0.015F * (float)Minecraft.frameTime
      );
      this.armorHealth = MathUtils.lerp(this.armorHealth, this.getArmorPercent01(target), 0.015F * (float)Minecraft.frameTime);
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;
      ItemStack[] array = stuff;
      int length = stuff.length;
      if ((double)Scale > 0.5) {
         for (int var41 = 0; var41 < length; var41++) {
            ItemStack i = array[var41];
            if (i != null) {
               i.getItem();
               stacks.add(i);
            }
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      int bgOutC = ColorUtils.swapAlpha(ClientColors.getColorQ(1), (float)ColorUtils.getAlphaFromColor(ClientColors.getColorQ(1)) * 0.9F * aPC * aPC);
      int bgOutC2 = ColorUtils.swapAlpha(ClientColors.getColorQ(2), (float)ColorUtils.getAlphaFromColor(ClientColors.getColorQ(2)) * 0.9F * aPC * aPC);
      int bgOutC3 = ColorUtils.swapAlpha(ClientColors.getColorQ(3), (float)ColorUtils.getAlphaFromColor(ClientColors.getColorQ(3)) * 0.9F * aPC * aPC);
      int bgOutC4 = ColorUtils.swapAlpha(ClientColors.getColorQ(4), (float)ColorUtils.getAlphaFromColor(ClientColors.getColorQ(4)) * 0.9F * aPC * aPC);
      int bgC1 = ColorUtils.getOverallColorFrom(bgOutC, 0, 0.6F);
      int bgC2 = ColorUtils.getOverallColorFrom(bgOutC2, 0, 0.6F);
      int hpBG = ColorUtils.getColor(0, 0, 0, 50.0F * aPC);
      int texC = ColorUtils.swapAlpha(-1, 200.0F * aPC * aPC);
      int texC2 = ColorUtils.swapAlpha(0, 175.0F * aPC);
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 5.0F - aPC * 5.0F, 0.0F);
      RenderUtils.customScaledObject2D(x, y, w, h, Scale);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 4.0F, 2.5F, bgOutC, bgOutC2, bgOutC3, bgOutC4, false, false, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 4.0F, 1.0F, bgC1, bgC2, bgC2, bgC1, false, true, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 4.0F, 1.5F, bgC1, bgC2, bgC2, bgC1, false, false, true
      );
      if ((double)Scale > 0.5) {
         float xExt = w / 2.0F * (Scale - 1.0F);
         float yExt = h / 2.0F * (Scale - 1.0F);
         RenderUtils.customScaledObject2D(x, y, w, h, 1.0F / Scale);
         GaussianBlur.drawRoundedBlurNotStencil(
            3.0F * aPC, x - xExt - 0.5F, y - yExt - 0.5F, x + w + xExt + 0.5F, y + h + yExt + 0.5F, 4.5F * Scale, "TH-renderWetWorn"
         );
         RenderUtils.customScaledObject2D(x, y, w, h, Scale);
      }

      if (target.hurtTime > 4) {
         float rt = (float)(-5.0 * Math.random() + 10.0 * Math.random());
         float rt2 = (float)(-5.0 * Math.random() + 10.0 * Math.random());
         particles.add(new TargetHUD.particle(x + h / 2.0F + rt, y + h / 2.0F + rt2));
      }

      for (int i = 0; i < particles.size(); i++) {
         TargetHUD.particle particle = particles.get(i);
         float timePC = (float)particle.getTime() / 700.0F;
         if (timePC >= 1.0F) {
            particles.remove(particle);
         } else {
            int pc = ColorUtils.getOverallColorFrom(bgOutC, bgOutC2, timePC);
            int pColor = ColorUtils.swapAlpha(pc, 255.0F * (1.0F - timePC) * aPC);
            particle.update(pColor);
         }
      }

      RenderUtils.customScaledObject2D(x + 4.0F, y + 4.0F, h - 4.0F, h - 4.0F, 0.8F + aPC / 5.0F);
      if (res != null) {
         mc.getTextureManager().bindTexture(res);
         StencilUtil.initStencilToWrite();
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x + 4.0F, y + 4.0F, x + h - 4.0F, y + h - 4.0F, 4.5F, 1.0F, -1, -1, -1, -1, false, true, false
         );
         StencilUtil.readStencilBuffer(1);
         HeadSkinUVLivingBase.of(target).setWhite(aPC * aPC, true).bindTex(res).tessellateDefaultQuad(x + 4.0F, y + 4.0F, x + h - 4.0F, y + h - 4.0F);
         StencilUtil.uninitStencilBuffer();
         RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
            x + 4.0F, y + 4.0F, x + h - 4.0F, y + h - 4.0F, 3.0F, 1.5F, 1.0F, bgOutC, bgOutC2, bgOutC3, bgOutC4, false, true, true
         );
      } else {
         Fonts.noise_24.drawString("?", x + h / 2.0F - 2.5F, y + h / 2.0F - 5.0F, bgOutC);
      }

      RenderUtils.customScaledObject2D(x + 4.0F, y + 4.0F, h - 4.0F, h - 4.0F, 1.0F / (0.8F + aPC / 5.0F));
      RenderUtils.customScaledObject2D(x + h + 2.0F, y + h - 13.0F, x + w - 4.0F - (x + h + 2.0F), 9.0F, 0.8F + aPC / 5.0F);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x + h + 2.0F, y + h - 13.0F, x + w - 4.0F, y + h - 4.0F, 2.5F, 0.5F, hpBG, hpBG, hpBG, hpBG, false, true, true
      );
      float hp2x = x + h + 3.0F + (x + w - 5.0F - (x + h + 3.0F)) * this.hpRectAnim;
      StencilUtil.initStencilToWrite();
      RenderUtils.drawRect((double)(x + h + 2.0F), (double)(y + h - 12.5F), (double)(hp2x + 1.0F), (double)(y + h - 4.5F), -1);
      StencilUtil.readStencilBuffer(1);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x + h + 3.0F,
         y + h - 12.0F,
         x + w - 5.0F,
         y + h - 5.0F,
         2.0F,
         1.0F,
         ColorUtils.getOverallColorFrom(bgOutC, bgOutC4, 0.725F),
         ColorUtils.getOverallColorFrom(bgOutC2, bgOutC3, 0.725F),
         bgOutC3,
         bgOutC4,
         false,
         true,
         true
      );
      if (ColorUtils.getAlphaFromColor(texC2) >= 33) {
         Fonts.mntsb_12
            .drawString(
               hp,
               MathUtils.clamp(
                  hp2x - Fonts.mntsb_12.getStringWidth(hp),
                  x + h + 3.5F,
                  x + h + 3.0F + (x + w - 5.0F - (x + h + 3.0F)) * 0.5F - Fonts.mntsb_12.getStringWidth(hp) / 2.0F + 1.0F
               ),
               y + h - 9.5F,
               texC2
            );
      }

      StencilUtil.uninitStencilBuffer();
      RenderUtils.customScaledObject2D(x + h + 2.0F, y + h - 13.0F, x + w - 4.0F - (x + h + 2.0F), 9.0F, 1.0F / (0.8F + aPC / 5.0F));
      if (ColorUtils.getAlphaFromColor(texC) >= 33) {
         if (stacksCount == 0) {
            y += 5.0F;
         }

         RenderUtils.customScaledObject2D(
            x + h + 3.0F + (x + w - 5.0F - (x + h + 3.0F)) * 0.5F - Fonts.mntsb_13.getStringWidth(this.targetName) / 2.0F,
            y + 6.0F,
            Fonts.mntsb_13.getStringWidth(this.targetName),
            Fonts.mntsb_13.getHeight() / 2.0F,
            0.8F + aPC / 5.0F
         );
         namefont.drawStringWithShadow(
            this.targetName, x + h + 3.0F + (x + w - 5.0F - (x + h + 3.0F)) * 0.5F - namefont.getStringWidth(this.targetName) / 2.0F - 1.0F, y + 5.0F, texC
         );
         RenderUtils.customScaledObject2D(
            x + h + 3.0F + (x + w - 5.0F - (x + h + 3.0F)) * 0.5F - Fonts.mntsb_13.getStringWidth(this.targetName) / 2.0F,
            y + 6.0F,
            Fonts.mntsb_13.getStringWidth(this.targetName),
            Fonts.mntsb_13.getHeight() / 2.0F,
            1.0F / (0.8F + aPC / 5.0F)
         );
         if (stacksCount == 0) {
            y -= 5.0F;
         }
      }

      RenderItem itemRender = mc.getRenderItem();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.enableDepth();
      float xn = h - 5.0F;
      float yn = 12.0F;
      GL11.glTranslated((double)x, (double)y, 0.0);
      xn *= 2.0F;
      yn *= 2.0F;
      GL11.glScaled(0.5, 0.5, 0.5);

      for (ItemStack stackx : stacks) {
         if (!stackx.isEmpty()) {
            xn += 17.0F;
         }

         GL11.glTranslated((double)xn, (double)yn, 0.0);
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)aPC, (double)aPC, 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         itemRender.zLevel = 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
         if (ColorUtils.getAlphaFromColor(texC) >= 33) {
            itemRender.renderItemOverlayIntoGUIWithTextColor(Fonts.minecraftia_16, stackx, 0, 0, stackx.getCount(), texC);
         }

         RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, aPC, 1.0F);
         itemRender.zLevel = 0.0F;
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)(1.0F / aPC), (double)(1.0F / aPC), 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
      }

      GL11.glScaled(2.0, 2.0, 2.0);
      xn /= 2.0F;
      yn /= 2.0F;
      GL11.glTranslated((double)(-x), (double)(-y), 0.0);
      RenderHelper.disableStandardItemLighting();
      GlStateManager.popMatrix();
   }

   private void neomoinCircle(float x, float y, float r, float lineWHP, float lineWDMG, float aPC, float pcHP, float pcDMG, boolean isBG, int colBG) {
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.disableAlpha();
      GlStateManager.shadeModel(7425);
      GlStateManager.resetColor();
      GL11.glScaled(0.5, 0.5, 0.5);
      GL11.glTranslated((double)x, (double)y, 0.0);
      GL11.glEnable(2832);
      GL11.glDisable(3008);
      GL11.glPointSize(lineWHP);
      float startHP = isBG ? 0.0F : 180.0F;
      float endHP = isBG ? 360.0F : 180.0F + pcHP * 360.0F;
      float endDMG = endHP + (pcDMG - pcHP) * 360.0F;
      int step = isBG ? 15 : 3;
      boolean draw = startHP < endHP;
      if (draw) {
         RenderUtils.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
      }

      for (float i = startHP; i < endHP; i += (float)step) {
         int c = isBG ? colBG : ClientColors.getColor1((int)(i * 3.0F), aPC * aPC * aPC);
         double x1 = (double)(x + MathHelper.sin(i * (float) Math.PI / 180.0F) * r * 2.0F);
         double y1 = (double)(y + MathHelper.cos(i * (float) Math.PI / 180.0F) * r * 2.0F);
         RenderUtils.buffer.pos(x1, y1).color(c).endVertex();
      }

      if (draw) {
         RenderUtils.tessellator.draw();
      }

      GL11.glPointSize(lineWDMG);
      if (!isBG) {
         draw = endHP < endDMG;
         if (draw) {
            RenderUtils.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
         }

         float cucan = 0.0F;

         for (float i = endHP; i < endDMG; i += (float)step) {
            cucan++;
         }

         float cucan2 = 0.0F;

         for (float i = endHP; i < endDMG; i += (float)step) {
            float progressAPC = cucan2 / cucan;
            int c = ClientColors.getColor1((int)(i * 3.0F), aPC * aPC * aPC * progressAPC * progressAPC);
            double x1 = (double)(x + MathHelper.sin(i * (float) Math.PI / 180.0F) * r * 2.0F);
            double y1 = (double)(y + MathHelper.cos(i * (float) Math.PI / 180.0F) * r * 2.0F);
            RenderUtils.buffer.pos(x1, y1).color(c).endVertex();
            cucan2++;
         }

         if (draw) {
            RenderUtils.tessellator.draw();
         }
      }

      GL11.glTranslated((double)(-x), (double)(-y), 0.0);
      GL11.glScaled(2.0, 2.0, 2.0);
      GL11.glPointSize(1.0F);
      GL11.glDisable(2832);
      GL11.glEnable(3008);
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableAlpha();
      GlStateManager.shadeModel(7424);
      GlStateManager.resetColor();
   }

   private final void renderNeomoin(EntityLivingBase target) {
      new ScaledResolution(mc);
      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      float aPC = this.Scaleclamp();
      float Scale = this.Scale();
      if (target.getHealth() == 0.0F) {
//         String var10000 = "";
      } else {
//         String.format("%.1f", this.hpRectAnim * target.getMaxHealth() + target.getAbsorptionAmount()) + "hp";
      }

      float w = widthHud;
      float h = heightHud;
      CFontRenderer namefont = Fonts.mntsb_13;
      float x = xPosHud;
      float y = yPosHud + (1.0F - aPC) * 3.0F;
      float texX = x + h * 2.0F - 4.0F;
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;

      for (ItemStack i : stuff) {
         if (i != null) {
            i.getItem();
            stacks.add(i);
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      float wHD = texX - x + namefont.getStringWidth(this.targetName) + 4.0F;
      if (h * 2.0F + 8.5F * (float)stacksCount + 5.0F > wHD) {
         wHD = h * 2.0F + 8.5F * (float)stacksCount + 5.0F;
      }

      widthHud = MathUtils.clamp(MathUtils.lerp(widthHud, wHD, 0.01F * (float)Minecraft.frameTime), h * 2.0F, 900.0F);
      heightHud = 30.0F;
      this.hpAnim.speed = 0.075F;
      this.hurtHpAnim.speed = 0.1F;
      this.hpAnim.to = MathUtils.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);
      if (target.hurtTime < 3) {
         this.hurtHpAnim.to = MathUtils.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F);
      }

      if (this.hurtHpAnim.getAnim() < this.hpAnim.getAnim()) {
         this.hurtHpAnim.setAnim(this.hpAnim.getAnim());
      }

      float hpRectAnim = this.hpAnim.getAnim();
      float hurtRectAnim = this.hurtHpAnim.getAnim();
      int accentV1 = ColorUtils.swapAlpha(ClientColors.getColor1(), (float)ColorUtils.getAlphaFromColor(ClientColors.getColor1()) * aPC);
      int accentV2 = ColorUtils.swapAlpha(ClientColors.getColor2(), (float)ColorUtils.getAlphaFromColor(ClientColors.getColor2()) * aPC);
      int accent1 = ColorUtils.swapAlpha(accentV1, (float)ColorUtils.getAlphaFromColor(accentV1) * aPC);
      int accent2 = ColorUtils.swapAlpha(accentV2, (float)ColorUtils.getAlphaFromColor(accentV2) * aPC);
      int bgc1 = ColorUtils.getOverallColorFrom(accent1, ColorUtils.getColor(0, 0, 0, ColorUtils.getAlphaFromColor(accent1)), 0.8F);
      int bgc2 = ColorUtils.getOverallColorFrom(accent2, ColorUtils.getColor(0, 0, 0, ColorUtils.getAlphaFromColor(accent2)), 0.8F);
      int bg1 = ColorUtils.swapAlpha(bgc1, (float)ColorUtils.getAlphaFromColor(bgc1) * aPC / 2.0F);
      int bg2 = ColorUtils.swapAlpha(bgc2, (float)ColorUtils.getAlphaFromColor(bgc2) * aPC / 2.0F);
      int bgOut1 = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(accent1, bgc1, 0.6F), (float)ColorUtils.getAlphaFromColor(bgc1) * aPC);
      int bgOut2 = ColorUtils.swapAlpha(ColorUtils.getOverallColorFrom(accent2, bgc2, 0.6F), (float)ColorUtils.getAlphaFromColor(bgc2) * aPC);
      int hpBgC = ColorUtils.getColor(11, 11, 11, 60.0F * aPC);
      int hpBgCOut = ColorUtils.getColor(11, 11, 11, 100.0F * aPC);
      int texCol = ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), 255.0F * aPC * aPC);
      int itemsBgCol = ColorUtils.getColor(11, 11, 11, 80.0F * aPC);
      GL11.glPushMatrix();
      RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
         x, y, x + w, y + h, 3.5F, 2.0F, 6.0F, bg1, bg2, bg2, bg1, true, true, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 5.0F, 1.0F, bg1, bg2, bg2, bg1, false, true, true
      );
      if ((double)Scale > 0.5) {
         float xExt = 0.0F;
         float yExt = 0.0F;
         GaussianBlur.drawRoundedBlurNotStencil(6.0F * aPC, x - xExt, y - yExt, x + w + xExt, y + h + yExt, 5.0F * Scale, "TH-renderNeomoin");
      }

      if (target.hurtTime > 7) {
         float rt = (float)(-5.0 * Math.random() + 10.0 * Math.random());
         float rt2 = (float)(-5.0 * Math.random() + 10.0 * Math.random());
         particles.add(new TargetHUD.particle(x + h / 2.0F + rt, y + h / 2.0F + rt2));
      }

      for (int ix = 0; ix < particles.size(); ix++) {
         TargetHUD.particle particle = particles.get(ix);
         float timePC = (float)particle.getTime() / 700.0F;
         if (timePC >= 1.0F) {
            particles.remove(particle);
         } else {
            int pC = ColorUtils.swapAlpha(ClientColors.getColor1(ix * 10), (float)ColorUtils.getAlphaFromColor(ClientColors.getColor1(ix * 10)) * aPC);
            int pc1 = ColorUtils.swapAlpha(pC, (float)ColorUtils.getAlphaFromColor(pC) * aPC * aPC);
            int pColor = ColorUtils.swapAlpha(pc1, 255.0F * (1.0F - timePC) * aPC);
            particle.update(pColor);
         }
      }

      if (res != null) {
         StencilUtil.renderInStencil(
            () -> RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                  x + 3.0F, y + 3.0F, x + h - 3.0F, y + h - 3.0F, 3.8F, 0.0F, -1, -1, -1, -1, false, true, false
               ),
            () -> HeadSkinUVLivingBase.of(target).setWhite(aPC * aPC, true).bindTex(res).tessellateDefaultQuad(x + 3.0F, y + 3.0F, x + h - 3.0F, y + h - 3.0F),
            1
         );
         RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
            x + 3.0F,
            y + 3.0F,
            x + h - 3.0F,
            y + h - 3.0F,
            3.0F,
            1.5F,
            1.0F,
            accent1,
            ColorUtils.getOverallColorFrom(accent1, accent2, h / w),
            ColorUtils.getOverallColorFrom(accent1, accent2, h / w),
            accent1,
            false,
            true,
            true
         );
      } else if (ColorUtils.getAlphaFromColor(bgOut1) >= 33) {
         Fonts.noise_24.drawString("?", x + h / 2.0F - 2.5F, y + h / 2.0F - 5.0F, bgOut1);
      }

      float rPlus = MathUtils.clamp((hurtRectAnim - hpRectAnim) * target.getMaxHealth() / 3.5F, 0.0F, 3.0F) * 1.2F;
      float cx = x + w - h / 2.0F;
      float cy = y + h / 2.0F;
      float cr = h / 2.0F - 4.0F + rPlus;
      float lpSCFactor = ScaledResolution.lpSCFactor();
      float cwHP = (3.5F + rPlus) * lpSCFactor;
      float cwDMG = (3.0F + rPlus) * lpSCFactor;
      this.neomoinCircle(cx, cy, cr, cwHP, cwDMG, aPC, 1.0F, 1.0F, true, hpBgCOut);
      this.neomoinCircle(cx, cy, cr, cwHP, cwDMG, aPC, hpRectAnim, hurtRectAnim, false, 0);
      float r = 8.5F;
      RenderUtils.drawSmoothCircle((double)(x + w - h / 2.0F), (double)(y + h / 2.0F), r, hpBgC);
      String hpStr = String.format("%.1f", hpRectAnim * target.getMaxHealth()).replace(".", ",");
      if (ColorUtils.getAlphaFromColor(texCol) >= 33) {
         Fonts.mntsb_12.drawString(hpStr, x + w - h / 2.0F - Fonts.mntsb_12.getStringWidth(hpStr) / 2.0F, y + h / 2.0F - 1.0F, texCol);
      }

      if (ColorUtils.getAlphaFromColor(texCol) >= 33) {
         namefont.drawString(this.targetName, x + h + 1.0F, y + 5.0F, texCol);
      }

      RenderItem itemRender = mc.getRenderItem();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.enableDepth();
      float xn = h - 5.0F;
      float yn = 16.0F;
      if (stacksCount != 0) {
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x + h + 2.5F,
            y + yn - 1.0F,
            x + h + 4.0F + 8.5F * (float)stacksCount,
            y + yn + 9.0F,
            2.0F,
            1.0F,
            itemsBgCol,
            itemsBgCol,
            itemsBgCol,
            itemsBgCol,
            false,
            true,
            true
         );
      }

      GlStateManager.enableDepth();
      GL11.glTranslated((double)x, (double)y, 0.0);
      xn *= 2.0F;
      yn *= 2.0F;
      GL11.glScaled(0.5, 0.5, 0.5);

      for (ItemStack stackx : stacks) {
         if (!stackx.isEmpty()) {
            xn += 17.0F;
         }

         double scaleI = (double)(aPC * aPC);
         if (scaleI < 0.1F) {
            break;
         }

         GL11.glTranslated((double)xn, (double)yn, 0.0);
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled(scaleI, scaleI, 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         itemRender.zLevel = 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
         if (ColorUtils.getAlphaFromColor(texCol) >= 32) {
            itemRender.renderItemOverlayIntoGUIWithTextColor(Fonts.minecraftia_16, stackx, 0, 0, stackx.getCount(), texCol);
         }

         RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, aPC, 1.0F);
         itemRender.zLevel = 0.0F;
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled(1.0 / scaleI, 1.0 / scaleI, 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
      }

      GL11.glScaled(2.0, 2.0, 2.0);
      xn /= 2.0F;
      yn /= 2.0F;
      GL11.glTranslated((double)(-x), (double)(-y), 0.0);
      RenderHelper.disableStandardItemLighting();
      GL11.glPopMatrix();
   }

   private void renderModern(EntityLivingBase target) {
      if (OldSkin == null) {
         ResourceLocation var10000 = skin != null ? skin : null;
      }

      float aPC = this.Scaleclamp();
      float w = widthHud;
      float h = heightHud;
      CFontRenderer namefont = Fonts.noise_17;
      CFontRenderer hpfont = Fonts.mntsb_10;
      float x = xPosHud;
      float y = yPosHud + (1.0F - aPC) * 3.0F;
      widthHud = 86.0F;
      heightHud = 21.0F;
      float hOf = 14.0F;
      int texCol = ColorUtils.swapAlpha(-1, 255.0F * aPC * aPC);
      int bgOutC = ColorUtils.swapAlpha(0, 210.0F * aPC);
      int bgOutCBG = ColorUtils.swapAlpha(0, 55.0F * aPC);
      this.absorbAnim.to = target.getAbsorptionAmount();
      float hpMax = target.getMaxHealth() + this.absorbAnim.getAnim();
      float hpALLPC = Math.min((target.smoothHealth.getAnim() + this.absorbAnim.getAnim()) / hpMax, 1.0F);
      float absALLPC = Math.min(this.absorbAnim.getAnim() / hpMax, 1.0F);
      float var36 = hpALLPC - absALLPC;
      String hpStr = target.getHealth() == 0.0F ? "" : String.format("%.1f", hpALLPC * hpMax) + "hp";
      GL11.glPushMatrix();
      float sclaePC = 0.45F + aPC * 0.55F;
      RenderUtils.customScaledObject2D(x, y, w, h, sclaePC);
      float round = 2.0F;
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y + hOf, x + w, y + h, round, 1.5F, bgOutC, bgOutC, bgOutC, bgOutC, false, false, true
      );
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y + hOf, x + w, y + h, round, -0.5F, bgOutCBG, bgOutCBG, bgOutCBG, bgOutCBG, false, true, true
      );
      if (aPC > 0.1F) {
         GaussianBlur.drawRoundedBlurNotStencil(5.0F * aPC, x - 0.5F, y + hOf - 0.5F, x + w + 0.5F, y + h + 0.5F, round, "TH-renderModern");
      }

      if (target.getHealth() != 0.0F) {
         float offsetX = 2.5F;
         float offsetY = 2.5F;
         float offsetDC = 0.0F;
         float hpYMin = y + hOf + offsetY;
         float hpYMax = y + h - offsetY;
         float hpMinX = x + offsetX;
         float hpMaxX = x + w - offsetX;
         float hpX1 = hpMinX + (w - offsetX * 2.0F) * var36;
         float hpX2 = hpMinX + (w - offsetX * 2.0F) * hpALLPC;
         int hpCol = ColorUtils.getColor(0, 255, 120, 255.0F * aPC);
         int absCol = ColorUtils.getColor(255, 220, 0, 255.0F * aPC);
         float grandX1 = MathUtils.clamp(hpX1 - offsetDC, hpMinX, hpMaxX);
         float grandX2 = MathUtils.clamp(hpX1 + offsetDC, hpMinX, hpMaxX);
         RenderUtils.drawRect((double)hpMinX, (double)hpYMin, (double)grandX1, (double)hpYMax, hpCol);
         hpCol = ColorUtils.swapAlpha(hpCol, (float)ColorUtils.getAlphaFromColor(hpCol) / 1.7F);
         RenderUtils.drawLightContureRectSidewaysSmooth((double)hpMinX, (double)hpYMin, (double)grandX1, (double)hpYMax, hpCol, 0);
         RenderUtils.drawRect((double)MathUtils.clamp(hpX1 + offsetDC, hpMinX, hpMaxX), (double)hpYMin, (double)hpX2, (double)hpYMax, absCol);
         absCol = ColorUtils.swapAlpha(absCol, (float)ColorUtils.getAlphaFromColor(hpCol) / 1.7F);
         RenderUtils.drawLightContureRectSidewaysSmooth(
            (double)MathUtils.clamp(hpX1 + offsetDC, hpMinX, hpMaxX), (double)hpYMin, (double)hpX2, (double)hpYMax, 0, absCol
         );
         if ((float)ColorUtils.getAlphaFromColor(texCol) / 2.0F >= 33.0F) {
            float textAPC = Math.min((x + w - (hpX2 + offsetDC + 1.5F)) / hpfont.getStringWidth(hpStr), 1.0F);
            if (textAPC > 0.1764706F) {
               int hpColText = ColorUtils.swapAlpha(texCol, (float)ColorUtils.getAlphaFromColor(texCol) / 2.0F * textAPC);
               hpfont.drawString(MathUtils.getStringPercent(hpStr, textAPC), hpX2 + 2.0F, y + hOf + 3.5F, hpColText);
            }
         }
      } else if (ColorUtils.getAlphaFromColor(texCol) >= 33) {
         hpfont.drawString("KILLED", x + w / 2.0F - hpfont.getStringWidth("KILLED") / 2.0F, y + hOf + 3.5F, texCol);
      }

      RenderUtils.customScaledObject2DPro(x, y, w, h, sclaePC * sclaePC, 1.0F);
      if (ColorUtils.getAlphaFromColor(texCol) >= 33) {
         namefont.drawStringWithShadow(this.targetName, x + w / 2.0F - namefont.getStringWidth(this.targetName) / 2.0F, y + 2.0F, texCol);
      }

      GL11.glPopMatrix();
   }

   private void renderBushy(EntityLivingBase target) {
      GL11.glPushMatrix();
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;

      for (ItemStack i : stuff) {
         if (i != null) {
            i.getItem();
            stacks.add(i);
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      float alphaPC = this.Scaleclamp();
      alphaPC *= alphaPC;
      CFontRenderer nameFont = Fonts.comfortaaBold_12;
      float w = widthHud;
      float h = heightHud;
      float x = xPosHud;
      float y = yPosHud;
      float texOrItemW = (float)stacksCount * 8.0F + 0.5F;
      float strW = nameFont.getStringWidth(this.targetName);
      if (texOrItemW < strW) {
         texOrItemW = strW;
      }

      widthHud = MathUtils.clamp(19.0F + texOrItemW, 60.0F, 1000.0F);
      heightHud = 18.0F;
      float hpHeight = 1.5F;
      int colBG1 = ColorUtils.getColor(0, 0, 0, 80.0F * alphaPC);
      int colBG2 = ColorUtils.getColor(0, 0, 0, 140.0F * alphaPC);
      int colBG3 = ColorUtils.getColor(0, 0, 0, 130.0F * alphaPC);
      int texCol = ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC);
      GL11.glTranslated(0.0, (double)(10.0F - this.Scale() * 10.0F), 0.0);
      GaussianBlur.drawRoundedBlurNotStencil(5.0F * alphaPC, x, y, x + w, y + h, 0.5F, "TH-renderBushy");
      if (res != null) {
         GL11.glDisable(3008);
         GL11.glEnable(3042);
         HeadSkinUVLivingBase.of(target).setWhite(alphaPC * alphaPC, true).bindTex(res).tessellateDefaultQuad(x, y, x + 16.0F, y + 16.0F);
         GL11.glEnable(3008);
         GlStateManager.resetColor();
         RenderUtils.drawAlphedRect((double)x, (double)(y + 16.0F), (double)(x + 16.0F), (double)(y + 16.5F), colBG1);
      } else if (255.0F * alphaPC >= 33.0F) {
         Fonts.mntsb_15.drawString("?", x + 5.0F, y + 6.0F, texCol);
      }

      RenderUtils.drawAlphedRect((double)(x + (float)(res == null ? 0 : 16)), (double)y, (double)(x + w), (double)(y + h - hpHeight), colBG1);
      RenderUtils.drawLightContureRectSmooth((double)x, (double)y, (double)(x + w), (double)(y + h), colBG1);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x, y, x + w, y + h, 0.0F, 4.0F, colBG2, colBG2, colBG2, colBG2, false, false, true
      );
      if (255.0F * alphaPC >= 33.0F) {
         float tw = w / 2.0F - 16.0F + 3.0F - strW / 2.0F + 16.0F + 5.5F;
         nameFont.drawString(this.targetName, x + tw, y + (stacksCount == 0 ? 6.5F : 2.5F), texCol);
      }

      RenderItem itemRender = mc.getRenderItem();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.enableDepth();
      float xn = 7.5F;
      float yn = 7.5F;
      int itemsBgCol = ColorUtils.getColor(0, 0, 0, 80.0F * alphaPC * alphaPC);
      GL11.glDisable(2896);
      if (stacksCount != 0) {
         RenderUtils.drawAlphedRect((double)(x + 16.5F), (double)(y + 7.0F), (double)(x + 16.0F + (float)stacksCount * 8.5F), (double)(y + 16.0F), itemsBgCol);
      }

      GlStateManager.enableDepth();
      GL11.glTranslated((double)x, (double)y, 0.0);
      xn *= 2.0F;
      yn *= 2.0F;
      GL11.glScaled(0.5, 0.5, 0.5);

      for (ItemStack stackx : stacks) {
         if (!stackx.isEmpty()) {
            xn += 17.0F;
         }

         float sc = alphaPC * alphaPC;
         GL11.glTranslated((double)xn, (double)yn, 0.0);
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)sc, (double)sc, 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         itemRender.zLevel = 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
         if (ColorUtils.getAlphaFromColor(texCol) >= 32) {
            itemRender.renderItemOverlayIntoGUIWithTextColor(Fonts.minecraftia_16, stackx, 0, 0, stackx.getCount(), texCol);
         }

         RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, alphaPC, 1.0F);
         itemRender.zLevel = 0.0F;
         GL11.glTranslated(8.0, 8.0, 0.0);
         GL11.glScaled((double)(1.0F / sc), (double)(1.0F / sc), 1.0);
         GL11.glTranslated(-8.0, -8.0, 0.0);
         GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
      }

      GL11.glScaled(2.0, 2.0, 2.0);
      xn /= 2.0F;
      yn /= 2.0F;
      GL11.glTranslated((double)(-x), (double)(-y), 0.0);
      RenderHelper.disableStandardItemLighting();
      this.absorbAnim.to = target.getAbsorptionAmount();
      float hpMax = target.getMaxHealth() + this.absorbAnim.getAnim();
      float hpALLPC = (target.smoothHealth.getAnim() + this.absorbAnim.getAnim()) / hpMax;
      float absALLPC = this.absorbAnim.getAnim() / hpMax;
      hpALLPC -= absALLPC;
      if (target.getHealth() == 0.0F) {
//         String var10000 = "";
      } else {
//         String.format("%.1f", hpALLPC * hpMax) + "hp";
      }

      float hOf = heightHud - hpHeight;
      float offsetX = 0.0F;
      float offsetY = 0.0F;
      float offsetDC = 0.0F;
      float hpYMin = y + hOf + offsetY;
      float hpYMax = y + h - offsetY;
      float hpMinX = x + offsetX;
      float hpMaxX = x + w - offsetX;
      float hpX1 = hpMinX + (w - offsetX * 2.0F) * hpALLPC;
      float hpX2 = hpMinX + (w - offsetX * 2.0F) * hpALLPC;
      int hpCol = ColorUtils.getColor(0, 255, 120, 255.0F * alphaPC);
      int absCol = ColorUtils.getColor(255, 220, 0, 255.0F * alphaPC);
      float grandX1 = MathUtils.clamp(hpX1 - offsetDC, hpMinX, hpMaxX);
      float grandX2 = MathUtils.clamp(hpX1 + offsetDC, hpMinX, hpMaxX);
      RenderUtils.drawAlphedRect((double)hpMinX, (double)hpYMin, (double)grandX1, (double)hpYMax, hpCol);
      RenderUtils.drawAlphedRect((double)MathUtils.clamp(hpX1 + offsetDC, hpMinX, hpMaxX), (double)hpYMin, (double)hpX2, (double)hpYMax, absCol);
      RenderUtils.drawAlphedRect((double)hpX2, (double)hpYMin, (double)(x + w), (double)hpYMax, colBG3);
      GL11.glPopMatrix();
   }

   private void renderSubtle(EntityLivingBase target) {
      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      this.absorbAnim.to = target.getAbsorptionAmount();
      float targetHealth = target.getSmoothHealth() + this.absorbAnim.getAnim();
      float targetHealtMax = target.getMaxHealth() + this.absorbAnim.anim;
      float targetHealthPC = Math.max(Math.min(targetHealth / targetHealtMax, 1.0F), 0.0F);
      String hpStr = String.format("%.1f", targetHealth).replace(".0", "") + "HP";
      CFontRenderer hpFont = Fonts.comfortaaBold_12;
      this.targetName = NameTags.getEntityName(target, true);
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;

      for (ItemStack i : stuff) {
         if (i != null) {
            i.getItem();
            stacks.add(i);
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      float alphaPC = this.Scaleclamp();
      alphaPC *= alphaPC;
      CFontRenderer nameFont = Fonts.comfortaaBold_12;
      float w = widthHud;
      float h = heightHud;
      float x = (float)((int)(xPosHud * 2.0F)) / 2.0F;
      float y = yPosHud;
      float texOrItemW = (float)stacksCount * 8.0F + 0.5F;
      float strW = nameFont.getStringWidth(this.targetName) - 22.0F;
      if (texOrItemW < strW) {
         texOrItemW = strW;
      }

      widthHud = MathUtils.clamp(h + 16.0F + hpFont.getStringWidth(hpStr) + texOrItemW, 100.0F, 1000.0F);
      heightHud = 34.0F;
      int col1 = ColorUtils.swapAlpha(ClientColors.getColorQ(1), 30.0F * alphaPC);
      int col2 = ColorUtils.swapAlpha(ClientColors.getColorQ(2), 30.0F * alphaPC);
      int col3 = ColorUtils.swapAlpha(ClientColors.getColorQ(3), 30.0F * alphaPC);
      int col4 = ColorUtils.swapAlpha(ClientColors.getColorQ(4), 30.0F * alphaPC);
      int colS1 = ColorUtils.swapAlpha(ClientColors.getColorQ(1), 60.0F * alphaPC);
      int colS2 = ColorUtils.swapAlpha(ClientColors.getColorQ(2), 60.0F * alphaPC);
      int colS3 = ColorUtils.swapAlpha(ClientColors.getColorQ(3), 60.0F * alphaPC);
      int colS4 = ColorUtils.swapAlpha(ClientColors.getColorQ(4), 60.0F * alphaPC);
      float round = 6.0F;
      float shadow = 2.0F * alphaPC;
      float blur = 10.0F;
      if (!((double)alphaPC < 0.1)) {
         GL11.glPushMatrix();
         this.hudScale(x, y, w, h);
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x, y, x + w, y + h, round, 0.0F, col1, col2, col3, col4, false, true, false
         );
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x, y, x + w, y + h, round, shadow, colS1, colS2, colS3, colS4, true, false, true
         );
         GL11.glPushMatrix();
         GaussianBlur.drawRoundedBlurNotStencil(blur * alphaPC, x + 0.5F, y + 0.5F, x + w - 0.5F, y + h - 0.5F, round, "TH-renderSubtle");
         GL11.glPopMatrix();
         RenderUtils.drawInsideFullRoundedFullGradientShadowRectWithBloomBool(x, y, x + w, y + h, round - 2.0F, 2.0F, colS1, colS2, colS3, colS4, true);
         int headSize = 24;
         float headX = x + 5.0F;
         float headY = y + 5.0F;
         if (res == null) {
            RenderUtils.drawRect(
               (double)headX,
               (double)headY,
               (double)(headX + (float)headSize),
               (double)(headY + (float)headSize),
               ColorUtils.getColor(0, 0, 0, 90.0F * alphaPC)
            );
            Fonts.comfortaaBold_18
               .drawString("?", x + (float)headSize / 2.0F + 2.5F, y + (float)headSize / 2.0F + 2.0F, ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC));
         } else {
            GlStateManager.enableTexture2D();
            HeadSkinUVLivingBase.of(target)
               .setWhite(alphaPC * alphaPC, true)
               .bindTex(res)
               .tessellateDefaultQuadWithOverlay(headX, headY, headX + (float)headSize, headY + (float)headSize, 0.5F);
            GlStateManager.resetColor();
            if (target.getHealth() == 0.0F) {
               RenderUtils.drawRect(
                  (double)(x + 3.0F),
                  (double)(y + 10.0F),
                  (double)(x + (float)headSize + 8.0F),
                  (double)(y + h - 11.0F),
                  ColorUtils.getColor(0, 0, 0, 195.0F * alphaPC)
               );
               Fonts.mntsb_18.drawStringWithShadow("DEAD", x + 3.5F, y + (float)headSize / 2.0F + 2.0F, ColorUtils.getColor(255, 65, 65, 255.0F * alphaPC));
            }
         }

         float postHeadX = headX + (float)headSize + 4.0F;
         RenderUtils.drawTwoAlphedSideways(
            (double)postHeadX, (double)(y + 4.5F), (double)(postHeadX + 2.0F), (double)(y + h - 4.5F), ColorUtils.getColor(0, 0, 0, 70.0F * alphaPC), 0, false
         );
         if (255.0F * alphaPC >= 33.0F) {
            float nameTextX = postHeadX + 7.0F + (w - (postHeadX - x) - 12.0F) / 2.0F - nameFont.getStringWidth(this.targetName) / 2.0F;
            nameFont.drawStringWithShadow(this.targetName, nameTextX, y + 5.5F, ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC));
         }

         float hpWidth = w - (postHeadX - x) - 12.0F;
         float hpX1 = postHeadX + 7.0F;
         float hpX2 = hpX1 + hpWidth * targetHealthPC;
         float hpX3 = hpX1 + hpWidth;
         int hpCol1 = ColorUtils.swapAlpha(ClientColors.getColor1(0), 255.0F * alphaPC);
         int hpCol2 = ColorUtils.swapAlpha(ClientColors.getColor2(-324), 255.0F * alphaPC);
         int hpBGCol = ColorUtils.swapAlpha(0, 65.0F * alphaPC);
         float hpRoundRect = 1.5F;
         float hpShadowRect = 1.0F;
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            hpX1 - 0.5F, y + h - 9.0F, hpX3 + 0.5F, y + h - 4.5F, hpRoundRect, hpShadowRect + 0.5F, hpBGCol, hpBGCol, hpBGCol, hpBGCol, false, true, true
         );
         if (target.isEntityAlive()) {
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               hpX1,
               y + h - 8.5F,
               Math.max(hpX2, hpX1 + hpRoundRect * 2.0F),
               y + h - 5.0F,
               hpRoundRect,
               hpShadowRect,
               hpCol1,
               hpCol2,
               hpCol2,
               hpCol1,
               false,
               true,
               true
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               hpX1,
               y + h - 8.5F,
               Math.max(hpX2, hpX1 + hpRoundRect * 2.0F),
               y + h - 5.0F,
               hpRoundRect,
               hpShadowRect,
               hpCol1,
               hpCol2,
               hpCol2,
               hpCol1,
               true,
               true,
               true
            );
         }

         if (255.0F * alphaPC >= 33.0F) {
            hpFont.drawString(hpStr, postHeadX + 6.0F, y + 16.5F, ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC));
         }

         float postHpTextX = postHeadX + 7.5F + hpFont.getStringWidth(hpStr);
         if (stacksCount > 0) {
            RenderUtils.drawVGradientRect(
               postHpTextX + 0.5F,
               y + 13.0F,
               postHpTextX + 1.0F,
               y + 22.0F - 4.0F,
               ColorUtils.getColor(255, 255, 255, 10.0F * alphaPC),
               ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC)
            );
            RenderUtils.drawVGradientRect(
               postHpTextX + 0.5F,
               y + 13.0F + 4.0F,
               postHpTextX + 1.0F,
               y + 22.0F,
               ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC),
               ColorUtils.getColor(255, 255, 255, 10.0F * alphaPC)
            );
         }

         RenderItem itemRender = mc.getRenderItem();
         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.enableDepth();
         float xn = postHpTextX - x - 5.5F;
         float yn = 13.0F;
         int itemsBgCol = ColorUtils.getColor(0, 0, 0, 40.0F * alphaPC);
         GlStateManager.enableDepth();
         GL11.glTranslated((double)x, (double)y, 0.0);
         xn *= 2.0F;
         yn *= 2.0F;
         GL11.glScaled(0.5, 0.5, 0.5);

         for (ItemStack stackx : stacks) {
            if (!stackx.isEmpty()) {
               xn += 17.0F;
            }

            GL11.glTranslated((double)xn, (double)yn, 0.0);
            GL11.glTranslated(8.0, 8.0, 0.0);
            GL11.glScaled((double)alphaPC, (double)alphaPC, 1.0);
            GL11.glTranslated(-8.0, -8.0, 0.0);
            itemRender.zLevel = 200.0F;
            itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
            if (255.0F * alphaPC >= 33.0F) {
               itemRender.renderItemOverlayIntoGUIWithTextColor(
                  Fonts.minecraftia_16, stackx, 0, 0, stackx.getCount(), ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC)
               );
            }

            RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, alphaPC, 1.0F);
            itemRender.zLevel = 0.0F;
            GL11.glTranslated(8.0, 8.0, 0.0);
            GL11.glScaled((double)(1.0F / alphaPC), (double)(1.0F / alphaPC), 1.0);
            GL11.glTranslated(-8.0, -8.0, 0.0);
            GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
         }

         GL11.glScaled(2.0, 2.0, 2.0);
         xn /= 2.0F;
         yn /= 2.0F;
         GL11.glTranslated((double)(-x), (double)(-y), 0.0);
         RenderHelper.disableStandardItemLighting();
         GL11.glPopMatrix();
      }
   }

   private void renderEntire(EntityLivingBase target) {
      ResourceLocation res = OldSkin != null ? OldSkin : (skin != null ? skin : null);
      float targetHealth = target.getSmoothHealth() + this.absorbAnim.getAnim();
      float targetHealtMax = target.getMaxHealth() + this.absorbAnim.anim;
      float targetHealthPC = Math.max(Math.min(targetHealth / targetHealtMax, 1.0F), 0.0F);
      String hpStr = String.format("%.1f", targetHealth).replace(".0", "");
      CFontRenderer hpFont = Fonts.comfortaaBold_12;
      this.targetName = NameTags.getEntityName(target, true);
      int stacksCount = 0;
      ItemStack offhand = target.getHeldItemOffhand();
      ItemStack boots = target.getItemStackFromSlot(EntityEquipmentSlot.FEET);
      ItemStack leggings = target.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
      ItemStack body = target.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
      ItemStack helm = target.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
      ItemStack inHand = target.getHeldItemMainhand();
      ItemStack[] stuff = new ItemStack[]{offhand, inHand, boots, leggings, body, helm};
      ArrayList<ItemStack> stacks = new ArrayList<>();
      int j = 0;

      for (ItemStack i : stuff) {
         if (i != null) {
            i.getItem();
            stacks.add(i);
         }
      }

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            stacksCount++;
         }
      }

      float alphaPC = this.Scaleclamp();
      CFontRenderer nameFont = Fonts.comfortaaRegular_13;
      float w = widthHud;
      float h = heightHud;
      float x = (float)((int)(xPosHud * 2.0F)) / 2.0F;
      float y = yPosHud;
      float textOrItemW = (float)stacksCount * 8.0F + 0.5F;
      float strW = nameFont.getStringWidth(this.targetName);
      if (textOrItemW < strW) {
         textOrItemW = strW;
      }

      float exts = 2.0F;
      float hpLineH = 4.0F;
      float alphaF = alphaPC * (0.5F + alphaPC / 2.0F);
      widthHud = MathUtils.clamp(24.0F + exts * 3.0F + textOrItemW + 1.0F, 80.0F, 180.0F);
      heightHud = 24.0F + exts * 3.0F + hpLineH;
      float round = 6.0F;
      float shadow = 7.0F * alphaPC;
      if (!(alphaPC < 0.1F) && !(alphaF < 0.1F)) {
         GL11.glPushMatrix();
         int[] headColors = this.getColorsFromLastResource(res, alphaPC / 3.25F);
         int bgCol = ColorUtils.swapAlpha(0, 130.0F * alphaF);
         int bg1 = ColorUtils.getOverallColorFrom(headColors[0], bgCol);
         int bg2 = ColorUtils.getOverallColorFrom(headColors[1], bgCol);
         int bg3 = ColorUtils.getOverallColorFrom(headColors[2], bgCol);
         int bg4 = ColorUtils.getOverallColorFrom(headColors[3], bgCol);
         RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBool(
            x, y, x + w, y + h, 8.0F, headColors[0], headColors[1], headColors[2], headColors[3], true
         );
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x, y, x + w, y + h, 0.0F, 0.5F, bg1, bg2, bg3, bg4, false, true, true
         );
         RenderUtils.drawLightContureRectSmoothFullGradient(x, y, x + w, y + h, bg1, bg2, bg3, bg4, true);
         GaussianBlur.drawRoundedBlurNotStencil(5.0F * alphaF, x + 0.5F, y + 0.5F, x + w - 0.5F, y + h - 0.5F, 0.5F, "TH-renderEntire");
         float hpRect1X = x + exts;
         float hpRect1X2 = x + exts + (w - exts * 2.0F) * targetHealthPC;
         float hpRectY2 = y + h - exts;
         float hpRectY = hpRectY2 - hpLineH;
         if (target.hurtTime <= 1) {
            if (target.hurtTime == 0) {
               this.hurtHpAnim.setAnim(targetHealthPC);
            }

            this.hurtHpAnim.to = targetHealthPC;
         }

         float hurtHpAnim = this.hurtHpAnim.getAnim();
         hurtHpAnim = Math.max(hurtHpAnim, targetHealthPC);
         float hpRect2X2 = x + exts + (w - exts * 2.0F) * hurtHpAnim;
         float hpRect3X2 = x + w - exts;
         int hpRectCol1 = ColorUtils.swapAlpha(headColors[3], 255.0F * alphaF);
         int hpRectCol2 = ColorUtils.swapAlpha(headColors[2], 255.0F * alphaF);
         if (MathUtils.getDifferenceOf(hpRect1X, hpRect1X2) >= 0.5F) {
            int c2 = ColorUtils.getOverallColorFrom(hpRectCol1, hpRectCol2, targetHealthPC);
            RenderUtils.drawAlphedSideways((double)hpRect1X, (double)hpRectY, (double)hpRect1X2, (double)hpRectY2, hpRectCol1, c2, false);
         }

         if (MathUtils.getDifferenceOf(hpRect1X2, hpRect2X2) >= 0.5F) {
            int c1 = ColorUtils.getOverallColorFrom(hpRectCol1, hpRectCol2, targetHealthPC);
            int c2 = ColorUtils.getOverallColorFrom(c1, hpRectCol2, hurtHpAnim);
            c1 = ColorUtils.swapAlpha(c1, (float)ColorUtils.getAlphaFromColor(c1) / 2.0F);
            c2 = ColorUtils.swapAlpha(c2, (float)ColorUtils.getAlphaFromColor(c2) / 2.0F);
            RenderUtils.drawAlphedSideways((double)hpRect1X2, (double)hpRectY, (double)hpRect2X2, (double)hpRectY2, c1, c2, false);
         }

         RenderUtils.drawLightContureRectSmooth((double)hpRect1X, (double)hpRectY, (double)hpRect3X2, (double)hpRectY2, bgCol);
         int textCol = ColorUtils.swapAlpha(headColors[0], 255.0F * alphaF);
         if (ColorUtils.getAlphaFromColor(textCol) >= 33) {
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            nameFont.drawStringWithShadow(this.targetName, x + h - (exts + hpLineH) + 1.0F, y + 4.0F, textCol);
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
         }

         float headX = x + exts;
         float headY = y + exts;
         float headSize = 24.0F;
         if (res != null) {
            GlStateManager.disableAlpha();
            GlStateManager.enableTexture2D();
            HeadSkinUVLivingBase.of(target)
               .setWhite(alphaPC * alphaPC, true)
               .bindTex(res)
               .tessellateDefaultQuadWithOverlay(headX, headY, headX + headSize, headY + headSize, 1.0F);
            if (target.getHealth() == 0.0F && 255.0F * alphaPC * alphaPC >= 33.0F) {
               Fonts.mntsb_16
                  .drawStringWithShadow(
                     "ЛОХ",
                     x + exts + headSize / 2.0F - Fonts.mntsb_16.getStringWidth("ЛОХ") / 2.0F,
                     y + headSize / 2.0F - 1.0F,
                     ColorUtils.getColor(255, 65, 65, 255.0F * alphaPC * alphaPC)
                  );
            }

            GlStateManager.enableAlpha();
         } else {
            RenderUtils.drawRect(
               (double)headX, (double)headY, (double)(headX + headSize), (double)(headY + headSize), ColorUtils.getColor(0, 0, 0, 90.0F * alphaPC)
            );
            Fonts.comfortaaBold_18
               .drawString("?", x + headSize / 2.0F + 2.5F, y + headSize / 2.0F + 2.0F, ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC));
         }

         RenderItem itemRender = mc.getRenderItem();
         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.enableDepth();
         float xn = h - exts - hpLineH - 8.0F;
         float yn = h - exts * 2.0F - hpLineH - 9.0F;
         GlStateManager.enableDepth();
         GL11.glTranslated((double)x, (double)y, 0.0);
         xn *= 2.0F;
         yn *= 2.0F;
         GL11.glScaled(0.5, 0.5, 0.5);

         for (ItemStack stackx : stacks) {
            if (!stackx.isEmpty()) {
               xn += 17.0F;
            }

            GL11.glTranslated((double)xn, (double)yn, 0.0);
            GL11.glTranslated(8.0, 8.0, 0.0);
            GL11.glScaled((double)alphaPC, (double)alphaPC, 1.0);
            GL11.glTranslated(-8.0, -8.0, 0.0);
            itemRender.zLevel = 200.0F;
            itemRender.renderItemAndEffectIntoGUI(stackx, 0, 0);
            if (255.0F * alphaPC >= 33.0F) {
               itemRender.renderItemOverlayIntoGUIWithTextColor(
                  Fonts.minecraftia_16, stackx, 0, 0, stackx.getCount(), ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC)
               );
            }

            RenderUtils.drawItemWarnIfLowDur(stackx, 0.0F, 0.0F, alphaPC, 1.0F);
            itemRender.zLevel = 0.0F;
            GL11.glTranslated(8.0, 8.0, 0.0);
            GL11.glScaled((double)(1.0F / alphaPC), (double)(1.0F / alphaPC), 1.0);
            GL11.glTranslated(-8.0, -8.0, 0.0);
            GL11.glTranslated((double)(-xn), (double)(-yn), 0.0);
         }

         RenderHelper.disableStandardItemLighting();
         GL11.glPopMatrix();
      }
   }

   private void renderOasis(EntityLivingBase target, ScaledResolution sr) {
      float alphaPC = this.Scaleclamp();
      if (!((alphaPC = alphaPC * alphaPC) < 0.003921569F)) {
         float finalAlphaPC = alphaPC;
         int alpha255 = (int)(alphaPC * 255.0F);
         boolean canDrawText = (float)alpha255 >= 33.0F;
         ResourceLocation res = OldSkin != null ? OldSkin : (TargetHUD.skin != null ? TargetHUD.skin : null);
         this.targetName = NameTags.getEntityName(target, false);
         boolean alive = !target.isDead && target.getHealth() > 0.0F;
         float headScalePix = 24.0F;
         float hpCircleScalePixRender = headScalePix - 4.0F;
         float hpCircleScalePixRenderBG = headScalePix - 3.0F;
         float xPadding = 0.0F;
         float bgShadow = 2.0F;
         float headScalePixRender = headScalePix - 3.0F;
         float hpOrAbsbCircleWidth = 3.0F;
         float hpOrAbsbBGCircleWidth = 4.0F;
         float itemsScale = 0.5F;
         float itemsXPadding = 1.0F;
         float minBgExt = 3.0F;
         CFontRenderer nameFont = Fonts.noise_15;
         CFontRenderer hpFont = Fonts.mntsb_10;
         ArrayList<ItemStack> stacks = new ArrayList<>();

         for (ItemStack stack : new ItemStack[]{
            target.getHeldItemOffhand(),
            target.getHeldItemMainhand(),
            target.getItemStackFromSlot(EntityEquipmentSlot.FEET),
            target.getItemStackFromSlot(EntityEquipmentSlot.LEGS),
            target.getItemStackFromSlot(EntityEquipmentSlot.CHEST),
            target.getItemStackFromSlot(EntityEquipmentSlot.HEAD)
         }) {
            if (!stack.isEmpty()) {
               stacks.add(stack);
            }
         }

         float stacksToPopsXPadding = 0.0F;
         float popsBgExt = 0.0F;
         float popsItemScalePix = 0.0F;
         float popsW = 0.0F;
         CFontRenderer popsFont = Fonts.comfortaaBold_12;
         float popsAnimOut01 = 0.0F;
         float popsWPlusToStacks = 0.0F;
         float totemPopAPC = 0.0F;
         if (target.totemsPopped > 0 && !stacks.isEmpty()) {
            popsAnimOut01 = MathUtils.clamp((float)target.totemTickOutHurt - mc.getRenderPartialTicks(), 0.0F, 10.0F) / 10.0F;
            totemPopAPC = ((float)target.totemsPopped == 1.0F ? Math.min((1.0F - popsAnimOut01) * 3.0F, 1.0F) : 1.0F) * alphaPC;
            stacksToPopsXPadding = stacks.isEmpty() ? 0.0F : 1.5F;
            popsBgExt = totemPopAPC;
            popsW = popsFont.getStringWidth("-" + target.totemsPopped) + totemPopAPC;
            popsItemScalePix = 8.0F;
            popsWPlusToStacks = (stacksToPopsXPadding * 2.0F + totemPopAPC * 2.0F + popsW + popsItemScalePix) * totemPopAPC;
         }

         float stacksDrawsW = (float)stacks.size() * itemsScale * 16.0F + (float)Math.max(stacks.size() - 1, 0) * itemsXPadding;
         float stacksDrawsWCalc = stacksDrawsW + popsWPlusToStacks;
         float nameW = nameFont.getStringWidth(this.targetName);
         widthHud = MathUtils.clamp(
            headScalePix + xPadding + Math.max(nameW, stacksDrawsWCalc) + xPadding + headScalePix + minBgExt * 2.0F,
            headScalePix + headScalePix + xPadding + minBgExt * 2.0F,
            220.0F
         );
         heightHud = Math.max(headScalePix, headScalePix);
         float coordMulRounding = (float)ScaledResolution.getScaleFactor();
         float x = (float)((int)(xPosHud * coordMulRounding)) / coordMulRounding;
         float y = (float)((int)(yPosHud * coordMulRounding)) / coordMulRounding;
         float w = (float)((int)(widthHud * coordMulRounding)) / coordMulRounding;
         float h = (float)((int)(heightHud * coordMulRounding)) / coordMulRounding;
         float x2 = x + w;
         float y2 = y + h;
         float circleCX = x2 - headScalePix / 2.0F;
         float circleCY = y2 - headScalePix / 2.0F;
         float hp = Math.min(target.getHealth(), target.getSmoothHealth());
         float absb = target.getAbsorptionAmount();
         float hpMax = target.getMaxHealth();
         float absbMaxDynamic = target.getAbsorptionAmount();
         float hpPC0 = 0.0F;
         float hpPC1 = hp / (hpMax + absbMaxDynamic);
         float absbPC1 = (hp + absb) / (hpMax + absbMaxDynamic);
         int hpCircleBGOutCol0 = ColorUtils.getColor(52, 52, 60, (float)alpha255 * 0.8F);
         int hpCircleBGOutCol1 = ColorUtils.getColor(100, 100, 100, (float)alpha255 * 0.25F);
         RenderUtils.drawRailCirclePointedInner(
            circleCX, circleCY, hpCircleScalePixRenderBG / 2.0F, 0.0F, 1.0F, hpOrAbsbBGCircleWidth, true, hpCircleBGOutCol0, hpCircleBGOutCol0, 0.25F
         );
         RenderUtils.drawSmoothCircle((double)circleCX, (double)circleCY, (hpCircleScalePixRenderBG - hpOrAbsbBGCircleWidth / 2.0F) / 4.0F, hpCircleBGOutCol1);
         int hpCircleCol0 = ColorUtils.getColor(255, 255, 255, alpha255);
         int absbCircleCol0 = ColorUtils.getColor(255, 190, 20, alpha255);
         RenderUtils.drawRailCirclePointedInner(
            circleCX, circleCY, hpCircleScalePixRender / 2.0F, hpPC0, hpPC1, hpOrAbsbCircleWidth, true, hpCircleCol0, hpCircleCol0, 0.35F
         );
         if (absbPC1 > hpPC1) {
            RenderUtils.drawRailCirclePointedInner(
               circleCX, circleCY, hpCircleScalePixRender / 2.0F, hpPC1, absbPC1, hpOrAbsbCircleWidth, true, absbCircleCol0, absbCircleCol0, 0.35F
            );
         }

         if (this.prevFrameTargetEntity != target) {
            this.prevFrameTargetEntity = target;
            this.prevHp = hp;
            this.prevAbsb = absb;
            this.hpStringAnim.reset();
         } else {
            if (this.prevHp < hp) {
               this.prevHp = hp;
            }

            if (this.prevAbsb < absb) {
               this.prevAbsb = absb;
            }
         }

         boolean captureFrame = this.prevHp > hp || this.prevAbsb > absb;
         int voronoiPoints = 20;
         if (captureFrame) {
            float hpPC0C = hp / (hpMax + this.prevAbsb);
            float hpPC1C = this.prevHp / (hpMax + this.prevAbsb);
            float absbPC0C = (this.prevHp + absb) / (hpMax + this.prevAbsb);
            float absbPC1C = (this.prevHp + this.prevAbsb) / (hpMax + this.prevAbsb);
            TargetHUD.VoronoiOfCopyRenderTempEvent voronoiOfCopyRenderTempEvent = new TargetHUD.VoronoiOfCopyRenderTempEvent(
               circleCX - hpCircleScalePixRender / 2.0F,
               circleCY - hpCircleScalePixRender / 2.0F,
               circleCX + hpCircleScalePixRender / 2.0F,
               circleCY + hpCircleScalePixRender / 2.0F,
               voronoiPoints,
               true,
               1200
            );
            this.voronoiRenderTempEventList.add(voronoiOfCopyRenderTempEvent);
            voronoiOfCopyRenderTempEvent.genFromCaptureRender2d(
               circleCX - hpCircleScalePixRender / 2.0F,
               circleCY - hpCircleScalePixRender / 2.0F,
               circleCX + hpCircleScalePixRender / 2.0F,
               circleCY + hpCircleScalePixRender / 2.0F,
               () -> {
                  RenderUtils.drawRailCirclePointedInner(
                     circleCX, circleCY, hpCircleScalePixRender / 2.0F, hpPC0C, hpPC1C, hpOrAbsbCircleWidth, true, hpCircleCol0, hpCircleCol0, 0.3F
                  );
                  RenderUtils.drawRailCirclePointedInner(
                     circleCX, circleCY, hpCircleScalePixRender / 2.0F, absbPC0C, absbPC1C, hpOrAbsbCircleWidth, true, absbCircleCol0, absbCircleCol0, 0.3F
                  );
               },
               voronoiPoints,
               false,
               3
            );
            this.prevHp = hp;
            this.prevAbsb = absb;
         }

         int hpToStr = (int)(target.getHealth() + target.getAbsorptionAmount());
         float hpTextY = circleCY - hpFont.getHeight() / 2.0F + 0.5F;
         int hpCol0 = ColorUtils.getColor(255, 255, 255, alpha255);
         this.hpStringAnim.controlAnimations(hpToStr, 0.03F);
         if (canDrawText && alive) {
            this.hpStringAnim.drawString(hpFont, circleCX, hpTextY, hpCol0, true, 3.0F);
         } else {
            hpFont.drawString("*-*", circleCX - hpFont.getStringWidth("*-*") / 2.0F + 0.5F, hpTextY, hpCol0);
         }

         int lbBright = (int)MathUtils.lerp(210.0F, 255.0F, totemPopAPC);
         int bgCol0 = ColorUtils.getColor(lbBright, lbBright, lbBright, alpha255);
         float bgX = x + headScalePix / 2.0F + xPadding;
         float bgX2 = circleCX - xPadding;
         RenderUtils.drawConcaveFullGradientRect(bgX, y, bgX2, y2, bgShadow, 1.0F, bgCol0, bgCol0, bgCol0, bgCol0, true, true, false);
         float bgOverlayX = bgX + headScalePix / 2.0F;
         float bgOverlayX2 = bgX2 - headScalePix / 2.0F;
         int bgOverlayCol0 = ColorUtils.getColor(0, 0, 0, (float)alpha255 / 3.0F);
         int bgOverlayCol1 = 0;
         RenderUtils.drawTwoAlphedSideways((double)bgOverlayX, (double)y, (double)bgOverlayX2, (double)y2, bgOverlayCol0, bgOverlayCol1, false);
         GlStateManager.resetColor();
         float itemsX = x + w / 2.0F - stacksDrawsWCalc / 2.0F;
         float itemsY = y + h / 1.5F - itemsScale * 8.0F + 1.0F;
         float itemsX2 = itemsX + stacksDrawsW;
         float itemsY2 = itemsY + itemsScale * 16.0F;
         float tempStackX = itemsX;
         if (!stacks.isEmpty()) {
            float itemsBgGrow = 0.5F - (1.0F - alphaPC) * (itemsY2 - itemsY) / 2.0F;
            float itemsBgRound = (itemsY2 - itemsY + itemsBgGrow * 2.0F) / 4.0F;
            int itemsBgCol = ColorUtils.getColor(0, 0, 0, alpha255);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               itemsX - itemsBgGrow,
               itemsY - itemsBgGrow,
               itemsX2 + itemsBgGrow,
               itemsY2 + itemsBgGrow,
               itemsBgRound,
               0.5F,
               itemsBgCol,
               itemsBgCol,
               itemsBgCol,
               itemsBgCol,
               false,
               true,
               true
            );

            for (ItemStack stackx : stacks) {
               if (this.drawStackClient(stackx, tempStackX, itemsY, itemsScale, alphaPC * alphaPC)) {
                  tempStackX += itemsScale * 16.0F + (stackx == stacks.get(stacks.size() - 1) ? 0.0F : itemsXPadding);
               }
            }
         }

         if (totemPopAPC > 0.0F) {
            ItemStack genStackPopTotem = new ItemStack(Items.TOTEM, 1);
            float itemsBgGrow = 0.5F - (1.0F - alphaPC) * (itemsY2 - itemsY) / 2.0F;
            float itemsBgRound = (itemsY2 - itemsY + itemsBgGrow * 2.0F) / 4.0F;
            float popX = x + w / 2.0F - stacksDrawsWCalc / 2.0F + stacksDrawsW + stacksToPopsXPadding * 2.0F;
            float popX2 = popX + popsW + popsItemScalePix + popsBgExt * 2.0F;
            float popTextX = popX + popsBgExt;
            float popItemX = popTextX + popsW;
            float popTextY = itemsY + (itemsY2 - itemsY) / 2.0F - popsFont.getHeight() / 2.0F + 0.5F;
            int popBgCol = ColorUtils.getColor(40, 0, 0, (float)alpha255 / 1.5F * totemPopAPC);
            int popOutCol = ColorUtils.getColor(40, 0, 0, (float)alpha255 * totemPopAPC);
            int popTextCol0 = ColorUtils.getOverallColorFrom(
               ColorUtils.getColor(160, 160, 160, (float)alpha255 * totemPopAPC), ColorUtils.getColor(255, 40, 0, (float)alpha255 * totemPopAPC), popsAnimOut01
            );
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               popX - itemsBgGrow,
               itemsY - itemsBgGrow,
               popX2 + itemsBgGrow,
               itemsY2 + itemsBgGrow,
               itemsBgRound,
               0.5F,
               popBgCol,
               popBgCol,
               popBgCol,
               popBgCol,
               false,
               true,
               true
            );
            RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
               popX - itemsBgGrow,
               itemsY - itemsBgGrow,
               popX2 + itemsBgGrow,
               itemsY2 + itemsBgGrow,
               itemsBgRound,
               itemsBgRound / 4.0F,
               0.5F,
               popOutCol,
               popOutCol,
               popOutCol,
               popOutCol,
               false,
               true,
               true
            );
            float popScale = (1.0F + (float)MathUtils.easeOutBack((double)popsAnimOut01) * 0.2F) * totemPopAPC * alphaPC;
            if (popScale > 1.05F) {
               GL11.glPushMatrix();
               RenderUtils.customScaledObject2D(popTextX, popTextY, popsW, popsFont.getHeight(), popScale);
               popsFont.drawStringWithBloomAndShadow("-" + target.totemsPopped, popTextX, popTextY, popTextCol0, Math.min(popsAnimOut01 * 2.0F, 1.0F) / 2.0F, 4);
               GL11.glPopMatrix();
            } else {
               GL11.glPushMatrix();
               RenderUtils.customScaledObject2D(popTextX, popTextY, popsW, popsFont.getHeight(), popScale);
               popsFont.drawStringWithShadow("-" + target.totemsPopped, popTextX, popTextY, popTextCol0);
               GL11.glPopMatrix();
            }

            this.drawStackClient(genStackPopTotem, popItemX, itemsY, itemsScale, alphaPC * alphaPC * popScale);
         }

         float headCX = x + headScalePix / 2.0F;
         float headCY = y + headScalePix / 2.0F;
         if (res != null) {
            HeadSkinUVLivingBase skin = HeadSkinUVLivingBase.of(target);
            skin.bindTex(res);
            skin.setWhite(alphaPC * (this.hasLastDispatchTick ? 0.5F : 1.0F), !alive);
            Vector4f skinUV = skin.getRenderQuadFaceUv();
            Vector4f skinUVOverlay = skin.getRenderQuadFaceOverlayUv();
            float overlayGrowPix = skinUVOverlay == null ? 0.0F : 2.0F;
            int headColor = skin.getColor();
            float smoothHurtPC = (float)MathUtils.easeInOutQuad(
               (double)(MathUtils.clamp((float)target.hurtTime - mc.getRenderPartialTicks(), 0.0F, 8.0F) / 8.0F)
            );
            float shadowOut = headScalePix + (FragEffects.get.strikeAnimation.anim + FragEffects.get.totemAnimation.anim) * (float)sr.getScaledHeight() / 4.0F;
            float shadowAPC = Math.min(
               0.06F + smoothHurtPC / 4.0F + (FragEffects.get.strikeAnimation.anim + FragEffects.get.totemAnimation.anim / 3.0F) / 2.0F, 1.0F
            );
            float shadowOverlayAPC = Math.min(0.1F + smoothHurtPC / 6.0F, 1.0F);
            float distortionTexPC01 = FragEffects.get.strikeAnimation.to == 1.0F || FragEffects.get.totemAnimation.to == 1.0F
               ? 0.0F
               : (alive ? MathUtils.valWave01(smoothHurtPC) * 0.75F : 0.0F);
            float distortionOverlayTexPC01 = distortionTexPC01 * 0.33333334F;
            float vingettePC01 = 0.6666666F * (1.0F - smoothHurtPC) * (this.hasLastDispatchTick ? 0.25F : 1.0F);
            RenderUtils.drawCircleShadowTex(
               headCX - headScalePixRender / 2.0F,
               headCY - headScalePixRender / 2.0F,
               headScalePixRender,
               skinUV.getX(),
               skinUV.getY(),
               skinUV.getZ(),
               skinUV.getW(),
               shadowOut,
               shadowAPC,
               distortionTexPC01,
               vingettePC01,
               headColor,
               true
            );
            if (skinUVOverlay != null) {
               RenderUtils.drawCircleShadowTex(
                  headCX - headScalePixRender / 2.0F - overlayGrowPix / 2.0F,
                  headCY - headScalePixRender / 2.0F - overlayGrowPix / 2.0F,
                  headScalePixRender + overlayGrowPix,
                  skinUVOverlay.getX(),
                  skinUVOverlay.getY(),
                  skinUVOverlay.getZ(),
                  skinUVOverlay.getW(),
                  shadowOut,
                  shadowOverlayAPC,
                  distortionOverlayTexPC01,
                  vingettePC01,
                  headColor,
                  true
               );
            }

            if (alive) {
               this.hasLastDispatchTick = false;
            } else {
               int deathCol0 = ColorUtils.getColor(255, 40, 40, (float)alpha255 * smoothHurtPC * alphaPC);
               if ((float)alpha255 * smoothHurtPC * alphaPC >= 33.0F) {
                  CFontRenderer deathFont = Fonts.minecraftia_14;
                  String deathText = "DEAD";
                  float deathTextX = headCX - deathFont.getStringWidth(deathText) / 2.0F;
                  float deathTextY = headCY - deathFont.getHeight() / 2.0F - 0.5F;
                  deathFont.drawStringWithBloomAndShadow(deathText, deathTextX, deathTextY, deathCol0, 0.6F, 8);
               }

               if (!this.hasLastDispatchTick) {
                  skin.setWhite(alphaPC, true);
                  RenderUtils.drawCircleShadowTex(
                     headCX - headScalePixRender / 2.0F,
                     headCY - headScalePixRender / 2.0F,
                     headScalePixRender,
                     skinUV.getX(),
                     skinUV.getY(),
                     skinUV.getZ(),
                     skinUV.getW(),
                     shadowOut,
                     shadowAPC,
                     distortionTexPC01,
                     vingettePC01,
                     headColor,
                     true
                  );
                  if (skinUVOverlay != null) {
                     RenderUtils.drawCircleShadowTex(
                        headCX - headScalePixRender / 2.0F - overlayGrowPix,
                        headCY - headScalePixRender / 2.0F - overlayGrowPix,
                        headScalePixRender + overlayGrowPix * 2.0F,
                        skinUVOverlay.getX(),
                        skinUVOverlay.getY(),
                        skinUVOverlay.getZ(),
                        skinUVOverlay.getW(),
                        shadowOut,
                        shadowOverlayAPC,
                        distortionOverlayTexPC01,
                        vingettePC01,
                        headColor,
                        true
                     );
                  }

                  TargetHUD.VoronoiOfCopyRenderTempEvent headDispatch = new TargetHUD.VoronoiOfCopyRenderTempEvent(
                     headCX - headScalePix / 2.0F - overlayGrowPix,
                     headCY - headScalePix / 2.0F - overlayGrowPix,
                     headCX + headScalePix / 2.0F + overlayGrowPix,
                     headCY + headScalePix / 2.0F + overlayGrowPix,
                     20,
                     true,
                     600
                  );
                  headDispatch.genFromCaptureRender2d(
                     headCX - headScalePix / 2.0F - overlayGrowPix,
                     headCY - headScalePix / 2.0F - overlayGrowPix,
                     headCX + headScalePix / 2.0F + overlayGrowPix,
                     headCY + headScalePix / 2.0F + overlayGrowPix,
                     () -> {
                        skin.bindTex(res);
                        RenderUtils.drawCircleShadowTex(
                           headCX - headScalePixRender / 2.0F,
                           headCY - headScalePixRender / 2.0F,
                           headScalePixRender,
                           skinUV.getX(),
                           skinUV.getY(),
                           skinUV.getZ(),
                           skinUV.getW(),
                           shadowOut,
                           shadowAPC,
                           distortionTexPC01,
                           vingettePC01,
                           headColor,
                           true
                        );
                        if (skinUVOverlay != null) {
                           RenderUtils.drawCircleShadowTex(
                              headCX - headScalePixRender / 2.0F - overlayGrowPix,
                              headCY - headScalePixRender / 2.0F - overlayGrowPix,
                              headScalePixRender + overlayGrowPix * 2.0F,
                              skinUVOverlay.getX(),
                              skinUVOverlay.getY(),
                              skinUVOverlay.getZ(),
                              skinUVOverlay.getW(),
                              shadowOut,
                              shadowOverlayAPC,
                              distortionOverlayTexPC01,
                              vingettePC01,
                              headColor,
                              true
                           );
                        }
                     },
                     voronoiPoints,
                     false,
                     2
                  );
                  this.voronoiRenderTempEventList.add(headDispatch);
                  this.hasLastDispatchTick = true;
               }
            }
         } else {
            int headBgCol0 = ColorUtils.getColor(0, 0, 0, (float)alpha255 / 3.0F);
            RenderUtils.drawSmoothCircle((double)headCX, (double)headCY, headScalePixRender / 2.0F, headBgCol0);
            CFontRenderer warningFont = Fonts.noise_15;
            String warningText = "?";
            float warningStrW = warningFont.getStringWidth(warningText);
            float warningTextX = headCX - warningStrW / 2.0F + 0.5F;
            float warningTextY = headCY - warningFont.getHeight() / 2.0F;
            int warningCol0 = ColorUtils.getColor(255, 255, 255, (float)alpha255 / 2.0F);
            if (canDrawText) {
               warningFont.drawStringWithShadow(warningText, warningTextX, warningTextY, warningCol0);
            }
         }

         float nameTextX = x + w / 2.0F - nameW / 2.0F;
         float nameTextY = stacks.isEmpty()
            ? y + h / 2.0F - nameFont.getHeight() / 2.0F
            : y + h / 2.0F + MathUtils.lerp(-nameFont.getHeight() / 2.0F, -h / 4.0F - nameFont.getHeight() / 2.0F, alphaPC);
         int nameTextCol = ColorUtils.getColor(0, 0, 0, alpha255);
         if (canDrawText) {
            GL11.glDisable(2896);
            nameFont.drawStringWithShadow(this.targetName, nameTextX, nameTextY - 0.5F, nameTextCol);
         }

         if (!this.voronoiRenderTempEventList.isEmpty()) {
            for (TargetHUD.VoronoiOfCopyRenderTempEvent voronoiOfCopyRenderTempEvent : this.voronoiRenderTempEventList) {
               float voronoiTimePC = voronoiOfCopyRenderTempEvent.getTimePC01();
               float transVoronoiPC01 = (float)MathUtils.easeOutCubic((double)voronoiTimePC);
               float rotVoronoiPC01 = (float)MathUtils.easeInOutQuad((double)voronoiTimePC) / 4.0F;
               float moveVoronoiDown = hpCircleScalePixRender * (1.0F - (float)MathUtils.easeOutCubic((double)(1.0F - voronoiTimePC)));
               float apcVoronoi01 = 1.0F - voronoiTimePC;
               float scaleVoronoi = 1.0F;
               GL11.glPushMatrix();
               GL11.glTranslated(0.0, (double)moveVoronoiDown, 0.0);
               RenderUtils.customScaledObject2D(circleCX, circleCY, 0.0F, 0.0F, scaleVoronoi);
               GL11.glDepthMask(false);
               voronoiOfCopyRenderTempEvent.setupVLAARender(false, true)
                  .renderCapturedSegments2d(true, 9, transVoronoiPC01, rotVoronoiPC01, finalAlphaPC * apcVoronoi01, -1, 1);
               GL11.glLineWidth(0.25F * scaleVoronoi * ScaledResolution.lpSCFactor());
               voronoiOfCopyRenderTempEvent.setupVLAARender(true, true)
                  .renderCapturedSegments2d(true, 2, transVoronoiPC01, rotVoronoiPC01, finalAlphaPC * apcVoronoi01, -1, 2);
               GL11.glLineWidth(1.0F);
               GL11.glDepthMask(true);
               GL11.glPopMatrix();
            }

            this.voronoiRenderTempEventList.removeIf(TargetHUD.VoronoiOfCopyRenderTempEvent::removeIf);
         }
      }
   }

   private boolean drawStackClient(ItemStack stack, float x, float y, float scaleMul, float alphaPC) {
      if (stack != null && !stack.isEmpty()) {
         RenderItem itemRender = mc.getRenderItem();
         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.enableDepth();
         GL11.glDepthMask(false);
         GL11.glPushMatrix();
         GL11.glTranslated((double)x, (double)y, 0.0);
         GL11.glScalef(scaleMul, scaleMul, scaleMul);
         GL11.glTranslatef(8.0F, 8.0F, 0.0F);
         GL11.glScalef(alphaPC, alphaPC, alphaPC);
         GL11.glTranslatef(-8.0F, -8.0F, 0.0F);
         itemRender.zLevel = 200.0F;
         itemRender.renderItemAndEffectIntoGUI(stack, 0, 0);
         alphaPC = Math.min(alphaPC, 1.0F);
         itemRender.renderItemOverlayIntoGUIWithTextColor(
            Fonts.minecraftia_16, stack, 0, 0, stack.getCount(), ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC)
         );
         RenderUtils.drawItemWarnIfLowDur(stack, 0.0F, 0.0F, alphaPC, 1.0F);
         ProContainer.get.injectPostDrawStack(stack, 0, 0.0F, 0.0F, alphaPC);
         itemRender.zLevel = 0.0F;
         GL11.glDepthMask(true);
         GL11.glPopMatrix();
         return true;
      } else {
         return false;
      }
   }

   private BufferedImage getBufferedImage(ResourceLocation res) {
      BufferedImage buffer = null;
      if (res != null) {
         try (InputStream inputStream = mc.getResourceManager().getResource(res).getInputStream()) {
            buffer = ImageIO.read(inputStream);
         } catch (IOException var8) {
            return null;
         }
      }

      return buffer;
   }

   private int getColorFromTextureCoord(BufferedImage img, int texX, int texY) {
      return img == null ? 0 : img.getRGB(texX, texY);
   }

   private int[] getColorsFromLastResource(ResourceLocation res) {
      if (res == null) {
         return new int[]{-1, -1, -1, -1};
      } else {
         try {
            BufferedImage bufferedImage;
            if (res != null && (bufferedImage = this.getBufferedImage(res)) != null) {
               float texW = (float)bufferedImage.getWidth();
               float texH = (float)bufferedImage.getHeight();
               float defS = 64.0F;
               int[] uPix = new int[]{(int)(8.0F / defS * texW), (int)(15.0F / defS * texW), (int)(15.0F / defS * texW), (int)(8.0F / defS * texW)};
               int[] vPix = new int[]{(int)(8.0F / defS * texH), (int)(8.0F / defS * texH), (int)(15.0F / defS * texH), (int)(15.0F / defS * texH)};
               this.lastUpdatedResColors[0] = this.getColorFromTextureCoord(bufferedImage, uPix[0], vPix[0]);
               this.lastUpdatedResColors[1] = this.getColorFromTextureCoord(bufferedImage, uPix[1], vPix[1]);
               this.lastUpdatedResColors[2] = this.getColorFromTextureCoord(bufferedImage, uPix[2], vPix[2]);
               this.lastUpdatedResColors[3] = this.getColorFromTextureCoord(bufferedImage, uPix[3], vPix[3]);
            }
         } catch (Exception var8) {
         }

         return this.lastUpdatedResColors;
      }
   }

   private int[] getColorsFromLastResource(ResourceLocation res, float alphaPC) {
      int[] colors = this.getColorsFromLastResource(res);
      colors[0] = ColorUtils.swapAlpha(colors[0], (float)ColorUtils.getAlphaFromColor(colors[0]) * alphaPC);
      colors[1] = ColorUtils.swapAlpha(colors[1], (float)ColorUtils.getAlphaFromColor(colors[1]) * alphaPC);
      colors[2] = ColorUtils.swapAlpha(colors[2], (float)ColorUtils.getAlphaFromColor(colors[2]) * alphaPC);
      colors[3] = ColorUtils.swapAlpha(colors[3], (float)ColorUtils.getAlphaFromColor(colors[3]) * alphaPC);
      return colors;
   }

   public static EntityLivingBase getTarget() {
      boolean pre = get.PreRangedTarget.getBool();
      if ((pre ? HitAura.TARGET_ROTS : HitAura.TARGET) != null) {
         return pre ? HitAura.TARGET_ROTS : HitAura.TARGET;
      } else {
         if (get.RaycastTarget.getBool()) {
            EntityLivingBase base = MathUtils.getPointedEntity(new Vector2f(Minecraft.player.rotationYaw, Minecraft.player.rotationPitch), 60.0, 1.0F, false);
            if (base != null
               && base != Minecraft.player
               && (FreeCam.fakePlayer == null || base != FreeCam.fakePlayer)
               && base.isEntityAlive()
               && !Client.friendManager.isFriend(base.getName())) {
               return base;
            }
         }

         if (BowAimbot.target != null) {
            return BowAimbot.target;
         } else if (CrystalField.get != null && CrystalField.get.getTargets() != null && CrystalField.get.getTargets().size() != 0) {
            return CrystalField.get.getTargets().get(0);
         } else {
            return !(mc.currentScreen instanceof GuiChat) && get.framesShowingForce <= 0 ? null : Minecraft.player;
         }
      }
   }

   @EventTarget
   public void onRender2D(EventRender2D event) {
      if (this.framesShowingForce > 0) {
         this.framesShowingForce--;
      }

      EntityLivingBase target = getTarget();
      this.updatePosition(event);
      double pDX = (double)Math.abs(xPosHud + widthHud / 2.0F - this.THudX.getFloat() * (float)event.getResolution().getScaledWidth());
      double pDY = (double)Math.abs(yPosHud + heightHud / 2.0F - this.THudY.getFloat() * (float)event.getResolution().getScaledHeight());
      float curScale = target == null
            && Math.sqrt(pDX * pDX + pDY * pDY) < Math.sqrt((double)(widthHud * widthHud + heightHud * heightHud)) / 2.0
            && this.framesShowingForce <= 0
         ? 0.0F
         : 1.0F;
      if (curScale == 0.0F) {
         if (this.Scale.to == 1.0F) {
            this.Scale.to = (double)this.Scale.getAnim() > 0.995 ? 1.15F : 0.0F;
            if ((double)MathUtils.getDifferenceOf(this.Scale.getAnim(), 1.0F) < 0.05) {
               this.Scale.setAnim(1.0F);
            }
         } else if (this.Scale() >= (curTarget != null && curTarget != Minecraft.player && !curTarget.isEntityAlive() ? 1.1499F : 1.075F)) {
            this.Scale.to = 0.0F;
         }
      } else {
         if (this.Scale() < 0.75F) {
            this.Scale.setAnim(0.75F);
         }

         if (this.Scale() >= 1.075F) {
            this.Scale.to = 1.0F;
         } else if (this.Scale.to == 0.0F) {
            this.Scale.to = 1.15F;
         }
      }

      this.Scale.speed = 0.07F;
      if (target != null && (target != Minecraft.player || mc.currentScreen instanceof GuiChat || this.framesShowingForce > 0)) {
         curTarget = target;
      }

      if (!((double)this.Scale() < 0.002)) {
         if (soundTarget != target && this.TargettingSFX.getBool()) {
            if (target != null && target != Minecraft.player) {
               ClientTune.get.playTargetSelect();
            }

            soundTarget = target;
         }

         if (curTarget != null) {
            if (this.targetHurt != curTarget.hurtTime) {
               this.targetHurt = curTarget.hurtTime;
            }

            if (skin != null && OldSkin != skin && target != null) {
               OldSkin = skin;
            }
         }

         if (Minecraft.player != null
            && getTarget() == Minecraft.player
            && (mc.currentScreen instanceof GuiChat || this.framesShowingForce > 0)
            && Minecraft.player.connection.getPlayerInfo(Minecraft.player.getName()) != null) {
            OldSkin = WorldRender.get
               .updatedResourceSkin(Minecraft.player.connection.getPlayerInfo(Minecraft.player.getName()).getLocationSkin(), Minecraft.player);
         }

         if (curTarget == null) {
            curTarget = Minecraft.player;
         }

         this.targetName = NameTags.getEntityName(target, false);
         String var8 = this.Mode.getMode();
         switch (var8) {
            case "Light":
               this.renderLight(curTarget);
               break;
            case "WetWorn":
               this.renderWetWorn(curTarget);
               break;
            case "Neomoin":
               this.renderNeomoin(curTarget);
               break;
            case "Modern":
               this.Scale.speed = 0.045F;
               this.noiser.update(target != null ? 0.1F : 0.0275F, target != null);
               this.noiser.insertRender2D(() -> this.renderModern(curTarget), event.getResolution(), 1);
               break;
            case "Bushy":
               this.Scale.speed = 0.03F;
               this.noiser.update(0.03F, target != null);
               this.noiser.insertRender2D(() -> this.renderBushy(curTarget), event.getResolution(), 4);
               break;
            case "Subtle":
               if (target == null) {
                  this.Scale.speed = 0.04F;
               }

               this.noiser.update(target != null ? 0.1F : 0.025F, target != null);
               this.noiser.insertRender2D(() -> this.renderSubtle(curTarget), event.getResolution(), 3);
               break;
            case "Entire":
               this.Scale.speed = 0.05F;
               this.noiser.update(target != null ? 0.08F : 0.03F, target != null);
               this.noiser.insertRender2D(() -> this.renderEntire(curTarget), event.getResolution(), 2);
               break;
            case "Oasis":
               if (target == null) {
                  this.Scale.speed = 0.04F;
               }

               this.noiser.update(this.Scale.to > 0.0F ? 0.06F : 0.04F, this.Scale.to != 0.0F);
               this.noiser.insertRender2D(() -> {
                  try {
                     this.renderOasis(curTarget, new ScaledResolution(mc));
                  } catch (Exception var2x) {
                     var2x.printStackTrace();
                  }
               }, event.getResolution(), 3);
         }
      }
   }

   public void hudScale(float x, float y, float width, float height) {
      if ((double)MathUtils.getDifferenceOf(this.Scale(), 1.0F) > 0.01) {
         RenderUtils.customScaledObject2D(x, y, width, height, this.Scale());
      }
   }

   public void hudScalePro(float x, float y, float width, float height) {
      float scale = MathUtils.clamp(this.Scale(), 0.0F, 2.0F) / 4.0F + 0.75F;
      RenderUtils.customScaledObject2DPro(x, y, width, height, scale, scale);
   }

   private final float getArmorPercent01(EntityLivingBase entity) {
      float armPC = 0.0F;

      for (ItemStack armorElement : entity.getArmorInventoryList()) {
         if (!armorElement.isEmpty() && armorElement != null) {
            float maxDurable = (float)armorElement.getMaxDamage();
            float armorHealth = maxDurable - (float)armorElement.getItemDamage();
            float durrablePC = armorHealth / maxDurable;
            armPC += durrablePC;
         }
      }

      return MathUtils.clamp(armPC / 4.0F, 0.0F, 1.0F);
   }

   private class VoronoiOfCopyRenderTempEvent extends VoronoiOfCopyRenderTemp {
      private final int lifeTimeMax;
      private final TimerHelper renderTime = TimerHelper.TimerHelperReseted();

      public VoronoiOfCopyRenderTempEvent(float x, float y, float x2, float y2, int countOfPoints, boolean createInitThread, int lifeTimeMax) {
         super(x, y, x2, y2, countOfPoints, createInitThread);
         this.lifeTimeMax = lifeTimeMax;
      }

      public VoronoiOfCopyRenderTempEvent(float x, float y, float x2, float y2, List<VoronoiOfQuad.Vec2f> points, boolean createInitThread, int lifeTimeMax) {
         super(x, y, x2, y2, points, createInitThread);
         this.lifeTimeMax = lifeTimeMax;
      }

      public float getTimePC01() {
         return Math.min((float)this.renderTime.getTime() / (float)this.lifeTimeMax, 1.0F);
      }

      public float getAlphaPC() {
         return 1.0F - this.getTimePC01();
      }

      public boolean removeIf() {
         return this.getTimePC01() == 1.0F;
      }
   }

   private class particle {
      long time = System.currentTimeMillis();
      float x;
      float y;
      AnimationUtils xs = new AnimationUtils(0.0F, 0.0F, 0.0075F);
      AnimationUtils ys = new AnimationUtils(0.0F, 0.0F, 0.0075F);
      float motionX;
      float motionY;

      public particle(float x, float y) {
         this.x = x;
         this.y = y;
         this.motionX = (float)MathUtils.getRandomInRange(-1.0, 1.0);
         this.motionY = (float)MathUtils.getRandomInRange(-1.0, 1.0);
         this.xs.setAnim(x);
         this.ys.setAnim(y);
         this.xs.to = this.motionX * 100.0F + x;
         this.ys.to = this.motionY * 100.0F + y;
      }

      public long getTime() {
         return System.currentTimeMillis() - this.time;
      }

      public void update(int color) {
         float rand = 0.0035F * Math.max((float)Minecraft.getDebugFPS(), 5.0F) * 15.0F;
         this.x = this.x + (this.motionX = this.motionX = (float)((double)this.motionX / 1.02)) * rand;
         this.y = this.y + (this.motionY = this.motionY = (float)((double)this.motionY / 1.02)) * rand;
         float ss = 3.0F * ((float)ColorUtils.getAlphaFromColor(color) / 255.0F);
         float x = this.xs.getAnim();
         float y = this.ys.getAnim();
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            x - ss, y - ss, x + ss, y + ss, ss, 1.0F, color, color, color, color, false, true, true
         );
      }
   }
}
