package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSnowMan;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.entity.monster.EntitySnowman;

public class ModelAdapterSnowman extends ModelAdapter {
   public ModelAdapterSnowman() {
      super(EntitySnowman.class, "snow_golem", 0.5F);
   }

   @Override
   public ModelBase makeModel() {
      return new ModelSnowMan();
   }

   @Override
   public ModelRenderer getModelRenderer(ModelBase model, String modelPart) {
      if (!(model instanceof ModelSnowMan modelsnowman)) {
         return null;
      } else if (modelPart.equals("body")) {
         return modelsnowman.body;
      } else if (modelPart.equals("body_bottom")) {
         return modelsnowman.bottomBody;
      } else if (modelPart.equals("head")) {
         return modelsnowman.head;
      } else if (modelPart.equals("left_hand")) {
         return modelsnowman.leftHand;
      } else {
         return modelPart.equals("right_hand") ? modelsnowman.rightHand : null;
      }
   }

   @Override
   public IEntityRenderer makeEntityRender(ModelBase modelBase, float shadowSize) {
      RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
      RenderSnowMan rendersnowman = new RenderSnowMan(rendermanager);
      rendersnowman.mainModel = modelBase;
      rendersnowman.shadowSize = shadowSize;
      return rendersnowman;
   }
}
