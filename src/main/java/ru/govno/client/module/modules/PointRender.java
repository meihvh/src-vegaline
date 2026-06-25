package ru.govno.client.module.modules;

import java.util.List;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class PointRender {
   private static final ResourceLocation TEXTURE = new ResourceLocation("vegaline/system/points/pointsmark.png");
   private static final ResourceLocation TEXTURE2 = new ResourceLocation("vegaline/system/points/pointsmark2.png");

   public void render2D() {
      this.renderVoid2d();
   }

   public void render3D() {
      this.renderVoid3d();
   }

   public void renderPoints2d() {
      List<PointTrace> pointsList = PointTrace.getPointList()
         .stream()
         .filter(
            point -> point.getServerName()
                  .equalsIgnoreCase(
                     !Minecraft.getMinecraft().isSingleplayer() && Minecraft.getMinecraft().getCurrentServerData() != null
                        ? Minecraft.getMinecraft().getCurrentServerData().serverIP
                        : "SinglePlayer"
                  )
         )
         .toList();
      pointsList = FriendsSLink.get.getPointTraceListAppendAtFriendLinks(pointsList);
      if (!pointsList.isEmpty()) {
         GL11.glPushMatrix();
         GL11.glDisable(2896);
         float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
         int scaleFactor = ScaledResolution.getScaleFactor();
         double scaling = (double)scaleFactor / Math.pow((double)scaleFactor, 2.0);
         GL11.glScaled(scaling, scaling, scaling);
         EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;

         for (PointTrace points : pointsList) {
            boolean isDeathPoint = points.getName().startsWith("Death");
            double px = PointTrace.getX(points);
            double py = PointTrace.getY(points);
            double pz = PointTrace.getZ(points);
            if (Minecraft.player.dimension == -1 && PointTrace.getDemension(points) != -1) {
               px = PointTrace.getX(points) / 8.0;
               py = PointTrace.getY(points);
               pz = PointTrace.getZ(points) / 8.0;
            } else if (Minecraft.player.dimension != -1 && PointTrace.getDemension(points) == -1) {
               px = PointTrace.getX(points) * 8.0;
               py = PointTrace.getY(points);
               pz = PointTrace.getZ(points) * 8.0;
            }

            float pTicks = Minecraft.getMinecraft().getRenderPartialTicks();
            double xposme = Minecraft.player.lastTickPosX + (Minecraft.player.posX - Minecraft.player.lastTickPosX) * (double)pTicks;
            double yposme = Minecraft.player.lastTickPosY + (Minecraft.player.posY - Minecraft.player.lastTickPosY) * (double)pTicks;
            double zposme = Minecraft.player.lastTickPosZ + (Minecraft.player.posZ - Minecraft.player.lastTickPosZ) * (double)pTicks;
            double x = px;
            double y = py;
            double z = pz;
            double distance = (double)MathHelper.sqrt((px - xposme) * (px - xposme) + (py - yposme) * (py - yposme) + (pz - zposme) * (pz - zposme));
            double distanceXZ = (double)MathHelper.sqrt((px - xposme) * (px - xposme) + (pz - zposme) * (pz - zposme));
            double maxDistance = 128.0;
            if (distanceXZ > maxDistance) {
               Vec3d fixatedDistantVec = new Vec3d(px - xposme, py - (yposme + (double)Minecraft.player.getEyeHeight()), pz - zposme)
                  .scale(1.0 / distance * maxDistance)
                  .addVector(xposme, yposme + (double)Minecraft.player.getEyeHeight(), zposme);
               x = fixatedDistantVec.xCoord;
               y = fixatedDistantVec.yCoord;
               z = fixatedDistantVec.zCoord;
            }

            AxisAlignedBB aabb = new AxisAlignedBB(x, y, z, x, y, z);
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
            entityRenderer.setupCameraTransformCompactCalcMatrix(partialTicks);
            Vector4d position = null;

            for (Vector3d vector : vectors) {
               vector = this.project2D(
                  scaleFactor, vector.x - RenderManager.viewerPosX, vector.y - RenderManager.viewerPosY, vector.z - RenderManager.viewerPosZ
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

            if (position != null) {
               entityRenderer.setupOverlayRendering();
               double posX = position.x;
               double posY = position.y;
               double endPosX = position.z;
               double endPosY = position.w;
               CFontRenderer font = Fonts.mntsb_7;
               int timeInterval = isDeathPoint ? 350 : 700;
               float pcTime = (float)((System.currentTimeMillis() + (long)points.getIndex() * 350L) % (long)timeInterval) / (float)timeInterval;
               pcTime = ((double)pcTime > 0.5 ? 1.0F - pcTime : pcTime) * 2.0F;
               pcTime *= pcTime;
               float yExtend = 0.0F;
               if (points.getName().toLowerCase().contains("home")
                  || points.getName().toLowerCase().contains("дом")
                  || points.getName().toLowerCase().contains("death")) {
                  yExtend += -(isDeathPoint ? 2.0F : 5.0F) * pcTime;
               }

               float xp = (float)(posX + (endPosX - posX) / 2.0);
               float yp = (float)(posY - 5.0) + yExtend;
               GlStateManager.enableBlend();
               Minecraft.getMinecraft().getTextureManager().bindTexture(isDeathPoint ? TEXTURE2 : TEXTURE);
               int markCol = ColorUtils.swapAlpha(
                  points.isLink()
                     ? ColorUtils.getColor(50, 100, 255)
                     : (
                        PointTrace.getDemension(points) == -1
                           ? ColorUtils.getColor(255, 40, 95)
                           : (PointTrace.getDemension(points) == 1 ? ColorUtils.getColor(255, 255, 95) : ColorUtils.getColor(40, 255, 95))
                     ),
                  155.0F
               );
               float texW = isDeathPoint ? 12.0F : 8.0F;
               float texH = 12.0F;
               float texX = xp - texW / 2.0F;
               float texY = yp - texH;
               String coords = (int)px + " " + (int)py + " " + (int)pz;
               String dst = String.format("%.1f", distanceXZ) + "m";
               Tessellator tessellator = Tessellator.getInstance();
               BufferBuilder bufferbuilder = tessellator.getBuffer();
               bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               bufferbuilder.pos((double)texX, (double)(texY + texH)).tex(0.0, 1.0).color(markCol).endVertex();
               bufferbuilder.pos((double)(texX + texW), (double)(texY + texH)).tex(1.0, 1.0).color(markCol).endVertex();
               bufferbuilder.pos((double)(texX + texW), (double)texY).tex(1.0, 0.0).color(markCol).endVertex();
               bufferbuilder.pos((double)texX, (double)texY).tex(0.0, 0.0).color(markCol).endVertex();
               GL11.glShadeModel(7425);
               GL11.glEnable(3553);
               tessellator.draw();
               markCol = ColorUtils.getOverallColorFrom(markCol, -1);
               if (isDeathPoint) {
                  float name$dstWidth = font.getStringWidth(points.getName() + " " + dst) + 2.0F;
                  font.drawString(points.getName() + " " + dst, texX + texW / 2.0F - name$dstWidth / 2.0F, texY + texH - yExtend + 2.0F, markCol);
                  font.drawString(coords, texX + texW / 2.0F - font.getStringWidth(coords) / 2.0F, texY - yExtend + texH + 6.0F, markCol);
               } else {
                  font.drawString(
                     dst + " " + points.getName(),
                     texX - font.getStringWidth(dst + " " + points.getName()) / 2.0F + texW / 2.0F,
                     texY + texH + 2.0F - yExtend / 2.0F,
                     markCol
                  );
                  font.drawString(coords, texX - font.getStringWidth(coords) / 2.0F + texW / 2.0F, texY + texH + 5.5F - yExtend / 1.5F, markCol);
               }
            }
         }

         GL11.glEnable(2929);
         GlStateManager.enableBlend();
         entityRenderer.setupOverlayRendering();
         GL11.glPopMatrix();
      }
   }

   public void drawSphere3dPolygon(double x, double y, double z, float radius, int shapes, int color) {
      Sphere sphere = new Sphere();
      RenderUtils.glColor(color);
      GL11.glTranslated(x, y, z);
      GL11.glRotated(90.0, 1.0, 0.0, 0.0);
      sphere.setDrawStyle(100012);
      sphere.draw(radius, shapes, shapes * 2);
      GL11.glRotated(90.0, -1.0, 0.0, 0.0);
      GL11.glTranslated(-x, -y, -z);
      GlStateManager.resetColor();
   }

   public void drawSphere3dPoints(double x, double y, double z, float radius, int shapes, int color) {
      Sphere sphere = new Sphere();
      RenderUtils.glColor(color);
      GL11.glTranslated(x, y, z);
      GL11.glRotated(90.0, 1.0, 0.0, 0.0);
      sphere.setDrawStyle(100010);
      sphere.draw(radius, shapes, shapes * 2);
      GL11.glRotated(90.0, -1.0, 0.0, 0.0);
      GL11.glTranslated(-x, -y, -z);
      GlStateManager.resetColor();
   }

   public void drawSphere3dLines(double x, double y, double z, float radius, int shapes, int color) {
      Sphere sphere = new Sphere();
      RenderUtils.glColor(color);
      GL11.glTranslated(x, y, z);
      GL11.glRotated(90.0, 1.0, 0.0, 0.0);
      sphere.setDrawStyle(100011);
      sphere.draw(radius, shapes, shapes * 2);
      GL11.glRotated(90.0, -1.0, 0.0, 0.0);
      GL11.glTranslated(-x, -y, -z);
      GlStateManager.resetColor();
   }

   public void renderPoints3d() {
      List<PointTrace> pointsList = PointTrace.getPointList()
         .stream()
         .filter(
            pointx -> pointx.getServerName()
                  .equalsIgnoreCase(
                     !Minecraft.getMinecraft().isSingleplayer() && Minecraft.getMinecraft().getCurrentServerData() != null
                        ? Minecraft.getMinecraft().getCurrentServerData().serverIP
                        : "SinglePlayer"
                  )
         )
         .toList();
      pointsList = FriendsSLink.get.getPointTraceListAppendAtFriendLinks(pointsList);
      if (!pointsList.isEmpty()) {
         long time = System.currentTimeMillis();
         long stepTime = 1500L;
         float timePC = (float)(time % stepTime) / (float)stepTime;
         float animDelta = (float)MathUtils.easeInOutQuadWave((double)timePC);
         if (animDelta != 0.0F && Minecraft.player != null) {
            double minRange = 0.7F;
            double maxRange = 0.85F;
            int minSteps = 20;
            int maxSteps = 70;
            int alphaTrace = 255;
            int minShapes = 6;
            int maxShapes = 9;
            double glX = RenderManager.viewerPosX;
            double glY = RenderManager.viewerPosY;
            double glZ = RenderManager.viewerPosZ;
            GL11.glPushMatrix();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            Minecraft.getMinecraft().entityRenderer.disableLightmap();
            GL11.glEnable(3042);
            GL11.glAlphaFunc(516, 0.0F);
            GL11.glLineWidth(1.0F);
            GL11.glDisable(2884);
            GL11.glDepthMask(false);
            GL11.glDisable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(2896);
            GL11.glPointSize(1.0F);
            GL11.glEnable(2832);
            GL11.glShadeModel(7425);
            GL11.glTranslated(-glX, -glY, -glZ);
            double rotate = (double)((float)(System.currentTimeMillis() % 9000L) / 9000.0F * 360.0F);

            for (PointTrace point : pointsList) {
               if (point.dimension == Minecraft.player.dimension) {
                  float dstAPC = (float)MathUtils.clamp((10.0 - Minecraft.player.getDistance(point.x, point.y, point.z)) / 9.0, 0.0, 1.0);
                  if (dstAPC != 0.0F) {
                     int steps = (int)MathUtils.lerp(
                        (double)minSteps, (double)maxSteps, Math.min(MathUtils.easeOutCirc((double)dstAPC) * (double)animDelta * 1.5, 1.0)
                     );
                     int shapes = maxShapes;
                     if (steps != 0) {
                        double statRange = MathUtils.lerp(minRange, maxRange, (double)(animDelta / 5.0F));
                        int markCol = point.isLink()
                           ? ColorUtils.getColor(50, 100, 255)
                           : (
                              PointTrace.getDemension(point) == -1
                                 ? ColorUtils.getColor(255, 40, 95)
                                 : (PointTrace.getDemension(point) == 1 ? ColorUtils.getColor(255, 255, 95) : ColorUtils.getColor(40, 255, 95))
                           );
                        GL11.glTranslated(point.x, point.y, point.z);
                        GL11.glRotated(rotate, 0.0, 1.0, 0.0);
                        GL11.glTranslated(-point.x, -point.y, -point.z);
                        GL11.glHint(3154, 4354);
                        GL11.glEnable(2848);
                        GL11.glLineWidth(0.1F);
                        this.drawSphere3dLines(
                           point.x,
                           point.y,
                           point.z,
                           (float)statRange,
                           maxShapes,
                           ColorUtils.swapAlpha(markCol, 255.0F * (float)(0.1F + MathUtils.easeInOutQuadWave((double)animDelta) * 0.4F) * dstAPC)
                        );
                        GL11.glHint(3154, 4352);
                        GL11.glDisable(2848);
                        GL11.glEnable(2884);
                        this.drawSphere3dPolygon(
                           point.x,
                           point.y,
                           point.z,
                           (float)statRange,
                           maxShapes * 3,
                           ColorUtils.swapAlpha(markCol, 20.0F * (float)(0.1F + MathUtils.easeInOutQuadWave((double)animDelta) * 0.9F) * dstAPC)
                        );
                        GL11.glDisable(2884);
                        GL11.glPointSize(0.025F);
                        double trans = 0.0;

                        for (int indexStep = 0; indexStep < steps; indexStep++) {
                           float ciclePC = (float)indexStep / (float)steps;
                           int color = ColorUtils.swapAlpha(
                              markCol,
                              (float)alphaTrace
                                 * dstAPC
                                 * animDelta
                                 * (float)MathUtils.easeInOutQuadWave((double)(1.0F - ciclePC * (1.0F - 1.0F / (float)steps)))
                           );
                           float range = (float)MathUtils.lerp(statRange, maxRange, (double)(animDelta * ciclePC));
                           GL11.glTranslated(0.0, trans += MathUtils.easeInCircle((double)ciclePC) * (double)range / 8.0 / (double)((float)steps), 0.0);
                           this.drawSphere3dPoints(point.x, point.y, point.z, range, shapes, color);
                        }

                        GL11.glTranslated(0.0, -trans, 0.0);
                        GL11.glTranslated(point.x, point.y, point.z);
                        GL11.glRotated(rotate, 0.0, -1.0, 0.0);
                        GL11.glTranslated(-point.x, -point.y, -point.z);
                     }
                  }
               }
            }

            GL11.glTranslated(glX, glY, glZ);
            GL11.glLineWidth(1.0F);
            GL11.glShadeModel(7424);
            GL11.glEnable(3553);
            GL11.glDepthMask(true);
            GL11.glEnable(2929);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            GlStateManager.resetColor();
            GL11.glEnable(2884);
            GL11.glAlphaFunc(516, 0.1F);
            GL11.glPointSize(1.0F);
            GL11.glPopMatrix();
         }
      }
   }

   void renderVoid2d() {
      this.renderPoints2d();
   }

   void renderVoid3d() {
      this.renderPoints3d();
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
}
