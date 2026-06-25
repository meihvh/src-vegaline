package net.minecraft.client.particle;

import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Particle {
   private static final AxisAlignedBB EMPTY_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   protected World worldObj;
   protected double prevPosX;
   protected double prevPosY;
   protected double prevPosZ;
   protected double posX;
   protected double posY;
   protected double posZ;
   protected double motionX;
   protected double motionY;
   protected double motionZ;
   private AxisAlignedBB boundingBox = EMPTY_AABB;
   protected boolean isCollided;
   protected boolean canCollide;
   protected boolean isExpired;
   protected float width = 0.6F;
   protected float height = 1.8F;
   protected Random rand = new Random();
   protected int particleTextureIndexX;
   protected int particleTextureIndexY;
   protected float particleTextureJitterX;
   protected float particleTextureJitterY;
   protected float particleAge;
   protected int particleMaxAge;
   protected float particleScale;
   protected float particleGravity;
   protected float particleRed;
   protected float particleGreen;
   protected float particleBlue;
   protected float particleAlpha = 1.0F;
   protected TextureAtlasSprite particleTexture;
   protected float particleAngle;
   protected float prevParticleAngle;
   public static double interpPosX;
   public static double interpPosY;
   public static double interpPosZ;
   public static Vec3d cameraViewDir;

   protected Particle(World worldIn, double posXIn, double posYIn, double posZIn) {
      this.worldObj = worldIn;
      this.setSize(0.2F, 0.2F);
      this.setPosition(posXIn, posYIn, posZIn);
      this.prevPosX = posXIn;
      this.prevPosY = posYIn;
      this.prevPosZ = posZIn;
      this.particleRed = 1.0F;
      this.particleGreen = 1.0F;
      this.particleBlue = 1.0F;
      this.particleTextureJitterX = this.rand.nextFloat() * 3.0F;
      this.particleTextureJitterY = this.rand.nextFloat() * 3.0F;
      this.particleScale = (this.rand.nextFloat() * 0.5F + 0.5F) * 2.0F;
      this.particleMaxAge = (int)(4.0F / (this.rand.nextFloat() * 0.9F + 0.1F));
      this.particleAge = 0.0F;
      this.canCollide = true;
   }

   public Particle(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn) {
      this(worldIn, xCoordIn, yCoordIn, zCoordIn);
      this.motionX = xSpeedIn + (Math.random() * 2.0 - 1.0) * 0.4F;
      this.motionY = ySpeedIn + (Math.random() * 2.0 - 1.0) * 0.4F;
      this.motionZ = zSpeedIn + (Math.random() * 2.0 - 1.0) * 0.4F;
      float f = (float)(Math.random() + Math.random() + 1.0) * 0.15F;
      float f1 = MathHelper.sqrt(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
      this.motionX = this.motionX / (double)f1 * (double)f * 0.4F;
      this.motionY = this.motionY / (double)f1 * (double)f * 0.4F + 0.1F;
      this.motionZ = this.motionZ / (double)f1 * (double)f * 0.4F;
   }

   public Particle multiplyVelocity(float multiplier) {
      this.motionX *= (double)multiplier;
      this.motionY = (this.motionY - 0.1F) * (double)multiplier + 0.1F;
      this.motionZ *= (double)multiplier;
      return this;
   }

   public Particle multipleParticleScaleBy(float scale) {
      this.setSize(0.2F * scale, 0.2F * scale);
      this.particleScale *= scale;
      return this;
   }

   public void setRBGColorF(float particleRedIn, float particleGreenIn, float particleBlueIn) {
      this.particleRed = particleRedIn;
      this.particleGreen = particleGreenIn;
      this.particleBlue = particleBlueIn;
   }

   public void setAlphaF(float alpha) {
      this.particleAlpha = alpha;
   }

   public boolean isTransparent() {
      return false;
   }

   public float getRedColorF() {
      return this.particleRed;
   }

   public float getGreenColorF() {
      return this.particleGreen;
   }

   public float getBlueColorF() {
      return this.particleBlue;
   }

   public void setMaxAge(int p_187114_1_) {
      this.particleMaxAge = p_187114_1_;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.particleAge++ >= (float)this.particleMaxAge) {
         this.setExpired();
      }

      this.motionY = this.motionY - 0.04 * (double)this.particleGravity;
      this.moveEntity(this.motionX, this.motionY, this.motionZ);
      this.motionX *= 0.98F;
      this.motionY *= 0.98F;
      this.motionZ *= 0.98F;
      if (this.isCollided) {
         this.motionX *= 0.7F;
         this.motionZ *= 0.7F;
      }
   }

   public void renderParticle(
      BufferBuilder worldRendererIn,
      Entity entityIn,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      float f = (float)this.particleTextureIndexX / 16.0F;
      float f1 = f + 0.0624375F;
      float f2 = (float)this.particleTextureIndexY / 16.0F;
      float f3 = f2 + 0.0624375F;
      float f4 = 0.1F * this.particleScale;
      if (this.particleTexture != null) {
         f = this.particleTexture.getMinU();
         f1 = this.particleTexture.getMaxU();
         f2 = this.particleTexture.getMinV();
         f3 = this.particleTexture.getMaxV();
      }

      float f5 = (float)(this.prevPosX + (this.posX - this.prevPosX) * (double)partialTicks - interpPosX);
      float f6 = (float)(this.prevPosY + (this.posY - this.prevPosY) * (double)partialTicks - interpPosY);
      float f7 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * (double)partialTicks - interpPosZ);
      int i = this.getBrightnessForRender(partialTicks);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      Vec3d[] avec3d = new Vec3d[]{
         new Vec3d((double)(-rotationX * f4 - rotationXY * f4), (double)(-rotationZ * f4), (double)(-rotationYZ * f4 - rotationXZ * f4)),
         new Vec3d((double)(-rotationX * f4 + rotationXY * f4), (double)(rotationZ * f4), (double)(-rotationYZ * f4 + rotationXZ * f4)),
         new Vec3d((double)(rotationX * f4 + rotationXY * f4), (double)(rotationZ * f4), (double)(rotationYZ * f4 + rotationXZ * f4)),
         new Vec3d((double)(rotationX * f4 - rotationXY * f4), (double)(-rotationZ * f4), (double)(rotationYZ * f4 - rotationXZ * f4))
      };
      if (this.particleAngle != 0.0F) {
         float f8 = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partialTicks;
         float f9 = MathHelper.cos(f8 * 0.5F);
         float f10 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.xCoord;
         float f11 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.yCoord;
         float f12 = MathHelper.sin(f8 * 0.5F) * (float)cameraViewDir.zCoord;
         Vec3d vec3d = new Vec3d((double)f10, (double)f11, (double)f12);

         for (int l = 0; l < 4; l++) {
            avec3d[l] = vec3d.scale(2.0 * avec3d[l].dotProduct(vec3d))
               .add(avec3d[l].scale((double)(f9 * f9) - vec3d.dotProduct(vec3d)))
               .add(vec3d.crossProduct(avec3d[l]).scale((double)(2.0F * f9)));
         }
      }

      worldRendererIn.pos((double)f5 + avec3d[0].xCoord, (double)f6 + avec3d[0].yCoord, (double)f7 + avec3d[0].zCoord)
         .tex((double)f1, (double)f3)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos((double)f5 + avec3d[1].xCoord, (double)f6 + avec3d[1].yCoord, (double)f7 + avec3d[1].zCoord)
         .tex((double)f1, (double)f2)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos((double)f5 + avec3d[2].xCoord, (double)f6 + avec3d[2].yCoord, (double)f7 + avec3d[2].zCoord)
         .tex((double)f, (double)f2)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
      worldRendererIn.pos((double)f5 + avec3d[3].xCoord, (double)f6 + avec3d[3].yCoord, (double)f7 + avec3d[3].zCoord)
         .tex((double)f, (double)f3)
         .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
         .lightmap(j, k)
         .endVertex();
   }

   public int getFXLayer() {
      return 0;
   }

   public void setParticleTexture(TextureAtlasSprite texture) {
      int i = this.getFXLayer();
      if (i == 1) {
         this.particleTexture = texture;
      } else {
         throw new RuntimeException("Invalid call to Particle.setTex, use coordinate methods");
      }
   }

   public void setParticleTextureIndex(int particleTextureIndex) {
      if (this.getFXLayer() != 0) {
         throw new RuntimeException("Invalid call to Particle.setMiscTex");
      } else {
         this.particleTextureIndexX = particleTextureIndex % 16;
         this.particleTextureIndexY = particleTextureIndex / 16;
      }
   }

   public void nextTextureIndexX() {
      this.particleTextureIndexX++;
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName()
         + ", Pos ("
         + this.posX
         + ","
         + this.posY
         + ","
         + this.posZ
         + "), RGBA ("
         + this.particleRed
         + ","
         + this.particleGreen
         + ","
         + this.particleBlue
         + ","
         + this.particleAlpha
         + "), Age "
         + this.particleAge;
   }

   public void setExpired() {
      this.isExpired = true;
   }

   protected void setSize(float p_187115_1_, float p_187115_2_) {
      if (p_187115_1_ != this.width || p_187115_2_ != this.height) {
         this.width = p_187115_1_;
         this.height = p_187115_2_;
         AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
         this.setEntityBoundingBox(
            new AxisAlignedBB(
               axisalignedbb.minX,
               axisalignedbb.minY,
               axisalignedbb.minZ,
               axisalignedbb.minX + (double)this.width,
               axisalignedbb.minY + (double)this.height,
               axisalignedbb.minZ + (double)this.width
            )
         );
      }
   }

   public void setPosition(double p_187109_1_, double p_187109_3_, double p_187109_5_) {
      this.posX = p_187109_1_;
      this.posY = p_187109_3_;
      this.posZ = p_187109_5_;
      float f = this.width / 2.0F;
      float f1 = this.height;
      this.setEntityBoundingBox(
         new AxisAlignedBB(
            p_187109_1_ - (double)f, p_187109_3_, p_187109_5_ - (double)f, p_187109_1_ + (double)f, p_187109_3_ + (double)f1, p_187109_5_ + (double)f
         )
      );
   }

   public void moveEntity(double x, double y, double z) {
      double d0 = y;
      if (this.canCollide) {
         List<AxisAlignedBB> list = this.worldObj.getCollisionBoxes((Entity)null, this.getEntityBoundingBox().addCoord(x, y, z));

         for (AxisAlignedBB axisalignedbb : list) {
            y = axisalignedbb.calculateYOffset(this.getEntityBoundingBox(), y);
         }

         this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, y, 0.0));

         for (AxisAlignedBB axisalignedbb1 : list) {
            x = axisalignedbb1.calculateXOffset(this.getEntityBoundingBox(), x);
         }

         this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, 0.0, 0.0));

         for (AxisAlignedBB axisalignedbb2 : list) {
            z = axisalignedbb2.calculateZOffset(this.getEntityBoundingBox(), z);
         }

         this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0.0, 0.0, z));
      } else {
         this.setEntityBoundingBox(this.getEntityBoundingBox().offset(x, y, z));
      }

      this.resetPositionToBB();
      this.isCollided = y != y && d0 < 0.0;
      if (x != x) {
         this.motionX = 0.0;
      }

      if (z != z) {
         this.motionZ = 0.0;
      }
   }

   protected void resetPositionToBB() {
      AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
      this.posX = (axisalignedbb.minX + axisalignedbb.maxX) / 2.0;
      this.posY = axisalignedbb.minY;
      this.posZ = (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0;
   }

   public int getBrightnessForRender(float p_189214_1_) {
      BlockPos blockpos = new BlockPos(this.posX, this.posY, this.posZ);
      return this.worldObj.isBlockLoaded(blockpos) ? this.worldObj.getCombinedLight(blockpos, 0) : 0;
   }

   public boolean isAlive() {
      return !this.isExpired;
   }

   public AxisAlignedBB getEntityBoundingBox() {
      return this.boundingBox;
   }

   public void setEntityBoundingBox(AxisAlignedBB bb) {
      this.boundingBox = bb;
   }
}
