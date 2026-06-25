package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventSendPacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;

public class OnFalling extends Module {
   BoolSettings SneakBack;
   BoolSettings FallBoost;
   BoolSettings NoDamage;
   ModeSettings BackMode;
   ModeSettings NoDmgMode;
   boolean fall = false;
   private double egX;
   private double egY;
   private double egZ;
   private final TimerHelper afterGroundTime = new TimerHelper();
   private float fallDistance;
   int torCheck = 0;
   private boolean torHasJes;
   BlockPos lastPlacedWater;
   private final List<Integer> clickableBlockIDs = Arrays.asList(
      96,
      167,
      54,
      130,
      146,
      58,
      64,
      71,
      193,
      194,
      195,
      196,
      197,
      324,
      330,
      427,
      428,
      429,
      430,
      431,
      154,
      61,
      23,
      158,
      145,
      69,
      107,
      187,
      186,
      185,
      184,
      183,
      107,
      116,
      84,
      356,
      404,
      151,
      25,
      219,
      220,
      221,
      222,
      223,
      224,
      225,
      226,
      227,
      228,
      229,
      230,
      231,
      232,
      233,
      234,
      389,
      379,
      380,
      138,
      321,
      323,
      77,
      143,
      379
   );

   public OnFalling() {
      super("OnFalling", 0, Module.Category.PLAYER);
      this.settings.add(this.SneakBack = new BoolSettings("SneakBack", true, this));
      this.settings
         .add(this.BackMode = new ModeSettings("BackMode", "Matrix", this, new String[]{"Matrix", "OldGround", "Vulcan"}, () -> this.SneakBack.getBool()));
      this.settings.add(this.FallBoost = new BoolSettings("FallBoost", true, this));
      this.settings.add(this.NoDamage = new BoolSettings("NoDamage", true, this));
      this.settings
         .add(
            this.NoDmgMode = new ModeSettings(
               "NoDmgMode", "MatrixOld", this, new String[]{"MatrixOld", "MatrixNew", "NCP", "PreGround", "WaterBucket"}, () -> this.NoDamage.getBool()
            )
         );
      this.setDemand(0, 0);
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate e) {
      if (this.actived) {
         if (!e.onGround()) {
            this.afterGroundTime.reset();
         }

         long timeOfGround = this.afterGroundTime.getTime();
         if (timeOfGround > 0L) {
            this.fallDistance = 0.0F;
         } else {
            if (Minecraft.player.fallDistance != 0.0F) {
               this.fallDistance = Minecraft.player.fallDistance;
            }

            if (timeOfGround == 0L && Minecraft.player.hurtTime == 9) {
               this.fallDistance = 0.0F;
            }
         }

         if (this.torHasJes && this.torCheck == 0) {
            JesusSpeed.get.toggleSilent(true);
            this.torHasJes = false;
         }

         if (this.NoDmgMode.getMode().equalsIgnoreCase("WaterBucket")) {
            if (this.torCheck != 0) {
               BlockPos cur = this.getCurBucketPlacePos(this.torCheck == 1);
               if (cur != null) {
                  float[] rotate = RotationUtil.getVecNeeded(
                     new Vec3d(cur).addVector(0.5, this.torCheck == 1 ? 1.9 : 1.0, 0.5), Minecraft.player.getPositionEyes(mc.getRenderPartialTicks())
                  );
                  e.setYaw(rotate[0]);
                  e.setPitch(rotate[1]);
                  Minecraft.player.rotationYawHead = rotate[0];
                  Minecraft.player.rotationPitchHead = rotate[1];
               }

               this.placeBucketDown(this.torCheck == 1);
               Minecraft.player.fall(100.0F, 0.0F);
               if (this.torCheck == 1) {
                  this.torCheck = 0;
               } else if (cur == null
                  || mc.world.getBlockState(cur).getBlock() != Blocks.WATER
                  || Minecraft.player.getDistanceAtEye((double)cur.getX() + 0.5, (double)cur.getY() + 1.0, (double)cur.getZ() + 0.5) > 5.0) {
                  this.torCheck = 0;
               }
            }

            if (this.fallDistance < 2.0F) {
               if (this.fall) {
                  if (this.torCheck == 0) {
                     BlockPos curx = this.getCurBucketPlacePos(false);
                     if (curx != null) {
                        float[] rotate = RotationUtil.getVecNeeded(
                           new Vec3d(curx).addVector(0.5, 1.9, 0.5), Minecraft.player.getPositionEyes(mc.getRenderPartialTicks())
                        );
                        e.setYaw(rotate[0]);
                        e.setPitch(rotate[1]);
                        Minecraft.player.rotationYawHead = rotate[0];
                        Minecraft.player.rotationPitchHead = rotate[1];
                        this.torCheck = 2;
                     }
                  }

                  this.fall = false;
               }
            } else if (this.nextFallTickDamaged(0) && !this.fall) {
               if (this.torCheck == 0) {
                  BlockPos curx = this.getCurBucketPlacePos(true);
                  if (curx != null) {
                     float[] rotate = RotationUtil.getVecNeeded(
                        new Vec3d(curx).addVector(0.5, 1.0, 0.5), Minecraft.player.getPositionEyes(mc.getRenderPartialTicks())
                     );
                     e.setYaw(rotate[0]);
                     e.setPitch(rotate[1]);
                     Minecraft.player.rotationYawHead = rotate[0];
                     Minecraft.player.rotationPitchHead = rotate[1];
                     if (JesusSpeed.get.isActived()) {
                        this.torHasJes = true;
                        JesusSpeed.get.toggleSilent(false);
                     }

                     this.torCheck = 1;
                  }
               }

               Minecraft.player.fall(100.0F, 0.0F);
               this.fall = true;
            }
         }

         if (e.ground) {
            this.egX = Minecraft.player.posX;
            this.egY = Minecraft.player.posY;
            this.egZ = Minecraft.player.posZ;
         }
      }
   }

