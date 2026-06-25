package ru.govno.client.utils.Render;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.module.modules.ESP;
import ru.govno.client.module.modules.EntityBox;
import ru.govno.client.utils.Math.TimerHelper;

public class ScopeEntityModelTo2D {
   private int fpsUpdate = Integer.MAX_VALUE;
   private final TimerHelper timerHelper;
   private final Entity entity;
   private Vector4f bounds2dUpdated;

   public ScopeEntityModelTo2D(Entity entity) {
      this.entity = entity;
      this.timerHelper = TimerHelper.TimerHelperReseted();
   }

   public void setFpsLimit(int fpsUpdate) {
      this.fpsUpdate = fpsUpdate;
   }

   public void unsetFpsLimit() {
      this.fpsUpdate = Integer.MAX_VALUE;
   }

   public void updateTagCenterRenderScope2dDataIn3d(Vec3d move) {
      label75:
      if (this.entity != null) {
         if (this.entity instanceof EntityPlayerSP playerSP && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            break label75;
         }

         boolean reachedToUpdateTime = this.fpsUpdate == Integer.MAX_VALUE || this.timerHelper.hasReached((double)(1000.0F / (float)this.fpsUpdate));
         if (reachedToUpdateTime) {
            this.timerHelper.reset();

            try {
               float pt = Minecraft.getMinecraft().getRenderPartialTicks();
               int scaleFactor = ScaledResolution.getScaleFactor();
               double x = RenderUtils.interpolate(this.entity.posX, this.entity.prevPosX, (double)pt);
               double y = RenderUtils.interpolate(this.entity.posY, this.entity.prevPosY, (double)pt);
               double z = RenderUtils.interpolate(this.entity.posZ, this.entity.prevPosZ, (double)pt);
               if (EntityBox.isRenderModelSyncPosAABB() && this.entity instanceof EntityLivingBase base && !(this.entity instanceof EntityPlayerSP)) {
                  Vec3d offsetAtPos = EntityBox.hitboxModAddVec(base, EntityBox.hitboxModPredictSize(base));
                  if (offsetAtPos.distanceTo(Vec3d.ZERO) > 0.0) {
                     move = move.add(offsetAtPos);
                  }
               }

               if (move != null && move.lengthVector() > 0.0) {
                  x += move.xCoord;
                  y += move.yCoord;
                  z += move.zCoord;
               }

               double var10000;
               double var10001;
               label57: {
                  var10000 = (double)this.entity.height;
                  if (this.entity instanceof EntityLivingBase basex && basex.isChild()) {
                     var10001 = 1.75;
                     break label57;
                  }

                  var10001 = 1.0;
               }

               double height = var10000 / var10001 + 0.1;
               AxisAlignedBB aabb = new AxisAlignedBB(x, y, z, x, y + height, z);
               if (!RenderUtils.isInView(aabb)) {
                  this.bounds2dUpdated = null;
                  return;
               }

               Vector3d coord = ESP.project2D(scaleFactor, x - RenderManager.viewerPosX, y + height - RenderManager.viewerPosY, z - RenderManager.viewerPosZ);
               float scMul = ScaledResolution.isPushScale2 ? ScaledResolution.lpSCFactor() : 1.0F;
               if (this.bounds2dUpdated == null) {
                  this.bounds2dUpdated = new Vector4f((float)coord.x * scMul, (float)coord.y * scMul, (float)coord.x * scMul, (float)coord.y * scMul);
                  return;
               }

               this.bounds2dUpdated.x = (float)coord.x * scMul;
               this.bounds2dUpdated.y = (float)coord.y * scMul;
               this.bounds2dUpdated.z = (float)coord.x * scMul;
               this.bounds2dUpdated.w = (float)coord.y * scMul;
            } catch (Exception var16) {
               var16.printStackTrace();
            }
         }

         return;
      }

      this.bounds2dUpdated = null;
   }

