package ru.govno.client.module.modules;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.InventoryHelper;
import ru.govno.client.utils.InventoryUtil;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.TimerHelper;

public class Spider extends Module {
   ModeSettings Modes;
   BoolSettings Fast;
   TimerHelper timer = new TimerHelper();
   boolean callPlace = false;

   public Spider() {
      super("Spider", 0, Module.Category.MOVEMENT);
      this.settings.add(this.Modes = new ModeSettings("Modes", "MatrixOld", this, new String[]{"MatrixOld", "MatrixNew", "Region", "GrimAc"}));
      this.settings.add(this.Fast = new BoolSettings("Fast", true, this, () -> this.Modes.getMode().equalsIgnoreCase("Region")));
      this.setDemand(0, 1);
   }

   boolean isBadOver(BlockPos pos) {
      Block block = pos == null ? null : mc.world.getBlockState(pos).getBlock();
      List<Integer> badBlockIDs = Arrays.asList(
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
      return !Minecraft.player.isSneaking() && block != null && badBlockIDs.stream().anyMatch(id -> Block.getIdFromBlock(block) == id);
   }

   @Override
   public void onMovement() {
      if (this.Modes.getMode().equalsIgnoreCase("GrimAc") && Minecraft.player.motionY < -0.07) {
         if (InventoryHelper.doesHotbarHaveBlock()
            && mc.playerController.getCurrentGameType() != GameType.ADVENTURE
            && mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
            BlockPos pos = new BlockPos(
               Minecraft.player.posX + (Minecraft.player.posX - Minecraft.player.lastReportedPosX),
               Minecraft.player.posY - 0.9,
               Minecraft.player.posZ + (Minecraft.player.posZ - Minecraft.player.lastReportedPosZ)
            );
            int slot = Minecraft.player.inventory.currentItem;
            int toSlot = InventoryUtil.getAnyBlockInHotbar();
            Minecraft.player.inventory.currentItem = toSlot;
            if (slot != toSlot) {
               mc.playerController.syncCurrentPlayItem();
            }

            EnumFacing enumFace = BlockUtils.getPlaceableSide(pos);
            if (enumFace == null) {
               return;
            }

            Vec3d placeFaceVec = new Vec3d(pos)
               .addVector(0.5, 0.5, 0.5)
               .addVector((double)enumFace.getFrontOffsetX() * 0.5, (double)enumFace.getFrontOffsetY() * 0.5, (double)enumFace.getFrontOffsetZ() * 0.5);
            float[] rotate = RotationUtil.getNeededFacing(placeFaceVec, false, Minecraft.player, false);
            float prevYaw = Minecraft.player.rotationYaw;
            float prevPitch = Minecraft.player.rotationPitch;
            Minecraft.player.rotationYaw = rotate[0];
            Minecraft.player.rotationPitch = rotate[1];
            Minecraft.player.rotationYawHead = rotate[0];
            Minecraft.player.renderYawOffset = rotate[0];
            Minecraft.player.rotationPitchHead = rotate[1];
            mc.entityRenderer.getMouseOver(0.0F);
            boolean badOver = mc.objectMouseOver.getBlockPos() != null
               && mc.objectMouseOver.getBlockPos() != null
               && this.isBadOver(mc.objectMouseOver.getBlockPos())
               && !Minecraft.player.isSneaking();
            if (badOver) {
               mc.gameSettings.keyBindSneak.pressed = true;
               Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
            } else if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
               if (!mc.playerController
                  .processRightClickBlock(
                     Minecraft.player, mc.world, mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec, EnumHand.MAIN_HAND
                  )
                  .equals(EnumActionResult.SUCCESS)) {
                  mc.playerController
                     .processRightClickBlock(Minecraft.player, mc.world, pos.offset(enumFace), enumFace.getOpposite(), placeFaceVec, EnumHand.MAIN_HAND);
               }

               Minecraft.player.swingArm();
               mc.gameSettings.keyBindSneak.pressed = false;
            }

            Minecraft.player.rotationYaw = prevYaw;
            Minecraft.player.rotationPitch = prevPitch;
            mc.entityRenderer.getMouseOver(0.0F);
            Minecraft.player.multiplyMotionXZ(0.0F);
            Minecraft.player.toCancelSprintTicks = 2;
            Minecraft.player.inventory.currentItem = slot;
            Minecraft.player.fallDistance = 0.0F;
            this.callPlace = false;
         } else {
            Client.msg("§f§lModules:§r §7[§lSpider§r§7]: " + this.trouble() + ".", false);
            this.toggle(false);
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.Modes.getMode().equalsIgnoreCase("MatrixOld")) {
         Minecraft.player.jumpTicks = 0;
         if (Minecraft.player.isCollidedHorizontally && Minecraft.player.ticksExisted % 4 == 0) {
            Minecraft.player.onGround = true;
            Minecraft.player.jump();
            Minecraft.player.motionY = 0.42;
         }
      }
   }

   String trouble() {
      if (mc.playerController.getCurrentGameType() == GameType.ADVENTURE) {
         return "§f§lModules:§r §7[§lSpider§r§7]: У вас гм 2.";
      } else if (mc.playerController.getCurrentGameType() == GameType.SPECTATOR) {
         return "§f§lModules:§r §7[§lSpider§r§7]: У вас гм 3.";
      } else {
         return !InventoryHelper.doesHotbarHaveBlock()
            ? "§f§lModules:§r §7[§lSpider§r§7]: У вас нет блока."
            : "§f§lModules:§r §7[§lSpider§r§7]: Что-то не так...";
      }
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate e) {
      if (this.actived && this.Modes.getMode().equalsIgnoreCase("GrimAc")) {
         e.setYaw(Minecraft.player.rotationYawHead);
         e.setPitch(Minecraft.player.rotationPitchHead);
      }

      if (this.Modes.getMode().equalsIgnoreCase("Region") && this.actived) {
         if (mc.playerController.getCurrentGameType() != GameType.ADVENTURE
            && mc.playerController.getCurrentGameType() != GameType.SPECTATOR
            && InventoryHelper.doesHotbarHaveBlock()) {
            int block = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack s = Minecraft.player.inventory.getStackInSlot(i);
               if (s.getItem() instanceof ItemBlock) {
                  block = i;
                  break;
               }
            }

            if (block != -1) {
               int slot = Minecraft.player.inventory.currentItem;
               if (this.timer.hasReached(50.0) && Minecraft.player.isCollidedHorizontally) {
                  try {
                     if (block != -1) {
                        Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(block));
                        float prevPitch = Minecraft.player.rotationPitch;
                        Minecraft.player.rotationPitch = -60.0F;
                        mc.entityRenderer.getMouseOver(1.0F);
                        Vec3d facing = mc.objectMouseOver.hitVec;
                        BlockPos stack = mc.objectMouseOver.getBlockPos();
                        float f = (float)(facing.xCoord - (double)stack.getX());
                        float f1 = (float)(facing.yCoord - (double)stack.getY());
                        float f2 = (float)(facing.zCoord - (double)stack.getZ());
                        Minecraft.player
                           .connection
                           .sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, BlockPos.ORIGIN.down(61), EnumFacing.UP));
                        Minecraft.player
                           .connection
                           .sendPacket(new CPacketPlayerTryUseItemOnBlock(stack, mc.objectMouseOver.sideHit, EnumHand.MAIN_HAND, f, f1, f2));
                        Minecraft.player.rotationPitch = prevPitch;
                        mc.entityRenderer.getMouseOver(1.0F);
                        Minecraft.player.connection.sendPacket(new CPacketHeldItemChange(Minecraft.player.inventory.currentItem));
                        if (this.Fast.getBool() && Minecraft.player.onGround) {
                           Timer.forceTimer(2.0F);
                        }

                        if (Minecraft.player.fallDistance != 0.0F) {
                           Minecraft.player.connection.sendPacket(new CPacketPlayer(true));
                           Minecraft.player.fallDistance = 0.0F;
                        }

                        e.ground = true;
                        Minecraft.player.onGround = true;
                        Minecraft.player.isCollidedVertically = true;
                        Minecraft.player.isCollidedHorizontally = true;
                        Minecraft.player.isAirBorne = true;
                        Minecraft.player.motionY = this.Fast.getBool() ? 0.4204 : 0.4198;
                        this.timer.reset();
                     }
                  } catch (Exception var10) {
                     var10.printStackTrace();
                  }
               }
            }
         } else {
            this.toggle(false);
            Client.msg("§f§lModules:§r §7[§lSpider§r§7]: " + this.trouble() + ".", false);
         }
      }

      if (this.Modes.getMode().equalsIgnoreCase("MatrixNew")
         && this.actived
         && Minecraft.player.isCollidedHorizontally
         && !Minecraft.player.isInWater()
         && !Minecraft.player.isInLava()
         && !Minecraft.player.isInWeb) {
         e.x = e.x + (double)(MathHelper.sin(MathHelper.toRadians(Minecraft.player.rotationYaw)) * 1.0E-11F);
         e.z = e.z - (double)(MathHelper.cos(MathHelper.toRadians(Minecraft.player.rotationYaw)) * 1.0E-11F);
         Minecraft.player.isAirBorne = !Minecraft.player.onGround;
         if (Entity.Getmotiony < 0.0) {
            Entity.motiony = Float.MIN_VALUE;
         }

         if (Minecraft.player.ticksExisted % 2 == 0) {
            e.ground = true;
            Minecraft.player.motionY = 0.42;
         }
      }
   }

   boolean canCollideDown() {
      return this.Modes.getMode().equalsIgnoreCase("GrimAc");
   }

   @Override
   public void onToggled(boolean actived) {
      if (!actived && this.Modes.getMode().equalsIgnoreCase("MatrixNew")) {
         Minecraft.player.speedInAir = 0.02F;
         mc.timer.speed = 1.0;
         Minecraft.player.stepHeight = 0.6F;
      }

      if (!actived && this.Modes.getMode().equalsIgnoreCase("Grim")) {
         mc.gameSettings.keyBindSneak.pressed = false;
      }

      super.onToggled(actived);
   }
}