   private double getCollideYPosition(BlockPos pos) {
      double value = (double)(pos.getY() + 1);
      IBlockState state = mc.world.getBlockState(pos);
      AxisAlignedBB aabb = state.getSelectedBoundingBox(mc.world, pos);
      return aabb == null ? value : aabb.maxY;
   }

   private boolean isCollidablePos(BlockPos pos) {
      return !mc.world.getCollisionBoxes(null, new AxisAlignedBB(pos)).isEmpty();
   }

   private boolean isLiquidPos(BlockPos pos) {
      Material material = mc.world.getBlockState(pos).getMaterial();
      return material.isLiquid() && material.getMaterialMapColor() == MapColor.WATER;
   }

   private double presentFallDistance(double appendOnPreY, int ticksPre) {
      EntityPlayer self = Minecraft.player;
      double fd = (double)this.fallDistance;
      if (fd > 3.0) {
         double underY = self.posY;
         double posX = self.posX;
         double posY = underY + (self.posY - self.lastTickPosY);
         double posZ = self.posZ;

         for (double y = underY; y > 0.0; y--) {
            BlockPos pos = new BlockPos(posX, y, posZ);
            if (this.isCollidablePos(pos)) {
               BlockPos posUp = pos.up();
               if (this.isLiquidPos(posUp)) {
                  return 0.0;
               }

               underY = this.getCollideYPosition(pos) - 1.0;
               break;
            }
         }

         double groundDiff = Math.abs(posY - underY);
         double fallSpeed = MathUtils.clamp(Minecraft.player.posY - Minecraft.player.lastTickPosY, 0.0, 10.0 * mc.timer.speed) * appendOnPreY;
         fd = !(groundDiff < 10.0) || !(fallSpeed >= groundDiff / (double)ticksPre) && !(groundDiff < 1.0) ? 0.0 : fd + groundDiff;
      }

      return fd;
   }

   private boolean nextFallTickDamaged(int hpDamageMinTrigger) {
      return !Minecraft.player.capabilities.allowFlying
         && !Minecraft.player.capabilities.disableDamage
         && Minecraft.player.getFallDistanceDamage(this.presentFallDistance(10.0, 3)) > (float)hpDamageMinTrigger;
   }

   public static double getDistanceTofall() {
      for (int i = 0; i < 500; i++) {
         if (Speed.posBlock(Minecraft.player.posX, Minecraft.player.posY - (double)i, Minecraft.player.posZ)) {
            return (double)i;
         }
      }

      return 0.0;
   }

