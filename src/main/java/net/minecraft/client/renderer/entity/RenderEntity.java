package net.minecraft.client.renderer.entity;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import ru.govno.client.module.modules.DistantAlpha;

public class RenderEntity extends Render<Entity> {
   public RenderEntity(RenderManager renderManagerIn) {
      super(renderManagerIn);
   }

   @Override
   public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
      DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)0);
      renderOffsetAABB(entity.getEntityBoundingBoxCL(), x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ);
      DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)1);
      super.doRender(entity, x, y, z, entityYaw, partialTicks);
      DistantAlpha.get.onRenderEntity(entity, x, y, z, entityYaw, partialTicks, (byte)2);
   }

   @Nullable
   @Override
   public ResourceLocation getEntityTexture(Entity entity) {
      return null;
   }
}
