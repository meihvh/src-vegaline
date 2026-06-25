package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityBoat;
import optifine.Config;
import optifine.Reflector;

public class ModelAdapterBoat extends ModelAdapter {
   public ModelAdapterBoat() {
      super(EntityBoat.class, "boat", 0.5F);
   }

   @Override
   public ModelBase makeModel() {
      return new ModelBoat();
   }

   @Override
   public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
      if (!(model instanceof ModelBoat modelboat)) {
         return null;
      } else if (modelPart.equals("bottom")) {
         return modelboat.boatSides[0];
      } else if (modelPart.equals("back")) {
         return modelboat.boatSides[1];
      } else if (modelPart.equals("front")) {
         return modelboat.boatSides[2];
      } else if (modelPart.equals("right")) {
         return modelboat.boatSides[3];
      } else if (modelPart.equals("left")) {
         return modelboat.boatSides[4];
      } else if (modelPart.equals("paddle_left")) {
         return modelboat.paddles[0];
      } else if (modelPart.equals("paddle_right")) {
         return modelboat.paddles[1];
      } else {
         return modelPart.equals("bottom_no_water") ? modelboat.noWater : null;
      }
   }

   @Override
   public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
      RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
      RenderBoat renderboat = new RenderBoat(rendermanager);
      if (!Reflector.RenderBoat_modelBoat.exists()) {
         Config.warn("Field not found: RenderBoat.modelBoat");
         return null;
      } else {
         Reflector.setFieldValue(renderboat, Reflector.RenderBoat_modelBoat, modelBase);
         renderboat.shadowSize = shadowSize;
         return renderboat;
      }
   }
}
