package ru.govno.client.utils.Voronoi;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import ru.govno.client.utils.Render.RenderUtils;

public class VoronoiOfCopyRenderTemp {
   private final UVoronoiIntegration uVoronoiIntegration;
   private Framebuffer memory = null;
   private int captureW;
   private int captureH;
   private boolean antialiasingForDraw;
   private boolean blurTexDrawForDraw;

   public VoronoiOfCopyRenderTemp(float x, float y, float x2, float y2, int countOfPoints, boolean createInitThread) {
      this.uVoronoiIntegration = new UVoronoiIntegration(new VoronoiOfQuad(x, y, x2, y2, countOfPoints, createInitThread));
   }

   public VoronoiOfCopyRenderTemp(float x, float y, float x2, float y2, List<VoronoiOfQuad.Vec2f> points, boolean createInitThread) {
      this.uVoronoiIntegration = new UVoronoiIntegration(new VoronoiOfQuad(x, y, x2, y2, points, createInitThread));
   }

   public UVoronoiIntegration genFromCaptureRender2d(
      float inX,
      float inY,
      float inX2,
      float inY2,
      Runnable renderInCoordsZoneForCapture2d,
      int countOfPoints,
      boolean createInitThread,
      int repeatsCaptureLayers
   ) {
      int scaleIntFactor = ScaledResolution.getScaleFactor();
      this.captureW = (int)(Math.ceil((double)(inX2 - inX)) * (double)scaleIntFactor);
      this.captureH = (int)(Math.ceil((double)(inY2 - inY)) * (double)scaleIntFactor);
      if (this.memory == null || this.memory.framebufferWidth != this.captureW || this.memory.framebufferHeight != this.captureH) {
         this.memory = new Framebuffer(this.captureW, this.captureH, false);
         this.memory.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
      }

      this.prepareMatrixForBindCapture(() -> {
         this.memory.framebufferClearNoViewPortSet();
         this.memory.bindFramebuffer(false);
         this.memory.setFramebufferFilter(this.blurTexDrawForDraw ? 9729 : 9728);
         this.prepareMatrixForCapture(inX, inY, () -> {
            for (int counter = 0; counter < repeatsCaptureLayers; counter++) {
               renderInCoordsZoneForCapture2d.run();
            }
         });
         this.memory.unbindFramebuffer();
      });
      return this.uVoronoiIntegration.setVoronoi(new VoronoiOfQuad(inX, inY, inX2, inY2, countOfPoints, createInitThread));
   }

   public UVoronoiIntegration genFromCaptureRender2d(
      float inX,
      float inY,
      float inX2,
      float inY2,
      Runnable renderInCoordsZoneForCapture2d,
      List<VoronoiOfQuad.Vec2f> points,
      boolean createInitThread,
      int repeatsCaptureLayers
   ) {
      int scaleIntFactor = ScaledResolution.getScaleFactor();
      this.captureW = (int)(Math.ceil((double)(inX2 - inX)) * (double)scaleIntFactor);
      this.captureH = (int)(Math.ceil((double)(inY2 - inY)) * (double)scaleIntFactor);
      if (this.memory == null || this.memory.framebufferWidth != this.captureW || this.memory.framebufferHeight != this.captureH || this.memory.useDepth) {
         this.memory = new Framebuffer(this.captureW, this.captureH, false);
         this.memory.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
      }

      this.prepareMatrixForBindCapture(() -> {
         this.memory.framebufferClearNoViewPortSet();
         this.memory.bindFramebuffer(false);
         this.memory.setFramebufferFilter(this.blurTexDrawForDraw ? 9729 : 9728);
         this.prepareMatrixForCapture(inX, inY, () -> {
            for (int counter = 0; counter < repeatsCaptureLayers; counter++) {
               renderInCoordsZoneForCapture2d.run();
            }
         });
         this.memory.unbindFramebuffer();
      });
      return this.uVoronoiIntegration.setVoronoi(new VoronoiOfQuad(inX, inY, inX2, inY2, points, createInitThread));
   }

