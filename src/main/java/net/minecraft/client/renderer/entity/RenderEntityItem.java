package net.minecraft.client.renderer.entity;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.NoRender;
import ru.govno.client.module.modules.WorldRender;
import ru.govno.client.utils.Command.impl.Panic;

public class RenderEntityItem extends Render<EntityItem> {
   private final RenderItem itemRenderer;
   private final Random random = new Random();
   private final Random randomm = new Random();
   public static Minecraft mc = Minecraft.getMinecraft();
   public final RenderItem renderItem = mc.getRenderItem();
   long tick;
   double rotation;

   public RenderEntityItem(RenderManager renderManagerIn, RenderItem p_i46167_2_) {
      super(renderManagerIn);
      this.itemRenderer = p_i46167_2_;
      this.shadowSize = 0.15F;
      this.shadowOpaque = 0.75F;
   }

   private int transformModelCount(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_) {
      ItemStack itemstack = itemIn.getItem();
      Item item = itemstack.getItem();
      if (item == null) {
         return 0;
      } else {
         boolean flag = p_177077_9_.isGui3d();
         int i = this.getModelCount(itemstack);
         float f = 0.25F;
         boolean ph = this.isPhisics();
         float f1 = ph ? 0.0F : MathHelper.sin(((float)itemIn.getAge() + p_177077_8_) / 10.0F + itemIn.hoverStart) * 0.1F + 0.1F;
         float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;
         GlStateManager.translate((float)p_177077_2_, (float)p_177077_4_ + f1 + 0.25F * f2, (float)p_177077_6_);
         if (!ph && (flag || this.renderManager.options != null)) {
            float f3 = (((float)itemIn.getAge() + p_177077_8_) / 20.0F + itemIn.hoverStart) * (180.0F / (float)Math.PI);
            GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
         }

         this.shadowSize = ph ? 0.0F : 0.15F;
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         return i;
      }
   }

   private int getModelCount(ItemStack stack) {
      int i = 1;
      if (stack.getCount2() > 48) {
         i = 5;
      } else if (stack.getCount2() > 32) {
         i = 4;
      } else if (stack.getCount2() > 16) {
         i = 3;
      } else if (stack.getCount2() > 1) {
         i = 2;
      }

      return i;
   }

   public static boolean shouldSpreadItems() {
      return true;
   }

   private final boolean isPhisics() {
      return WorldRender.get.isItemPhysics && !Panic.stop;
   }

   public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
      Entity camera;
      if (Panic.stop
         || NoRender.get == null
         || !NoRender.get.isActived()
         || !NoRender.get.ItemEntity.getBool()
         || entity.ticksExisted <= 30
         || (camera = mc.getRenderViewEntity()) == null
         || !((double)entity.getDistanceToEntity(camera) > 8.0)) {
         if (!this.isPhisics()) {
            ItemStack itemstack = entity.getItem();
            int i = itemstack.isEmpty() ? 187 : Item.getIdFromItem(itemstack.getItem()) + itemstack.getMetadata();
            this.random.setSeed((long)i);
            boolean flag = false;
            if (this.bindEntityTexture(entity)) {
               this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).setBlurMipmap(false, false);
               flag = true;
            }

            GlStateManager.enableRescaleNormal();
            GL11.glEnable(2929);
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            GlStateManager.pushMatrix();
            IBakedModel ibakedmodel = this.itemRenderer.getItemModelWithOverrides(itemstack, entity.world, null);
            int j = this.transformModelCount(entity, x, y, z, partialTicks, ibakedmodel);
            float f = ibakedmodel.getItemCameraTransforms().ground.scale.x;
            float f1 = ibakedmodel.getItemCameraTransforms().ground.scale.y;
            float f2 = ibakedmodel.getItemCameraTransforms().ground.scale.z;
            boolean flag1 = ibakedmodel.isGui3d();
            if (!flag1) {
               float f3 = -0.0F * (float)(j - 1) * 0.5F * f;
               float f4 = -0.0F * (float)(j - 1) * 0.5F * f1;
               float f5 = -0.09375F * (float)(j - 1) * 0.5F * f2;
               GlStateManager.translate(f3, f4, f5);
            }

            if (this.renderOutlines) {
               GlStateManager.enableColorMaterial();
               if (!EntityItem.tempGlowing) {
                  GlStateManager.enableOutlineMode(this.getTeamColor(entity));
               }
            }

            for (int k = 0; k < j; k++) {
               if (flag1) {
                  GlStateManager.pushMatrix();
                  if (k > 0) {
                     float f7 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                     float f9 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                     float f6 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                     GlStateManager.translate(f7, f9, f6);
                  }

                  ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                  this.itemRenderer.renderItem(itemstack, ibakedmodel);
                  GlStateManager.popMatrix();
               } else {
                  GlStateManager.pushMatrix();
                  if (k > 0) {
                     float f8 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                     float f10 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                     GlStateManager.translate(f8, f10, 0.0F);
                  }

                  ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GROUND);
                  this.itemRenderer.renderItem(itemstack, ibakedmodel);
                  GlStateManager.popMatrix();
                  GlStateManager.translate(0.0F * f, 0.0F * f1, 0.09375F * f2);
               }
            }