   public void updateRenderScope2dDataIn3d(boolean center) {
      label106:
      if (this.entity != null) {
         if (this.entity instanceof EntityPlayerSP playerSP && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            break label106;
         }

         boolean reachedToUpdateTime = this.fpsUpdate == Integer.MAX_VALUE || this.timerHelper.hasReached((double)(1000.0F / (float)this.fpsUpdate));
         if (reachedToUpdateTime) {
            this.timerHelper.reset();

            try {
               float pt = Minecraft.getMinecraft().getRenderPartialTicks();
               int scaleFactor = ScaledResolution.getScaleFactor();
               double x = RenderUtils.interpolate(this.entity.posX, this.entity.prevPosX, (double)pt);
               double y = RenderUtils.interpolate(this.entity.posY, this.entity.prevPosY, (double)pt);
               double z = RenderUtils.interpolate(this.entity.posZ, this.entity.prevPosZ, (double)pt);
               if (EntityBox.isRenderModelSyncPosAABB() && this.entity instanceof EntityLivingBase base && !(this.entity instanceof EntityPlayerSP)) {
                  Vec3d offsetAtPos = EntityBox.hitboxModAddVec(base, EntityBox.hitboxModPredictSize(base));
                  if (offsetAtPos.distanceTo(Vec3d.ZERO) > 0.0) {
                     x += offsetAtPos.xCoord;
                     y += offsetAtPos.yCoord;
                     z += offsetAtPos.zCoord;
                  }
               }

               double var10000;
               double var10001;
               label91: {
                  var10000 = (double)this.entity.height;
                  if (this.entity instanceof EntityLivingBase basex && basex.isChild()) {
                     var10001 = 1.75;
                     break label91;
                  }

                  var10001 = 1.0;
               }

               double height = var10000 / var10001 + 0.1;
               if (center) {
                  var10000 = 0.0;
               } else {
                  label84: {
                     var10000 = (double)this.entity.width;
                     if (this.entity instanceof EntityLivingBase basex && basex.isChild()) {
                        var10001 = 1.5;
                        break label84;
                     }

                     var10001 = 1.0;
                  }

                  var10000 /= var10001;
               }

               double width = var10000;
               AxisAlignedBB aabb = new AxisAlignedBB(x - width / 2.0, y, z - width / 2.0, x + width / 2.0, y + height, z + width / 2.0);
               if (!RenderUtils.isInView(aabb)) {
                  this.bounds2dUpdated = null;
                  return;
               }

               if (center || width == 0.0) {
                  Vector3d coord = ESP.project2D(scaleFactor, x - RenderManager.viewerPosX, y + height - RenderManager.viewerPosY, z - RenderManager.viewerPosZ);
                  if (this.bounds2dUpdated == null) {
                     this.bounds2dUpdated = new Vector4f((float)coord.x, (float)coord.y, (float)coord.x, (float)coord.y);
                     return;
                  }

                  this.bounds2dUpdated.x = (float)coord.x;
                  this.bounds2dUpdated.y = (float)coord.y;
                  this.bounds2dUpdated.z = (float)coord.x;
                  this.bounds2dUpdated.w = (float)coord.y;
                  return;
               }

               Vector3d[] vectors = new Vector3d[]{
                  new Vector3d(aabb.minX, aabb.minY, aabb.minZ),
                  new Vector3d(aabb.minX, aabb.maxY, aabb.minZ),
                  new Vector3d(aabb.maxX, aabb.minY, aabb.minZ),
                  new Vector3d(aabb.maxX, aabb.maxY, aabb.minZ),
                  new Vector3d(aabb.minX, aabb.minY, aabb.maxZ),
                  new Vector3d(aabb.minX, aabb.maxY, aabb.maxZ),
                  new Vector3d(aabb.maxX, aabb.minY, aabb.maxZ),
                  new Vector3d(aabb.maxX, aabb.maxY, aabb.maxZ)
               };
               int vecLength = vectors.length;
               this.bounds2dUpdated = null;

               for (int vecI = 0; vecI < vecLength; vecI++) {
                  Vector3d vector = vectors[vecI];
                  vector = ESP.project2D(
                     scaleFactor, vector.x - RenderManager.viewerPosX, vector.y - RenderManager.viewerPosY, vector.z - RenderManager.viewerPosZ
                  );
                  if (vector != null) {
                     if (this.bounds2dUpdated == null) {
                        this.bounds2dUpdated = new Vector4f((float)vector.x, (float)vector.y, (float)vector.z, 0.0F);
                     }

                     this.bounds2dUpdated.x = (float)Math.min(vector.x, (double)this.bounds2dUpdated.x);
                     this.bounds2dUpdated.y = (float)Math.min(vector.y, (double)this.bounds2dUpdated.y);
                     this.bounds2dUpdated.z = (float)Math.max(vector.x, (double)this.bounds2dUpdated.z);
                     this.bounds2dUpdated.w = (float)Math.max(vector.y, (double)this.bounds2dUpdated.w);
                  }
               }
            } catch (Exception var20) {
               var20.printStackTrace();
            }
         }

         return;
      }

      this.bounds2dUpdated = null;
   }

   public boolean canUseCoords(ScaledResolution sr) {
      if (this.bounds2dUpdated == null) {
         return false;
      } else {
         float boundW = this.bounds2dUpdated.z - this.bounds2dUpdated.x;
         float boundH = this.bounds2dUpdated.w - this.bounds2dUpdated.y;
         float scrW = (float)sr.getScaledWidth();
         float scrH = (float)sr.getScaledHeight();
         return this.bounds2dUpdated.x + boundW > 0.0F
            && this.bounds2dUpdated.y + boundH > 0.0F
            && this.bounds2dUpdated.x < scrW
            && this.bounds2dUpdated.y < scrW;
      }
   }

   public Vector4f getBounds2dUpdated() {
      return this.bounds2dUpdated;
   }
}