   private void prepareMatrixForBindCapture(Runnable bindingDrawsProcess) {
      GL11.glEnable(36281);
      GL11.glAlphaFunc(516, 0.0F);
      GL11.glDisable(3008);
      bindingDrawsProcess.run();
      GL11.glEnable(3008);
      GL11.glAlphaFunc(516, 0.1F);
      GL11.glDisable(36281);
      Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
   }

   private void prepareMatrixForCapture(float renderObjX, float renderObjY, Runnable render) {
      ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
      float fcX = (float)this.captureW / (float)sr.getScaledWidth();
      float fcY = (float)this.captureH / (float)sr.getScaledHeight();
      float factorDistortion = (float)ScaledResolution.getScaleFactor();
      GlStateManager.pushMatrix();
      GL11.glScalef(1.0F / fcX * factorDistortion, 1.0F / fcY * factorDistortion, 1.0F);
      GlStateManager.translate(-renderObjX, -renderObjY, 0.0F);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDepthMask(false);
      render.run();
      GL11.glDepthMask(true);
      GL11.glEnable(2884);
      GL11.glEnable(2929);
      GlStateManager.popMatrix();
   }

   public int getGLTextureId() {
      return this.memory == null ? 0 : this.memory.framebufferTexture;
   }

   public void prepareRenderVoronoi(Runnable renderVoronoi, boolean polyVLAA, boolean lineVLAA, boolean pointVLAA, boolean blurTex) {
      RenderUtils.glRenderStart();
      this.memory.bindFramebufferTexture();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3008);
      GL11.glEnable(3553);
      if (polyVLAA) {
         GL11.glEnable(2881);
      }

      if (lineVLAA) {
         GL11.glEnable(2848);
      }

      if (pointVLAA) {
         GL11.glEnable(2832);
      } else {
         GL11.glDisable(2832);
      }

      if (blurTex) {
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
      }

      renderVoronoi.run();
      if (blurTex) {
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
      }

      if (!pointVLAA) {
         GL11.glEnable(2848);
      }

      if (lineVLAA) {
         GL11.glDisable(2848);
      }

      if (polyVLAA) {
         GL11.glDisable(2881);
      }

      GL11.glDisable(3553);
      GL11.glEnable(3008);
      this.memory.unbindFramebufferTexture();
      RenderUtils.glRenderStop();
   }

   public VoronoiOfCopyRenderTemp setupVLAARender(boolean antialiasing, boolean texFilter) {
      this.antialiasingForDraw = antialiasing;
      this.blurTexDrawForDraw = texFilter;
      return this;
   }

   public void renderCapturedSegments2d(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
      if (this.memory != null && this.memory.framebufferTexture != 0) {
         this.prepareRenderVoronoi(
            () -> this.uVoronoiIntegration.renderBindTextureSegmentsRevUV(temporalMode, begin, trans, rot, aPC, color, tryDraws),
            this.antialiasingForDraw && (begin == 9 || begin == 4 || begin == 6 || begin == 5 || begin == 7 || begin == 8),
            this.antialiasingForDraw && (begin == 1 || begin == 3 || begin == 2),
            this.antialiasingForDraw && begin == 0,
            this.blurTexDrawForDraw
         );
      }
   }

   public void renderCapturedSegments2d(
      boolean temporalMode, int begin, float trans, float rot, float aPC, int color1, int color2, int color3, int color4, int tryDraws
   ) {
      if (this.memory != null && this.memory.framebufferTexture != 0) {
         this.prepareRenderVoronoi(
            () -> this.uVoronoiIntegration.renderBindTextureSegmentsRevUV(temporalMode, begin, trans, rot, aPC, color1, color2, color3, color4, tryDraws),
            this.antialiasingForDraw && (begin == 9 || begin == 4 || begin == 6 || begin == 5 || begin == 7 || begin == 8),
            this.antialiasingForDraw && (begin == 1 || begin == 3 || begin == 2),
            this.antialiasingForDraw && begin == 0,
            this.blurTexDrawForDraw
         );
      }
   }
}
