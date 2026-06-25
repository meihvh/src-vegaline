package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelArmorStand;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderArmorStand;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityArmorStand;

public class ModelAdapterArmorStand extends ModelAdapter {
   public ModelAdapterArmorStand() {
      super(EntityArmorStand.class, "armor_stand", 0.7F);
   }

   @Override
   public ModelBase makeModel() {
      return new ModelArmorStand();
   }

   @Override
   public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
      if (!(model instanceof ModelArmorStand modelarmorstand)) {
         return null;
      } else if (modelPart.equals("right")) {
         return modelarmorstand.standRightSide;
      } else if (modelPart.equals("left")) {
         return modelarmorstand.standLeftSide;
      } else if (modelPart.equals("waist")) {
         return modelarmorstand.standWaist;
      } else {
         return modelPart.equals("base") ? modelarmorstand.standBase : null;
      }
   }

   @Override
   public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
      RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
      RenderArmorStand renderarmorstand = new RenderArmorStand(rendermanager);
      renderarmorstand.mainModel = modelBase;
      renderarmorstand.shadowSize = shadowSize;
      return renderarmorstand;
   }
}
