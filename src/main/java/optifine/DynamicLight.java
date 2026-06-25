package optifine;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import ru.govno.client.utils.Math.TimerHelper;

public class DynamicLight {
   private Entity entity = null;
   private double offsetY = 0.0;
   private double lastPosX = -2.1474836E9F;
   private double lastPosY = -2.1474836E9F;
   private double lastPosZ = -2.1474836E9F;
   private int lastLightLevel = 0;
   private boolean underwater = false;
   private long timeCheckMs = 0L;
   private Set<BlockPos> setLitChunkPos = new HashSet<>();
   private final BlockPos.MutableBlockPos blockPosMutable = new BlockPos.MutableBlockPos();
   public boolean custom = false;
   public Vec3d position;
   public float light015;
   public float strengthPC01;
   public int ticks;
   public TimerHelper timeAlive = TimerHelper.TimerHelperReseted();

   public DynamicLight(Entity p_i36_1_) {
      this.entity = p_i36_1_;
      this.offsetY = (double)p_i36_1_.getEyeHeight();
   }

   private DynamicLight(Vec3d pos, float lightPC01, float strengthPC01) {
      this.custom = true;
      this.position = pos;
      this.light015 = Math.min(lightPC01, 1.0F) * 15.0F;
      this.strengthPC01 = strengthPC01;
      this.ticks = 2;
      this.lastPosX = this.position.xCoord;
      this.lastPosY = this.position.yCoord;
      this.lastPosZ = this.position.zCoord;
      this.lastLightLevel = MathHelper.floor(this.light015);
   }

   public boolean isCustomLight() {
      return this.custom;
   }

   public boolean toBeRemovedIfCustom() {
      boolean removed = this.custom && this.timeAlive.hasReached((double)((long)this.ticks * 50L));
      if (removed) {
         this.updateLitChunks(Minecraft.getMinecraft().renderGlobal);
      }

      return removed;
   }

   public static DynamicLight createCustomDynamicLightForTick(Vec3d pos, float lightPC01, float strengthPC01) {
      return new DynamicLight(pos, lightPC01, strengthPC01);
   }

   public DynamicLight updateCustom(RenderGlobal renderGlobal) {
      Set<BlockPos> set = new HashSet<>();
      if (this.custom) {
         World world = renderGlobal.getWorld();
         if (world != null) {
            this.blockPosMutable.setPos(MathHelper.floor(this.position.xCoord), MathHelper.floor(this.position.yCoord), MathHelper.floor(this.position.zCoord));
            IBlockState iblockstate = world.getBlockState(this.blockPosMutable);
            Block block = iblockstate.getBlock();
            this.underwater = block == Blocks.WATER;
         }

         if (this.light015 > 0.0F) {
            EnumFacing enumfacing2 = (MathHelper.floor(this.position.xCoord) & 15) >= 8 ? EnumFacing.EAST : EnumFacing.WEST;
            EnumFacing enumfacing = (MathHelper.floor(this.position.yCoord) & 15) >= 8 ? EnumFacing.UP : EnumFacing.DOWN;
            EnumFacing enumfacing1 = (MathHelper.floor(this.position.zCoord) & 15) >= 8 ? EnumFacing.SOUTH : EnumFacing.NORTH;
            BlockPos blockpos = new BlockPos(this.position.xCoord, this.position.yCoord, this.position.zCoord);
            RenderChunk renderchunk = renderGlobal.getRenderChunk(blockpos);
            BlockPos blockpos1 = this.getChunkPos(renderchunk, blockpos, enumfacing2);
            RenderChunk renderchunk1 = renderGlobal.getRenderChunk(blockpos1);
            BlockPos blockpos2 = this.getChunkPos(renderchunk, blockpos, enumfacing1);
            RenderChunk renderchunk2 = renderGlobal.getRenderChunk(blockpos2);
            BlockPos blockpos3 = this.getChunkPos(renderchunk1, blockpos1, enumfacing1);
            RenderChunk renderchunk3 = renderGlobal.getRenderChunk(blockpos3);
            BlockPos blockpos4 = this.getChunkPos(renderchunk, blockpos, enumfacing);
            RenderChunk renderchunk4 = renderGlobal.getRenderChunk(blockpos4);
            BlockPos blockpos5 = this.getChunkPos(renderchunk4, blockpos4, enumfacing2);
            RenderChunk renderchunk5 = renderGlobal.getRenderChunk(blockpos5);
            BlockPos blockpos6 = this.getChunkPos(renderchunk4, blockpos4, enumfacing1);
            RenderChunk renderchunk6 = renderGlobal.getRenderChunk(blockpos6);
            BlockPos blockpos7 = this.getChunkPos(renderchunk5, blockpos5, enumfacing1);
            RenderChunk renderchunk7 = renderGlobal.getRenderChunk(blockpos7);
            this.updateChunkLight(renderchunk, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk1, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk2, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk3, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk4, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk5, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk6, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk7, this.setLitChunkPos, set);
         }

         this.updateLitChunks(renderGlobal);
         this.setLitChunkPos = set;
      }

      return this;
   }