   @EventTarget
   public void onPacket(EventSendPacket event) {
      if (this.NoDamage.getBool() && this.fall && this.NoDmgMode.getMode().equalsIgnoreCase("MatrixNew")) {
         CPacketPlayer packet = (CPacketPlayer)event.getPacket();
         this.fall = false;
         packet.onGround = true;
         Minecraft.player.motionY = -0.0199F;
      }

      if (mc.timer.speed == 0.650000243527852 && Minecraft.player.ticksExisted % 2 != 0) {
         mc.timer.speed = 1.0;
      }
   }

   @Override
   public void onUpdate() {
      if (!FreeCam.get.actived && !Fly.get.actived && !ElytraBoost.get.actived) {
         if (mc.gameSettings.keyBindSneak.isKeyDown() && this.SneakBack.getBool() && Minecraft.player.fallDistance >= 3.3F) {
            if (this.BackMode.getMode().equalsIgnoreCase("OldGround")) {
               Minecraft.player.fallDistance = 0.0F;
               if (this.egX != 0.0 && this.egY != 0.0 && this.egZ != 0.0) {
                  Minecraft.player.setPosition(this.egX, this.egY, this.egZ);
                  this.egX = 0.0;
                  this.egY = 0.0;
                  this.egZ = 0.0;
                  Minecraft.player.motionX = 0.0;
                  Minecraft.player.motionZ = 0.0;
               } else {
                  Minecraft.player.setPosition(Minecraft.player.posX, Minecraft.player.posY + (double)Minecraft.player.height, Minecraft.player.posZ);
                  Minecraft.player.motionY = MoveMeHelp.getBaseJumpHeight();
                  Minecraft.player.motionY += 0.164157;
               }
            } else {
               boolean oldGravity = Minecraft.player.hasNoGravity();
               Minecraft.player.fallDistance = (float)((double)Minecraft.player.fallDistance - 0.2);
               Minecraft.player.onGround = true;
               Minecraft.player.motionY = -0.01F;
               Entity.motiony = Minecraft.player.motionY;
               Timer.forceTimer(0.2F);
               Minecraft.player.setNoGravity(oldGravity);
            }
         }

         if (mc.gameSettings.keyBindSneak.isKeyDown()
            && this.SneakBack.getBool()
            && Minecraft.player.fallDistance > 4.0F
            && this.BackMode.getMode().equalsIgnoreCase("Vulcan")) {
            Minecraft.player.onGround = true;
            Entity.motiony = -Entity.Getmotiony;
            Minecraft.player.fallDistance = 0.0F;
         }

         if (Minecraft.player.posY > 0.0) {
            if (this.FallBoost.getBool() && getDistanceTofall() > 5.0) {
               if ((int)Minecraft.player.fallDistance >= 4 && Minecraft.player.fallDistance < 10.0F) {
                  Minecraft.player.connection.sendPacket(new CPacketPlayer(true));
                  Minecraft.player.fallDistance += 10.0F;
               }

               if (Minecraft.player.fallDistance > 5.0F && Minecraft.player.motionY < 0.0 && Minecraft.player.hurtTime != 0) {
                  Minecraft.player.connection.sendPacket(new CPacketPlayer(false));
                  Minecraft.player.motionY = -10.0;
               }
            }

            if (this.NoDamage.getBool()) {
               if (Minecraft.player.fallDistance > (float)(Minecraft.player.getHealth() > 6.0F ? 3 : 2)
                  && this.NoDmgMode.getMode().equalsIgnoreCase("MatrixOld")) {
                  Minecraft.player.fallDistance = (float)(Math.random() * 1.0E-12);
                  Minecraft.player.connection.sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, true));
                  Minecraft.player.jumpMovementFactor = 0.0F;
               }

               if (Minecraft.player.fallDistance > 5.0F && this.NoDmgMode.getMode().equalsIgnoreCase("MatrixNew")) {
                  Minecraft.player.fallDistance = 0.0F;
                  mc.timer.speed = 0.650000243527852;
                  this.fall = true;
               }

               if (mc.timer.speed == 0.650000243527852 && this.NoDmgMode.getMode().equalsIgnoreCase("MatrixNew") && Minecraft.player.ticksExisted % 4 == 0) {
                  mc.timer.speed = 1.0;
               }

               if (Minecraft.player.fallDistance >= 3.0F && this.NoDmgMode.getMode().equalsIgnoreCase("NCP")) {
                  Minecraft.player.onGround = false;
                  Minecraft.player.motionY = 0.02F;

                  for (int i = 0; i < 30; i++) {
                     Minecraft.player
                        .connection
                        .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 110000.0, Minecraft.player.posZ, false));
                     Minecraft.player
                        .connection
                        .sendPacket(new CPacketPlayer.Position(Minecraft.player.posX, Minecraft.player.posY + 2.0, Minecraft.player.posZ, false));
                  }

                  Minecraft.player.connection.sendPacket(new CPacketPlayer(true));
                  Minecraft.player.fallDistance = 0.0F;
               }

