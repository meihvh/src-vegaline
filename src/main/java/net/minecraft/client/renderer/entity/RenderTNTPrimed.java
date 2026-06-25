package net.minecraft.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import ru.govno.client.module.modules.BadTrip;
import ru.govno.client.utils.Math.MathUtils;

public class RenderTNTPrimed extends Render<EntityTNTPrimed> {
   public RenderTNTPrimed(RenderManager renderManagerIn) {
      super(renderManagerIn);
      this.shadowSize = 0.5F;
   }

   public void doRender(EntityTNTPrimed entity, double x, double y, double z, float entityYaw, float partialTicks) {
      BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)x, (float)y + 0.5F, (float)z);
      if ((float)entity.getFuse() - partialTicks + 1.0F < 10.0F) {
         float f = 1.0F - ((float)entity.getFuse() - partialTicks + 1.0F) / 10.0F;
         f = MathHelper.clamp(f, 0.0F, 1.0F);
         f *= f;
         f *= f;
         float f1 = 1.0F + f * 0.3F;
         GlStateManager.scale(f1, f1, f1);
      }

      float f2 = (1.0F - ((float)entity.getFuse() - partialTicks + 1.0F) / 100.0F) * 0.8F;
      this.bindEntityTexture(entity);
      float callNT = entity.getBrightness();
      if (BadTrip.get.isPsichoTnt()) {
         float wobbleMax = 1.3F;
         float tickSpeed = 2.0F + (float)entity.ticksExisted / 2.0F;
         float smT = (float)MathUtils.easeInOutQuadWave((double)(((float)(entity.ticksExisted % (int)tickSpeed) + partialTicks) / tickSpeed));
         float wobble = wobbleMax * smT;
         double jump = MathUtils.easeInOutQuadWave((double)(((float)entity.ticksExisted + partialTicks) % 12.0F / 12.0F));
         GlStateManager.rotate(((float)entity.ticksExisted + partialTicks) * 720.0F / 20.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(0.0, jump, 0.0);
         GlStateManager.scale((double)(1.0F + wobble), 1.0 - jump / 2.0, (double)(1.0F + wobble));
         callNT = 1.0F;
      }

      GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.translate(-0.5F, -0.5F, 0.5F);
      blockrendererdispatcher.renderBlockBrightness(Blocks.TNT.getDefaultState(), callNT);
      GlStateManager.translate(0.0F, 0.0F, 1.0F);
      if (this.renderOutlines) {
         GlStateManager.enableColorMaterial();
         GlStateManager.enableOutlineMode(this.getTeamColor(entity));
         blockrendererdispatcher.renderBlockBrightness(Blocks.TNT.getDefaultState(), 1.0F);
         GlStateManager.disableOutlineMode();
         GlStateManager.disableColorMaterial();
      } else if (entity.getFuse() / 5 % 2 == 0) {
         GlStateManager.disableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.DST_ALPHA);
         GlStateManager.color(1.0F, 1.0F, 1.0F, f2);
         GlStateManager.doPolygonOffset(-3.0F, -3.0F);
         GlStateManager.enablePolygonOffset();
         blockrendererdispatcher.renderBlockBrightness(Blocks.TNT.getDefaultState(), 1.0F);
         GlStateManager.doPolygonOffset(0.0F, 0.0F);
         GlStateManager.disablePolygonOffset();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableBlend();
         GlStateManager.enableLighting();
         GlStateManager.enableTexture2D();
      }

      GlStateManager.popMatrix();
      super.doRender(entity, x, y, z, entityYaw, partialTicks);
   }

   protected ResourceLocation getEntityTexture(EntityTNTPrimed entity) {
      return TextureMap.LOCATION_BLOCKS_TEXTURE;
   }
}
