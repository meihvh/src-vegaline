package ru.govno.client.utils.Render;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4f;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import ru.govno.client.module.modules.ESP;
import ru.govno.client.utils.Math.TimerHelper;

public class ScopeTileEntityModelTo2D {
   private int fpsUpdate = Integer.MAX_VALUE;
   private final TimerHelper timerHelper;
   private final TileEntity tile;
   private Vector4f bounds2dUpdated = null;

   public ScopeTileEntityModelTo2D(TileEntity tile) {
      this.tile = tile;
      this.timerHelper = TimerHelper.TimerHelperReseted();
   }

   public void setFpsLimit(int fpsUpdate) {
      this.fpsUpdate = fpsUpdate;
   }

   public void unsetFpsLimit() {
      this.fpsUpdate = Integer.MAX_VALUE;
   }

   public void updateRenderScope2dDataIn3d() {
      if (this.tile != null && this.tile.getPos() != null) {
         boolean reachedToUpdateTime = this.fpsUpdate == Integer.MAX_VALUE || this.timerHelper.hasReached((double)(1000.0F / (float)this.fpsUpdate));
         if (reachedToUpdateTime) {
            this.timerHelper.reset();
            AxisAlignedBB aabb = new AxisAlignedBB(this.tile.getPos());
            if (!RenderUtils.isInView(aabb)) {
               this.bounds2dUpdated = null;
               return;
            }

            int scaleFactor = ScaledResolution.getScaleFactor();
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
         }
      } else {
         this.bounds2dUpdated = null;
      }
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
