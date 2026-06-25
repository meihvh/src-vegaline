package ru.govno.client.utils.Render;

import javax.annotation.Nullable;
import javax.vecmath.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityElderGuardian;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityMule;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import ru.govno.client.utils.Math.MathUtils;

public class HeadSkinUVLivingBase {
   private final EntityLivingBase livingBase;
   private int color = -1;

   private HeadSkinUVLivingBase(EntityLivingBase livingBase) {
      this.livingBase = livingBase;
   }

   public static HeadSkinUVLivingBase of(EntityLivingBase livingBase) {
      return new HeadSkinUVLivingBase(livingBase);
   }

   public HeadSkinUVLivingBase setWhite(float alphaPC01, boolean applyHurtRedAnimation) {
      alphaPC01 = Math.min(Math.max(alphaPC01, 0.0F), 1.0F);
      float toRedPC01 = applyHurtRedAnimation && this.livingBase != null
         ? (float)MathUtils.easeInOutQuad(
               (double)(MathUtils.clamp((float)this.livingBase.hurtTime - Minecraft.getMinecraft().getRenderPartialTicks(), 0.0F, 8.0F) / 8.0F)
            )
            / 2.0F
         : 0.0F;
      this.color = ColorUtils.getColor(255, (int)(255.0F - toRedPC01 * 255.0F), (int)(255.0F - toRedPC01 * 255.0F), 255.0F * alphaPC01);
      return this;
   }

   public int getColor() {
      return this.color;
   }

