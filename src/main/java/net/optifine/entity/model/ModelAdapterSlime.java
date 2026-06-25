package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSlime;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSlime;
import net.minecraft.entity.monster.EntitySlime;
import optifine.Reflector;

public class ModelAdapterSlime extends ModelAdapter {
   public ModelAdapterSlime() {
      super(EntitySlime.class, "slime", 0.25F);
   }

   @Override
   public ModelBase makeModel() {
      return new ModelSlime(16);
   }

   @Override
   public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
      if (!(model instanceof ModelSlime modelslime)) {
         return null;
      } else if (modelPart.equals("body")) {
         return (ModelRenderer)Reflector.getFieldValue(modelslime, Reflector.ModelSlime_ModelRenderers, 0);
      } else if (modelPart.equals("left_eye")) {
         return (ModelRenderer)Reflector.getFieldValue(modelslime, Reflector.ModelSlime_ModelRenderers, 1);
      } else if (modelPart.equals("right_eye")) {
         return (ModelRenderer)Reflector.getFieldValue(modelslime, Reflector.ModelSlime_ModelRenderers, 2);
      } else {
         return modelPart.equals("mouth") ? (ModelRenderer)Reflector.getFieldValue(modelslime, Reflector.ModelSlime_ModelRenderers, 3) : null;
      }
   }

   @Override
   public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
      RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
      RenderSlime renderslime = new RenderSlime(rendermanager);
      renderslime.mainModel = modelBase;
      renderslime.shadowSize = shadowSize;
      return renderslime;
   }
}
