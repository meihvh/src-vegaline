package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventAction;
import ru.govno.client.event.events.EventCanPlaceBlock;
import ru.govno.client.event.events.EventMoveKeys;
import ru.govno.client.event.events.EventMovementInput;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.event.events.EventRotationJump;
import ru.govno.client.event.events.EventRotationStrafe;
import ru.govno.client.event.events.EventSafeWalk;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.BlockUtils;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Movement.MoveMeHelp;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class ScaffWalk extends Module {
   public static ScaffWalk get;
   public final TimerHelper placeDelay = new TimerHelper();
   public BoolSettings MovePreSearchBlock;
   public BoolSettings PlaceAtRotation;
   public BoolSettings RotationSleep;
   public BoolSettings ElevatorBoost;
   public BoolSettings RotateMoveSide;
   public BoolSettings KeepJumpWalking;
   public BoolSettings NoKeepDiagonal;
   public BoolSettings OnlyFallPlaceOnKeep;
   public BoolSettings JitterMovement;
   public FloatSettings PlaceDelay;
   public FloatSettings MulNoGroundMotion;
   public FloatSettings MulGroundMotion;
   public ModeSettings Rotation;
   public ModeSettings FallSaver;
   public ModeSettings Elevator;
   public ModeSettings Sprinting;
   public ModeSettings SwingAction;
   public ModeSettings PlaceTick;
   public ModeSettings SwitchMode;
   public ModeSettings SneakingMode;
   private boolean keepWalkJumpingSet;
   private boolean placeTick;
   public int placed;
   public int haveCount;
   public int hotbarSlotOnEnable = -1;
   private float[] lastRotate;
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
   protected boolean hasRClickSucessful = false;
   public EnumHand getHasActiveHand;
   protected boolean hasSuccessfulRClick;
   BlockPos posToPlace;
   BlockPos forceBlockPos;
   boolean forcePlaceFromInventory;
   boolean forceSilentSwitch;
   boolean runPlace;
   boolean forceElevator;
   boolean tempRotationStatus;
   long forceDelay;
   private boolean possibleToPlacingStatic;
   private boolean elevatorIsPossibleUpdated;
   private Vec3d blockFaceVec;
   private Vec3d lastBlockFaceVec;
   private int ticksNotRotated;
   private Vec3d lastLerpedLOCK;
   private Vec3d updatedSelfPos = new Vec3d(0.0, 0.0, 0.0);

   public ScaffWalk() {
      super("ScaffWalk", 0, Module.Category.PLAYER);
      this.settings.add(this.MovePreSearchBlock = new BoolSettings("MovePreSearchBlock", false, this));
      this.settings.add(this.PlaceDelay = new FloatSettings("PlaceDelay", 150.0F, 350.0F, 10.0F, this));
      this.settings.add(this.Rotation = new ModeSettings("Rotation", "Fast", this, new String[]{"None", "Fast", "Smooth"}));
      this.settings.add(this.PlaceAtRotation = new BoolSettings("PlaceAtRotation", false, this, () -> !this.Rotation.getMode().equals("None")));
      this.settings.add(this.RotationSleep = new BoolSettings("RotationSleep", false, this, () -> !this.Rotation.getMode().equals("None")));
      this.settings.add(this.FallSaver = new ModeSettings("FallSaver", "Always", this, new String[]{"None", "Always", "InAir", "OnGround"}));
      this.settings.add(this.Elevator = new ModeSettings("Elevator", "None", this, new String[]{"None", "Matrix", "Strict", "NCP"}));
      this.settings.add(this.ElevatorBoost = new BoolSettings("ElevatorBoost", false, this, () -> !this.Elevator.getMode().equalsIgnoreCase("None")));
      this.settings.add(this.Sprinting = new ModeSettings("Sprinting", "Never", this, new String[]{"Default", "Always", "Never", "AlmostRage"}));
      this.settings.add(this.RotateMoveSide = new BoolSettings("RotateMoveSide", true, this));
      this.settings.add(this.SwingAction = new ModeSettings("SwingAction", "Packet", this, new String[]{"None", "Packet", "Client"}));
      this.settings.add(this.PlaceTick = new ModeSettings("PlaceTick", "Pre", this, new String[]{"Pre", "Post"}));
      this.settings.add(this.MulGroundMotion = new FloatSettings("MulGroundMotion", 0.82F, 1.0F, 0.0F, this));
      this.settings.add(this.MulNoGroundMotion = new FloatSettings("MulNoGroundMotion", 1.0F, 1.0F, 0.0F, this));
      this.settings.add(this.SwitchMode = new ModeSettings("SwitchMode", "HotbarClient", this, new String[]{"HotbarClient", "HotbarPacket", "FullInvPacket"}));
      this.settings
         .add(
            this.SneakingMode = new ModeSettings("SneakingMode", "None", this, new String[]{"None", "Always", "PrePlace", "AlwaysIfGround", "PrePlaceIfGround"})
         );
      this.settings.add(this.KeepJumpWalking = new BoolSettings("KeepJumpWalking", false, this));
      this.settings.add(this.NoKeepDiagonal = new BoolSettings("NoKeepDiagonal", false, this, () -> this.KeepJumpWalking.getBool()));
      this.settings.add(this.OnlyFallPlaceOnKeep = new BoolSettings("OnlyFallPlaceOnKeep", false, this, () -> this.KeepJumpWalking.getBool()));
      this.settings.add(this.JitterMovement = new BoolSettings("JitterMovement", false, this));
      this.setDemand(0, 1);
      get = this;
   }

   private boolean isKeepJumpWalking() {
      if (!this.KeepJumpWalking.getBool()) {
         return false;
      } else {
         double movementYaw = (double)(MathUtils.wrapDegrees(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw)) + 360.0F);
         boolean isDiagonal = movementYaw % 90.0 > 7.0 && movementYaw % 90.0 < 83.0;
         return (!isDiagonal || !this.NoKeepDiagonal.getBool())
            && (this.haveCount = this.getAnyBlocksCount(this.SwitchMode.getMode().equalsIgnoreCase("FullInvPacket"))) >= 5
            && this.placed > 0;
      }
   }

   private boolean elevatorIsPossible(boolean forCalcDelay) {
      if (this.haveCount == 0) {
         return false;
      } else {
         if (forCalcDelay) {
            String var2 = this.Elevator.currentMode;
            switch (var2) {
               case "Matrix":
                  return Minecraft.player.isJumping() && !MoveMeHelp.moveKeysPressed();
               case "Strict":
                  return Minecraft.player.isJumping() && !MoveMeHelp.moveKeysPressed() && MoveMeHelp.getSpeed() == 0.0 && this.haveCount > 0;
               case "NCP":
                  return Minecraft.player.isJumping();
            }
         } else {
            String var4 = this.Elevator.currentMode;
            switch (var4) {
               case "Matrix":
                  return Minecraft.player.isJumping()
                     && !MoveMeHelp.moveKeysPressed()
                     && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.999)).isEmpty()
                     && !this.placeDelay.hasReached(10.0);
               case "Strict":
                  return Minecraft.player.isJumping()
                     && !MoveMeHelp.moveKeysPressed()
                     && MoveMeHelp.getSpeed() == 0.0
                     && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.05)).isEmpty()
                     && this.placeDelay.hasReached(5.0)
                     && this.haveCount > 0;
               case "NCP":
                  return Minecraft.player.isJumping()
                     && !mc.world.getCollisionBoxes(Minecraft.player, Minecraft.player.boundingBox.offsetMinDown(0.25)).isEmpty();
            }
         }

         return false;
      }
   }

   private boolean controlSprinting(EventAction event, boolean isRotated) {
      boolean sprintState = event == null ? Minecraft.player.isSprinting() : event.getSprintState();
      boolean edit = false;
      boolean push = false;
      String var6 = this.Sprinting.currentMode;
      switch (var6) {
         case "Default":
            sprintState = !isRotated;
            break;
         case "Always":
            sprintState = true;
            break;
         case "Never":
            sprintState = false;
            break;
         case "AlmostRage":
            sprintState = !isRotated;
      }

      mc.gameSettings.keyBindSprint.pressed = sprintState && Minecraft.player.movementInput.forwardKeyDown;
      return sprintState && Minecraft.player.movementInput.forwardKeyDown;
   }

   private void controlSneaking(boolean reachVoid) {
      boolean setSneak = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) && mc.currentScreen == null;
      boolean prePlace = !BlockUtils.blockMaterialIsCurrent(new BlockPos(this.updatedSelfPos.addVector(0.0, -1.0E-5, 0.0)));
      if (!setSneak && mc.currentScreen == null && !this.elevatorIsPossible(true) && !Minecraft.player.capabilities.isFlying) {
         String var4 = this.SneakingMode.getMode();
         switch (var4) {
            case "Always":
               setSneak = true;
               break;
            case "PrePlace":
               setSneak = prePlace;
               break;
            case "AlwaysIfGround":
               setSneak = Minecraft.player.onGround && !Minecraft.player.movementInput.jump;
               break;
            case "PrePlaceIfGround":
               setSneak = prePlace && Minecraft.player.onGround && !Minecraft.player.movementInput.jump;
         }
      }

      if (!this.SneakingMode.getMode().equalsIgnoreCase("None")) {
         mc.gameSettings.keyBindSneak.pressed = setSneak;
      }
   }

   private boolean fallSaverIsPossible() {
      String var1 = this.FallSaver.currentMode;
      switch (var1) {
         case "Always":
            if (Minecraft.player.onGround) {
               return true;
            }

            RayTraceResult result = mc.world
               .rayTraceBlocks(
                  new Vec3d(Minecraft.player.posX, Minecraft.player.posY, Minecraft.player.posZ),
                  new Vec3d(Minecraft.player.posX, Minecraft.player.posY - 12.0, Minecraft.player.posZ),
                  false,
                  true,
                  true
               );
            return result == null || result.getBlockPos() == null || result.hitVec == null || !(result.hitVec.lengthVector() > 1.0);
         case "InAir":
            return !Minecraft.player.onGround;
         case "OnGround":
            return Minecraft.player.onGround;
         default:
            return false;
      }
   }

   private void doMulSpeed() {
      if (MoveMeHelp.isMoving() && this.haveCount != 0) {
         float mul = !Minecraft.player.movementInput.jump && Minecraft.player.onGround ? this.MulGroundMotion.getFloat() : this.MulNoGroundMotion.getFloat();
         if (!(mul >= 1.0F) && !(mul < -1.0F)) {
            Minecraft.player.multiplyMotionXZ(mul);
         }
      }
   }

   private BlockPos findBlockPosToPlace(boolean ignoreSelfAABB, boolean advancedSearchAndPredict) {
      if (!this.isKeepJumpWalking()
         || !this.OnlyFallPlaceOnKeep.getBool()
         || !(Math.abs(this.updatedSelfPos.yCoord - this.virtSelfPosVector().yCoord) > 0.2F)
         || mc.gameSettings.keyBindJump.isKeyDown()
         || Minecraft.player.fallDistance != 0.0F && !Minecraft.player.onGround) {
         BlockPos downBlockPosBase = new BlockPos(this.updatedSelfPos.xCoord, this.updatedSelfPos.yCoord - 0.999, this.updatedSelfPos.zCoord);
         List<BlockPos> allDownPositions = new ArrayList<>();
         allDownPositions.add(downBlockPosBase);
         if (!this.canPlaceBlock(downBlockPosBase, false, true)) {
            float wD2 = Minecraft.player.width / 2.0F - 1.0E-13F;
            BlockPos s0 = downBlockPosBase.add((double)(-wD2), 0.0, (double)(-wD2));
            BlockPos s1 = downBlockPosBase.add((double)wD2, 0.0, (double)(-wD2));
            BlockPos s2 = downBlockPosBase.add((double)wD2, 0.0, (double)wD2);
            BlockPos s3 = downBlockPosBase.add((double)(-wD2), 0.0, (double)wD2);
            if (downBlockPosBase.getDistanceToBlockPos(s0) != 0.0) {
               allDownPositions.add(s0);
            }

            if (downBlockPosBase.getDistanceToBlockPos(s1) != 0.0) {
               allDownPositions.add(s1);
            }

            if (downBlockPosBase.getDistanceToBlockPos(s2) != 0.0) {
               allDownPositions.add(s2);
            }

            if (downBlockPosBase.getDistanceToBlockPos(s3) != 0.0) {
               allDownPositions.add(s3);
            }
         }

         Iterator var19 = allDownPositions.iterator();
         if (var19.hasNext()) {
            BlockPos downBlockPos = (BlockPos)var19.next();
            if (downBlockPos == null) {
               return null;
            } else {
               boolean hasDownPlaceable = this.canPlaceBlock(downBlockPos, true, false)
                  && BlockUtils.getPlaceableSideSeen(downBlockPos, Minecraft.player) != null;
               if (hasDownPlaceable) {
                  return downBlockPos;
               } else {
                  if (mc.world.isAirBlock(downBlockPos)) {
                     for (EnumFacing tempHorizontalFace : EnumFacing.HORIZONTALS) {
                        BlockPos downPosWithFaceOffset = downBlockPos.offset(tempHorizontalFace);
                        if (downPosWithFaceOffset != null
                           && this.canPlaceBlock(downPosWithFaceOffset, ignoreSelfAABB, true)
                           && BlockUtils.getPlaceableSideSeen(downPosWithFaceOffset, Minecraft.player) != null) {
                           return downPosWithFaceOffset;
                        }
                     }

                     for (BlockPos downNext : IntStream.range(1, 4).mapToObj(index -> downBlockPos.down(index)).toList()) {
                        if (downNext == null) {
                           break;
                        }

                        if (this.canPlaceBlock(downNext, ignoreSelfAABB, true) && BlockUtils.getPlaceableSideSeen(downNext, Minecraft.player) != null) {
                           return downNext;
                        }
                     }

                     if (advancedSearchAndPredict) {
                        int rangeXZ = 4;

                        for (BlockPos downNext : IntStream.range(1, 4).mapToObj(index -> downBlockPos.down(index)).toList()) {
                           if (downNext == null) {
                              break;
                           }

                           for (int xzOffset = 0; xzOffset <= rangeXZ; xzOffset++) {
                              for (EnumFacing tempHorizontalFacex : EnumFacing.HORIZONTALS) {
                                 BlockPos downNextWithFaceOffset = downNext.offset(tempHorizontalFacex, xzOffset);
                                 if (downNextWithFaceOffset != null
                                    && this.canPlaceBlock(downNextWithFaceOffset, ignoreSelfAABB, true)
                                    && BlockUtils.getPlaceableSideSeen(downNextWithFaceOffset, Minecraft.player) != null) {
                                    MoveMeHelp.setSpeed(0.0);
                                    return downNextWithFaceOffset;
                                 }
                              }
                           }
                        }
                     }
                  } else if (advancedSearchAndPredict && MoveMeHelp.isMoving()) {
                     float motionYaw = MathUtils.wrapAngleTo180_float(MoveMeHelp.moveYaw(Minecraft.player.rotationYaw));
                     float motionRadian = MathHelper.toRadians(motionYaw);
                     int scanRange = 4;
                     List<BlockPos> rayBlocksInRange = new ArrayList<>();
                     Vec3d baseDownVecCenter = new Vec3d(
                        (double)downBlockPos.getX() + 0.5, (double)downBlockPos.getY() + 0.4999999, (double)downBlockPos.getZ() + 0.5
                     );

                     for (int meter = 0; meter <= scanRange; meter++) {
                        double offsetX = (double)(-MathHelper.sin(motionRadian) * (float)meter);
                        double offsetZ = (double)(MathHelper.cos(motionRadian) * (float)meter);
                        Vec3d baseDownVecCenterWithOffset = baseDownVecCenter.addVector(offsetX, 0.0, offsetZ);
                        rayBlocksInRange.add(new BlockPos(baseDownVecCenterWithOffset));
                     }

                     if (!rayBlocksInRange.isEmpty()) {
                        for (BlockPos downXZPredict : rayBlocksInRange) {
                           if (downXZPredict != null
                              && this.canPlaceBlock(downXZPredict, ignoreSelfAABB, true)
                              && BlockUtils.getPlaceableSideSeen(downXZPredict, Minecraft.player) != null) {
                              return downXZPredict;
                           }
                        }
                     }
                  }

                  return downBlockPos;
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private boolean canRotate() {
      if (AirStuck.ifStopMotionOrder()) {
         return false;
      } else {
         String var1 = this.Rotation.currentMode;
         switch (var1) {
            case "Fast":
            case "Smooth":
               return true;
            default:
               return false;
         }
      }
   }

   private void doElevatorMoveActions() {
      String var1 = this.Elevator.currentMode;
      switch (var1) {
         case "Matrix":
            Minecraft.player.jumpTicks = 0;
            Minecraft.player.onGround = true;
            if (!MoveMeHelp.isMoving()) {
               float yawRadMove = MathHelper.toRadians(Minecraft.player.rotationYaw - 90.0F);
               float extXZ = Minecraft.player.ticksExisted % 2 == 0 ? 0.001F : -0.001F;
               Minecraft.player.motionX = (double)(-MathHelper.sin(yawRadMove) * extXZ);
               Minecraft.player.motionZ = (double)(MathHelper.cos(yawRadMove) * extXZ);
            }
            break;
         case "Strict":
            Vec3d selfVec = this.updatedSelfPos;
            mc.getConnection().sendPacket(new CPacketPlayer.Position(selfVec.xCoord, selfVec.yCoord + 0.41999998688698, selfVec.zCoord, false));
            mc.getConnection().sendPacket(new CPacketPlayer.Position(selfVec.xCoord, selfVec.yCoord + 0.7531999805211997, selfVec.zCoord, false));
            mc.getConnection().sendPacket(new CPacketPlayer.Position(selfVec.xCoord, selfVec.yCoord + 1.00133597911214, selfVec.zCoord, false));
            Minecraft.player.setPosY(selfVec.yCoord + 1.0);
            this.updatedSelfPos.yCoord++;
            break;
         case "NCP":
            if ((double)Minecraft.player.fallDistance > 0.0 && !Minecraft.player.onGround) {
               Minecraft.player.motionY = 0.42;
               Minecraft.player.setPosY((double)((int)Minecraft.player.posY));
               Minecraft.player.fallDistance = 0.0F;
               Minecraft.player.onGround = true;
            }
      }
   }

   public Vec3d placeRotateVec(BlockPos pos) {
      EnumFacing face = BlockUtils.getPlaceableSide(pos);
      if (face == null) {
         return null;
      } else {
         AxisAlignedBB blockAxis = mc.world.getBlockState(pos).getSelectedBoundingBox(mc.world, pos);
         if (blockAxis == null) {
            blockAxis = new AxisAlignedBB(pos);
         }

         double sizeX;
         double sizeY;
         double sizeZ;
         Vec3d axisCenter = new Vec3d(blockAxis.minX, blockAxis.minY, blockAxis.minZ)
            .add(
               new Vec3d(sizeX = blockAxis.maxX - blockAxis.minX, sizeY = blockAxis.maxY - blockAxis.minY, sizeZ = blockAxis.maxZ - blockAxis.minZ).scale(0.5)
            );
         double sizeXd2 = sizeX / 2.0;
         double sizeYd2 = sizeY / 2.0;
         double sizeZd2 = sizeZ / 2.0;
         float offset = 0.001F;
         Vec3d vec = axisCenter.addVector(
            (double)face.getFrontOffsetX() * ((double)offset + sizeXd2),
            (double)face.getFrontOffsetY() * ((double)offset + sizeYd2),
            (double)face.getFrontOffsetZ() * ((double)offset + sizeZd2)
         );
         float syncCoordPC01 = 1.0F;
         if (this.canRotate()) {
            double yaw90TH = (double)(RotationUtil.Yaw % 90.0F);
            if (yaw90TH > 10.0 && yaw90TH < 80.0) {
               syncCoordPC01 = 0.99F;
            }
         }

         boolean origin = false;
         if (origin) {
            vec.xCoord = vec.xCoord
               + (
                     sizeXd2
                        - Math.max(
                              (double)((float)pos.getX() + 1.0F) - this.updatedSelfPos.xCoord - (Minecraft.player.posZ - Minecraft.player.lastTickPosZ), 0.0
                           )
                           * sizeX
                  )
                  * (double)syncCoordPC01;
            vec.zCoord = vec.zCoord
               + (
                     sizeZd2
                        - Math.max(
                              (double)((float)pos.getZ() + 1.0F) - this.updatedSelfPos.zCoord - (Minecraft.player.posX - Minecraft.player.lastTickPosX), 0.0
                           )
                           * sizeZ
                  )
                  * (double)syncCoordPC01;
         } else {
            switch (face) {
               case EAST:
               case WEST:
                  vec.zCoord = vec.zCoord + (sizeZd2 - (Math.ceil(this.updatedSelfPos.zCoord) - this.updatedSelfPos.zCoord) * sizeZ) * (double)syncCoordPC01;
                  break;
               case SOUTH:
               case NORTH:
                  vec.xCoord = vec.xCoord + (sizeXd2 - (Math.ceil(this.updatedSelfPos.xCoord) - this.updatedSelfPos.xCoord) * sizeX) * (double)syncCoordPC01;
            }
         }

         return vec;
      }
   }

   public boolean canPlaceBlock(BlockPos pos, boolean ignoreSelfBox, boolean rayCast) {
      if (mc.world != null && mc.world.isBlockLoaded(pos)) {
         boolean aired = BlockUtils.getBlockMaterial(pos).isReplaceable();
         boolean neared = BlockUtils.blockMaterialIsCurrentWithSideSets(pos);
         return aired
            && neared
            && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos)).stream().filter(e -> !ignoreSelfBox).collect(Collectors.toList()).isEmpty()
            && (!rayCast || this.placeRotateVec(pos) != null);
      } else {
         return false;
      }
   }

   public boolean itemStackIsCurrentToPlace(ItemStack stack) {
      boolean isBlock = stack != null && stack.getItem() instanceof ItemBlock;
      Block sample = Block.getBlockFromItem(stack.getItem());
      if (isBlock) {
         IBlockState state = sample.getDefaultState();
         if (state != null) {
            return state.getMaterial().blocksMovement()
               && !state.getMaterial().isReplaceable()
               && !(sample instanceof BlockFalling)
               && !(sample instanceof BlockChest)
               && !(sample instanceof BlockCactus);
         }
      }

      return false;
   }

   private boolean isClickableBlock(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         Block block = mc.world.getBlockState(pos).getBlock();
         return !Minecraft.player.isSneaking() && this.clickableBlockIDs.stream().anyMatch(id -> Block.getIdFromBlock(block) == id);
      }
   }

   public int getAnyBlocksCount(boolean placeFromInventory) {
      int count = this.itemStackIsCurrentToPlace(Minecraft.player.getHeldItemOffhand()) ? Minecraft.player.getHeldItemOffhand().stackSize : 0;

      for (int slot = 0; slot < (placeFromInventory ? 44 : 9); slot++) {
         ItemStack stack = Minecraft.player.inventory.getStackInSlot(slot);
         if (this.itemStackIsCurrentToPlace(stack)) {
            count += stack.stackSize;
         }
      }

      return count;
   }

   private void doSwingAction(EnumHand hand) {
      String var2 = this.SwingAction.currentMode;
      switch (var2) {
         case "Packet":
            mc.getConnection().sendPacket(new CPacketAnimation(hand));
            break;
         case "Client":
            Minecraft.player.swingArm(hand);
      }
   }

   private long getPlaceDelay() {
      return this.elevatorIsPossible(true) ? -1L : (long)((double)this.PlaceDelay.getFloat() / mc.timer.speed);
   }

   private float[] getRotationToVectorBase(Vec3d at, Vec3d to) {
      float[] base = RotationUtil.getVecNeeded(to, at);
      float yaw = base[0];
      float pitch = base[1];
      float finalYaw = yaw;
      float finalPitch = pitch;
      AtomicBoolean cancelEdit = new AtomicBoolean(false);
      if (this.RotationSleep.getBool() && !this.Rotation.getMode().equalsIgnoreCase("None")) {
         if (this.elevatorIsPossible(true)) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.sideHit == EnumFacing.UP) {
               pitch = 90.0F;
               cancelEdit.set(false);
            }
         } else {
            this.updateObjectMouseOverSilent(new float[]{yaw, pitch}, () -> {
               double yawDiff = (double)MathUtils.getAngleDifference(RotationUtil.Yaw, finalYaw);
               double pitchDiff = (double)MathUtils.getAngleDifference(RotationUtil.Pitch, finalPitch);
               if (yawDiff > 8.0 || pitchDiff > 30.0) {
                  cancelEdit.set(false);
               } else if (mc.objectMouseOver == null) {
                  cancelEdit.set(true);
               } else if (mc.objectMouseOver.sideHit == EnumFacing.UP) {
                  cancelEdit.set(true);
               } else {
                  cancelEdit.set(pitchDiff < 1.0 && yawDiff < 1.0);
               }
            });
         }
      }

      if (!cancelEdit.get()) {
         float randomSpeedPNP = 0.1F;
         float baseSpeedYaw = 180.0F;
         float baseSpeedPitch = 180.0F;
         String maxSpeedYaw = this.Rotation.getMode();
         switch (maxSpeedYaw) {
            case "Fast":
               baseSpeedYaw = 179.6F;
               baseSpeedPitch = 146.8F;
               break;
            case "Smooth":
               baseSpeedYaw = 44.6F;
               baseSpeedPitch = 39.8F;
         }

         float maxSpeedYawx = baseSpeedYaw - MathUtils.lerp(-randomSpeedPNP / 2.0F, randomSpeedPNP / 2.0F, (float)Math.random());
         float maxSpeedPitch = baseSpeedPitch - MathUtils.lerp(-randomSpeedPNP / 2.0F, randomSpeedPNP / 2.0F, (float)Math.random());
         float randYaw = 1.25F;
         float randPitch = 0.2F;
         yaw += MathUtils.lerp(-randYaw, randYaw, (float)Math.random()) / 2.0F;
         pitch += MathUtils.lerp(-randPitch, randPitch, (float)Math.random()) / 2.0F;
         yaw = MathUtils.wrapDegrees(yaw - Minecraft.player.rotationYaw) + Minecraft.player.rotationYaw;
         pitch = Math.max(Math.min(pitch, 90.0F), -90.0F);
         RotationUtil.Yaw = RotationUtil.Yaw + MathUtils.clamp(MathUtils.wrapDegrees(yaw - RotationUtil.Yaw), -maxSpeedYawx, maxSpeedYawx);
         RotationUtil.Pitch = RotationUtil.Pitch + MathUtils.clamp(pitch - RotationUtil.Pitch, -maxSpeedPitch, maxSpeedPitch);
         RotationUtil.Yaw = RotationUtil.getSensitivity(RotationUtil.Yaw);
         RotationUtil.Pitch = RotationUtil.getSensitivity(RotationUtil.Pitch);
         yaw = RotationUtil.Yaw;
         pitch = RotationUtil.Pitch;
      } else {
         yaw = RotationUtil.Yaw;
         pitch = RotationUtil.Pitch;
      }

      base[0] = yaw;
      base[1] = pitch;
      return base;
   }

   private float[] getRotation(Vec3d toVec) {
      return toVec == null
         ? new float[]{Minecraft.player.rotationYaw + 180.0F, Minecraft.player.rotationPitch}
         : this.getRotationToVectorBase(
            Minecraft.player.getPositionVector().addVector(0.0, (double)Minecraft.player.getEyeHeight(), 0.0),
            toVec == null ? Minecraft.player.getPositionVector() : toVec
         );
   }

   private float[] setRotation(EventPlayerMotionUpdate event, float[] yaw$pitch) {
      event.setYaw(yaw$pitch[0]);
      event.setPitch(yaw$pitch[1]);
      Minecraft.player.rotationYawHead = event.getYaw();
      Minecraft.player.renderYawOffset = event.getYaw();
      Minecraft.player.rotationPitchHead = event.getPitch();
      HitAura.get.rotations = yaw$pitch;
      HitAura.get.noRotateTick = true;
      return yaw$pitch;
   }

   public void updateObjectMouseOverSilent(float[] rotateSilent, Runnable dataRunner) {
      float prevYaw = Minecraft.player.rotationYaw;
      float prevPitch = Minecraft.player.rotationPitch;
      Minecraft.player.rotationYaw = rotateSilent[0];
      Minecraft.player.rotationPitch = rotateSilent[1];
      mc.entityRenderer.getMouseOver(1.0F);
      dataRunner.run();
      Minecraft.player.rotationYaw = prevYaw;
      Minecraft.player.rotationPitch = prevPitch;
      mc.entityRenderer.getMouseOver(mc.getRenderPartialTicks());
   }

   public boolean rClickBlockLawFully(BlockPos pos, EnumHand clickHand) {
      this.hasRClickSucessful = false;
      boolean rotatedPlace = this.possibleToPlacingStatic && this.lastRotate != null && this.PlaceAtRotation.getBool();
      if (pos != null) {
         AtomicBoolean cancelRotatedPlace = new AtomicBoolean(false);
         BlockPos.MutableBlockPos tempPos0 = new BlockPos.MutableBlockPos();
         BlockPos.MutableBlockPos tempPos1 = new BlockPos.MutableBlockPos();
         if (rotatedPlace) {
            this.updateObjectMouseOverSilent(this.lastRotate, () -> {
               BlockPos placePos = mc.objectMouseOver.getBlockPos();
               if (placePos != null) {
                  tempPos0.setPos(placePos);
               }
            });
            this.updateObjectMouseOverSilent(this.getRotation(this.placeRotateVec(pos)), () -> {
               BlockPos placePos = mc.objectMouseOver.getBlockPos();
               if (placePos != null) {
                  tempPos1.setPos(placePos);
               }
            });

            try {
               if (tempPos0.getDistanceToBlockPos(tempPos1) != 0.0) {
                  cancelRotatedPlace.set(true);
               }
            } catch (Exception var9) {
               var9.printStackTrace();
            }
         }

         if (rotatedPlace && !cancelRotatedPlace.get()) {
            float[] rotation = rotatedPlace ? this.lastRotate : this.getRotation(this.placeRotateVec(pos));
            this.updateObjectMouseOverSilent(
               rotation,
               () -> {
                  BlockPos placePos = mc.objectMouseOver.getBlockPos();
                  EnumFacing sideHit = mc.objectMouseOver.sideHit;
                  Vec3d hitVec = mc.objectMouseOver.hitVec;
                  RayTraceResult.Type typeOfHit = mc.objectMouseOver.typeOfHit;
                  if (placePos != null && sideHit != null && hitVec != null && typeOfHit == RayTraceResult.Type.BLOCK) {
                     boolean willBeInteractx = this.isClickableBlock(placePos);
                     if (willBeInteractx) {
                        mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
                        Minecraft.player.movementInput.sneak = true;
                        Minecraft.player.setSneaking(true);
                     }

                     this.hasRClickSucessful = mc.playerController
                           .processRightClickBlock(Minecraft.player, mc.world, placePos, mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec, clickHand)
                        == EnumActionResult.SUCCESS;
                     if (willBeInteractx) {
                        mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
                     }
                  }
               }
            );
         } else if (pos != null) {
            EnumFacing face = BlockUtils.getPlaceableSide(pos);
            if (face != null) {
               boolean willBeInteract = this.isClickableBlock(pos.offset(face));
               if (willBeInteract) {
                  mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.START_SNEAKING));
                  Minecraft.player.movementInput.sneak = true;
                  Minecraft.player.setSneaking(true);
               }

               this.hasRClickSucessful = mc.playerController
                     .processRightClickBlock(Minecraft.player, mc.world, pos.offset(face), face.getOpposite(), new Vec3d(0.5, 0.5, 0.5), clickHand)
                  == EnumActionResult.SUCCESS;
               if (willBeInteract) {
                  mc.getConnection().sendPacket(new CPacketEntityAction(Minecraft.player, CPacketEntityAction.Action.STOP_SNEAKING));
               }
            }
         }
      }

      return this.hasRClickSucessful;
   }

   public void afterSwitchActionHand(boolean canPlaceFromInventory, boolean silentSwitch, boolean hasElevator, Runnable dataRunner) {
      this.getHasActiveHand = null;
      int oldSlot = Minecraft.player.inventory.currentItem;
      int currentSlot = -1;
      ItemStack mainhandStack = Minecraft.player.getHeldItemMainhand();
      boolean haveInMainHand = this.itemStackIsCurrentToPlace(mainhandStack);
      if (!haveInMainHand) {
         ItemStack offhandStack = Minecraft.player.getHeldItemOffhand();
         boolean haveInOffHand = this.itemStackIsCurrentToPlace(offhandStack);
         if (haveInOffHand) {
            this.getHasActiveHand = EnumHand.OFF_HAND;
         }
      } else {
         this.getHasActiveHand = EnumHand.MAIN_HAND;
      }

      if (this.getHasActiveHand == null) {
         for (int slot = 0; slot < (canPlaceFromInventory ? 44 : 9); slot++) {
            ItemStack stack = Minecraft.player.inventory.getStackInSlot(slot);
            if (this.itemStackIsCurrentToPlace(stack)) {
               currentSlot = slot;
               this.getHasActiveHand = EnumHand.MAIN_HAND;
               break;
            }
         }
      }

      boolean doSwap = this.getHasActiveHand == EnumHand.MAIN_HAND && currentSlot != -1;
      if (doSwap) {
         if (currentSlot <= 8) {
            Minecraft.player.inventory.currentItem = currentSlot;
            mc.playerController.syncCurrentPlayItem();
         } else {
            mc.playerController.windowClick(0, currentSlot, oldSlot, ClickType.SWAP, Minecraft.player);
         }

         if (this.getHasActiveHand != null) {
            dataRunner.run();
         }

         if (silentSwitch) {
            if (currentSlot <= 8) {
               Minecraft.player.inventory.currentItem = oldSlot;
               mc.playerController.syncCurrentPlayItem();
            } else {
               mc.playerController.windowClick(0, currentSlot, oldSlot, ClickType.SWAP, Minecraft.player);
            }
         }
      } else {
         if (this.getHasActiveHand != null) {
            dataRunner.run();
         }
      }
   }

   public boolean hasBlockPlaceAction(BlockPos pos, boolean canPlaceFromInventory, boolean silentSwitch, boolean hasElevator) {
      if (this.canPlaceBlock(pos, hasElevator, true)) {
         this.afterSwitchActionHand(canPlaceFromInventory, silentSwitch, hasElevator, () -> {
            if (this.hasSuccessfulRClick = this.rClickBlockLawFully(pos, this.getHasActiveHand)) {
               this.doSwingAction(this.getHasActiveHand);
            }
         });
      } else {
         this.hasSuccessfulRClick = false;
      }

      return this.hasSuccessfulRClick;
   }

   private Vec3d virtSelfPosVector() {
      return Minecraft.player.getPositionVector().addVector(0.0, 0.0, 0.0);
   }

   @Override
   public void alwaysRender2D(ScaledResolution sr) {
      float aPC;
      if ((aPC = this.stateAnim.getAnim()) * 255.0F >= 1.0F) {
         CFontRenderer font = Fonts.comfortaaBold_13;
         float strPC01 = Math.min(aPC * aPC * aPC * 1.3F, 1.0F);
         String first = MathUtils.getStringPercent("Placed: " + this.placed, strPC01);
         String second = MathUtils.getStringPercent(
            "Have: " + (int)MathUtils.clamp((float)this.haveCount * ((double)aPC > 0.95 ? 1.0F : aPC), 0.0F, (float)this.haveCount), strPC01
         );
         ItemStack current = null;
         boolean currentIsOffHand = false;
         if (this.itemStackIsCurrentToPlace(Minecraft.player.getHeldItemOffhand())) {
            current = Minecraft.player.getHeldItemOffhand();
            currentIsOffHand = true;
         } else if (this.itemStackIsCurrentToPlace(Minecraft.player.getHeldItemMainhand())) {
            current = Minecraft.player.getHeldItemMainhand();
         } else {
            for (int slot = 0; slot < (this.SwitchMode.getMode().equalsIgnoreCase("FullInvPacket") ? 44 : 9); slot++) {
               ItemStack stack = Minecraft.player.inventory.getStackInSlot(slot);
               if (this.itemStackIsCurrentToPlace(stack)) {
                  current = Minecraft.player.inventory.getStackInSlot(slot);
                  break;
               }
            }
         }

         float strWMax = Math.max(font.getStringWidth(first), font.getStringWidth(second)) + 5.0F;
         float sizeY = 18.0F;
         float offsetItem = current == null ? 0.0F : 15.0F;
         float x = (float)sr.getScaledWidth() / 2.0F - (offsetItem + strWMax) / 2.0F;
         float xPost = x + offsetItem;
         float y = (float)sr.getScaledHeight() / 1.8F - sizeY / 2.0F;
         int bgC = ColorUtils.getColor(0, 0, 9, 60.0F * aPC);
         int bgCOut1 = ClientColors.getColor1(0, aPC);
         int bgCOut2 = ClientColors.getColor2(0, aPC);
         int texC = ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), 255.0F * aPC);
         RenderUtils.drawAlphedRect((double)x, (double)y, (double)(x + offsetItem + strWMax), (double)(y + sizeY), bgC);
         float ext = 1.0F;
         RenderUtils.drawLightContureRectFullGradient(x + ext, y + ext, x + offsetItem + strWMax - ext, y + sizeY - ext, bgCOut1, bgCOut2, true);
         RenderUtils.drawLightContureRectFullGradient(x + ext, y + ext, x + offsetItem + strWMax - ext, y + sizeY - ext, bgCOut1, bgCOut2, true);
         RenderUtils.drawBloomedFullShadowFullGradientRectBool(
            x + ext,
            y + ext,
            x + offsetItem + strWMax - ext,
            y + sizeY - ext,
            sizeY / 2.0F,
            bgCOut1,
            bgCOut2,
            bgCOut2,
            bgCOut1,
            (int)(30.0F * aPC),
            true,
            false,
            true
         );
         RenderUtils.drawBloomedFullShadowFullGradientRectBool(
            x + ext,
            y + ext,
            x + offsetItem + strWMax - ext,
            y + sizeY - ext,
            sizeY / 4.0F,
            bgCOut1,
            bgCOut2,
            bgCOut2,
            bgCOut1,
            (int)(60.0F * aPC),
            true,
            false,
            true
         );
         font.drawString(first, x + strWMax / 2.0F + offsetItem - font.getStringWidth(first) / 2.0F, y + 4.0F, texC);
         font.drawString(second, x + strWMax / 2.0F + offsetItem - font.getStringWidth(second) / 2.0F, y + 4.0F + font.getHeight() + 2.0F, texC);
         if (current != null) {
            RenderUtils.drawAlphedRect((double)(x + 1.5F), (double)(y + 1.5F), (double)(x + offsetItem + 1.5F), (double)(y + offsetItem + 1.5F), bgC);
            GL11.glPushMatrix();
            RenderUtils.resetBlender();
            GL11.glDepthMask(false);
            GL11.glEnable(2929);
            RenderUtils.customScaledObject2D(
               x,
               y,
               offsetItem,
               offsetItem,
               (0.666666F + 0.3333333F * Math.min((float)this.placeDelay.getTime() / 300.0F, 1.0F)) * (float)MathUtils.easeInOutExpo((double)aPC)
            );
            RenderUtils.customRotatedObject2D(
               x,
               y,
               offsetItem,
               offsetItem,
               MathUtils.easeInOutQuadWave((double)(1.0F - Math.min((float)this.placeDelay.getTime() / 200.0F, 1.0F)))
                  * 5.0
                  * (double)(currentIsOffHand ? -1.0F : 1.0F)
            );
            GL11.glTranslatef(x + 1.0F, y + 1.0F, 0.0F);
            RenderUtils.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemIntoGUI(current, 0, 0);
            mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, current, 0, 0, "");
            GL11.glDepthMask(true);
            GL11.glPopMatrix();
         }
      }
   }

   private void onSucessPlaceBlock() {
      this.placeTick = true;
      this.placed++;
      this.placeDelay.reset();
   }

   private void updatePostPlacingActions() {
      if (this.runPlace && this.forceBlockPos != null && this.placeDelay.hasReached((double)this.forceDelay)) {
         if (this.hasBlockPlaceAction(this.forceBlockPos, this.forcePlaceFromInventory, this.forceSilentSwitch, this.forceElevator)) {
            this.onSucessPlaceBlock();
         }

         this.forceDelay = Long.MAX_VALUE;
         this.forceBlockPos = null;
         this.forcePlaceFromInventory = false;
         this.forceSilentSwitch = false;
         this.forceElevator = false;
         this.runPlace = false;
      }
   }

   private void resetActionsData() {
      this.forceDelay = Long.MAX_VALUE;
      this.forceBlockPos = null;
      this.forcePlaceFromInventory = false;
      this.forceSilentSwitch = false;
      this.forceElevator = false;
      this.runPlace = false;
      this.placeTick = false;
   }

   private void runPlaceBlockIfCan(BlockPos posToPlace, boolean useInventory, boolean silentSwitch, boolean postPlacing, boolean hasElevator) {
      if (this.possibleToPlacingStatic && posToPlace != null && this.placeDelay.hasReached((double)this.getPlaceDelay())) {
         if (!postPlacing) {
            if (this.hasBlockPlaceAction(posToPlace, useInventory, silentSwitch, hasElevator)) {
               this.onSucessPlaceBlock();
            }
         } else {
            this.forceBlockPos = posToPlace;
            this.forcePlaceFromInventory = useInventory;
            this.forceSilentSwitch = silentSwitch;
            this.forceElevator = hasElevator;
            this.forceDelay = this.getPlaceDelay();
            this.runPlace = true;
         }
      }
   }

   private Vec3d getLerpBlockFaceVec(float partialTicks, boolean getLastHandled) {
      Vec3d vec = null;
      if (this.blockFaceVec != null && this.lastBlockFaceVec != null) {
         vec = new Vec3d(
            MathUtils.lerp(this.lastBlockFaceVec.xCoord, this.blockFaceVec.xCoord, (double)partialTicks),
            MathUtils.lerp(this.lastBlockFaceVec.yCoord, this.blockFaceVec.yCoord, (double)partialTicks),
            MathUtils.lerp(this.lastBlockFaceVec.zCoord, this.blockFaceVec.zCoord, (double)partialTicks)
         );
      } else if (this.blockFaceVec != null) {
         vec = this.blockFaceVec;
      } else if (this.lastBlockFaceVec != null) {
         vec = this.lastBlockFaceVec;
      }

      if (getLastHandled) {
         if (vec != null) {
            this.lastLerpedLOCK = vec;
         }

         return this.lastLerpedLOCK;
      } else {
         return vec;
      }
   }

   private float getPC01OfNotRotatedTicksPT(float partialTicks, int maxTicksToPC1) {
      return MathUtils.clamp((float)(this.ticksNotRotated - 1) + partialTicks, 0.0F, (float)maxTicksToPC1) / (float)maxTicksToPC1;
   }

   @EventTarget
   public void onPlayerUpdateEvent(EventPlayerMotionUpdate event) {
      this.tempRotationStatus = false;
      this.possibleToPlacingStatic = false;
      this.lastRotate = null;
      if (this.blockFaceVec == null) {
         this.ticksNotRotated++;
      } else {
         this.ticksNotRotated = 0;
      }

      this.lastBlockFaceVec = this.blockFaceVec;
      this.blockFaceVec = null;
      if (this.posToPlace == null) {
         this.lastRotate = null;
      } else if (!this.canRotate()) {
         this.possibleToPlacingStatic = true;
      } else {
         this.blockFaceVec = this.placeRotateVec(this.posToPlace);
         if (this.blockFaceVec != null) {
            this.lastRotate = this.getRotation(this.blockFaceVec);
            if (this.lastRotate != null) {
               this.lastRotate = this.setRotation(event, this.lastRotate);
               this.tempRotationStatus = this.lastRotate != null;
               boolean isDownBlock = this.posToPlace == null || new BlockPos(this.updatedSelfPos).getDistanceToBlockPosXZ(this.posToPlace) == 0.0;
               this.updateObjectMouseOverSilent(
                  this.lastRotate,
                  () -> {
                     if (mc.objectMouseOver != null
                        && mc.objectMouseOver.sideHit != null
                        && (isDownBlock || mc.objectMouseOver.sideHit != EnumFacing.DOWN && mc.objectMouseOver.sideHit != EnumFacing.UP)) {
                        this.possibleToPlacingStatic = true;
                     }
                  }
               );
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      this.keepWalkJumpingSet = this.isKeepJumpWalking();
      Vec3d playerPos = this.virtSelfPosVector();
      if (this.keepWalkJumpingSet) {
         this.updatedSelfPos.xCoord = playerPos.xCoord;
         this.updatedSelfPos.zCoord = playerPos.zCoord;
         boolean canUpdateYPos = Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode())
            || this.updatedSelfPos.yCoord > playerPos.yCoord
            || this.updatedSelfPos.yCoord < playerPos.yCoord - 2.0;
         if (canUpdateYPos) {
            this.updatedSelfPos.yCoord = playerPos.yCoord;
         }
      } else {
         this.updatedSelfPos = playerPos;
      }

      boolean useInventory = this.SwitchMode.getMode().equalsIgnoreCase("FullInvPacket");
      if ((this.haveCount = this.getAnyBlocksCount(useInventory)) != 0) {
         this.controlSneaking(this.tempRotationStatus);
      }

      if (this.elevatorIsPossibleUpdated = this.elevatorIsPossible(false)) {
         this.doElevatorMoveActions();
         if (this.ElevatorBoost.getBool() && Timer.percent > (double)(Timer.get.BoundUp.getFloat() + 0.03F)) {
            Timer.forceTimer(1.5F);
         }
      }

      this.doMulSpeed();
   }

   @EventTarget
   public void onCanPlaceMillis(EventCanPlaceBlock event) {
      if (Minecraft.player == null) {
         this.resetActionsData();
      } else {
         boolean useInventory = this.SwitchMode.getMode().equalsIgnoreCase("FullInvPacket");
         boolean silentSwitch = useInventory || this.SwitchMode.getMode().equalsIgnoreCase("HotbarPacket");
         if ((this.haveCount = this.getAnyBlocksCount(useInventory)) == 0) {
            this.resetActionsData();
         } else {
            this.posToPlace = this.findBlockPosToPlace(false, this.MovePreSearchBlock.getBool());
            if (this.posToPlace != null) {
               boolean hasElevator = this.elevatorIsPossibleUpdated;
               boolean postPlacing = this.PlaceTick.currentMode.equalsIgnoreCase("Post") && !hasElevator;
               this.updatePostPlacingActions();
               this.runPlaceBlockIfCan(this.posToPlace, useInventory, silentSwitch, postPlacing, hasElevator);
            }
         }
      }
   }

   private boolean isSilentStrafeFix() {
      return this.RotateMoveSide.getBool();
   }

   @EventTarget(0)
   public void onMovementInput(EventMovementInput event) {
      if (this.isActived() && this.haveCount != 0) {
         int additionYawMovement = 0;
         if (this.JitterMovement.getBool()) {
            int tts4 = Minecraft.player.ticksExisted % 3 + 1;
            int sideX = tts4 <= 1 ? -1 : (tts4 > 2 ? 1 : 0);
            additionYawMovement = sideX * 45;
         }

         if (this.lastRotate != null && this.isSilentStrafeFix() && this.tempRotationStatus) {
            MoveMeHelp.fixDirMove(event, this.lastRotate[0] + (float)additionYawMovement);
         } else if (additionYawMovement != 0) {
            MoveMeHelp.fixDirMove(event, Minecraft.player.rotationYaw + (float)additionYawMovement);
         }

         if (this.keepWalkJumpingSet && MoveMeHelp.isMoving() && MoveMeHelp.moveYaw(0.0F) <= 45.0F) {
            event.setJump(true);
         }
      }
   }

   @EventTarget
   public void onRotationStrafe(EventRotationStrafe event) {
      if (this.isActived() && this.lastRotate != null && this.isSilentStrafeFix() && this.tempRotationStatus && this.haveCount > 0) {
         event.setYaw(this.lastRotate[0]);
      }
   }

   @EventTarget
   public void onRotationJump(EventRotationJump event) {
      if (this.isActived() && this.lastRotate != null && this.isSilentStrafeFix() && this.tempRotationStatus && this.haveCount > 0) {
         event.setYaw(this.lastRotate[0]);
      }
   }

   @EventTarget
   public void onKeysMovement(EventMoveKeys event) {
   }

   @EventTarget
   public void onSprintingSet(EventAction event) {
      if (this.isActived() && this.haveCount > 0) {
         event.setSprintState(
            this.controlSprinting(
               event,
               this.tempRotationStatus && this.lastRotate != null && RotationUtil.getAngleDifference(Minecraft.player.rotationYaw, this.lastRotate[0]) > 45.0F
            )
         );
      }
   }

   @EventTarget
   public void onSilentSneakEvent(EventSafeWalk event) {
      if (this.isActived() && this.fallSaverIsPossible()) {
         event.cancel();
      }
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      float alphaPC;
      if ((alphaPC = this.stateAnim.getAnim()) * 255.0F >= 1.0F) {
         Vec3d placeRotateVec = this.getLerpBlockFaceVec(partialTicks, true);
         float placeVecAlphaPC = (1.0F - this.getPC01OfNotRotatedTicksPT(partialTicks, 20)) * alphaPC;
         if (placeRotateVec != null && placeVecAlphaPC > 0.0F) {
            int baseColor = -1;
            int cubicColorFill = ColorUtils.swapAlpha(baseColor, placeVecAlphaPC * 10.0F);
            int cubicColorCorner = ColorUtils.swapAlpha(baseColor, placeVecAlphaPC * 155.0F);
            int cubicColorIterGlow = ColorUtils.swapAlpha(baseColor, placeVecAlphaPC * 15.0F);
            int glowIterations = (int)(6.0F * alphaPC);
            float baseCubicScale = 0.125F * (0.5F + 0.5F * (float)MathUtils.easeInOutQuad((double)placeVecAlphaPC));
            float scaleMulMaxToLastIterationGlow = 1.5F;
            Runnable drawCubic = () -> {
               AxisAlignedBB baseAABB = new AxisAlignedBB(placeRotateVec).expandXyz((double)(baseCubicScale / 2.0F));
               RenderUtils.drawCanisterBox(baseAABB, false, true, true, 0, cubicColorCorner, cubicColorFill);
               float scalePlusPerIteration = scaleMulMaxToLastIterationGlow * baseCubicScale / (float)glowIterations / 2.0F;

               for (int iterationNum = 0; iterationNum < glowIterations; iterationNum++) {
                  float ciclePC = (float)iterationNum / (float)glowIterations;
                  int editCubicColorIterGlow = ColorUtils.getOverallColorFrom(cubicColorIterGlow, 0, ciclePC);
                  baseAABB = baseAABB.expandXyz((double)scalePlusPerIteration);
                  RenderUtils.drawCanisterBox(baseAABB, true, false, false, editCubicColorIterGlow, 0, 0);
               }
            };
            RenderUtils.setup3dForBlockPos(drawCubic, true);
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.stateAnim.to = actived ? 1.0F : 0.0F;
      this.posToPlace = null;
      this.lastRotate = null;
      this.elevatorIsPossibleUpdated = false;
      this.possibleToPlacingStatic = false;
      this.blockFaceVec = null;
      this.lastBlockFaceVec = null;
      this.lastLerpedLOCK = null;
      this.resetActionsData();
      if (actived) {
         this.keepWalkJumpingSet = Minecraft.player != null && this.isKeepJumpWalking();
         if (Minecraft.player != null && this.canRotate()) {
            RotationUtil.Yaw = Minecraft.player.rotationYaw;
            RotationUtil.Pitch = Minecraft.player.rotationPitch;
         }

         if (Minecraft.player != null && Minecraft.player.inventory != null) {
            this.hotbarSlotOnEnable = Minecraft.player.inventory.currentItem;
         }
      } else {
         mc.gameSettings.keyBindSneak.pressed = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()) && mc.currentScreen == null;
         this.placed = 0;
         if (this.hotbarSlotOnEnable != -1
            && this.SwitchMode.getMode().equalsIgnoreCase("HotbarClient")
            && Minecraft.player != null
            && Minecraft.player.inventory != null) {
            Minecraft.player.inventory.currentItem = this.hotbarSlotOnEnable;
            this.hotbarSlotOnEnable = -1;
         }
      }

      super.onToggled(actived);
   }
}
