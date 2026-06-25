package ru.govno.client.utils.Render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import ru.govno.client.utils.Math.TimerHelper;

public class GaussianBlur {
   static Minecraft mc = Minecraft.getMinecraft();
   public static ShaderUtility blurShader = new ShaderUtility("vegaline/system/shaders/gaussian.frag");
   public static Framebuffer framebuffer = new Framebuffer(1, 1, false);
   public static Framebuffer framebuffer1 = ShaderUtility.createFrameBuffer(new Framebuffer(1, 1, false));
   private static final Map<String, TimerHelper> delayUpdateFrameOBJS = new HashMap<>();

   public static void setupUniforms(float dir1, float dir2, float radius) {
      blurShader.setUniformi("textureIn", 0);
      blurShader.setUniformf("texelSize", 1.0F / (float)mc.displayWidth, 1.0F / (float)mc.displayHeight);
      blurShader.setUniformf("direction", dir1, dir2);
      blurShader.setUniformf("radius", radius);
      FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(256);

      for (int i = 0; (float)i <= radius; i++) {
         weightBuffer.put(ShaderUtility.calculateGaussianValue((float)i, radius / 2.0F));
      }

      weightBuffer.rewind();
      GL20.glUniform1(blurShader.getUniform("weights"), weightBuffer);
   }

   public static void drawBlur(float radius, Runnable data, String uniqueSourceObjectString) {
      StencilUtil.initStencilToWrite();
      data.run();
      StencilUtil.readStencilBuffer(1);
      renderBlur(radius, uniqueSourceObjectString);
      StencilUtil.uninitStencilBuffer();
   }

   public static void update(Framebuffer framebuffer) {
      if (framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight) {
         framebuffer.createBindFramebuffer(mc.displayWidth, mc.displayHeight);
      }
   }

