package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class ChestChecker extends Module {
   public static ChestChecker get;
   private final TimerHelper delayOpen = TimerHelper.TimerHelperReseted();
   ModeSettings ProcessRateLevel;
   BoolSettings Rotations;
   private final List<ChestChecker.ContainerWithData> CHESTS_MEMORY = new ArrayList<>();
   private ChestChecker.ContainerWithData TARGET_TO_OPEN_CHEST;

   public ChestChecker() {
      super("ChestChecker", 0, Module.Category.PLAYER);
      this.settings.add(this.ProcessRateLevel = new ModeSettings("ProcessRateLevel", "Normal", this, new String[]{"Low", "Normal", "Fast"}));
      this.settings.add(this.Rotations = new BoolSettings("Rotations", true, this));
      this.setDemand(3, 3);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   private List<ChestChecker.ContainerWithData> getChestMemoryWithStacks() {
      return this.CHESTS_MEMORY.stream().filter(chestWithData -> !chestWithData.getAllStacksList().isEmpty()).toList();
   }

   private ChestChecker.ContainerWithData findTargetContainerForOpen() {
      boolean canDoOpeningActions = !this.CHESTS_MEMORY.isEmpty() && mc.currentScreen == null && mc.playerController != null && Minecraft.player != null;
      if (canDoOpeningActions) {
         for (ChestChecker.ContainerWithData chestWithData : this.CHESTS_MEMORY) {
            if (chestWithData.isWaitingReceive() || chestWithData.isWaitItemsChecking()) {
               break;
            }

            if (!chestWithData.isClicked()
               && !chestWithData.isChecked()
               && chestWithData.distanceToSelf() < (double)(mc.playerController.getBlockReachDistance() + 0.5F)) {
               return chestWithData;
            }
         }
      }

      return null;
   }

   private void manageListChestsMemory(boolean clear) {
      boolean chests = true;
      boolean shulkers = true;
      if (clear) {
         this.CHESTS_MEMORY.clear();
      } else if (!this.CHESTS_MEMORY.removeIf(ChestChecker.ContainerWithData::removeIf)) {
         if (mc.world != null && Minecraft.player != null) {
            for (TileEntity tile : mc.world.getLoadedTileEntityList()) {
               if (tile instanceof TileEntityChest && chests || tile instanceof TileEntityShulkerBox && shulkers) {
                  if (tile instanceof TileEntityChest) {
                     TileEntityChest chest = (TileEntityChest)tile;
                     if (chest.adjacentChestXNeg != null
                           && this.CHESTS_MEMORY.stream().anyMatch(containerWithData -> containerWithData.isOnPos(chest.adjacentChestXNeg.getPos()))
                        || chest.adjacentChestZNeg != null
                           && this.CHESTS_MEMORY.stream().anyMatch(containerWithData -> containerWithData.isOnPos(chest.adjacentChestZNeg.getPos()))
                        || chest.adjacentChestXPos != null
                           && this.CHESTS_MEMORY.stream().anyMatch(containerWithData -> containerWithData.isOnPos(chest.adjacentChestXPos.getPos()))
                        || chest.adjacentChestZPos != null
                           && this.CHESTS_MEMORY.stream().anyMatch(containerWithData -> containerWithData.isOnPos(chest.adjacentChestZPos.getPos()))) {
                        continue;
                     }
                  }

                  if (Minecraft.player
                           .getDistanceAtEye((double)((float)tile.getX() + 0.5F), (double)((float)tile.getY() + 0.5F), (double)((float)tile.getZ() + 0.5F))
                        <= 8.0
                     && (this.CHESTS_MEMORY.isEmpty() || this.CHESTS_MEMORY.stream().noneMatch(chestWithData -> chestWithData.isOnPos(tile.getPos())))) {
                     this.CHESTS_MEMORY.add(new ChestChecker.ContainerWithData(tile, 2));
                  }
               }
            }
         }

         if (this.CHESTS_MEMORY.size() > 1) {
            Collections.sort(this.CHESTS_MEMORY, Comparator.comparingDouble(obj -> -obj.distanceToSelf()));
         }
      }
   }

   private void updateListChestsMemory() {
      this.CHESTS_MEMORY.forEach(ChestChecker.ContainerWithData::onUpdate);
   }

   private boolean onReceiveInventoryWindowListChestsMemory(IInventory iinventory, int windowId) {
      boolean anyReceive = false;

      for (ChestChecker.ContainerWithData chestWithData : this.CHESTS_MEMORY) {
         if (chestWithData.onReceiveInventory(iinventory, windowId)) {
            anyReceive = true;
         }
      }

      return anyReceive;
   }

   private void onBlockClickListChestsMemory(BlockPos pos) {
      this.CHESTS_MEMORY.forEach(chestWithData -> chestWithData.onAnyClickBlock(pos));
   }

   @EventTarget
   public void onPlayerMotionUpdate(EventPlayerMotionUpdate event) {
      if (this.isActived() && Minecraft.player != null) {
         this.TARGET_TO_OPEN_CHEST = this.findTargetContainerForOpen();
         if (this.TARGET_TO_OPEN_CHEST != null) {
            BlockPos toOpenPos = this.TARGET_TO_OPEN_CHEST.getPos();
            if (toOpenPos != null) {
               if (mc.playerController != null) {
                  long delay = 100L;
                  String rotations = this.ProcessRateLevel.getMode();
                  switch (rotations) {
                     case "Low":
                        delay = 450L;
                        break;
                     case "Normal":
                        delay = 150L;
                        break;
                     case "Fast":
                        delay = 100L;
                  }

                  if (this.delayOpen.hasReached((double)delay)) {
                     this.delayOpen.reset();
                     mc.playerController.processRightClickBlock(Minecraft.player, mc.world, toOpenPos, EnumFacing.UP, Vec3d.ZERO, EnumHand.MAIN_HAND);
                     this.onBlockClickListChestsMemory(toOpenPos);
                     if (this.Rotations.getBool()) {
                        float[] rotationsx = RotationUtil.getVecNeeded(new Vec3d(toOpenPos).addVector(0.5, 0.5, 0.5), Minecraft.player.getPositionEyes(1.0F));
                        event.setYaw(rotationsx[0]);
                        event.setPitch(rotationsx[1]);
                        Minecraft.player.rotationYawHead = rotationsx[0];
                        Minecraft.player.renderYawOffset = RotationUtil.calcYawOffset(rotationsx[0]);
                        Minecraft.player.rotationPitchHead = rotationsx[1];
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onToggled(boolean actived) {
      this.manageListChestsMemory(true);
      super.onToggled(actived);
   }

   @Override
   public void onUpdate() {
      this.manageListChestsMemory(false);
      this.updateListChestsMemory();
   }

   public boolean onReceiveWindowCancelOpen(boolean pre, IInventory iinventory, int windowId) {
      return !pre && this.isActived() ? this.onReceiveInventoryWindowListChestsMemory(iinventory, windowId) : false;
   }

   private int[] drawItems2d(int x, int y, List<ItemStack> stacks, boolean silent) {
      int w = 144;
      int h = (int)((float)stacks.size() / 9.0F) * 16;
      if (!silent) {
         int index = 0;
         int prevX = x;
         RenderItem renderItem = mc.getRenderItem();
         renderItem.zLevel = -100.0F;
         RenderHelper.enableStandardItemLighting();
         GL11.glDepthMask(true);
         GL11.glEnable(3042);
         GL11.glEnable(2884);
         long time = System.currentTimeMillis();

         for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
               GL11.glPushMatrix();
               GL11.glTranslated((double)(x + 8), (double)(y + 8), 0.0);
               float timePC = (float)MathUtils.easeInOutQuadWave((double)((float)((time - (long)(index * 200)) % 2000L) / 2000.0F));
               float timePC2 = (float)MathUtils.easeInOutQuadWave((double)((float)((time - (long)(index * 400)) % 4000L) / 4000.0F));
               float yaw = MathUtils.lerp(-10.0F, 10.0F, timePC);
               float flip = MathUtils.lerp(-8.0F, 8.0F, timePC2);
               GL11.glRotatef(yaw, 0.0F, 0.0F, 1.0F);
               GL11.glRotatef(flip, 1.0F, 0.0F, 0.0F);
               GL11.glTranslated((double)(-(x + 8)), (double)(-(y + 8)), 0.0);
               GL11.glCullFace(1028);
               renderItem.renderItemIntoGUI(stack, x, y);
               GL11.glCullFace(1029);
               renderItem.renderItemOverlays(mc.fontRendererObj, stack, x, y);
               RenderUtils.drawItemWarnIfLowDur(stack, (float)x, (float)y, 1.0F, 16.0F);
               ProContainer.get.injectPostDrawStack(stack, index, (float)x, (float)y, 1.0F);
               GL11.glPopMatrix();
            }

            if (index % 9 == 8) {
               x = prevX;
               y += 16;
            } else {
               x += 16;
            }

            index++;
         }

         GL11.glDepthMask(false);
         RenderHelper.disableStandardItemLighting();
         renderItem.zLevel = 0.0F;
      }

      return new int[]{w, h};
   }

   private void transformRenderToCamera(Vec3d positionObj, Runnable renderAction2dCentered, double scale) {
      GL11.glPushMatrix();
      GL11.glTranslated(positionObj.xCoord, positionObj.yCoord, positionObj.zCoord);
      GL11.glNormal3i(1, 1, 1);
      float[] rotate = RotationUtil.getVecNeeded(
         mc.getRenderViewEntity().getPositionEyes(mc.getRenderPartialTicks()).addVector(0.0, -0.1, 0.0).add(WorldRender.get.getLastTranslated().scale(-1.0)),
         positionObj
      );
      GL11.glRotated((double)(rotate[0] + 180.0F), 0.0, -1.0, 0.0);
      GL11.glRotated((double)rotate[1], -1.0, 0.0, 0.0);
      GL11.glScaled(-scale, -scale, scale);
      renderAction2dCentered.run();
      GL11.glPopMatrix();
   }

   private Vec3d closerToCameraVector(Vec3d positionObj, float shift, boolean clampToDistance) {
      Vec3d cameraPosition = mc.getRenderViewEntity()
         .getPositionEyes(mc.getRenderPartialTicks())
         .addVector(0.0, -0.1, 0.0)
         .add(WorldRender.get.getLastTranslated().scale(-1.0));
      if (cameraPosition == null) {
         return positionObj;
      } else {
         if (clampToDistance) {
            shift = Math.min(shift, (float)cameraPosition.distanceTo(positionObj) + mc.getRenderViewEntity().width);
         }

         float[] angles = RotationUtil.getVecNeeded(cameraPosition, positionObj);
         float yShift = MathHelper.sin(MathHelper.toRadians(angles[1])) * shift;
         float decreaseShiftXZ;
         float xShift = -MathHelper.sin(MathHelper.toRadians(angles[0])) * shift * (decreaseShiftXZ = Math.max(1.0F - Math.abs(angles[1] / 90.0F), 0.0F));
         float zShift = MathHelper.cos(MathHelper.toRadians(angles[0])) * shift * decreaseShiftXZ;
         return new Vec3d(positionObj.xCoord + (double)xShift, positionObj.yCoord - (double)yShift, positionObj.zCoord + (double)zShift);
      }
   }

   private int getTileEntityStorageColor(TileEntity storage) {
      if (storage instanceof TileEntityChest chest) {
         return chest.getChestType() == BlockChest.Type.TRAP ? ColorUtils.getColor(255, 85, 0) : ColorUtils.getColor(255, 160, 10);
      } else if (storage instanceof TileEntityEnderChest) {
         return ColorUtils.getColor(120, 0, 160);
      } else {
         return storage instanceof TileEntityShulkerBox box
            ? ColorUtils.getColor(
               (int)(box.func_190592_s().field_193352_x()[0] * 255.0F),
               (int)(box.func_190592_s().field_193352_x()[1] * 255.0F),
               (int)(box.func_190592_s().field_193352_x()[2] * 255.0F)
            )
            : -1;
      }
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      if (this.isActived()) {
         List<ChestChecker.ContainerWithData> MEMORIES_WITH_STACKS = this.getChestMemoryWithStacks();
         if (!MEMORIES_WITH_STACKS.isEmpty()) {
            float toCamOffset = 1.0F;
            RenderUtils.setup3dForBlockPos(
               () -> {
                  GL11.glDepthRange(0.0, 0.01F);
                  GL11.glEnable(2929);
                  GL11.glEnable(3553);
                  GL11.glDepthMask(false);
                  int index = 0;
                  long time = System.currentTimeMillis();

                  for (ChestChecker.ContainerWithData chestWithData : MEMORIES_WITH_STACKS) {
                     float scale = MathUtils.lerp(
                        0.005F, 0.003F, (float)MathUtils.easeInOutQuadWave(MathUtils.clamp(chestWithData.distanceToSelf() - 0.75, 0.0, 1.0))
                     );
                     int indexF = index;
                     if (chestWithData != null && chestWithData.getPos() != null && RenderUtils.isInView(new AxisAlignedBB(chestWithData.getPos()))) {
                        List<ItemStack> stacks = chestWithData.getAllStacksList();
                        if (!stacks.isEmpty()) {
                           Runnable drawItemsPlate2dCentered = () -> {
                              int[] wh = this.drawItems2d(0, 0, stacks, true);
                              GL11.glDisable(2884);
                              GL11.glDepthMask(false);
                              float[] timePCS = new float[4];
                              float timePCInterval01 = 0.75F;
                              float timePCDelay = 2000.0F / (1.0F - timePCInterval01);

                              for (int i = 0; i < 4; i++) {
                                 float distance0 = 0.0F;
                                 switch (i) {
                                    case 0:
                                       distance0 = 0.0F;
                                       break;
                                    case 1:
                                       distance0 = 0.025F;
                                       break;
                                    case 2:
                                       distance0 = 0.1F;
                                       break;
                                    case 3:
                                       distance0 = 0.05F;
                                 }

                                 float timePC = (float)((time - (long)((int)((float)indexF * timePCDelay / 100.0F))) % (long)((int)timePCDelay)) / timePCDelay;
                                 timePC -= timePCInterval01;
                                 timePC = Math.max(timePC, 0.0F);
                                 timePC /= 1.0F - timePCInterval01;
                                 timePC = Math.min(timePC, 1.0F);
                                 float animation = (1.0F + timePC - distance0) % 1.0F;
                                 animation = (float)MathUtils.easeInOutQuadWave((double)animation);
                                 timePCS[i] = animation;
                              }

                              int baseColor = ColorUtils.getOverallColorFrom(this.getTileEntityStorageColor(chestWithData.tile), -1, 0.2F);
                              int[] bgColors = new int[]{
                                 ColorUtils.swapAlpha(ColorUtils.toDark(baseColor, 0.15F + timePCS[0] * timePCS[0] * 0.2F), 155.0F),
                                 ColorUtils.swapAlpha(ColorUtils.toDark(baseColor, 0.15F + timePCS[1] * timePCS[1] * 0.2F), 155.0F),
                                 ColorUtils.swapAlpha(ColorUtils.toDark(baseColor, 0.15F + timePCS[2] * timePCS[2] * 0.2F), 155.0F),
                                 ColorUtils.swapAlpha(ColorUtils.toDark(baseColor, 0.15F + timePCS[3] * timePCS[3] * 0.2F), 155.0F)
                              };
                              RenderUtils.fixShadows();
                              if (ColorUtils.getGLAlphaFromColor(bgColors[0]) > 0.05F
                                 || ColorUtils.getGLAlphaFromColor(bgColors[1]) > 0.05F
                                 || ColorUtils.getGLAlphaFromColor(bgColors[2]) > 0.05F
                                 || ColorUtils.getGLAlphaFromColor(bgColors[3]) > 0.05F) {
                                 RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                                    (float)(-wh[0]) / 2.0F,
                                    (float)(-wh[1]) / 2.0F,
                                    (float)wh[0] / 2.0F,
                                    (float)wh[1] / 2.0F,
                                    6.0F,
                                    4.0F,
                                    bgColors[0],
                                    bgColors[1],
                                    bgColors[2],
                                    bgColors[3],
                                    false,
                                    true,
                                    true
                                 );
                              }

                              GL11.glDepthMask(true);
                              GL11.glEnable(2884);
                              this.drawItems2d(-wh[0] / 2, -wh[1] / 2, stacks, false);
                           };
                           Vec3d vecPos = new Vec3d(chestWithData.getPos()).addVector(0.5, 0.6, 0.5);
                           if (chestWithData.tile instanceof TileEntityChest chest) {
                              if (chest.adjacentChestXNeg != null) {
                                 vecPos.xCoord -= 0.5;
                              } else if (chest.adjacentChestXPos != null) {
                                 vecPos.xCoord += 0.5;
                              } else if (chest.adjacentChestZNeg != null) {
                                 vecPos.zCoord -= 0.5;
                              } else if (chest.adjacentChestZPos != null) {
                                 vecPos.zCoord += 0.5;
                              }
                           }

                           Vec3d renderPos = this.closerToCameraVector(vecPos, 1.0F, true);
                           this.transformRenderToCamera(renderPos, drawItemsPlate2dCentered, (double)scale);
                        }
                     }

                     index++;
                  }

                  GL11.glDepthMask(true);
                  GL11.glDepthRange(0.0, 1.0);
               },
               false
            );
         }
      }
   }

   private class ContainerWithData {
      private final TileEntity tile;
      private final List<ItemStack> stacks;
      private boolean isChecked = false;
      private boolean waitItemsReceive = false;
      private final World worldIn;
      private final int clickTimeOutCheck;
      private boolean isClicked;
      private int ticksPostOpenClick;

      public boolean isChecked() {
         return this.isChecked;
      }

      public ContainerWithData(TileEntity tile, int clickTimeOutCheck) {
         this.tile = tile;
         this.worldIn = Module.mc.world;
         this.stacks = new ArrayList<>();
         this.clickTimeOutCheck = clickTimeOutCheck;
      }

      public boolean isClicked() {
         return this.isClicked;
      }

      public boolean isOnPos(BlockPos pos) {
         return this.tile != null && this.tile.getPos().distanceSq(pos) == 0.0;
      }

      public BlockPos getPos() {
         return this.tile != null ? this.tile.getPos() : null;
      }

      private double distanceToSelf() {
         return this.tile != null && Minecraft.player != null
            ? (double)Minecraft.player.getSmoothDistanceToCoord((float)this.tile.getX() + 0.5F, (float)this.tile.getY() + 0.5F, (float)this.tile.getZ() + 0.5F)
            : Double.MAX_VALUE;
      }

      public void onAnyClickBlock(BlockPos pos) {
         if (this.isOnPos(pos)) {
            this.isClicked = true;
            this.ticksPostOpenClick = 0;
            this.isChecked = false;
         }
      }

      public void onUpdate() {
         if (this.isClicked && ++this.ticksPostOpenClick > this.clickTimeOutCheck) {
            this.isClicked = false;
            this.ticksPostOpenClick = 0;
         }

         this.updateItemsReading();
      }

      private boolean canReceiveWindowItems() {
         return this.isClicked() && !this.isChecked;
      }

      public boolean onReceiveInventory(IInventory iinventory, int windowId) {
         if (this.canReceiveWindowItems()) {
            boolean isChest = Minecraft.player != null
               && (Minecraft.player.openContainer instanceof ContainerChest || Minecraft.player.openContainer instanceof ContainerShulkerBox);
            if (isChest) {
               this.stacks.clear();
               this.isChecked = true;
               this.waitItemsReceive = true;
               return true;
            }
         }

         return false;
      }

      public void updateItemsReading() {
         if (this.waitItemsReceive
            && Minecraft.player != null
            && (Minecraft.player.openContainer instanceof ContainerChest || Minecraft.player.openContainer instanceof ContainerShulkerBox)) {
            this.stacks.clear();
            if (Minecraft.player.openContainer instanceof ContainerChest chest) {
               for (int slot = 0; slot < chest.getLowerChestInventory().getSizeInventory(); slot++) {
                  this.stacks.add(chest.getLowerChestInventory().getStackInSlot(slot));
               }
            } else if (Minecraft.player.openContainer instanceof ContainerShulkerBox shulkerBox) {
               for (int slot = 0; slot < shulkerBox.getInventory().size(); slot++) {
                  if (slot <= 26) {
                     this.stacks.add(shulkerBox.getSlot(slot).getStack());
                  }
               }
            }

            this.waitItemsReceive = false;
            Minecraft.player.closeScreen();
            Minecraft.player.connection.sendPacket(new CPacketCloseWindow());
         }
      }

      public boolean removeIf() {
         return this.worldIn == null
            || this.worldIn != null && !this.worldIn.equals(Module.mc.world)
            || Module.mc.world.getTileEntity(this.tile.getPos()) != this.tile
            || this.distanceToSelf() > 24.0;
      }

      public boolean isWaitingReceive() {
         return this.isClicked() && !this.isChecked;
      }

      public void resetCheckStatus() {
         this.isChecked = false;
      }

      public List<ItemStack> getAllStacksList() {
         return this.stacks;
      }

      public boolean isWaitItemsChecking() {
         return this.waitItemsReceive;
      }
   }
}
