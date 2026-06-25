package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.tileentity.TileEntityBed;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Sphere;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.Event3D;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class Nuker extends Module {
   FloatSettings MaxBlocksShare;
   FloatSettings Range;
   BoolSettings Rotations;
   BoolSettings ClientLook;
   BoolSettings SilentMoveRot;
   BoolSettings IgnoreWalls;
   BoolSettings BreakFermBlocks;
   BoolSettings BedwarsMode;
   BoolSettings CustomRange;
   ModeSettings Target;
   public static Nuker get;
   private double smoothProgress = 0.0;
   private float alphaHPG = 0.0F;
   private float hpgX = 0.0F;
   private float hpgY = 0.0F;
   private float hpgZ = 0.0F;
   private Vec3d animatedHPG = null;
   final List<BlockPos> positions = new ArrayList<>();
   List<BlockPos> targetedPoses = new ArrayList<>();
   private static BlockPos targetedPosition = null;
   public static BlockPos renderPosition = null;
   float yaw;
   float lastRYaw;
   float lastRPitch;

   public Nuker() {
      super("Nuker", 0, Module.Category.MISC);
      this.settings.add(this.MaxBlocksShare = new FloatSettings("MaxBlocksShare", 1.0F, 4.0F, 1.0F, this));
      this.settings.add(this.CustomRange = new BoolSettings("CustomRange", true, this));
      this.settings.add(this.Range = new FloatSettings("Range", 5.0F, 10.0F, 1.0F, this, () -> this.CustomRange.getBool()));
      this.settings.add(this.Rotations = new BoolSettings("Rotations", true, this));
      this.settings.add(this.ClientLook = new BoolSettings("ClientLook", false, this, () -> this.Rotations.getBool()));
      this.settings.add(this.SilentMoveRot = new BoolSettings("SilentMoveRot", true, this, () -> this.Rotations.getBool()));
      this.settings.add(this.IgnoreWalls = new BoolSettings("IgnoreWalls", false, this));
      this.settings
         .add(this.Target = new ModeSettings("Target", "All", this, new String[]{"All", "Sand", "Ores", "Wooden", "Stones", "Ferma", "Bed", "Web", "Leaves"}));
      this.settings.add(this.BreakFermBlocks = new BoolSettings("BreakFermBlocks", false, this, () -> this.Target.currentMode.equalsIgnoreCase("Ferma")));
      this.settings
         .add(
            this.BedwarsMode = new BoolSettings("BedwarsMode", true, this, () -> this.Target.currentMode.equalsIgnoreCase("Bed") && this.IgnoreWalls.getBool())
         );
      this.setDemand(2, 3);
      get = this;
   }

   private boolean isBedWarsModeDoes() {
      return this.Target.getMode().equalsIgnoreCase("Bed") && this.BedwarsMode.getBool() && this.IgnoreWalls.getBool();
   }

   private Vec3d playerVec3dPos() {
      return new Vec3d(
         mc.getRenderManager().getRenderPosX() + Minecraft.player.motionX,
         mc.getRenderManager().getRenderPosY() + 0.51,
         mc.getRenderManager().getRenderPosZ() + Minecraft.player.motionZ
      );
   }

   private ArrayList<BlockPos> positionsZone(Vec3d pos, float range) {
      boolean bedwarsMode = this.isBedWarsModeDoes();
      ArrayList<BlockPos> poses = new ArrayList<>();

      for (int x = (int)(-range); (float)x < range; x++) {
         for (int z = (int)(-range); (float)z < range; z++) {
            for (int y = bedwarsMode ? (int)(-range) : 0; (float)y < range; y++) {
               BlockPos pos1 = new BlockPos(pos.xCoord + (double)x, pos.yCoord + (double)y, pos.zCoord + (double)z);
               poses.add(pos1);
            }
         }
      }

      poses.sort(Comparator.comparing(pos1x -> pos.distanceTo(new Vec3d(pos1x).addVector(0.5, 0.5, 0.5))));
      return poses;
   }

   private boolean isUnbreakebleBlock(Block block) {
      return block == Blocks.AIR
         || (block == Blocks.BEDROCK || block == Blocks.BARRIER || block == Blocks.END_PORTAL_FRAME || block == Blocks.END_PORTAL)
            && !Minecraft.player.isCreative()
         || block instanceof BlockLiquid;
   }

   private Vec3d[] getPositionsZone01(Vec3d playerPos, float range) {
      return new Vec3d[]{
         new Vec3d(playerPos.xCoord - (double)range, playerPos.yCoord, playerPos.zCoord - (double)range),
         new Vec3d(playerPos.xCoord + (double)range, playerPos.yCoord + (double)range, playerPos.zCoord + (double)range)
      };
   }

   private void drawZone(float range) {
      Vec3d vec = this.playerVec3dPos().addVector(0.0, -0.51 + (double)Minecraft.player.getEyeHeight(), 0.0);
      GL11.glAlphaFunc(516, 0.003921569F);
      int color = ColorUtils.getColor(255, 255, 255, 10);
      int color2 = ColorUtils.getColor(255, 255, 255, 5);
      Sphere sphere = new Sphere();
      GL11.glTranslated(vec.xCoord, vec.yCoord, vec.zCoord);
      GL11.glRotated(90.0, 1.0, 0.0, 0.0);
      sphere.setDrawStyle(100011);
      RenderUtils.glColor(color);
      sphere.draw(range, 12, 12);
      RenderUtils.glColor(color2);
      sphere.setDrawStyle(100012);
      sphere.draw(range, 12, 12);
      GL11.glRotated(90.0, -1.0, 0.0, 0.0);
      GL11.glTranslated(-vec.xCoord, -vec.yCoord, -vec.zCoord);
      GL11.glAlphaFunc(516, 0.1F);
      GlStateManager.resetColor();
   }

   public void resetRenderHittingProgress() {
      this.smoothProgress = 0.0;
   }

   private void drawHittingProgress() {
      float animationsSpeed = (float)(0.02F * Minecraft.frameTime);
      BlockPos pos = this.getRenderPosition();
      if (pos != null) {
         float toX = (float)pos.getX();
         float toY = (float)pos.getY();
         float toZ = (float)pos.getZ();
         this.hpgX = MathUtils.harp(this.hpgX, toX, animationsSpeed);
         this.hpgY = MathUtils.harp(this.hpgY, toY, animationsSpeed);
         this.hpgZ = MathUtils.harp(this.hpgZ, toZ, animationsSpeed);
         this.alphaHPG = MathUtils.harp(this.alphaHPG, 255.0F, animationsSpeed * 3.0F);
      } else if (MathUtils.getDifferenceOf(this.alphaHPG, 0.0F) > 0.0F) {
         this.alphaHPG = MathUtils.harp(this.alphaHPG, 0.0F, animationsSpeed);
      }

      this.animatedHPG = new Vec3d((double)this.hpgX + 0.5, (double)this.hpgY + 0.5, (double)this.hpgZ + 0.5);
      float progress = mc.playerController.isHittingBlock ? mc.playerController.curBlockDamageMP : 0.0F;
      this.smoothProgress = (double)MathUtils.lerp((float)this.smoothProgress, progress, animationsSpeed * 3.0F);
      if (this.smoothProgress != 0.0) {
         Vec3d firstPoint = new Vec3d(
            this.animatedHPG.xCoord - 0.5 * this.smoothProgress,
            this.animatedHPG.yCoord - 0.5 * this.smoothProgress,
            this.animatedHPG.zCoord - 0.5 * this.smoothProgress
         );
         Vec3d lastPoint = new Vec3d(
            this.animatedHPG.xCoord + 0.5 * this.smoothProgress,
            this.animatedHPG.yCoord + 0.5 * this.smoothProgress,
            this.animatedHPG.zCoord + 0.5 * this.smoothProgress
         );
         AxisAlignedBB axisBox = new AxisAlignedBB(
            firstPoint.xCoord, firstPoint.yCoord, firstPoint.zCoord, lastPoint.xCoord, lastPoint.yCoord, lastPoint.zCoord
         );
         GL11.glColor4f(1.0F, 1.0F, 1.0F, this.alphaHPG / 255.0F);
         int color = ColorUtils.getColor(255, 255, 255, this.alphaHPG / 3.0F);
         int color2 = ColorUtils.getColor(255, 255, 255, this.alphaHPG / 25.5F);
         RenderUtils.drawCanisterBox(axisBox, true, true, true, color, color, color2);
      }
   }

   private boolean seenBlockPos(BlockPos pos) {
      return Minecraft.player.canEntityBeSeenCoords((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
   }

   private boolean canSeenBlock(BlockPos pos, boolean ignoreNoSeen) {
      return !this.seenBlockPos(pos)
            && !this.seenBlockPos(pos.add(1, 1, 1))
            && !this.seenBlockPos(pos.add(-1, 1, -1))
            && !this.seenBlockPos(pos.add(1, 1, -1))
            && !this.seenBlockPos(pos.add(-1, 1, 1))
            && !this.seenBlockPos(pos.add(1, 1, 0))
            && !this.seenBlockPos(pos.add(-1, 1, 0))
            && !this.seenBlockPos(pos.add(0, 1, 1))
            && !this.seenBlockPos(pos.add(0, 1, -1))
            && !this.seenBlockPos(pos.add(1, 0, 1))
            && !this.seenBlockPos(pos.add(-1, 0, -1))
            && !this.seenBlockPos(pos.add(1, 0, -1))
            && !this.seenBlockPos(pos.add(-1, 0, 1))
            && !this.seenBlockPos(pos.add(1, 0, 0))
            && !this.seenBlockPos(pos.add(-1, 0, 0))
            && !this.seenBlockPos(pos.add(0, 0, 1))
            && !this.seenBlockPos(pos.add(0, 0, -1))
            && pos != Minecraft.player.getPosition().add(0, 1, 0)
         ? ignoreNoSeen
         : true;
   }

   private boolean blockIsInRange(BlockPos pos, float range, boolean returnTrue) {
      int pointX = pos.getX() + ((double)pos.getX() < this.playerVec3dPos().xCoord ? 1 : 0);
      int pointY = pos.getY() + ((double)pos.getY() < this.playerVec3dPos().yCoord + (double)Minecraft.player.getEyeHeight() ? 1 : 0);
      int pointZ = pos.getZ() + ((double)pos.getZ() < this.playerVec3dPos().zCoord ? 1 : 0);
      double xDifference = (double)pointX - this.playerVec3dPos().xCoord;
      double yDifference = (double)pointY - (this.playerVec3dPos().yCoord + (double)Minecraft.player.getEyeHeight());
      double zDifference = (double)pointZ - this.playerVec3dPos().zCoord;
      return returnTrue || Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference) < (double)range;
   }

   private boolean canBreakBlock(BlockPos pos, String mode) {
      IBlockState state = mc.world.getBlockState(pos);
      Block block = state.getBlock();
      Material mat = state.getMaterial();
      switch (mode) {
         case "All":
            return true;
         case "Sand":
            return block instanceof BlockSand;
         case "Ores":
            return block instanceof BlockOre || block == Blocks.LIT_REDSTONE_ORE || block == Blocks.REDSTONE_ORE;
         case "Wooden":
            return mat == Material.WOOD;
         case "Stones":
            return mat == Material.ROCK && !(block instanceof BlockOre) && block != Blocks.LIT_REDSTONE_ORE && block != Blocks.REDSTONE_ORE;
         case "Ferma":
            return block instanceof BlockCrops crop && crop.isMaxAge(state)
               || block == Blocks.REEDS
                  && (
                     !Minecraft.player.capabilities.isFlying
                           && mc.world.getBlockState(pos.up()).getBlock() == Blocks.REEDS
                           && mc.world.getBlockState(pos.down()).getBlock() != Blocks.REEDS
                        || Minecraft.player.capabilities.isFlying
                           && mc.world.getBlockState(pos.up()).getBlock() == Blocks.REEDS
                           && mc.world.getBlockState(pos.down()).getBlock() != Blocks.REEDS
                  )
               || block instanceof BlockNetherWart && state.getValue(BlockNetherWart.AGE) == 3
               || (block == Blocks.MELON_BLOCK || block == Blocks.PUMPKIN || block instanceof BlockCocoa cocoa && state.getValue(BlockCocoa.AGE) == 2)
                  && this.BreakFermBlocks.getBool();
         case "Bed":
            return mc.world
               .getLoadedTileEntityList()
               .stream()
               .map(tile -> tile instanceof TileEntityBed ? (TileEntityBed)tile : null)
               .filter(Objects::nonNull)
               .anyMatch(bed -> bed.getPos() != null && bed.getPos().equals(pos));
         case "Web":
            return block == Blocks.WEB;
         case "Leaves":
            return block instanceof BlockLeaves;
         default:
            return false;
      }
   }

   private List<BlockPos> getTargetBlocks(int maxCount, Vec3d playerPos, float range, boolean ignoreWalls, boolean checkDistance, String mode) {
      this.positions.clear();

      for (BlockPos position : this.positionsZone(playerPos, range + 1.0F)) {
         IBlockState state = mc.world.getBlockState(position);
         Block block = state.getBlock();
         if (this.blockIsInRange(position, range - 0.3F, checkDistance)
            && !this.isUnbreakebleBlock(block)
            && this.canSeenBlock(position, ignoreWalls)
            && this.canBreakBlock(position, mode)
            && this.positions.size() < maxCount
            && block != Blocks.AIR) {
            this.positions.add(position);
         }
      }

      return this.positions;
   }

   private float getRange() {
      return this.CustomRange.getBool() ? Math.max(this.Range.getFloat(), 0.0F) : 5.0F;
   }

   private void setTargetPositions(Vec3d playerPos, float range, boolean ignoreWalls, boolean checkDistance, int maxPosesCount) {
      this.targetedPoses = this.getTargetBlocks(maxPosesCount, playerPos, range, ignoreWalls, checkDistance, this.Target.currentMode);
      targetedPosition = this.targetedPoses != null && !this.targetedPoses.isEmpty() && this.targetedPoses.get(0) != null ? this.targetedPoses.get(0) : null;
   }

   private BlockPos getTargetedPosition() {
      return targetedPosition;
   }

   private BlockPos getRenderPosition() {
      return renderPosition;
   }

   private void processBreakBlock(List<BlockPos> poses) {
      if (!poses.isEmpty()) {
         poses.forEach(pos -> {
            if (pos != null) {
               EnumFacing face = BlockUtils.getPlaceableSide(pos);
               if (face == null) {
                  face = EnumFacing.UP;
               }

               face = face.getOpposite();
               if (!this.IgnoreWalls.getBool() && this.Rotations.getBool() && mc.objectMouseOver != null) {
                  float prevYaw = Minecraft.player.rotationYaw;
                  float prevPitch = Minecraft.player.rotationPitch;
                  Minecraft.player.rotationYaw = this.lastRYaw;
                  Minecraft.player.rotationPitch = this.lastRPitch;
                  mc.entityRenderer.getMouseOver(1.0F);
                  if (mc.objectMouseOver != null) {
                     face = mc.objectMouseOver.sideHit;
                     pos = mc.objectMouseOver.getBlockPos();
                  }

                  mc.entityRenderer.getMouseOver(1.0F);
                  Minecraft.player.rotationYaw = prevYaw;
                  Minecraft.player.rotationPitch = prevPitch;
               }

               if (face != null && pos != null) {
                  if (this.isBedWarsModeDoes()) {
                     BlockPos finalPos = pos;
                     EnumFacing finalFace = face;
                     Runnable breaking = () -> {
                        mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, finalPos, finalFace));
                        mc.getConnection().sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
                        mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, finalPos, finalFace), 50);
                        mc.getConnection().sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND), 50);
                     };
                     if (Minecraft.player.onGround) {
                        if (!Minecraft.player.isJumping()) {
                           Minecraft.player.jump();
                        }
                     } else if (Minecraft.player.fallDistance == 0.0F) {
                        breaking.run();
                        mc.playerController.onPlayerDestroyBlock(pos);
                        mc.timer.tempSpeed = 0.5;
                        Timer.forceTimer(0.5F);
                        this.toggle();
                        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ENDERDRAGON_HURT, 1.0F));
                        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ENDERDRAGON_HURT, 1.0F));
                        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ENDERDRAGON_HURT, 1.0F));
                        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_ENDERDRAGON_HURT, 5.0F));
                     }

                     return;
                  }

                  if (mc.playerController.onPlayerDamageBlock(pos, face)) {
                     Minecraft.player.swingArm(EnumHand.MAIN_HAND);
                  }
               }
            }
         });
      }
   }

   private void rotations(EventPlayerMotionUpdate e, BlockPos pos) {
      if (pos != null) {
         AxisAlignedBB blockAABB = new AxisAlignedBB(pos);
         if (mc.world != null) {
            blockAABB = mc.world.getBlockState(pos).getSelectedBoundingBox(mc.world, pos);
         }

         e.setYaw(RotationUtil.getMatrixRotations4BlockPos(pos.add(0.5, (blockAABB.maxY - blockAABB.minY) / 2.0, 0.5))[0]);
         e.setPitch(RotationUtil.getMatrixRotations4BlockPos(pos.add(0.5, (blockAABB.maxY - blockAABB.minY) / 3.0, 0.5))[1]);
         Minecraft.player.rotationYawHead = e.getYaw();
         Minecraft.player.renderYawOffset = e.getYaw();
         Minecraft.player.rotationPitchHead = e.getPitch();
         if (this.ClientLook.getBool()) {
            Minecraft.player.rotationYaw = e.getYaw();
            Minecraft.player.rotationPitch = e.getPitch();
         }

         this.lastRYaw = e.getYaw();
         this.lastRPitch = e.getPitch();
      }
   }

   @Override
   public void onUpdate() {
      boolean ignoreWalls = this.IgnoreWalls.getBool();
      boolean checkDistance = false;
      int maxBlocksSame = this.MaxBlocksShare.getInt();
      this.setTargetPositions(this.playerVec3dPos(), this.getRange(), ignoreWalls, false, maxBlocksSame);
      this.processBreakBlock(this.targetedPoses);
   }

   @EventTarget
   public void onUpdate(EventPlayerMotionUpdate e) {
      if (this.Rotations.getBool() && !PotionThrower.get.forceThrow && !PotionThrower.get.callThrowPotions) {
         this.rotations(e, this.getTargetedPosition());
         this.yaw = e.getYaw();
      } else {
         this.yaw = -10001.0F;
      }
   }

   @EventTarget
   public void onStrafeSide(EventRotationStrafe e) {
      if (this.SilentMoveRot.getBool() && this.Rotations.getBool() && this.getTargetedPosition() != null && this.yaw != -10001.0F) {
         e.setYaw(this.yaw);
      }
   }

   @EventTarget
   public void onJumpSide(EventRotationJump e) {
      if (this.SilentMoveRot.getBool() && this.Rotations.getBool() && this.getTargetedPosition() != null && this.yaw != -10001.0F) {
         e.setYaw(this.yaw);
      }
   }

   @EventTarget
   public void onRender3D(Event3D e) {
      RenderUtils.setup3dForBlockPos(() -> {
         this.drawHittingProgress();
         GL11.glEnable(2929);
         GL11.glDisable(2884);
         GL11.glDepthMask(false);
         this.drawZone(this.getRange() - 0.315F);
         GL11.glDepthMask(false);
         GL11.glEnable(2884);
      }, true);
   }
}
