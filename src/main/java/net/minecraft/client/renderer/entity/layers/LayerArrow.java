package net.minecraft.client.renderer.entity.layers;

import java.util.Random;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.util.math.MathHelper;
import ru.govno.client.module.modules.NoRender;

public class LayerArrow implements LayerRenderer<EntityLivingBase> {
   private final RenderLivingBase<?> renderer;

   public LayerArrow(RenderLivingBase<?> rendererIn) {
      this.renderer = rendererIn;
   }

   @Override
   public void doRenderLayer(
      EntityLivingBase entitylivingbaseIn,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      float scale
   ) {
      if (NoRender.get == null || !NoRender.get.actived || !NoRender.get.ArrowLayers.getBool()) {
         int i = entitylivingbaseIn.getArrowCountInEntity();
         if (i > 0) {
            Entity entity = new EntityTippedArrow(entitylivingbaseIn.world, entitylivingbaseIn.posX, entitylivingbaseIn.posY, entitylivingbaseIn.posZ);
            Random random = new Random((long)entitylivingbaseIn.getEntityId());
            RenderHelper.disableStandardItemLighting();

            for (int j = 0; j < i; j++) {
               GlStateManager.pushMatrix();
               ModelRenderer modelrenderer = this.renderer.getMainModel().getRandomModelBox(random);
               modelrenderer.postRender(0.0625F);
               float f = random.nextFloat();
               float f1 = random.nextFloat();
               float f2 = random.nextFloat();
               f = f * 2.0F - 1.0F;
               f1 = f1 * 2.0F - 1.0F;
               f2 = f2 * 2.0F - 1.0F;
               f *= -1.0F;
               f1 *= -1.0F;
               f2 *= -1.0F;
               float f6 = MathHelper.sqrt(f * f + f2 * f2);
               entity.rotationYaw = (float)(Math.atan2((double)f, (double)f2) * (180.0 / Math.PI));
               entity.rotationPitch = (float)(Math.atan2((double)f1, (double)f6) * (180.0 / Math.PI));
               entity.prevRotationYaw = entity.rotationYaw;
               entity.prevRotationPitch = entity.rotationPitch;
               double d0 = 0.0;
               double d1 = 0.0;
               double d2 = 0.0;
               this.renderer.getRenderManager().doRenderEntity(entity, 0.0, 0.0, 0.0, 0.0F, partialTicks, false);
               GlStateManager.popMatrix();
            }

            RenderHelper.enableStandardItemLighting();
         }
      }
   }

   @Override
   public boolean shouldCombineTextures() {
      return false;
   }
}
