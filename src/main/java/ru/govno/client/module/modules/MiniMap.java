package ru.govno.client.module.modules;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Minimap.MinimapData;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class MiniMap extends Module {
   public static MiniMap get;
   private final MinimapData data;
   public FloatSettings MapOpacity;
   public FloatSettings MapX;
   public FloatSettings MapY;
   public FloatSettings MapScale;
   public BoolSettings MapSmoothing;
   public BoolSettings ShowMaximalLoad;
   public BoolSettings MapBlooming;
   private final boolean[] ISHOVERED = new boolean[2];
   private final AnimationUtils mousePushX = new AnimationUtils(0.0F, 0.0F, 0.04F);
   private final AnimationUtils mousePushY = new AnimationUtils(0.0F, 0.0F, 0.04F);
   private final AnimationUtils hoverInScaleFactor = new AnimationUtils(1.0F, 1.0F, 0.07F);
   private final Tessellator tessellator = Tessellator.getInstance();
   private final BufferBuilder buffer = this.tessellator.getBuffer();
   private float smoothMapRotate;

   public MiniMap() {
      super("MiniMap", 0, Module.Category.RENDER);
      this.data = new MinimapData(64);
      this.settings.add(this.MapOpacity = new FloatSettings("MapOpacity", 0.8F, 1.0F, 0.1F, this));
      this.settings.add(this.MapX = new FloatSettings("MapX", 0.025F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.MapY = new FloatSettings("MapY", 0.2F, 1.0F, 0.0F, this, () -> false));
      this.settings.add(this.MapScale = new FloatSettings("MapScale", 80.0F, 400.0F, 40.0F, this, () -> false));
      this.settings.add(this.MapSmoothing = new BoolSettings("MapSmoothing", true, this));
      this.settings.add(this.ShowMaximalLoad = new BoolSettings("ShowMaximalLoad", false, this));
      this.settings.add(this.MapBlooming = new BoolSettings("MapBlooming", false, this));
      this.setDemand(3, 3);
      get = this;
   }

   public float getMapX(ScaledResolution sr) {
      return (float)sr.getScaledWidth() * this.MapX.getAnimation();
   }

   public float getMapY(ScaledResolution sr) {
      return (float)sr.getScaledHeight() * this.MapY.getAnimation();
   }

   public float getMapScale() {
      return this.MapScale.getAnimation();
   }

   public boolean[] isHoveredToMinimap(int mouseX, int mouseY, ScaledResolution sr) {
      float lpSCFactor = ScaledResolution.lpSCFactor();
      if (this.isActived() && (!ComfortUi.get.isPaintInChat() || !GuiChat.paintUI.isHoveredAny(mouseX, mouseY, false))) {
         this.ISHOVERED[1] = RenderUtils.isHovered(
            (float)mouseX, (float)mouseY, this.getMapX(sr) + this.getMapScale() - 8.0F, this.getMapY(sr) - 8.0F + this.getMapScale(), 8.0F, 8.0F
         );
         this.ISHOVERED[0] = !this.ISHOVERED[1]
            && RenderUtils.isHovered((float)mouseX, (float)mouseY, this.getMapX(sr), this.getMapY(sr), this.getMapScale(), this.getMapScale());
      } else {
         this.ISHOVERED[1] = false;
         this.ISHOVERED[0] = false;
      }

      return this.ISHOVERED;
   }

   private void mouseHoverPushScreen(int mouseX, int mouseY, float centerX, float centerY, float mapScale, ScaledResolution sr) {
      boolean[] hover = this.isHoveredToMinimap(mouseX, mouseY, sr);
      boolean isInChat = mc.currentScreen instanceof GuiChat;
      boolean hoverScreen = isInChat && (hover[0] || ((GuiChat)mc.currentScreen).dragging12[0]) && !((GuiChat)mc.currentScreen).dragging12[1];
      boolean hoverDrag = isInChat && ((GuiChat)mc.currentScreen).dragging12[1] && !hoverScreen;
      float scaleTo = hoverDrag ? 0.9F : (hoverScreen ? 1.5F : 1.0F);
      this.hoverInScaleFactor.to = scaleTo;
      float moveFactor = this.hoverInScaleFactor.getAnim() * 1.5F;
      float scallingToX = hoverDrag ? centerX + mapScale / 2.0F : (hoverScreen ? MathUtils.lerp(centerX, (float)mouseX, moveFactor) : centerX);
      float scallingToY = hoverDrag ? centerY + mapScale / 2.0F : (hoverScreen ? MathUtils.lerp(centerY, (float)mouseY, moveFactor) : centerY);
      if (this.mousePushX.getAnim() == 0.0F) {
         this.mousePushX.setAnim(centerX);
      }

      if (this.mousePushY.getAnim() == 0.0F) {
         this.mousePushY.setAnim(centerY);
      }

      this.mousePushX.speed = scallingToX == centerX ? 0.03F : (isInChat && ((GuiChat)mc.currentScreen).dragging12[0] ? 1.0F : 0.08F);
      this.mousePushY.speed = scallingToY == centerY ? 0.03F : (isInChat && ((GuiChat)mc.currentScreen).dragging12[0] ? 1.0F : 0.08F);
      this.mousePushX.to = scallingToX;
      this.mousePushY.to = scallingToY;
      RenderUtils.customScaledObject2D(this.mousePushX.anim, this.mousePushY.anim, 0.0F, 0.0F, this.hoverInScaleFactor.anim);
   }

   public float getRound() {
      return this.getMapScale() / 10.0F;
   }

   private void drawVecsOfPoints(Runnable drawMap, boolean bloom) {
      GL11.glDepthMask(false);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2884);
      GL11.glEnable(3553);
      GL11.glAlphaFunc(516, 0.003921569F);
      if (bloom) {
         GL11.glBlendFunc(770, 32772);
      }

      drawMap.run();
      if (bloom) {
         GL11.glBlendFunc(770, 771);
      }

      GlStateManager.resetColor();
      GlStateManager.enableRescaleNormal();
      GL11.glEnable(2884);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glDepthMask(true);
   }

   private List<EntityLivingBase> basesToPointing() {
      return (List<EntityLivingBase>)(mc.world == null
         ? new ArrayList<>()
         : mc.world
            .getLoadedEntityList()
            .stream()
            .map(Entity::getLivingBaseOf)
            .filter(base -> !(base instanceof EntityPlayerSP))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
   }

   private int getEntityPointColor(EntityLivingBase base) {
      int color = ColorUtils.getColor(164, 255, 100, 70);
      if (base instanceof EntityPlayer) {
         color = Client.friendManager.isFriend(base.getName())
            ? ColorUtils.getColor(40, 255, 40)
            : (
               !base.getUniqueID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + base.getName()).getBytes(StandardCharsets.UTF_8)))
                     && base instanceof EntityOtherPlayerMP
                  ? ColorUtils.getColor(220, 125, 30)
                  : ColorUtils.getColor(255, 40, 20)
            );
      }

      return color;
   }

   private void drawEntityPoint(
      EntityLivingBase base,
      float centerX,
      float centerY,
      float cameraX,
      float cameraZ,
      float mapScale,
      float scaleFactor,
      float pTicks,
      float alphaPC,
      MinimapData data
   ) {
      if (base != null && base.isEntityAlive()) {
         scaleFactor *= mapScale;
         scaleFactor /= (float)data.getRange();
         scaleFactor /= 1.5F;
         double smoothPosX = base.prevPosX + (base.posX - base.prevPosX) * (double)pTicks;
         double smoothPosZ = base.prevPosZ + (base.posZ - base.prevPosZ) * (double)pTicks;
         float x = (float)(smoothPosX - (double)cameraX) * scaleFactor;
         float y = (float)(smoothPosZ - (double)cameraZ) * scaleFactor;
         if (!(Math.sqrt((double)(x * x + y * y)) > (double)mapScale)) {
            x += centerX;
            y += centerY;
            float degrRadius = 0.05F * mapScale * 60.0F / (float)data.getRange();
            float degress = 52.5F;
            float lpSCFactor = ScaledResolution.lpSCFactor();
            float pointS1 = mapScale / (base instanceof EntityOtherPlayerMP ? 16.0F : 32.0F) * lpSCFactor;
            float pointS2 = pointS1 * 1.125F;
            int baseColor = this.getEntityPointColor(base);
            baseColor = ColorUtils.swapAlpha(baseColor, (float)ColorUtils.getAlphaFromColor(baseColor) * alphaPC);
            GL11.glDisable(3553);
            GL11.glShadeModel(7425);
            float entityYaw = base.prevRotationYaw + (base.rotationYaw - base.prevRotationYaw) * pTicks + 180.0F;
            float rotMin = entityYaw - degress;
            float rotMax = entityYaw + degress;
            float rotMinRadian = MathHelper.toRadians(rotMin);
            float rotMaxRadian = MathHelper.toRadians(rotMax);
            if (base instanceof EntityOtherPlayerMP) {
               GL11.glBlendFunc(770, 32772);
               GL11.glLineWidth(0.5F * lpSCFactor);
               this.buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);
               this.buffer
                  .pos((double)(x + MathHelper.sin(rotMinRadian) * degrRadius), (double)(y - MathHelper.cos(rotMinRadian) * degrRadius))
                  .color(ColorUtils.swapAlpha(baseColor, (float)ColorUtils.getAlphaFromColor(baseColor) / 5.0F))
                  .endVertex();
               this.buffer.pos((double)x, (double)y).color(baseColor).endVertex();
               this.buffer
                  .pos((double)(x + MathHelper.sin(rotMaxRadian) * degrRadius), (double)(y - MathHelper.cos(rotMaxRadian) * degrRadius))
                  .color(ColorUtils.swapAlpha(baseColor, (float)ColorUtils.getAlphaFromColor(baseColor) / 5.0F))
                  .endVertex();
               this.tessellator.draw();
               GL11.glLineWidth(1.0F);
               int polygonC1 = ColorUtils.swapAlpha(baseColor, (float)ColorUtils.getAlphaFromColor(baseColor) / 4.0F);
               int polygonC2 = ColorUtils.swapAlpha(baseColor, 0.0F);
               this.buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
               this.buffer.pos((double)x, (double)y).color(polygonC1).endVertex();

               for (float rot = rotMin; rot <= rotMax; rot += (rotMax - rotMin) / 3.0F) {
                  float radian = MathHelper.toRadians(rot);
                  this.buffer
                     .pos((double)(x + MathHelper.sin(radian) * degrRadius), (double)(y - MathHelper.cos(radian) * degrRadius))
                     .color(polygonC2)
                     .endVertex();
               }

               this.tessellator.draw();
               GL11.glBlendFunc(770, 771);
            }

            GL11.glEnable(2832);
            GL11.glPointSize(pointS2);
            this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
            this.buffer.pos((double)x, (double)y).color(ColorUtils.toDark(baseColor, 0.275F)).endVertex();
            this.tessellator.draw();
            GL11.glPointSize(pointS1);
            this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
            this.buffer.pos((double)x, (double)y).color(baseColor).endVertex();
            this.tessellator.draw();
            GL11.glPointSize(1.0F);
            GL11.glShadeModel(7424);
            GL11.glEnable(3553);
         }
      }
   }

   private void drawSelfPoint(float screenX, float screenY, float mapScale, float scaleFactor, float alphaPC) {
      float lpSCFactor = ScaledResolution.lpSCFactor();
      GlStateManager.resetColor();
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 32772);
      GL11.glEnable(2832);
      int shadowCol = ColorUtils.swapAlpha(-1, 40.0F * alphaPC);
      int shadowCol2 = ColorUtils.swapAlpha(-1, 35.0F * alphaPC);
      float shadowSize = 10.0F * scaleFactor * mapScale / 200.0F;
      float shadowRadius = 2.25F * mapScale / 200.0F * 1.5F;
      RenderUtils.drawCroneShadow((double)screenX, (double)screenY, 0, 360, shadowRadius, shadowSize, shadowCol, 0, false);
      RenderUtils.drawCroneShadow((double)screenX, (double)screenY, 0, 360, shadowRadius, shadowSize / 5.0F, shadowCol2, 0, false);
      RenderUtils.drawCroneShadow((double)screenX, (double)screenY, 0, 360, shadowRadius / 1.5F, shadowRadius / 3.0F, 0, shadowCol2, false);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glPointSize(15.0F * scaleFactor * mapScale / 200.0F * 1.05F * lpSCFactor);
      this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
      this.buffer.pos((double)screenX, (double)screenY).color(ColorUtils.swapAlpha(ColorUtils.toDark(-1, 0.5F), 255.0F * alphaPC)).endVertex();
      this.tessellator.draw();
      GL11.glPointSize(15.0F * scaleFactor * mapScale / 200.0F * lpSCFactor);
      this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
      this.buffer.pos((double)screenX, (double)screenY).color(ColorUtils.swapAlpha(-1, 255.0F * alphaPC)).endVertex();
      this.tessellator.draw();
      GL11.glPointSize(15.0F * scaleFactor * mapScale / 200.0F / 1.25F * lpSCFactor);
      this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
      this.buffer.pos((double)screenX, (double)screenY).color(ColorUtils.swapAlpha(ColorUtils.toDark(-1, 0.25F), 255.0F * alphaPC)).endVertex();
      this.tessellator.draw();
      GL11.glPointSize(15.0F * scaleFactor * mapScale / 200.0F / 1.75F * lpSCFactor);
      this.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
      this.buffer.pos((double)screenX, (double)screenY).color(ColorUtils.swapAlpha(ColorUtils.toDark(-1, 0.75F), 255.0F * alphaPC)).endVertex();
      this.tessellator.draw();
      GL11.glPointSize(1.0F);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3553);
   }

   private void drawAllChunkColorBuffers(
      MinimapData data,
      float screenX,
      float screenY,
      float scale,
      float alphaPC,
      float mapRotate,
      float mapScale,
      float pTicks,
      ScaledResolution sr,
      boolean smoothPixels,
      boolean bloom
   ) {
      if (Minecraft.player != null && !(alphaPC * 255.0F < 1.0F)) {
         float delta = (float)MathUtils.easeInOutQuad((double)MathUtils.clamp(MathUtils.getDifferenceOf(this.smoothMapRotate, mapRotate) / 90.0F, 0.0F, 1.0F));
         delta *= delta;
         this.smoothMapRotate = MathUtils.lerp(this.smoothMapRotate, mapRotate, delta);
         double dx = data.getLastPlayerDX() - MathUtils.lerp(Minecraft.player.lastTickPosX, Minecraft.player.posX, (double)pTicks);
         double dz = data.getLastPlayerDZ() - MathUtils.lerp(Minecraft.player.lastTickPosZ, Minecraft.player.posZ, (double)pTicks);
         float round = this.getRound();
         int bColor1 = ColorUtils.getColor(0, (int)(70.0F * alphaPC));
         int outColor2 = ColorUtils.getColor(0, (int)(95.0F * alphaPC));
         int outColor3 = ColorUtils.getColor(0, (int)(145.0F * alphaPC));
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            screenX, screenY, screenX + scale, screenY + scale, round, round / 4.0F, bColor1, bColor1, bColor1, bColor1, false, true, true
         );
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            screenX, screenY, screenX + scale, screenY + scale, round, 0.5F, outColor3, outColor3, outColor3, outColor3, false, false, true
         );
         StencilUtil.initStencilToWrite();
         RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
            screenX, screenY, screenX + scale, screenY + scale, round, 0.0F, -1, -1, -1, -1, false, true, false
         );
         StencilUtil.readStencilBuffer(1);
         GL11.glPushMatrix();
         this.mouseHoverPushScreen(GuiScreen.staticMouseX, GuiScreen.staticMouseY, screenX + scale / 2.0F, screenY + scale / 2.0F, scale, sr);
         float finalScreenX = (float)((double)screenX + dx * (double)scale / (double)((float)data.getRange()) - 0.25);
         float finalScreenY = (float)((double)screenY + dz * (double)scale / (double)((float)data.getRange()) - 0.25);
         if (mapScale != 1.0F) {
            RenderUtils.customScaledObject2D(screenX, screenY, scale, scale, mapScale);
         }

         int mapColor = ColorUtils.swapAlpha(-1, 255.0F * alphaPC);
         this.drawVecsOfPoints(
            () -> {
               GL11.glPushMatrix();
               data.getTexture().setBlurMipmap(smoothPixels, smoothPixels);
               TextureUtil.bindTexture(data.getTexture().getGlTextureId());
               RenderUtils.customRotatedObject2D(screenX, screenY, scale, scale, (double)(-this.smoothMapRotate + 180.0F));
               this.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.buffer.pos((double)finalScreenX, (double)finalScreenY).tex(0.0, 0.0).color(mapColor).endVertex();
               this.buffer.pos((double)(finalScreenX + scale), (double)finalScreenY).tex(1.0, 0.0).color(mapColor).endVertex();
               this.buffer.pos((double)(finalScreenX + scale), (double)(finalScreenY + scale)).tex(1.0, 1.0).color(mapColor).endVertex();
               this.buffer.pos((double)finalScreenX, (double)(finalScreenY + scale)).tex(0.0, 1.0).color(mapColor).endVertex();
               GL11.glAlphaFunc(516, 27.0F * alphaPC / 255.0F);
               if (bloom) {
                  GL11.glBlendFunc(770, 32772);
               }

               this.tessellator.draw();
               if (bloom) {
                  GL11.glBlendFunc(770, 771);
               }

               GL11.glAlphaFunc(516, 0.003921569F);
               this.basesToPointing()
                  .forEach(
                     base -> this.drawEntityPoint(
                           base,
                           screenX + scale / 2.0F,
                           screenY + scale / 2.0F,
                           (float)RenderManager.renderPosX,
                           (float)RenderManager.renderPosZ,
                           scale,
                           mapScale,
                           pTicks,
                           alphaPC,
                           data
                        )
                  );
               this.drawSelfPoint(screenX + scale / 2.0F, screenY + scale / 2.0F, scale, mapScale, alphaPC);
               GL11.glPopMatrix();
            },
            bloom
         );
         GL11.glPopMatrix();
         StencilUtil.uninitStencilBuffer();
         RenderUtils.drawInsideFullRoundedFullGradientShadowRectWithBloomBool(
            screenX, screenY, screenX + scale, screenY + scale, round - 1.0F, 1.0F, outColor3, outColor3, outColor3, outColor3, false
         );
         RenderUtils.drawRoundedFullGradientInsideShadow(
            screenX, screenY, screenX + scale, screenY + scale, round * 2.0F, outColor2, outColor2, outColor2, outColor2, false
         );
         if (mc.currentScreen instanceof GuiChat chat && !chat.dragging12[1]) {
            int moveMarkColor = ColorUtils.swapAlpha(-1, 155.0F * (0.3F + 0.7F * alphaPC));
            RenderUtils.drawLightContureRectSmooth(
               (double)(screenX + scale - 8.0F), (double)(screenY + scale - 8.0F), (double)(screenX + scale), (double)(screenY + scale), moveMarkColor
            );
            RenderUtils.render2D(3, DefaultVertexFormats.POSITION_COLOR, 0.5F, () -> {
               RenderUtils.buffer.pos((double)(screenX + scale - 8.0F), (double)(screenY + scale)).color(moveMarkColor).endVertex();
               RenderUtils.buffer.pos((double)(screenX + scale), (double)(screenY + scale - 8.0F)).color(moveMarkColor).endVertex();
            });
         }
      }
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      if (!this.isActived()) {
         this.stateAnim.to = 0.0F;
         if (this.stateAnim.anim * 255.0F < 1.0F) {
            return;
         }
      }

      float alphaPC = this.stateAnim.getAnim();
      if (this.actived && this.stateAnim.to == 0.0F) {
         this.stateAnim.to = 1.0F;
      }

      alphaPC *= this.MapOpacity.getFloat();
      float mapX = this.getMapX(sr);
      float mapY = this.getMapY(sr);
      float scale = this.getMapScale();
      float pTicks = mc.getRenderPartialTicks();
      float yaw = Minecraft.player.rotationYaw;
      if (Minecraft.player.isRiding()) {
         float pYaw = Minecraft.player.rotationYaw;
         yaw = Minecraft.player.ridingEntity.prevRotationYaw
            + (Minecraft.player.ridingEntity.rotationYaw - Minecraft.player.ridingEntity.prevRotationYaw) * pTicks;
         yaw += pYaw - yaw;
      }

      this.drawAllChunkColorBuffers(
         this.data,
         mapX,
         mapY,
         scale,
         alphaPC,
         yaw,
         1.4F - 0.4F * this.ShowMaximalLoad.getAnimation(),
         pTicks,
         sr,
         this.MapSmoothing.getBool(),
         this.MapBlooming.getBool()
      );
   }

   @Override
   public void onUpdate() {
      if (mc.world != null && Minecraft.player != null) {
         int range = (int)MathUtils.clamp((float)(16 * mc.gameSettings.renderDistanceChunks + 16) / 1.5F, 48.0F, 192.0F);
         if (range != this.data.getRange()) {
            this.data.setRange(range);
         }

         this.data.updateMap(mc.world, Minecraft.player);
      }
   }
}
