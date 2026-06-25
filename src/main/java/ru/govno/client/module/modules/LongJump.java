package ru.govno.client.module.modules;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import ru.govno.client.Client;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventReceivePacket;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Wrapper;
import ru.govno.client.utils.Combat.EntityUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Movement.MovementHelper;

public class LongJump extends Module {
   public static boolean isFallDamage;
   private int ticks;
   private double packetMotionY;
   private float speed;
   public static boolean doSpeed;
   public static boolean doBow;
   public static boolean stopBow;
   public static Item oldSlot = null;
   private final TimerHelper timerHelper = new TimerHelper();
   public static LongJump get;
   public ModeSettings Type;
   public BoolSettings AutoBow;
   public BoolSettings Instant;
   public BoolSettings UltraBoost;
   TimerHelper wait = new TimerHelper();
   boolean toDo = false;
   BlockPos state;
   boolean flag;
   int flagBoostTicks = 0;
   private int ticksGlide;
   private boolean postBoost;

   public LongJump() {
      super("LongJump", 0, Module.Category.MOVEMENT);
      this.settings
         .add(
            this.Type = new ModeSettings(
               "Type",
               "LongJump",
               this,
               new String[]{
                  "LongJump", "BowBoost", "Solid", "DamageFly", "InstantLong", "FlagBoost", "Matrix&AACWait", "Matrix&AACDestruct", "Glide", "Matrix7"
               }
            )
         );
      this.settings.add(this.AutoBow = new BoolSettings("AutoBow", true, this, () -> this.Type.currentMode.equalsIgnoreCase("DamageFly")));
      this.settings
         .add(
            this.Instant = new BoolSettings(
               "Instant",
               false,
               this,
               () -> this.Type.currentMode.equalsIgnoreCase("Matrix&AACDestruct")
                     || this.Type.currentMode.equalsIgnoreCase("Glide")
                     || this.Type.currentMode.equalsIgnoreCase("Matrix7")
            )
         );
      this.settings.add(this.UltraBoost = new BoolSettings("UltraBoost", false, this, () -> this.Type.currentMode.equalsIgnoreCase("Matrix7")));
      this.setDemand(0, 1);
      get = this;
   }

   private boolean stackIsBlockStack(ItemStack stack) {
      return stack != null && stack.getItem() instanceof ItemBlock;
   }

   private void silentJumpRotDown(Runnable centre) {
      double y = Minecraft.player.posY;
      double[] offsets = new double[]{0.42F, 0.7531999805212024, 1.0013359791121417, 1.1661092609382138};
      if (!Minecraft.player.onGround) {
         new CPacketPlayer.Position(Minecraft.player.posX, y, Minecraft.player.posZ, true);
      }

      for (double offset : offsets) {
         mc.getConnection()
            .sendPacket(
               (Packet<?>)(offset == offsets[offsets.length - 1]
                  ? new CPacketPlayer.PositionRotation(Minecraft.player.posX, y + offset, Minecraft.player.posZ, Minecraft.player.rotationYaw, 90.0F, false)
                  : new CPacketPlayer.Position(Minecraft.player.posX, y + offset, Minecraft.player.posZ, false))
            );
         Minecraft.player.setPosition(Minecraft.player.posX, y + offset, Minecraft.player.posZ);
      }

      float prevPitch = Minecraft.player.rotationPitch;
      Minecraft.player.rotationPitch = 90.0F;
      Minecraft.player.rotationPitchHead = 90.0F;
      mc.entityRenderer.getMouseOver(1.0F);
      centre.run();
      Minecraft.player.rotationPitch = prevPitch;
      mc.entityRenderer.getMouseOver(1.0F);
   }

