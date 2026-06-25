package ru.govno.client.utils.URender.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.RenderUtils;
import ru.govno.client.utils.Render.StencilUtil;

public class NoiseAnimation {
   private static final ResourceLocation noise = new ResourceLocation("vegaline/ui/noises/noise.png");
   private final AnimationUtils noiseAnimation = new AnimationUtils(1.0F, 1.0F, 0.0F);
   private float noiseProgress;

   public void update(float animationSpeed, boolean toShow) {
      this.noiseAnimation.to = toShow ? 0.0F : 1.0F;
      if (this.noiseAnimation.to == 0.0F && this.noiseAnimation.anim > 0.0F || this.noiseAnimation.to == 1.0F && this.noiseAnimation.anim < 1.0F) {
         this.noiseAnimation.getAnim();
      }

      if (this.noiseAnimation.to == 1.0F && this.noiseAnimation.anim > 0.9980392F) {
         this.noiseAnimation.setAnim(1.0F);
      } else if (this.noiseAnimation.to == 0.0F && this.noiseAnimation.anim < 0.003921569F) {
         this.noiseAnimation.setAnim(0.0F);
      }

      this.noiseAnimation.speed = animationSpeed;
      this.noiseProgress = this.noiseAnimation.anim;
   }

   private void quad(BufferBuilder buffer, float x, float y, float x2, float y2) {
      buffer.pos((double)x, (double)y).tex(0.0, 0.0).endVertex();
      buffer.pos((double)x2, (double)y).tex(1.0, 0.0).endVertex();
      buffer.pos((double)x2, (double)y2).tex(1.0, 1.0).endVertex();
      buffer.pos((double)x, (double)y2).tex(0.0, 1.0).endVertex();
   }

   public void setNoisePC(float noisePC) {
      this.noiseProgress = noisePC;
   }

   public void insertRender2D(Runnable drawable, ScaledResolution sr, int reduction) {
      if (this.noiseProgress != 1.0F) {
         if (this.noiseProgress == 0.0F) {
            drawable.run();
         } else {
            Runnable stencilShape = () -> {
               Minecraft.getMinecraft().getTextureManager().bindTexture(noise);
               Tessellator tessellator = Tessellator.getInstance();
               BufferBuilder buffer = tessellator.getBuffer();
               float w = (float)(sr.getScaledWidth() * reduction);
               float h = (float)(sr.getScaledHeight() * reduction);
               int densityIterations = 3;
               h /= (float)densityIterations;
               w /= (float)densityIterations;
               w *= 1.7777778F * (h / w);
               buffer.begin(7, DefaultVertexFormats.POSITION_TEX);

               for (int x = 0; x < densityIterations; x++) {
                  for (int y = 0; y < densityIterations; y++) {
                     this.quad(buffer, w * (float)x, h * (float)y, w * (float)x + w, h * (float)y + h);
                  }
               }

               RenderUtils.glRenderStart();
               GL11.glDisable(2929);
               GL11.glDepthRange(0.0, 0.01);
               RenderUtils.resetBlender();
               RenderUtils.resetColor();
               GL11.glEnable(3042);
               GL11.glBlendFunc(770, 771);
               GL11.glDisable(2896);
               GL11.glEnable(3553);
               GL11.glBlendFunc(770, 32772);
               GL11.glTexParameteri(3553, 10240, 9729);
               GL11.glEnable(3008);
               float anim = 1.0F - this.noiseProgress;
               anim *= anim * anim;
               anim = MathUtils.clamp(anim, 0.0F, 1.0F);
               GL11.glAlphaFunc(516, anim);
               tessellator.draw();
               GL11.glAlphaFunc(516, 0.1F);
               GL11.glTexParameteri(3553, 10240, 9728);
               GL11.glBlendFunc(770, 771);
               GL11.glEnable(2929);
               GL11.glDepthRange(0.0, 1.0);
               RenderUtils.glRenderStop();
            };
            StencilUtil.renderInStencil(stencilShape, drawable, 0);
         }
      }
   }

   public boolean hasFinished() {
      return this.noiseProgress == 0.0F || this.noiseProgress == 1.0F;
   }

   public float getNoiseProgress() {
      return this.noiseProgress;
   }
}