               if (Minecraft.player.fallDistance >= 3.0F
                  && Minecraft.player.motionY < -0.4
                  && Minecraft.player.motionY > -1.0
                  && this.NoDmgMode.getMode().equalsIgnoreCase("PreGround")
                  && mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox).isEmpty()
                  && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.999)).isEmpty()) {
                  Minecraft.player.fallDistance = 0.0F;
                  Minecraft.player
                     .forceUpdatePlayerServerPosition(
                        Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ, Minecraft.player.rotationYaw, Minecraft.player.rotationPitch, true
                     );
               }
            } else {
               if (mc.timer.speed == 0.650000243527852) {
                  mc.timer.speed = 1.0;
               }

               this.fall = false;
            }
         }
      }
   }

   private BlockPos getCurBucketPlacePos(boolean putWater) {
      BlockPos targetPlacePos = this.lastPlacedWater;
      if (putWater) {
         List<RayTraceResult> rays = new ArrayList<>();
         float offXZ = Minecraft.player.width / 2.0F - 1.0E-6F;
         float motionTicksPredict = 1.0F;
         Vec3d virtSelfPos = Minecraft.player
            .getPositionVector()
            .addVector(
               (Minecraft.player.posX - Minecraft.player.lastTickPosX) * (double)motionTicksPredict,
               0.0,
               (Minecraft.player.posZ - Minecraft.player.lastTickPosZ) * (double)motionTicksPredict
            );
         rays.add(
            mc.world
               .rayTraceBlocks(
                  new Vec3d(Minecraft.player.posX, Minecraft.player.posY - 1.0E-5, Minecraft.player.posZ),
                  new Vec3d(Minecraft.player.posX, Minecraft.player.posY - 6.0, Minecraft.player.posZ),
                  false,
                  true,
                  false
               )
         );
         rays.add(
            mc.world
               .rayTraceBlocks(
                  new Vec3d(virtSelfPos.xCoord - (double)offXZ, virtSelfPos.yCoord - 1.0E-5, virtSelfPos.zCoord - (double)offXZ),
                  new Vec3d(virtSelfPos.xCoord - (double)offXZ, virtSelfPos.yCoord - 6.0, virtSelfPos.zCoord - (double)offXZ),
                  false,
                  true,
                  false
               )
         );
         rays.add(
            mc.world
               .rayTraceBlocks(
                  new Vec3d(virtSelfPos.xCoord + (double)offXZ, virtSelfPos.yCoord - 1.0E-5, virtSelfPos.zCoord - (double)offXZ),
                  new Vec3d(virtSelfPos.xCoord + (double)offXZ, virtSelfPos.yCoord - 6.0, virtSelfPos.zCoord - (double)offXZ),
                  false,
                  true,
                  false
               )
         );
         rays.add(
            mc.world
               .rayTraceBlocks(
                  new Vec3d(virtSelfPos.xCoord + (double)offXZ, virtSelfPos.yCoord - 1.0E-5, virtSelfPos.zCoord + (double)offXZ),
                  new Vec3d(virtSelfPos.xCoord + (double)offXZ, virtSelfPos.yCoord - 6.0, virtSelfPos.zCoord + (double)offXZ),
                  false,
                  true,
                  false
               )
         );
         rays.add(
            mc.world
               .rayTraceBlocks(
                  new Vec3d(virtSelfPos.xCoord - (double)offXZ, virtSelfPos.yCoord - 1.0E-5, virtSelfPos.zCoord + (double)offXZ),
                  new Vec3d(virtSelfPos.xCoord - (double)offXZ, virtSelfPos.yCoord - 6.0, virtSelfPos.zCoord + (double)offXZ),
                  false,
                  true,
                  false
               )
         );

         for (RayTraceResult ray : rays.stream()
            .filter(rayx -> rayx != null && rayx.hitVec != null)
            .sorted(Comparator.comparing(a -> -a.hitVec.distanceTo(Minecraft.player.getPositionEyes(1.0F))))
            .toList()) {
            if (ray != null && ray.getBlockPos() != null) {
               return ray.getBlockPos();
            }
         }
      }

      return targetPlacePos;
   }

   private boolean placeBucketDown(boolean putWater) {
      if (mc.world != null && Minecraft.player != null) {
         BlockPos targetPlacePos = this.getCurBucketPlacePos(putWater);
         if (targetPlacePos != null) {
            Vec3d toRot = new Vec3d((double)targetPlacePos.getX() + 0.5, (double)targetPlacePos.getY() + 1.0, (double)targetPlacePos.getZ() + 0.5);
            if (mc.world.getBlockState(targetPlacePos.up()).getBlock() == Blocks.WATER) {
               toRot.yCoord += 0.85F;
            }

            float[] rotate = RotationUtil.getVecNeeded(toRot, Minecraft.player.getPositionEyes(1.0F));
            ScaffWalk.get.updateObjectMouseOverSilent(rotate, () -> {
               this.placeOnPosBucket(putWater, targetPlacePos);
               if (putWater) {
                  this.lastPlacedWater = targetPlacePos.down();
               } else {
                  this.lastPlacedWater = null;
               }
            });
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isClickableBlock(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         Block block = mc.world.getBlockState(pos).getBlock();
         return !Minecraft.player.isSneaking() && this.clickableBlockIDs.stream().anyMatch(id -> Block.getIdFromBlock(block) == id);
      }
   }

   private void placeOnPosBucket(boolean water, BlockPos pos) {
      Item currentItem = water ? Items.WATER_BUCKET : Items.BUCKET;
      int bucketSlot = -999;
      if (Minecraft.player.getHeldItemOffhand().getItem() == currentItem) {
         bucketSlot = -2;
      } else {
         for (int i = 0; i < 44; i++) {
            if (Minecraft.player.inventory.getStackInSlot(i).getItem() == currentItem) {
               bucketSlot = i;
               break;
            }
         }
      }

      this.switchWithActionsPlace(bucketSlot, pos);
   }

   private void rClickPos(BlockPos pos, EnumHand hand) {
      boolean willBeInteract = this.isClickableBlock(pos);
      if (willBeInteract) {
         mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
         Minecraft.player.movementInput.sneak = true;
         Minecraft.player.setSneaking(true);
      }

      mc.getConnection().sendPacket(new CPacketPlayerTryUseItem(hand));
      if (willBeInteract) {
         mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
      }
   }

   private void switchWithActionsPlace(int slotTo, BlockPos pos) {
      if (slotTo <= -1) {
         if (slotTo == -2) {
            this.rClickPos(pos, EnumHand.OFF_HAND);
         }
      } else {
         boolean invSwap = slotTo > 8;
         if (invSwap) {
            mc.playerController.windowClick(0, slotTo, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player);
            mc.playerController.syncCurrentPlayItem();
            this.rClickPos(pos, EnumHand.MAIN_HAND);
            mc.playerController.windowClickMemory(0, slotTo, Minecraft.player.inventory.currentItem, ClickType.SWAP, Minecraft.player, 100);
         } else {
            int handSlot = Minecraft.player.inventory.currentItem;
            Minecraft.player.inventory.currentItem = slotTo;
            mc.playerController.syncCurrentPlayItem();
            this.rClickPos(pos, EnumHand.MAIN_HAND);
            Minecraft.player.inventory.currentItem = handSlot;
            mc.playerController.syncCurrentPlayItem();
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived) {
         this.fall = false;
      }

      super.onToggled(actived);
   }
}