   private void selfPlace(EnumHand hand) {
      if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null && mc.objectMouseOver.hitVec != null && mc.objectMouseOver.sideHit != null) {
         if (!Minecraft.player.isSneaking()) {
            Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
         }

         mc.playerController
            .processRightClickBlock(Minecraft.player, mc.world, mc.objectMouseOver.getBlockPos(), mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec, hand);
         if (!Minecraft.player.isSneaking()) {
            Minecraft.player.connection.sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
         }
      }
   }

   private boolean sentBlockPlacement(boolean canUseInventory) {
      int oldSlot = Minecraft.player.inventory.currentItem;
      int currentSlot = -1;
      EnumHand placeHand = null;
      ItemStack offStack = Minecraft.player.getHeldItemOffhand();
      if (this.stackIsBlockStack(offStack)) {
         placeHand = EnumHand.OFF_HAND;
      } else {
         ItemStack mainStack = Minecraft.player.getHeldItemMainhand();
         if (this.stackIsBlockStack(mainStack)) {
            placeHand = EnumHand.MAIN_HAND;
         }
      }

      if (placeHand == null) {
         for (int slot = 0; slot < (canUseInventory ? 44 : 8); slot++) {
            ItemStack stackInSlot = Minecraft.player.inventory.getStackInSlot(slot);
            if (this.stackIsBlockStack(stackInSlot)) {
               currentSlot = slot;
               placeHand = EnumHand.MAIN_HAND;
               break;
            }
         }

         if (placeHand == EnumHand.MAIN_HAND && currentSlot != -1) {
            int finalCurrentSlot = currentSlot;
            EnumHand finalPlaceHand = placeHand;
            this.silentJumpRotDown(() -> {
               if (finalCurrentSlot <= 8) {
                  Minecraft.player.inventory.currentItem = finalCurrentSlot;
                  mc.playerController.syncCurrentPlayItem();
               } else {
                  mc.playerController.windowClick(0, finalCurrentSlot, oldSlot, ClickType.SWAP, Minecraft.player);
                  mc.playerController.windowClickMemory(0, finalCurrentSlot, oldSlot, ClickType.SWAP, Minecraft.player, 150);
               }

               this.selfPlace(finalPlaceHand);
               if (finalCurrentSlot <= 8) {
                  Minecraft.player.inventory.currentItem = oldSlot;
                  mc.playerController.syncCurrentPlayItem();
               }
            });
            return true;
         }
      }

      if (placeHand != null) {
         EnumHand finalPlaceHand1 = placeHand;
         this.silentJumpRotDown(() -> this.selfPlace(finalPlaceHand1));
         return true;
      } else {
         return false;
      }
   }

   void flagHop() {
      if (this.Type.currentMode.equalsIgnoreCase("FlagBoost")) {
         Minecraft.player.motionY = 0.42;
         MoveMeHelp.setSpeed(1.9);
      } else {
         Minecraft.player.motionY = 0.42;
         MoveMeHelp.setSpeed(1.9);
      }
   }

   @EventTarget
   public void onReceivePacket(EventReceivePacket event) {
      if (!this.flag && event.getPacket() instanceof SPacketPlayerPosLook look && this.Type.currentMode.equalsIgnoreCase("Matrix7")) {
         event.cancel();
         Minecraft.player.connection.sendPacket(new CPacketConfirmTeleport(look.getTeleportId()));
         Minecraft.player.setPosition(look.getX(), look.getY(), look.getZ());
         this.flag = true;
         this.flagBoostTicks = 16;
         if (!this.UltraBoost.getBool()) {
            mc.timer.tempSpeed = 2.0;
         } else {
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_WITHER_BREAK_BLOCK, 1.0F));
         }
      }

      if (event.getPacket() instanceof SPacketPlayerPosLook lookx
         && this.Type.currentMode.equalsIgnoreCase("FlagBoost")
         && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.expand(0.1, 0.0, 1.0).offsetMinDown(1.7)).isEmpty()) {
         Minecraft.player.setPosition(lookx.getX(), lookx.getY(), lookx.getZ());
         Minecraft.player.connection.sendPacket(new CPacketConfirmTeleport(lookx.getTeleportId()));
         this.flagHop();
         mc.timer.tempSpeed = 3.5;
         event.setCancelled(true);
      }

      if (!isFallDamage) {
         if (event.getPacket() instanceof SPacketEntityVelocity && ((SPacketEntityVelocity)event.getPacket()).getEntityID() == Minecraft.player.getEntityId()) {
            this.packetMotionY = (double)((SPacketEntityVelocity)event.getPacket()).motionY / 8000.0;
         }

         if (event.getPacket() instanceof SPacketEntityStatus sPacketEntityStatus
            && sPacketEntityStatus.getOpCode() == 2
            && sPacketEntityStatus.getEntity(mc.world) == Minecraft.player) {
            doSpeed = true;
         }
      } else {
         EntityLivingBase.isMatrixDamaged = false;
         doSpeed = false;
         isFallDamage = false;
         stopBow = true;
      }

      if ((this.Type.currentMode.equalsIgnoreCase("Matrix&AACWait") || this.Type.currentMode.equalsIgnoreCase("Matrix&AACDestruct"))
         && event.getPacket() instanceof SPacketChat packet
         && (
            packet.getChatComponent().getUnformattedText().contains("Извините, но вы не можете")
               || packet.getChatComponent().getUnformattedText().contains("but you can't")
         )) {
         if (this.Type.currentMode.equalsIgnoreCase("Matrix&AACWait")) {
            this.toDo = true;
         }

         event.setCancelled(true);
      }
   }

   @Override
   public void onUpdate() {
      if (this.Type.getMode().equalsIgnoreCase("Matrix7")) {
         boolean canBoost = false;
         if (!Minecraft.player.onGround && !Minecraft.player.isCollidedVertically) {
            if (this.UltraBoost.getBool()) {
               AxisAlignedBB posAABB = Minecraft.player.boundingBox;
               AxisAlignedBB nextPosAABB = Minecraft.player
                  .boundingBox
                  .addCoord(
                     Minecraft.player.posX - Minecraft.player.lastTickPosX,
                     (Minecraft.player.posY - Minecraft.player.lastTickPosY) * 1.25,
                     Minecraft.player.posZ - Minecraft.player.lastTickPosZ
                  );
               if (mc.world != null) {
                  float checkOffsetDown = 0.0F;
                  if (mc.world.getCollisionBoxes(Minecraft.player, posAABB.offsetMinDown((double)checkOffsetDown)).isEmpty()
                     && !mc.world.getCollisionBoxes(Minecraft.player, nextPosAABB.offsetMinDown((double)checkOffsetDown)).isEmpty()) {
                     canBoost = Minecraft.player.fallDistance > 0.0F;
                  }
               }
            } else if (Minecraft.player.fallDistance > 0.2F && Minecraft.player.fallDistance < 0.6F && mc.world != null) {
               canBoost = true;
            }
         }

         if (this.postBoost) {
            if (this.UltraBoost.getBool()) {
               mc.timer.tempSpeed = 0.1F;
            } else {
               mc.timer.tempSpeed = 0.5;
            }
         }

         this.postBoost = false;
         if (canBoost && !this.flag) {
            Minecraft.player.jump();
            double speed = this.UltraBoost.getBool() ? 6.0 : 2.6F;
            float radSet = MathHelper.toRadians(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw));
            Minecraft.player.motionX = (double)(-MathHelper.sin(radSet)) * speed;
            Minecraft.player.motionZ = (double)MathHelper.cos(radSet) * speed;
            Minecraft.player.jumpMovementFactor = 0.0F;
            if (this.UltraBoost.getBool()) {
               mc.timer.tempSpeed = 3.0;
            } else {
               mc.timer.tempSpeed = 0.5;
            }

            this.postBoost = true;
         }

         this.flagBoostTicks--;
         if (this.flag && this.flagBoostTicks <= 0) {
            this.flag = false;
         }

         if (!canBoost && !Minecraft.player.isJumping() && Minecraft.player.onGround && Minecraft.player.isCollidedVertically) {
            Minecraft.player.jump();
         }

         if (this.flag && (this.Instant.getBool() || this.UltraBoost.getBool())) {
            this.toggle();
         }
      }

      if (this.Type.getMode().equalsIgnoreCase("Glide")) {
         float slowing = this.Instant.getBool() ? (this.ticksGlide % 3 != 1 ? 1.25F : 0.8F) : 0.5F;
         if (mc.timer.speed == (double)slowing) {
            mc.timer.speed = 1.0;
         }

         double speedOn = this.Instant.getBool() ? 0.61F : MoveMeHelp.getSpeed() * 1.25;
         if (!Minecraft.player.onGround) {
            if (Minecraft.player.motionY <= 0.0) {
               mc.timer.speed = (double)slowing;
               mc.timer.tempSpeed = (double)slowing;
               if (this.Instant.getBool()) {
                  Minecraft.player.motionY = -0.0;
               } else {
                  Minecraft.player.motionY = -0.0784000015258789;
                  Entity.motiony = -0.001F;
               }

               this.ticksGlide++;
               Strafe.speed = 0.0F;
               MoveMeHelp.setSpeed(speedOn);
               if (this.ticksGlide > (this.Instant.getBool() ? 23 : 13)) {
                  if (this.Instant.getBool()) {
                     mc.timer.speed = 0.2F;
                     mc.timer.tempSpeed = 0.2F;
                  }

                  this.toggle(false);
               }
            }
         } else if (!Minecraft.player.isJumping()) {
            Minecraft.player.jump();
         }
      } else {
         if (this.Type.currentMode.equalsIgnoreCase("FlagBoost")) {
            if (Minecraft.player.motionY != -0.0784000015258789) {
               this.timerHelper.reset();
            }

            if (!MoveMeHelp.isMoving()) {
               this.timerHelper.setTime(this.timerHelper.getCurrentMS() + 50L);
            } else {
               Minecraft.player.rotationYaw = Minecraft.player.rotationYaw + (Minecraft.player.ticksExisted % 2 == 0 ? 0.01F : -0.01F);
            }

            if (this.timerHelper.hasReached(MoveMeHelp.getSpeed() > 0.7 ? 50.0 : 100.0)) {
               Minecraft.player.onGround = false;
               Entity.motiony = 1.0;
            }
         }

         if (this.Type.currentMode.equalsIgnoreCase("InstantLong") && Minecraft.player.hurtTime == 7) {
            MoveMeHelp.setCuttingSpeed(6.603774F);
            Minecraft.player.motionY = 0.42;
         }

         if (this.Type.currentMode.equalsIgnoreCase("BowBoost")) {
            if (Minecraft.player.onGround && doSpeed) {
               float dir1 = -MathHelper.sin(MovementHelper.getDirection()) * (float)(mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 1);
               float dir2 = MathHelper.cos(MovementHelper.getDirection()) * (float)(mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 1);
               if (MovementHelper.isMoving() || mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown()) {
                  if (MoveMeHelp.getSpeed() < 0.08) {
                     MoveMeHelp.setSpeed(0.42);
                  } else {
                     Minecraft.player.addVelocity((double)dir1 * 9.8 / 25.0, 0.0, (double)dir2 * 9.8 / 25.0);
                     MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
                  }
               } else if (Minecraft.player.isInWater()) {
                  Minecraft.player.addVelocity((double)dir1 * 8.5 / 25.0, 0.0, (double)dir2 * 9.5 / 25.0);
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
               } else if (!Minecraft.player.onGround) {
                  if (MoveMeHelp.getSpeed() < 0.22) {
                     MoveMeHelp.setSpeed(0.22);
                  } else {
                     MoveMeHelp.setSpeed(MoveMeHelp.getSpeed() * (Minecraft.player.isMoving() ? 1.0082 : 1.0088));
                  }
               }

               if (mc.gameSettings.keyBindJump.isKeyDown() && MoveMeHelp.getSpeed() > 0.7 && Minecraft.player.fallDistance == 0.0F) {
                  MoveMeHelp.setSpeed(0.7);
               }
            } else {
               MoveMeHelp.setCuttingSpeed(0.0);
            }
         }

         if (this.Type.currentMode.equalsIgnoreCase("LongJump")) {
            if (EntityLivingBase.isMatrixDamaged) {
               Minecraft.player.speedInAir = 0.3F;
            } else if (Minecraft.player.speedInAir == 0.3F) {
               Minecraft.player.speedInAir = 0.02F;
            }
         }

         if (this.Type.currentMode.equalsIgnoreCase("Solid")) {
            if (Minecraft.player.onGround) {
               this.ticks++;
            } else {
               this.ticks = 0;
            }

            if (EntityLivingBase.isMatrixDamaged) {
               Minecraft.player.stepHeight = 0.0F;
               if (this.ticks > 1
                  && MoveMeHelp.getSpeed() < 1.2
                  && !mc.world
                     .getCollisionBoxes(Minecraft.player, Minecraft.player.getEntityBoundingBoxCLCompact().offset(0.0, Minecraft.player.motionY, 0.0))
                     .isEmpty()) {
                  float dir1x = -MathHelper.sin(MovementHelper.getDirection()) * (float)(mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 1);
                  float dir2x = MathHelper.cos(MovementHelper.getDirection()) * (float)(mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 1);
                  if (MovementHelper.isMoving() || mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown()) {
                     if (MoveMeHelp.getSpeed() < 0.08) {
                        MoveMeHelp.setSpeed(0.42);
                     } else {
                        Minecraft.player.addVelocity((double)dir1x * 9.8 / 25.0, 0.0, (double)dir2x * 9.8 / 25.0);
                        MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
                     }
                  } else if (Minecraft.player.isInWater()) {
                     Minecraft.player.addVelocity((double)dir1x * 8.5 / 15.0, 0.0, (double)dir2x * 9.5 / 15.0);
                     MoveMeHelp.setSpeed(MoveMeHelp.getSpeed());
                  } else if (!Minecraft.player.onGround) {
                     if (MoveMeHelp.getSpeed() < 0.22) {
                        MoveMeHelp.setSpeed(0.22);
                     } else {
                        MoveMeHelp.setSpeed(MoveMeHelp.getSpeed() * (Minecraft.player.isMoving() ? 1.0082 : 1.0088));
                     }
                  }

                  if (mc.gameSettings.keyBindJump.isKeyDown() && MoveMeHelp.getSpeed() > 0.7 && Minecraft.player.fallDistance == 0.0F) {
                     MoveMeHelp.setSpeed(0.7);
                  }
               } else if (Speed.canMatrixBoost()) {
                  MoveMeHelp.setSpeed(MoveMeHelp.getSpeed() * 2.0);
               }

               if (this.timerHelper.hasReached(1350.0)) {
                  doSpeed = false;
                  Minecraft.player.stepHeight = 0.6F;
                  Minecraft.player.speedInAir = 0.02F;
                  this.timerHelper.reset();
                  mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
               }
            }
         }

         if (this.Type.currentMode.equalsIgnoreCase("Matrix&AACWait")) {
            if (EntityLivingBase.isMatrixDamaged) {
               this.wait.reset();
            }

            boolean has = this.state != null
               && this.wait.hasReached((double)(250.0F + mc.world.getBlockState(this.state).getBlockHardness(mc.world, this.state) * 1400.0F));
            if (this.state != null && has && (Minecraft.player.onGround || JesusSpeed.isJesused) || this.toDo && Minecraft.player.onGround) {
               float moveYawRad = MathHelper.toRadians(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw));
               Minecraft.player.addVelocity((double)(-MathHelper.sin(moveYawRad) * 1.8F), 0.8F, (double)(MathHelper.cos(moveYawRad) * 1.8F));
               this.wait.reset();
               this.toggle(false);
            }

            if (Minecraft.player.motionY > 0.43 || Minecraft.player.motionY < -0.6) {
               Minecraft.player.jumpMovementFactor = 0.0F;
               Minecraft.player.setSprinting(true);
            }

            List<BlockPos> mixPoses = new CopyOnWriteArrayList<>();
            Vec3d ePos = new Vec3d(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ);
            float r = 5.0F;

            for (float x = -5.0F; x < 5.0F; x++) {
               for (float y = -5.0F; y < 1.0F; y++) {
                  for (float z = -5.0F; z < 5.0F; z++) {
                     BlockPos poss = new BlockPos((double)x + ePos.xCoord, (double)y + ePos.yCoord, (double)z + ePos.zCoord);
                     Block block = mc.world.getBlockState(poss).getBlock();
                     if (block != Blocks.AIR
                        && block != Blocks.BARRIER
                        && block != Blocks.BEDROCK
                        && poss != null
                        && Minecraft.player.getDistanceAtEye((double)poss.getX(), (double)poss.getY(), (double)poss.getZ()) <= 5.0) {
                        mixPoses.add(poss);
                     }
                  }
               }
            }

            if (mixPoses.size() != 0) {
               mixPoses.sort(Comparator.comparing(current -> mc.world.getBlockState(current).getBlockHardness(mc.world, current)));
               this.state = mixPoses.get(0);
               if (this.state != null && !this.toDo && !has) {
                  Minecraft.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, this.state, EnumFacing.UP));
                  Minecraft.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, this.state, EnumFacing.UP));
               }
            }
         }

         if (this.Type.currentMode.equalsIgnoreCase("Matrix&AACDestruct")) {
            if (Minecraft.player.motionY >= 0.0 || (double)Minecraft.player.fallDistance > 1.2) {
               this.toggle(false);
            }

            if (!Minecraft.player.onGround) {
               return;
            }

            if (this.sentBlockPlacement(true)) {
               float moveYawRad = MathHelper.toRadians(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw));
               Minecraft.player.onGround = false;
               double speed = this.Instant.getBool() ? 3.3 : 1.4;
               Minecraft.player
                  .addVelocity((double)(-MathHelper.sin(moveYawRad)) * speed, this.Instant.getBool() ? 1.7F : 0.8, (double)MathHelper.cos(moveYawRad) * speed);
               if (this.Instant.getBool()) {
                  mc.world
                     .playSound(
                        Minecraft.player,
                        Minecraft.player.posX,
                        Minecraft.player.posY,
                        Minecraft.player.posZ,
                        SoundEvents.BLOCK_PISTON_EXTEND,
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                     );
                  mc.world
                     .playSound(
                        Minecraft.player,
                        Minecraft.player.posX,
                        Minecraft.player.posY,
                        Minecraft.player.posZ,
                        SoundEvents.BLOCK_SLIME_PLACE,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                     );
               }

               Timer.forceTimer(this.Instant.getBool() ? 0.15F : 0.25F);
            } else {
               Client.msg("§f§lModules:§r §7[§lLongJump§r§7]: что-то пошло не так или нет блоков в инвентаре.", false);
               this.toggle(false);
            }
         }
      }
   }

   public int oldSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(i).getStack();
         if (itemStack.getItem() == oldSlot) {
            return i;
         }
      }

      return -1;
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate e) {
      if (this.Type.currentMode.equalsIgnoreCase("DamageFly") && this.actived) {
         if (this.AutoBow.getBool() && !stopBow && !doSpeed) {
            if (!stopBow) {
               for (int i = 0; i < 9; i++) {
                  if (Minecraft.player.inventory.currentItem != EntityUtil.getBowAtHotbar() && !doBow) {
                     oldSlot = Minecraft.player.inventoryContainer.getSlot(Minecraft.player.inventory.currentItem).getStack().getItem();
                  }

                  if (Minecraft.player.inventory.getStackInSlot(i).getItem() instanceof ItemBow && !doSpeed && doBow) {
                     Minecraft.player.inventory.currentItem = EntityUtil.getBowAtHotbar();
                     e.setPitch(-90.0F);
                     Minecraft.player.rotationPitchHead = e.getPitch();
                  }
               }

               if (Minecraft.player.inventory.currentItem == EntityUtil.getBowAtHotbar() && !doSpeed && doBow && e.getPitch() == -90.0F) {
                  if (Minecraft.player.getItemInUseMaxCount() > 4) {
                     mc.getConnection().sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
                     mc.getConnection().sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP));
                     mc.gameSettings.keyBindUseItem.pressed = false;
                     doBow = false;
                  } else {
                     mc.gameSettings.keyBindUseItem.pressed = true;
                  }
               }
            }

            if (!doBow && oldSlot != null && Minecraft.player.inventory.currentItem != this.oldSlot()) {
               Minecraft.player.inventory.currentItem = this.oldSlot();
               oldSlot = null;
               stopBow = true;
            }
         }

         if (!doSpeed && this.AutoBow.getBool() && Minecraft.player.onGround) {
            if (Minecraft.player.getItemInUseMaxCount() > 4 || doBow) {
               MoveMeHelp.setSpeed(0.0);
               e.ground = Minecraft.player.onGround;
               Minecraft.player.onGround = false;
               Minecraft.player.jumpMovementFactor = 0.0F;
            }

            doBow = true;
         }

         if (doSpeed && !MoveMeHelp.isBlockAboveHead()) {
            stopBow = false;
            this.ticks = 0;
            if (this.AutoBow.getBool()) {
               doBow = false;
            }

            if (EntityLivingBase.isMatrixDamaged) {
               if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
                  Minecraft.player.jump();
               }

               if (!doBow) {
                  Minecraft.player.motionY = Minecraft.player.onGround ? 0.42 : this.packetMotionY;
                  MoveMeHelp.setSpeed(
                     Minecraft.player.hurtTime == 9
                        ? 3.0
                        : (Minecraft.player.hurtTime == 0 ? MoveMeHelp.getSpeed() : (Minecraft.player.hurtTime == 0 ? 0.85 : 1.0))
                  );
                  stopBow = false;
               }
            } else if (doSpeed) {
               doSpeed = false;
               if (Minecraft.player.onGround) {
                  doBow = true;
               }
            }
         }
      }

      if (this.Type.currentMode.equalsIgnoreCase("BowBoost") && this.actived) {
         this.speed = MathUtils.lerp(this.speed, doSpeed ? 0.8F : 0.0F, 0.2F);

         for (int i = 0; i < 9; i++) {
            if (Minecraft.player.inventory.currentItem != EntityUtil.getBowAtHotbar() && !doBow) {
               oldSlot = Minecraft.player.inventoryContainer.getSlot(Minecraft.player.inventory.currentItem).getStack().getItem();
            }

            if (Minecraft.player.inventory.getStackInSlot(i).getItem() instanceof ItemBow && !doSpeed && doBow) {
               Minecraft.player.inventory.currentItem = EntityUtil.getBowAtHotbar();
            }
         }

         if (!doBow && oldSlot != null && Minecraft.player.inventory.currentItem != this.oldSlot()) {
            Minecraft.player.inventory.currentItem = this.oldSlot();
            oldSlot = null;
         }

         if (Minecraft.player.inventory.currentItem == EntityUtil.getBowAtHotbar() && !doSpeed && doBow) {
            mc.gameSettings.keyBindUseItem.pressed = Minecraft.player.getItemInUseMaxCount() < 4;
            if ((double)Minecraft.player.getItemInUseMaxCount() > 2.5) {
               e.setPitch(Wrapper.getPlayer().isPotionActive(Potion.getPotionById(1)) ? -30.0F : -45.0F);
               Minecraft.player.rotationPitchHead = e.getPitch();
            }
         }

         if ((double)Minecraft.player.getItemInUseMaxCount() > 3.5) {
            doBow = false;
         }

         if (doBow && Minecraft.player.hurtTime != 0) {
            mc.gameSettings.keyBindUseItem.pressed = false;
            doBow = false;
         }

         if (Minecraft.player.hurtTime != 0) {
            doSpeed = true;
            if (Minecraft.player.hurtTime > 7) {
               this.timerHelper.reset();
            }
         }

         if (doSpeed) {
            MoveMeHelp.setSpeed(doSpeed ? (double)this.speed : 0.0);
         }

         if (this.timerHelper.hasReached(1300.0)) {
            doSpeed = false;
            if (this.timerHelper.hasReached(1460.0) && mc.gameSettings.keyBindForward.isKeyDown()) {
               doBow = true;
               this.timerHelper.reset();
            }
         }
      }
   }

   @Override
   public String getDisplayName() {
      return this.getDisplayByMode(this.Type.currentMode);
   }

   @Override
   public void onToggled(boolean actived) {
      if (actived) {
         if (this.Type.getMode().equalsIgnoreCase("Matrix7") && this.Instant.getBool()) {
            if (Minecraft.player.isJumping() && Minecraft.player.fallDistance == 0.0F) {
               this.toggle();
            } else if (Minecraft.player.onGround && !Minecraft.player.isJumping()) {
               Minecraft.player.jump();
               float speedUp = 6.0F;
               mc.timer.tempSpeed = (double)speedUp;
               Timer.forceTimer(speedUp);
            }
         }

         this.toDo = false;
         this.wait.reset();
         this.ticksGlide = 0;
         this.flag = false;
         this.flagBoostTicks = 0;
      } else {
         this.toDo = false;
         stopBow = false;
         this.ticks = 0;
         isFallDamage = false;
         mc.gameSettings.keyBindJump.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
         if (this.Type.currentMode.equalsIgnoreCase("BowBoost")) {
            mc.gameSettings.keyBindUseItem.pressed = false;
         }

         oldSlot = null;
         doSpeed = false;
         doBow = false;
         Minecraft.player.stepHeight = 0.6F;
         Minecraft.player.speedInAir = 0.02F;
         this.ticksGlide = 0;
         mc.timer.speed = 1.0;
      }

      super.onToggled(actived);
   }
}
