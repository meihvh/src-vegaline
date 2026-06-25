package net.minecraft.client.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import ru.govno.client.module.modules.ESP;
import ru.govno.client.utils.Math.MathUtils;

public class ModelEnderCrystal extends ModelBase {
   private final ModelRenderer cube;
   private final ModelRenderer glass = new ModelRenderer(this, "glass");
   private ModelRenderer base;
   public static boolean cancelBase;
   public static boolean canDeformate = true;

   public ModelEnderCrystal(float p_i1170_1_, boolean renderBase) {
      this.glass.setTextureOffset(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
      this.cube = new ModelRenderer(this, "cube");
      this.cube.setTextureOffset(32, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
      if (renderBase) {
         this.base = new ModelRenderer(this, "base");
         this.base.setTextureOffset(0, 16).addBox(-6.0F, 0.0F, -6.0F, 12, 4, 12);
      }
   }

   @Override
   public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      boolean improve = ESP.get.crystalImprove();
      if (improve) {
         GlStateManager.pushMatrix();
         GlStateManager.scale(2.0, 0.002, 2.0);
         if (!cancelBase) {
            this.glass.render(scale);
         }

         float arks = limbSwingAmount % 360.0F / 360.0F;
         arks *= 1.15F;
         arks = arks > 1.0F ? 1.0F : (arks < 0.0F ? 0.0F : arks);
         arks = (float)((double)arks * MathUtils.easeInOutQuadWave((double)arks));
         arks = ((double)arks > 0.5 ? 1.0F - arks : arks) * 2.0F;
         float var12 = (float)MathUtils.easeInOutQuad((double)arks) * 180.0F;
         float axis = limbSwingAmount % 20.0F / 20.0F;
         limbSwingAmount = --var12 + (float)MathUtils.easeInOutQuadWave((double)axis) * 2.0F;
         GlStateManager.rotate(limbSwingAmount * 1.5F, 0.0F, 1.0F, 0.0F);
         GlStateManager.scale(0.5, 1.0, 0.5);
         if (!cancelBase) {
            this.glass.render(scale);
         }

         GlStateManager.rotate(-limbSwingAmount * 3.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(0.0, 0.002, 0.0);
         if (!cancelBase) {
            this.glass.render(scale);
         }

         GlStateManager.scale(2.0F, 1.0F, 2.0F);
         GlStateManager.rotate(limbSwingAmount * 1.5F, 0.0F, 1.0F, 0.0F);
         GlStateManager.scale(1.0F, 1000.0F, 1.0F);
         GlStateManager.translate(0.0, 0.35, 0.0);
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.translate(0.0, (double)ageInTicks - 0.15, 0.0);
         GlStateManager.rotate(limbSwingAmount * 4.0F, 0.0F, 1.0F, 1.0F);
         this.cube.render(scale);
         this.glass.render(scale);
         GlStateManager.scale(1.2, 1.2, 1.2);
         GlStateManager.rotate(-limbSwingAmount * 6.0F, 0.0F, 1.0F, 1.0F);
         this.glass.render(scale);
         GlStateManager.scale(1.2, 1.2, 1.2);
         GlStateManager.rotate(-limbSwingAmount * 3.0F, 1.0F, 1.0F, 0.0F);
         this.glass.render(scale);
         GlStateManager.popMatrix();
      } else {
         GlStateManager.pushMatrix();
         GlStateManager.scale(2.0F, 2.0F, 2.0F);
         GlStateManager.translate(0.0F, -0.5F, 0.0F);
         if (this.base != null) {
            this.base.render(scale);
         }

         GlStateManager.rotate(limbSwingAmount, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(0.0F, 0.8F + ageInTicks, 0.0F);
         GlStateManager.rotate(60.0F, 0.7071F, 0.0F, 0.7071F);
         this.glass.render(scale);
         float f = 0.875F;
         GlStateManager.scale(0.875F, 0.875F, 0.875F);
         GlStateManager.rotate(60.0F, 0.7071F, 0.0F, 0.7071F);
         GlStateManager.rotate(limbSwingAmount, 0.0F, 1.0F, 0.0F);
         this.glass.render(scale);
         GlStateManager.scale(0.875F, 0.875F, 0.875F);
         GlStateManager.rotate(60.0F, 0.7071F, 0.0F, 0.7071F);
         GlStateManager.rotate(limbSwingAmount, 0.0F, 1.0F, 0.0F);
         this.cube.render(scale);
         GlStateManager.popMatrix();
      }
   }
}