   public static void renderBlur(float radius, List<Runnable> run, String uniqueSourceObjectString) {
      update(framebuffer1);
      framebuffer1.framebufferClear();
      framebuffer1.bindFramebuffer(true);
      run.forEach(Runnable::run);
      framebuffer1.unbindFramebuffer();
      mc.getFramebuffer().bindFramebuffer(true);
      if (framebuffer1 != null) {
         GL11.glPushMatrix();
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.0F);
         GlStateManager.enableBlend();
         OpenGlHelper.glBlendFunc(770, 771, 1, 0);
         mc.getFramebuffer().bindFramebuffer(true);
         ShaderUtility.bindTexture(framebuffer1.framebufferTexture);
         drawBlur(radius, ShaderUtility::drawQuads, uniqueSourceObjectString);
         mc.getFramebuffer().bindFramebuffer(false);
         GlStateManager.disableAlpha();
         GL11.glPopMatrix();
      }
   }

   private static float[] getUVQuad(float x, float y, float x2, float y2, ScaledResolution sr) {
      float w = (float)sr.getScaledWidth();
      float h = (float)sr.getScaledHeight();
      return new float[]{x / w, fixV(y / h), x2 / w, fixV(y2 / h)};
   }

   private static float[] getUVVertex(float x, float y, ScaledResolution sr) {
      float w = (float)sr.getScaledWidth();
      float h = (float)sr.getScaledHeight();
      return new float[]{Math.max(Math.min(x / w, 1.0F), 0.0F), Math.max(Math.min(fixV(y / h), 1.0F), 0.0F)};
   }

   private static float fixV(float vSrc) {
      vSrc = Math.max(vSrc, 1.0E-6F);
      vSrc = 1.0F - vSrc;
      vSrc %= 1.0F;
      return Math.min(vSrc, 0.999999F);
   }

   private static void bufferDoTexVertexOnShaderMask(BufferBuilder buffer, float x, float y, ScaledResolution sr) {
      float[] uv = getUVVertex(x, y, sr);
      buffer.pos((double)x, (double)y).tex((double)uv[0], (double)uv[1]).color(ColorUtils.getColor(205)).endVertex();
   }

   private static void drawRound01Compact(float x, float y, float x2, float y2, float round) {
      if (!mc.gameSettings.ofFastRender) {
         ScaledResolution sr = new ScaledResolution(mc);
         List<Vec2f> vertices = new ArrayList<>();
         int degreesStep = 9;

         for (int deg = -90; deg < 0; deg += degreesStep) {
            float rad;
            vertices.add(new Vec2f(x + round + MathHelper.sin(rad = MathHelper.toRadians((float)deg)) * round, y + round - MathHelper.cos(rad) * round));
         }

         for (int deg = 0; deg < 90; deg += degreesStep) {
            float rad;
            vertices.add(new Vec2f(x2 - round + MathHelper.sin(rad = MathHelper.toRadians((float)deg)) * round, y + round - MathHelper.cos(rad) * round));
         }

         for (int deg = 90; deg < 180; deg += degreesStep) {
            float rad;
            vertices.add(new Vec2f(x2 - round + MathHelper.sin(rad = MathHelper.toRadians((float)deg)) * round, y2 - round - MathHelper.cos(rad) * round));
         }

         for (int deg = 180; deg < 270; deg += degreesStep) {
            float rad;
            vertices.add(new Vec2f(x + round + MathHelper.sin(rad = MathHelper.toRadians((float)deg)) * round, y2 - round - MathHelper.cos(rad) * round));
         }

         if (!vertices.isEmpty()) {
            Collections.reverse(vertices);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(9, DefaultVertexFormats.POSITION_TEX_COLOR);

            for (Vec2f vertex : vertices) {
               bufferDoTexVertexOnShaderMask(bufferbuilder, vertex.x, vertex.y, sr);
            }

            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9729);
            tessellator.draw();
            GL11.glTexParameteri(3553, 10241, 9728);
            GL11.glTexParameteri(3553, 10240, 9728);
         }
      }
   }

   public static void drawRoundedBlurNotStencil(float radius, float x, float y, float x2, float y2, float round, String uniqueSourceObjectString) {
      boolean reached = false;
      TimerHelper findTimer = autoFindTimer(uniqueSourceObjectString);
      if (findTimer == null) {
         reached = true;
      } else if (reached = findTimer.hasReached((double)(1000.0F / (float)fpsCapUpdate()))) {
         findTimer.reset();
         reached = true;
      }

      mc.getFramebuffer().bindFramebuffer(true);
      GL11.glDisable(2929);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      float off = 1.3333333F;
      if (reached) {
         update(framebuffer);
         framebuffer.framebufferClear();
         framebuffer.bindFramebuffer(true);
         blurShader.init();
         setupUniforms(off, 0.0F, radius);
         GL11.glBindTexture(3553, mc.getFramebuffer().framebufferTexture);
         ShaderUtility.drawQuads();
         framebuffer.unbindFramebuffer();
         blurShader.unload();
      }

      mc.getFramebuffer().bindFramebuffer(true);
      blurShader.init();
      if (reached) {
         setupUniforms(0.0F, off, radius);
      }

      GL11.glBindTexture(3553, framebuffer.framebufferTexture);
      drawRound01Compact(x, y, x2, y2, round);
      blurShader.unload();
      GL11.glBindTexture(3553, 0);
      GL11.glEnable(2929);
   }

   public static int fpsCapUpdate() {
      return 1200;
   }

   private static TimerHelper autoFindTimer(String objIn) {
      if (objIn.contains("INFINITY")) {
         return null;
      } else {
         TimerHelper timerFind = delayUpdateFrameOBJS.get(objIn);
         return timerFind == null ? delayUpdateFrameOBJS.put(objIn, TimerHelper.TimerHelperReseted()) : timerFind;
      }
   }

   public static void renderBlur(float radius, String uniqueSourceObjectString) {
      boolean reached = false;
      TimerHelper findTimer = autoFindTimer(uniqueSourceObjectString);
      if (findTimer == null) {
         reached = true;
      } else if (reached = findTimer.hasReached((double)(1000.0F / (float)fpsCapUpdate()))) {
         findTimer.reset();
         reached = true;
      }

      mc.getFramebuffer().bindFramebuffer(true);
      GlStateManager.enableBlend();
      GL11.glDisable(2929);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      OpenGlHelper.glBlendFunc(770, 771, 1, 0);
      if (reached) {
         update(framebuffer);
         framebuffer.framebufferClear();
         framebuffer.bindFramebuffer(true);
         blurShader.init();
         setupUniforms(1.0F, 0.0F, radius);
         ShaderUtility.bindTexture(mc.getFramebuffer().framebufferTexture);
         ShaderUtility.drawQuads();
         blurShader.unload();
         framebuffer.unbindFramebuffer();
      }

      mc.getFramebuffer().bindFramebuffer(true);
      blurShader.init();
      if (reached) {
         setupUniforms(0.0F, 1.0F, radius);
      }

      ShaderUtility.bindTexture(framebuffer.framebufferTexture);
      ShaderUtility.drawQuads();
      blurShader.unload();
      GlStateManager.resetColor();
      GlStateManager.bindTexture(0);
      GL11.glEnable(2929);
   }
}