   public Vector4f getRenderQuadFaceUv() {
      Vector4f uv = new Vector4f();
      float[] resolution = new float[]{64.0F, 64.0F};
      if (this.livingBase == null) {
         uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         uv.set(uv.getX() / resolution[0], uv.getY() / resolution[1], uv.getZ() / resolution[0], uv.getW() / resolution[1]);
         return uv;
      } else {
         if (this.livingBase instanceof EntityPlayer) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityBat) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(6.0F, 6.0F, 12.0F, 12.0F);
         } else if (this.livingBase instanceof EntityArmorStand) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(12.0F, 32.0F, 24.0F, 44.0F);
         } else if (this.livingBase instanceof EntityPolarBear) {
            resolution[0] = 128.0F;
            resolution[1] = 64.0F;
            uv.set(7.0F, 7.0F, 14.0F, 14.0F);
         } else if (this.livingBase instanceof EntityOcelot) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(5.0F, 5.0F, 10.0F, 10.0F);
         } else if (this.livingBase instanceof EntityCow || this.livingBase instanceof EntityMooshroom) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(6.0F, 6.0F, 14.0F, 14.0F);
         } else if (this.livingBase instanceof EntityCreeper) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityDragon) {
            resolution[0] = 256.0F;
            resolution[1] = 256.0F;
            uv.set(128.0F, 46.0F, 144.0F, 63.0F);
         } else if (this.livingBase instanceof EntityEnderman) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityGhast) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(16.0F, 16.0F, 32.0F, 32.0F);
         } else if (this.livingBase instanceof EntityHorse
            || this.livingBase instanceof EntitySkeletonHorse
            || this.livingBase instanceof EntityZombieHorse
            || this.livingBase instanceof EntityDonkey
            || this.livingBase instanceof EntityMule) {
            resolution[0] = 128.0F;
            resolution[1] = 128.0F;
            uv.set(3.0F, 37.0F, 11.0F, 45.0F);
         } else if (this.livingBase instanceof EntityEvoker || this.livingBase instanceof EntityIllusionIllager || this.livingBase instanceof EntityVindicator) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 10.0F, 16.0F, 18.0F);
         } else if (this.livingBase instanceof EntityVex) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityLlama) {
            resolution[0] = 128.0F;
            resolution[1] = 64.0F;
            uv.set(48.0F, 31.0F, 56.0F, 39.0F);
         } else if (this.livingBase instanceof EntityParrot) {
            resolution[0] = 32.0F;
            resolution[1] = 32.0F;
            uv.set(3.0F, 4.0F, 7.0F, 7.0F);
         } else if (this.livingBase instanceof EntityPig) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(47.0F, 8.0F, 55.0F, 16.0F);
         } else if (this.livingBase instanceof EntityRabbit) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(37.0F, 4.0F, 42.0F, 9.0F);
         } else if (this.livingBase instanceof EntitySheep) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 14.0F, 14.0F);
         } else if (this.livingBase instanceof EntityShulker) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(16.0F, 0.0F, 32.0F, 16.0F);
         } else if (this.livingBase instanceof EntitySkeleton || this.livingBase instanceof EntityWitherSkeleton || this.livingBase instanceof EntityStray) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityMagmaCube) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(32.0F, 15.0F, 40.0F, 23.0F);
         } else if (this.livingBase instanceof EntitySlime) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(6.0F, 16.0F, 12.0F, 22.0F);
         } else if (this.livingBase instanceof EntitySpider || this.livingBase instanceof EntityCaveSpider) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(40.0F, 12.0F, 48.0F, 20.0F);
         } else if (this.livingBase instanceof EntityZombieVillager) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 10.0F, 16.0F, 18.0F);
         } else if (this.livingBase instanceof EntityVillager) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 10.0F, 16.0F, 18.0F);
         } else if (this.livingBase instanceof EntityWither) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityWolf) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(4.0F, 4.0F, 10.0F, 10.0F);
         } else if (this.livingBase instanceof EntityZombie || this.livingBase instanceof EntityHusk) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityBlaze) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityBlaze) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntityChicken) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(2.0F, 3.0F, 8.0F, 9.0F);
         } else if (this.livingBase instanceof EntityEndermite) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(2.0F, 1.0F, 6.0F, 5.0F);
         } else if (this.livingBase instanceof EntityGuardian || this.livingBase instanceof EntityElderGuardian) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(16.0F, 16.0F, 28.0F, 28.0F);
         } else if (this.livingBase instanceof EntityIronGolem) {
            resolution[0] = 128.0F;
            resolution[1] = 128.0F;
            uv.set(7.0F, 8.0F, 17.0F, 18.0F);
         } else if (this.livingBase instanceof EntityIronGolem) {
            resolution[0] = 128.0F;
            resolution[1] = 128.0F;
            uv.set(7.0F, 8.0F, 17.0F, 18.0F);
         } else if (this.livingBase instanceof EntitySnowman) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         } else if (this.livingBase instanceof EntitySquid) {
            resolution[0] = 64.0F;
            resolution[1] = 32.0F;
            uv.set(24.0F, 0.0F, 36.0F, 12.0F);
         } else if (this.livingBase instanceof EntityWitch) {
            resolution[0] = 64.0F;
            resolution[1] = 128.0F;
            uv.set(8.0F, 10.0F, 16.0F, 18.0F);
         } else if (this.livingBase instanceof EntityPigZombie) {
            resolution[0] = 64.0F;
            resolution[1] = 64.0F;
            uv.set(8.0F, 8.0F, 16.0F, 16.0F);
         }

         uv.set(uv.getX() / resolution[0], uv.getY() / resolution[1], uv.getZ() / resolution[0], uv.getW() / resolution[1]);
         return uv;
      }
   }

   @Nullable
   public Vector4f getRenderQuadFaceOverlayUv() {
      Vector4f uv = new Vector4f();
      float[] resolution = new float[]{64.0F, 64.0F};
      if (this.livingBase instanceof EntityPlayer) {
         resolution[0] = 64.0F;
         resolution[1] = 64.0F;
         uv.set(40.0F, 8.0F, 48.0F, 16.0F);
         uv.set(uv.getX() / resolution[0], uv.getY() / resolution[1], uv.getZ() / resolution[0], uv.getW() / resolution[1]);
         return uv;
      } else {
         return null;
      }
   }

   public HeadSkinUVLivingBase bindTex(ResourceLocation skinTex) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(skinTex);
      return this;
   }

   public void tessellateDefaultQuad(float x, float y, float x2, float y2) {
      Vector4f UV = this.getRenderQuadFaceUv();
      RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      RenderUtils.buffer.pos((double)x2, (double)y).tex((double)UV.getZ(), (double)UV.getY()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x, (double)y).tex((double)UV.getX(), (double)UV.getY()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x, (double)y2).tex((double)UV.getX(), (double)UV.getW()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x2, (double)y2).tex((double)UV.getZ(), (double)UV.getW()).color(this.color).endVertex();
      RenderUtils.tessellator.draw();
   }

   public void tessellateDefaultQuadWithOverlay(float x, float y, float x2, float y2, float mulExpand) {
      Vector4f UV = this.getRenderQuadFaceUv();
      RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      RenderUtils.buffer.pos((double)x2, (double)y).tex((double)UV.getZ(), (double)UV.getY()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x, (double)y).tex((double)UV.getX(), (double)UV.getY()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x, (double)y2).tex((double)UV.getX(), (double)UV.getW()).color(this.color).endVertex();
      RenderUtils.buffer.pos((double)x2, (double)y2).tex((double)UV.getZ(), (double)UV.getW()).color(this.color).endVertex();
      RenderUtils.tessellator.draw();
      Vector4f UV_OVERLAY = this.getRenderQuadFaceOverlayUv();
      if (UV_OVERLAY != null) {
         float expand = Math.min(x2 - x, y2 - y) / 16.0F * 2.0F * mulExpand;
         x -= expand;
         y -= expand;
         x2 += expand;
         y2 += expand;
         RenderUtils.buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         RenderUtils.buffer.pos((double)x2, (double)y).tex((double)UV_OVERLAY.getZ(), (double)UV_OVERLAY.getY()).color(this.color).endVertex();
         RenderUtils.buffer.pos((double)x, (double)y).tex((double)UV_OVERLAY.getX(), (double)UV_OVERLAY.getY()).color(this.color).endVertex();
         RenderUtils.buffer.pos((double)x, (double)y2).tex((double)UV_OVERLAY.getX(), (double)UV_OVERLAY.getW()).color(this.color).endVertex();
         RenderUtils.buffer.pos((double)x2, (double)y2).tex((double)UV_OVERLAY.getZ(), (double)UV_OVERLAY.getW()).color(this.color).endVertex();
         RenderUtils.tessellator.draw();
      }
   }
}