   public void update(RenderGlobal p_update_1_) {
      if (Config.isDynamicLightsFast()) {
         long i = System.currentTimeMillis();
         if (i < this.timeCheckMs + 500L) {
            return;
         }

         this.timeCheckMs = i;
      }

      double d6 = this.entity.posX - 0.5;
      double d0 = this.entity.posY - 0.5 + this.offsetY;
      double d1 = this.entity.posZ - 0.5;
      int j = this.entity == null ? 0 : DynamicLights.getLightLevel(this.entity);
      double d2 = d6 - this.lastPosX;
      double d3 = d0 - this.lastPosY;
      double d4 = d1 - this.lastPosZ;
      double d5 = 0.1;
      if (Math.abs(d2) > d5 || Math.abs(d3) > d5 || Math.abs(d4) > d5 || this.lastLightLevel != j) {
         this.lastPosX = d6;
         this.lastPosY = d0;
         this.lastPosZ = d1;
         this.lastLightLevel = j;
         this.underwater = false;
         World world = p_update_1_.getWorld();
         if (world != null) {
            this.blockPosMutable.setPos(MathHelper.floor(d6), MathHelper.floor(d0), MathHelper.floor(d1));
            IBlockState iblockstate = world.getBlockState(this.blockPosMutable);
            Block block = iblockstate.getBlock();
            this.underwater = block == Blocks.WATER;
         }

         Set<BlockPos> set = new HashSet<>();
         if (j > 0) {
            EnumFacing enumfacing2 = (MathHelper.floor(d6) & 15) >= 8 ? EnumFacing.EAST : EnumFacing.WEST;
            EnumFacing enumfacing = (MathHelper.floor(d0) & 15) >= 8 ? EnumFacing.UP : EnumFacing.DOWN;
            EnumFacing enumfacing1 = (MathHelper.floor(d1) & 15) >= 8 ? EnumFacing.SOUTH : EnumFacing.NORTH;
            BlockPos blockpos = new BlockPos(d6, d0, d1);
            RenderChunk renderchunk = p_update_1_.getRenderChunk(blockpos);
            BlockPos blockpos1 = this.getChunkPos(renderchunk, blockpos, enumfacing2);
            RenderChunk renderchunk1 = p_update_1_.getRenderChunk(blockpos1);
            BlockPos blockpos2 = this.getChunkPos(renderchunk, blockpos, enumfacing1);
            RenderChunk renderchunk2 = p_update_1_.getRenderChunk(blockpos2);
            BlockPos blockpos3 = this.getChunkPos(renderchunk1, blockpos1, enumfacing1);
            RenderChunk renderchunk3 = p_update_1_.getRenderChunk(blockpos3);
            BlockPos blockpos4 = this.getChunkPos(renderchunk, blockpos, enumfacing);
            RenderChunk renderchunk4 = p_update_1_.getRenderChunk(blockpos4);
            BlockPos blockpos5 = this.getChunkPos(renderchunk4, blockpos4, enumfacing2);
            RenderChunk renderchunk5 = p_update_1_.getRenderChunk(blockpos5);
            BlockPos blockpos6 = this.getChunkPos(renderchunk4, blockpos4, enumfacing1);
            RenderChunk renderchunk6 = p_update_1_.getRenderChunk(blockpos6);
            BlockPos blockpos7 = this.getChunkPos(renderchunk5, blockpos5, enumfacing1);
            RenderChunk renderchunk7 = p_update_1_.getRenderChunk(blockpos7);
            this.updateChunkLight(renderchunk, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk1, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk2, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk3, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk4, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk5, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk6, this.setLitChunkPos, set);
            this.updateChunkLight(renderchunk7, this.setLitChunkPos, set);
         }

         this.updateLitChunks(p_update_1_);
         this.setLitChunkPos = set;
      }
   }

   private BlockPos getChunkPos(RenderChunk p_getChunkPos_1_, BlockPos p_getChunkPos_2_, EnumFacing p_getChunkPos_3_) {
      return p_getChunkPos_1_ != null ? p_getChunkPos_1_.getBlockPosOffset16(p_getChunkPos_3_) : p_getChunkPos_2_.offset(p_getChunkPos_3_, 16);
   }

   private void updateChunkLight(RenderChunk p_updateChunkLight_1_, Set<BlockPos> p_updateChunkLight_2_, Set<BlockPos> p_updateChunkLight_3_) {
      if (p_updateChunkLight_1_ != null) {
         CompiledChunk compiledchunk = p_updateChunkLight_1_.getCompiledChunk();
         if (compiledchunk != null && !compiledchunk.isEmpty()) {
            p_updateChunkLight_1_.setNeedsUpdate(false);
         }

         BlockPos blockpos = p_updateChunkLight_1_.getPosition().toImmutable();
         if (p_updateChunkLight_2_ != null) {
            p_updateChunkLight_2_.remove(blockpos);
         }

         if (p_updateChunkLight_3_ != null) {
            p_updateChunkLight_3_.add(blockpos);
         }
      }
   }

   public void updateLitChunks(RenderGlobal p_updateLitChunks_1_) {
      for (BlockPos blockpos : this.setLitChunkPos) {
         RenderChunk renderchunk = p_updateLitChunks_1_.getRenderChunk(blockpos);
         this.updateChunkLight(renderchunk, null, null);
      }
   }

   public Entity getEntity() {
      return this.entity;
   }

   public double getLastPosX() {
      return this.lastPosX;
   }

   public double getLastPosY() {
      return this.lastPosY;
   }

   public double getLastPosZ() {
      return this.lastPosZ;
   }

   public int getLastLightLevel() {
      return this.lastLightLevel;
   }

   public boolean isUnderwater() {
      return this.underwater;
   }

   public double getOffsetY() {
      return this.offsetY;
   }

   @Override
   public String toString() {
      return "Entity: " + this.entity + ", offsetY: " + this.offsetY;
   }
}