            if (this.renderOutlines) {
               GlStateManager.disableOutlineMode();
               GlStateManager.disableColorMaterial();
            }

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            this.bindEntityTexture(entity);
            if (flag) {
               this.renderManager.renderEngine.getTexture(this.getEntityTexture(entity)).restoreLastBlurMipmap();
            }
         } else {
            ItemStack itemstackx = entity.getItem();
            this.rotation = mc.inGameHasFocus ? (double)(System.nanoTime() - this.tick) / 3000000.0 * 0.4 * mc.timer.speed : 0.0;
            if (itemstackx != null) {
               this.randomm.setSeed(187L);
               if (TextureMap.LOCATION_BLOCKS_TEXTURE != null) {
                  mc.getRenderManager().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                  mc.getRenderManager().renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
               }

               boolean flag2 = TextureMap.LOCATION_BLOCKS_TEXTURE != null;
               GlStateManager.enableRescaleNormal();
               GL11.glEnable(2929);
               GlStateManager.alphaFunc(516, 0.1F);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.pushMatrix();
               IBakedModel ibakedmodelx = this.renderItem.getItemModelMesher().getItemModel(itemstackx);
               int kx = this.transformModelCount(entity, x, y, z, partialTicks, ibakedmodelx);
               BlockPos pos = new BlockPos(entity);
               if (entity.rotationPitch > 360.0F) {
                  entity.rotationPitch = 0.0F;
               }

               if (entity != null
                  && !Double.isNaN((double)entity.getAge())
                  && !Double.isNaN((double)entity.getAir())
                  && !Double.isNaN((double)entity.getEntityId())
                  && entity.getPosition() != null
                  && !entity.onGround) {
                  BlockPos posUp = new BlockPos(entity);
                  posUp.add(0, 1, 0);
                  Material m1 = entity.world.getBlockState(posUp).getBlock().getMaterial(null);
                  Material m2 = entity.world.getBlockState(pos).getBlock().getMaterial(null);
                  boolean m3 = entity.isInsideOfMaterial(Material.WATER);
                  boolean m4 = entity.isInWater();
                  entity.rotationPitch = m3 | m1 == Material.WATER | m2 == Material.WATER | m4
                     ? (entity.rotationPitch = entity.rotationPitch + (float)(this.rotation / 4.0))
                     : (entity.rotationPitch = entity.rotationPitch + (float)(this.rotation * 2.0));
               }

               GlStateManager.translate(0.0, entity.fix + (itemstackx.getItem() instanceof ItemBlock ? 0.1 : 0.0), 0.0);
               GL11.glRotatef(entity.rotationYaw, 0.0F, 1.0F, 0.0F);
               GL11.glRotatef(
                  entity.rotationPitch + 90.0F + (float)(!(itemstackx.getItem() instanceof ItemTool) && !(itemstackx.getItem() instanceof ItemSword) ? 0 : 90),
                  1.0F,
                  0.0F,
                  0.0F
               );
               GL11.glRotatef((float)(!(itemstackx.getItem() instanceof ItemSword) ? 0 : 45), 0.0F, 0.0F, 1.0F);

               for (int l = 0; l < kx; l++) {
                  if (ibakedmodelx.isAmbientOcclusion()) {
                     GlStateManager.pushMatrix();
                     GlStateManager.scale(0.3F, 0.3F, 0.3F);
                     this.renderItem.renderItem(itemstackx, ibakedmodelx);
                     GlStateManager.popMatrix();
                  } else {
                     GlStateManager.pushMatrix();
                     GlStateManager.scale(0.6F, 0.6F, 0.6F);
                     if (l > 0 && shouldSpreadItems()) {
                        GlStateManager.translate(0.0F, 0.0F, 0.046875F * (float)l);
                     }

                     this.renderItem.renderItem(itemstackx, ibakedmodelx);
                     if (!shouldSpreadItems()) {
                        GlStateManager.translate(0.0F, 0.0F, 0.046875F);
                     }

                     GlStateManager.popMatrix();
                  }
               }

               GlStateManager.popMatrix();
               GlStateManager.disableRescaleNormal();
               GlStateManager.disableBlend();
               GlStateManager.alphaFunc(516, 0.1F);
               GlStateManager.enableBlend();
               RenderHelper.enableStandardItemLighting();
               mc.getRenderManager().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
               if (flag2) {
                  mc.getRenderManager().renderEngine.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
               }
            }
         }

         super.doRender(entity, x, y, z, entityYaw, partialTicks);
      }
   }

   protected ResourceLocation getEntityTexture(EntityItem entity) {
      return TextureMap.LOCATION_BLOCKS_TEXTURE;
   }
}
