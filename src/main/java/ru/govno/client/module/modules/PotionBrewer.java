package ru.govno.client.module.modules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketClickWindow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IInteractionObject;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.event.EventTarget;
import ru.govno.client.event.events.EventPlayerMotionUpdate;
import ru.govno.client.module.Module;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.BrewUtil;
import ru.govno.client.utils.Combat.RotationUtil;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class PotionBrewer extends Module {
   public static PotionBrewer get;
   private final BoolSettings doBrewStrength;
   private final BoolSettings doBrewSpeed;
   private final BoolSettings doBrewResistance;
   private final BoolSettings doBrewHealing;
   private final BoolSettings doBrewRegen;
   private final BoolSettings doSplashPotion;
   private final BoolSettings doEffectBoost;
   private final BrewUtil brewUtil;
   public final int[] SLOTS_BOTTLES = new int[]{0, 1, 2};
   public final int SLOT_INGRIDIENT = 3;
   public final int SLOT_POWER = 4;
   private static final PotionBrewer.BrewStand openedBrewStand = null;
   private final CopyOnWriteArrayList<PotionBrewer.BrewStand> brewingStands = new CopyOnWriteArrayList<>();
   private PotionBrewer.BrewStand brewStandToOpen;
   private PotionBrewer.BrewStand lastOpennedBrewStand;
   private final TimerHelper timeInScreenBrewer = TimerHelper.TimerHelperReseted();
   private GuiScreen screenOnReceive;
   private GuiScreen screenPreReceive;
   private Container containerOnReceive;
   private Container containerPreReceive;
   private int windowIdOnReceive;
   private boolean rotateStatus;
   private Vec3d targetRotateVec;

   public PotionBrewer() {
      super("PotionBrewer", 0, Module.Category.MISC);
      this.settings.add(this.doBrewStrength = new BoolSettings("BrewStrength", true, this));
      this.settings.add(this.doBrewSpeed = new BoolSettings("BrewSpeed", true, this));
      this.settings.add(this.doBrewResistance = new BoolSettings("BrewResistance", true, this));
      this.settings.add(this.doBrewHealing = new BoolSettings("BrewHealing", true, this));
      this.settings.add(this.doBrewRegen = new BoolSettings("BrewRegen", true, this));
      this.settings.add(this.doSplashPotion = new BoolSettings("DoPotionSplash", true, this));
      this.settings.add(this.doEffectBoost = new BoolSettings("DoPotionBoostEffect", true, this));
      this.brewUtil = new BrewUtil();
      this.setDemand(0, 0);
      get = this;
   }

   @Override
   public boolean isBetaModule() {
      return true;
   }

   private double getHandlingRange() {
      return 16.0 * (double)mc.gameSettings.renderDistanceChunks;
   }

   private double getTargetingRange() {
      return 12.0;
   }

   private double getOpeningRange() {
      double blockGapAtCanter = 0.5;
      return 5.5;
   }

   private int getBrewingTicksMax() {
      return 400;
   }

   private int getTimeOutCheckTicks() {
      return 200;
   }

   private int maxWaitTicksAfterBrew() {
      return 10;
   }

   private int waitForOpenTicksPostBrewing() {
      return 3;
   }

   private CopyOnWriteArrayList<PotionBrewer.BrewStand> updateCollectHandledBrewStandsList(double handleRange, boolean moduleActived) {
      if (!moduleActived && this.brewingStands.isEmpty()) {
         return this.brewingStands;
      } else {
         this.brewingStands.removeIf(stand -> stand.alwaysUpdatedRemoveIf(handleRange, moduleActived));
         if (!moduleActived) {
            return this.brewingStands;
         } else {
            for (TileEntity tile : mc.world.getLoadedTileEntityList()) {
               if (tile instanceof TileEntityBrewingStand) {
                  TileEntityBrewingStand brewingStandTile = (TileEntityBrewingStand)tile;
                  if (brewingStandTile != null
                     && !(
                        Minecraft.player
                              .getDistanceAtEye(
                                 (double)brewingStandTile.getX() + 0.5, (double)brewingStandTile.getY() + 0.5, (double)brewingStandTile.getZ() + 0.5
                              )
                           > handleRange
                     )
                     && !this.brewingStands.stream().anyMatch(brewStand -> brewStand.looksLikeAnotherBrewTile(brewingStandTile))) {
                     this.brewingStands.add(new PotionBrewer.BrewStand(brewingStandTile));
                  }
               }
            }

            return this.brewingStands;
         }
      }
   }

   private List<PotionBrewer.BrewStand> findTargetedBrewStandsList(
      CopyOnWriteArrayList<PotionBrewer.BrewStand> handledList, double rangeTargeting, boolean distanceSorting
   ) {
      if (handledList != null && !handledList.isEmpty()) {
         List<PotionBrewer.BrewStand> copy = new ArrayList<>();
         copy.addAll(handledList.stream().filter(handled -> handled.isTargeted(rangeTargeting)).toList());
         if (distanceSorting && copy.size() > 1) {
            copy = copy.stream().sorted(Comparator.comparingDouble(a -> a.getDistance())).toList();
         }

         return copy;
      } else {
         return new ArrayList<>();
      }
   }

   private PotionBrewer.BrewStand findPossibleBrewStand(
      CopyOnWriteArrayList<PotionBrewer.BrewStand> targetedList,
      double openRange,
      boolean have3Bottles,
      boolean boostEffect,
      boolean splash,
      boolean brewStrength,
      boolean brewSpeed,
      boolean brewResistance,
      boolean brewHealing,
      boolean brewRegen
   ) {
      if (targetedList != null && !targetedList.isEmpty()) {
         return targetedList.isEmpty()
            ? null
            : targetedList.stream()
               .filter(
                  handled -> handled.canOpen(openRange, have3Bottles, boostEffect, splash, brewStrength, brewSpeed, brewResistance, brewHealing, brewRegen)
               )
               .findFirst()
               .orElse(null);
      } else {
         return null;
      }
   }

   private PotionBrewer.BrewStand findAsPosition3dBrewStand(double x, double y, double z) {
      return this.brewingStands.isEmpty() ? null : this.brewingStands.stream().filter(brewStand -> brewStand.looksLikePosition(x, y, z)).findAny().orElse(null);
   }

   private boolean callOpenBrewStand(PotionBrewer.BrewStand brewStand) {
      if (brewStand != null
         && brewStand.getBlockPos() != null
         && mc.playerController.processRightClickBlock(Minecraft.player, mc.world, brewStand.getBlockPos(), EnumFacing.UP, Vec3d.ZERO, EnumHand.MAIN_HAND)
            == EnumActionResult.SUCCESS) {
         this.brewStandToOpen = brewStand;
         this.callRotation(brewStand.getPositionVecCentered());
         return true;
      } else {
         return false;
      }
   }

   private boolean callCloseBrewStand() {
      if (Minecraft.player.openContainer instanceof ContainerBrewingStand) {
         this.brewStandToOpen = null;
         this.lastOpennedBrewStand = null;
         if (this.screenPreReceive != null) {
            mc.getConnection().sendPacket(new CPacketClickWindow());
            mc.currentScreen = this.screenPreReceive;
            Minecraft.player.closeScreen();
            this.screenPreReceive = null;
         }

         if (this.containerOnReceive != null) {
            Minecraft.player.openContainer = new ContainerPlayer(Minecraft.player.inventory, !mc.world.isRemote, Minecraft.player);
            this.containerOnReceive = null;
         }

         this.containerPreReceive = null;
         return true;
      } else {
         return false;
      }
   }

   private PotionBrewer.BrewStand updateLastOpenedStandPreCallOpen() {
      if (Minecraft.player.openContainer instanceof ContainerBrewingStand) {
         this.lastOpennedBrewStand = this.brewStandToOpen;
      } else {
         this.brewStandToOpen = null;
         this.lastOpennedBrewStand = null;
      }

      return this.lastOpennedBrewStand;
   }

   private boolean have3Bottles() {
      int countItemInContainer = 0;

      for (int slotIndex = 0; slotIndex < Minecraft.player.inventory.allInventories.size(); slotIndex++) {
         ItemStack stack = Minecraft.player.inventory.getStackInSlot(slotIndex);
         BrewUtil.PotionItemData opt;
         if (stack != null && stack.getItem() == Items.POTIONITEM && (opt = new BrewUtil.PotionItemData(stack)).isWaterBottle() && opt.getMainEffect() == null) {
            countItemInContainer += stack.stackSize;
            if (countItemInContainer >= 3) {
               return true;
            }
         }
      }

      return false;
   }

   private void updateActionsBrewer(double targetRange, double openRange) {
      long timeOutInScreen = 300L;
      long processDelay = 40L;
      List<PotionBrewer.BrewStand> targetedStands = this.findTargetedBrewStandsList(this.brewingStands, targetRange, true);
      this.updateLastOpenedStandPreCallOpen();
      if (!targetedStands.isEmpty()
         && !(Minecraft.player.openContainer instanceof ContainerBrewingStand)
         && openedBrewStand == null
         && (mc.currentScreen == null || mc.currentScreen instanceof GuiBrewingStand)) {
         boolean have3Bottles = this.have3Bottles();
         PotionBrewer.BrewStand opennableBrewStand = this.findPossibleBrewStand(
            this.brewingStands,
            openRange,
            have3Bottles,
            this.doEffectBoost.getBool(),
            this.doSplashPotion.getBool(),
            this.doBrewStrength.getBool(),
            this.doBrewSpeed.getBool(),
            this.doBrewResistance.getBool(),
            this.doBrewHealing.getBool(),
            this.doBrewRegen.getBool()
         );
         if (opennableBrewStand != null && this.lastOpennedBrewStand == null) {
            this.callOpenBrewStand(opennableBrewStand);
         }
      }

      if (this.lastOpennedBrewStand == null) {
         this.callCloseBrewStand();
      } else {
         this.callRotation(this.lastOpennedBrewStand.getPositionVecCentered());
         if (this.timeInScreenBrewer.hasReached(300.0)) {
            this.lastOpennedBrewStand.setTimeOutBlockingOpenAction();
            this.callCloseBrewStand();
         }

         if (Minecraft.player.openContainer instanceof ContainerBrewingStand containerBrewingStand) {
            mc.setIngameFocus();
            Mouse.setGrabbed(true);
            this.callRotationToFeet();
            this.brewUtil
               .handleBrewingStand(
                  this.lastOpennedBrewStand,
                  containerBrewingStand,
                  this.doSplashPotion.getBool(),
                  this.doEffectBoost.getBool(),
                  40L,
                  () -> this.callCloseBrewStand(),
                  this.doBrewStrength.getBool(),
                  this.doBrewSpeed.getBool(),
                  this.doBrewResistance.getBool(),
                  this.doBrewHealing.getBool(),
                  this.doBrewRegen.getBool()
               );
         }
      }
   }

   @Override
   public void alwaysUpdateLimitedDelay() {
      this.updateCollectHandledBrewStandsList(this.getHandlingRange(), this.isActived());
      if (!this.brewingStands.isEmpty()) {
         this.brewingStands.forEach(PotionBrewer.BrewStand::updateOnTick);
      }

      if (this.isActived()) {
         this.updateActionsBrewer(this.getTargetingRange(), this.getOpeningRange());
      }
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

   private Vec3d closerToCameraVector(Vec3d positionObj, double shift, boolean clampToDistance) {
      Vec3d cameraPosition = mc.getRenderViewEntity()
         .getPositionEyes(mc.getRenderPartialTicks())
         .addVector(0.0, -0.1, 0.0)
         .add(WorldRender.get.getLastTranslated().scale(-1.0));
      if (cameraPosition == null) {
         return positionObj;
      } else {
         if (clampToDistance) {
            shift = Math.min(shift, cameraPosition.distanceTo(positionObj) + (double)mc.getRenderViewEntity().width);
         }

         float[] angles = RotationUtil.getVecNeeded(cameraPosition, positionObj);
         double yShift = (double)MathHelper.sin(MathHelper.toRadians(angles[1])) * shift;
         double decreaseShiftXZ;
         double xShift = (double)(-MathHelper.sin(MathHelper.toRadians(angles[0])))
            * shift
            * (decreaseShiftXZ = (double)Math.max(1.0F - MathHelper.abs(angles[1] / 90.0F), 0.0F));
         double zShift = (double)MathHelper.cos(MathHelper.toRadians(angles[0])) * shift * decreaseShiftXZ;
         return new Vec3d(positionObj.xCoord + xShift, positionObj.yCoord - yShift, positionObj.zCoord + zShift);
      }
   }

   private void drawStack16px(float x, float y, int colorFillRectDefault, ItemStack stack) {
      if (colorFillRectDefault != 0) {
         float round = 2.0F;
         GL11.glDisable(2884);
         RenderUtils.drawRound(x + round, y + round, 16.0F - round * 1.5F, 16.0F - round * 1.5F, round, colorFillRectDefault);
         GL11.glEnable(2884);
      }

      if (stack != null && !stack.isEmpty()) {
         RenderItem renderItem = mc.getRenderItem();
         renderItem.zLevel = -100.0F;
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GL11.glEnable(3008);
         RenderHelper.enableGUIStandardItemLighting();
         GL11.glDisable(2884);
         GL11.glDepthMask(true);
         GL11.glTranslatef(x, y, 0.0F);
         renderItem.renderItemIntoGUI(stack, 0, 0);
         renderItem.renderItemOverlays(Fonts.comfortaaBold_13, stack, 0, 0);
         GL11.glTranslatef(-x, -y, 0.0F);
         GL11.glDepthMask(false);
         GL11.glEnable(2884);
         RenderHelper.disableStandardItemLighting();
         renderItem.zLevel = 0.0F;
      } else {
         Fonts.minecraftia_20.drawCenteredString("_", x + 8.0F, y, -1);
      }
   }

   private float drawStacks16pxGetWidth(float centerX, float y, int colorFillRectDefault, boolean cancelRender, ItemStack... stacks) {
      if (cancelRender) {
         return (float)stacks.length * 16.0F;
      } else {
         centerX -= 8.0F * (float)stacks.length;

         for (ItemStack stack : stacks) {
            this.drawStack16px(centerX, y, colorFillRectDefault, stack);
            centerX += 16.0F;
         }

         return (float)stacks.length * 16.0F;
      }
   }

   private float setupAlphaPC() {
      this.stateAnim.to = this.isActived() ? 1.0F : 0.0F;
      return (double)MathUtils.getDifferenceOf(this.stateAnim.getAnim(), this.stateAnim.to) < 0.003 ? this.stateAnim.to : this.stateAnim.getAnim();
   }

   @Override
   public void alwaysRender3D(float partialTicks) {
      float alphaPC = this.setupAlphaPC();
      if (alphaPC != 0.0F && !this.brewingStands.isEmpty()) {
         double targetRange = this.getTargetingRange();
         RenderUtils.setup3dForBlockPos(
            () -> {
               GL11.glEnable(2929);
               GL11.glDepthMask(false);
               double processPlateHeight = 0.05;
               double processPlateOffsetOfEdge = 0.0624;

               for (PotionBrewer.BrewStand brewStand : this.brewingStands) {
                  if (brewStand != null) {
                     float brewPC = brewStand.getBrewingProcessPC(partialTicks);
                     float aPC = 1.0F - brewPC;
                     int mainColor = ColorUtils.getColor(10, 10, 10, 90.0F * alphaPC);
                     float waveTime500 = (float)(System.currentTimeMillis() % 500L) / 500.0F;
                     waveTime500 = (float)MathUtils.easeInOutQuadWave((double)waveTime500);
                     if (brewStand.isTargeted(targetRange)) {
                        mainColor = brewStand.getTargetPotionEffect() != null
                           ? ColorUtils.swapAlpha(brewStand.getTargetPotionEffect().getPotion().getLiquidColor(), (155.0F + 100.0F * aPC) * alphaPC)
                           : ColorUtils.getColor(40, 40, 40, (90.0F + 165.0F * waveTime500) * alphaPC);
                     }

                     if (mainColor != 0) {
                        double upwardOffsetPlate = 0.95 * (double)brewPC;
                        AxisAlignedBB aabbProgressPlate = new AxisAlignedBB(
                           (double)brewStand.getTile().getX() + 0.0624,
                           (double)brewStand.getTile().getY() + upwardOffsetPlate,
                           (double)brewStand.getTile().getZ() + 0.0624,
                           (double)brewStand.getTile().getX() + 1.0 - 0.0624,
                           (double)brewStand.getTile().getY() + upwardOffsetPlate + 0.05,
                           (double)brewStand.getTile().getZ() + 1.0 - 0.0624
                        );
                        boolean preBrewingAction = aPC == 0.0F && brewStand.getTargetPotionEffect() != null;
                        AxisAlignedBB aabbBlock = new AxisAlignedBB(brewStand.getBlockPos()).expandXyz(-0.001);
                        if (brewStand.isBrewing()) {
                           RenderUtils.drawGradientAlphaBox(aabbProgressPlate, false, true, 0, mainColor);
                           RenderUtils.drawCanisterBox(
                              aabbProgressPlate.offsetMinDown(aabbProgressPlate.minY - aabbBlock.minY - 0.05).offset(0.0, -0.05, 0.0),
                              false,
                              false,
                              true,
                              0,
                              0,
                              ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * 0.3F)
                           );
                        }

                        RenderUtils.drawCanisterBox(
                           aabbBlock,
                           true,
                           false,
                           true,
                           mainColor,
                           0,
                           ColorUtils.swapAlpha(mainColor, (float)ColorUtils.getAlphaFromColor(mainColor) * (preBrewingAction ? 0.1F * waveTime500 : 0.08F))
                        );
                        if (this.lastOpennedBrewStand != null
                           && brewStand.looksLikePosition(
                              (double)this.lastOpennedBrewStand.getTile().getX(),
                              (double)this.lastOpennedBrewStand.getTile().getY(),
                              (double)this.lastOpennedBrewStand.getTile().getZ()
                           )) {
                           RenderUtils.drawCanisterBox(
                              aabbBlock.expandXyz(-0.0624),
                              true,
                              true,
                              false,
                              ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC),
                              ColorUtils.getColor(255, 255, 255, 90.0F * alphaPC),
                              0
                           );
                        }
                     }
                  }
               }

               double toCamOffset = 0.5;
               double scale = 0.0125F;
               GL11.glEnable(2929);

               for (PotionBrewer.BrewStand brewStand : this.brewingStands) {
                  if (brewStand != null && brewStand.isStacksCheckedOnInit() && brewStand.isTargeted(targetRange)) {
                     Vec3d renderPos = this.closerToCameraVector(brewStand.getPositionVecCentered(), 0.5, true);
                     Runnable renderAction2d = () -> {
                        float expandAllSides = 4.0F;
                        float x = 0.0F;
                        float y = 0.0F;
                        float w = Math.max(
                              this.drawStacks16pxGetWidth(x, y, 0, true, brewStand.getStackPower(), brewStand.getStackIngridient()),
                              this.drawStacks16pxGetWidth(x, y + 16.0F, 0, true, brewStand.getStacksInBottleSlots())
                           )
                           + expandAllSides * 2.0F;
                        float h = 32.0F + expandAllSides * 2.0F;
                        x -= w / 2.0F;
                        y -= h / 2.0F;
                        int mainColorx = ColorUtils.getColor(20, 20, 20, 90.0F * alphaPC);
                        if (brewStand.isTargeted(targetRange) && brewStand.getTargetPotionEffect() != null) {
                           mainColorx = ColorUtils.swapAlpha(brewStand.getTargetPotionEffect().getPotion().getLiquidColor(), 255.0F * alphaPC);
                        }

                        int white = ColorUtils.getColor(255, 255, 255, 255.0F * alphaPC);
                        int bgColor = ColorUtils.getOverallColorFrom(mainColorx, white);
                        int stackBgColor = ColorUtils.toDark(ColorUtils.getOverallColorFrom(bgColor, 0, 0.333333F), 0.2F);
                        if (alphaPC < 1.0F) {
                           GL11.glPushMatrix();
                           RenderUtils.customScaledObject2D(x, y, w, h, alphaPC);
                        }

                        GL11.glDisable(2884);
                        RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
                           x, y, x + w, y + h, 8.0F, 4.0F, 1.0F, bgColor, bgColor, bgColor, bgColor, true, true, true
                        );
                        GL11.glEnable(2884);
                        GL11.glDepthMask(false);
                        this.drawStacks16pxGetWidth(
                           x + w / 2.0F, y + expandAllSides, stackBgColor, false, brewStand.getStackPower(), brewStand.getStackIngridient()
                        );
                        this.drawStacks16pxGetWidth(x + w / 2.0F, y + 16.0F + expandAllSides, stackBgColor, false, brewStand.getStacksInBottleSlots());
                        if (alphaPC < 1.0F) {
                           GL11.glPopMatrix();
                        }
                     };
                     this.transformRenderToCamera(renderPos, renderAction2d, 0.0125F);
                  }
               }

               GL11.glDepthMask(true);
            },
            true
         );
      }
   }

   public boolean onReceiveWindowCancelOpen(boolean pre, IInventory iinventory, int windowId) {
      if (!this.isActived()) {
         return false;
      } else {
         boolean var10000;
         label26: {
            if (iinventory != null && iinventory instanceof IInteractionObject it && "minecraft:brewing_stand".equals(it.getGuiID())) {
               var10000 = true;
               break label26;
            }

            var10000 = false;
         }

         boolean canCancel = var10000;
         if (canCancel) {
            if (pre) {
               this.screenOnReceive = mc.currentScreen;
               this.containerOnReceive = Minecraft.player.openContainer;
               this.windowIdOnReceive = windowId;
               this.callRotationToFeet();
            } else {
               this.screenPreReceive = mc.currentScreen;
               this.containerPreReceive = Minecraft.player.openContainer;
               this.timeInScreenBrewer.reset();
               mc.currentScreen = this.screenOnReceive;
               mc.setIngameFocus();
               Mouse.setGrabbed(true);
            }

            return false;
         } else {
            return false;
         }
      }
   }

   public void onBrewSound(double x, double y, double z) {
      if (Minecraft.player != null && !this.brewingStands.isEmpty()) {
         this.brewingStands.stream().filter(brewStand -> brewStand.looksLikePosition(x, y, z)).forEach(brewStand -> {
            brewStand.updateOnBrewingProcessEnded();
            if (this.brewUtil.handleOfflineBrewingStandOnReceiveSound(brewStand)) {
               brewStand.onBrewingEndReceived();
            }
         });
      }
   }

   private void callRotation(Vec3d rotateTo) {
   }

   private void callRotationToFeet() {
      this.rotateStatus = true;
      this.targetRotateVec = new Vec3d(
         Minecraft.player.posX - (double)MathHelper.sin(MathHelper.toRadians(Minecraft.player.rotationYaw)) * 0.01,
         Minecraft.player.posY,
         Minecraft.player.posZ + (double)MathHelper.cos(MathHelper.toRadians(Minecraft.player.rotationYaw)) * 0.01
      );
   }

   @EventTarget
   public void onUpdate(EventPlayerMotionUpdate event) {
      if (this.isActived() && this.rotateStatus && this.targetRotateVec != null) {
         float[] rotation = RotationUtil.getNeededFacing(this.targetRotateVec, false, Minecraft.player, false);
         if (rotation != null) {
            event.setYaw(rotation[0]);
            event.setPitch(rotation[1]);
            Minecraft.player.rotationYawHead = event.getYaw();
            Minecraft.player.renderYawOffset = event.getYaw();
            Minecraft.player.rotationPitchHead = event.getPitch();
            HitAura.get.rotations = new float[]{event.getYaw(), event.getPitch()};
         }

         this.rotateStatus = false;
      }
   }

   public class BrewStand {
      private final TileEntityBrewingStand brewingStandTile;
      private ItemStack stackPower;
      private ItemStack stackIngridient;
      private ItemStack stackBottle0;
      private ItemStack stackBottle1;
      private ItemStack stackBottle2;
      private int updatedBrewTimeOutTicks;
      private int ticksTimeOut;
      private int power;
      private boolean brewingStatus;
      private boolean stacksCheckedOnInit;
      private final Vec3d positionVecCentered;
      private PotionEffect targetPotionEffect;
      private int waitTicksEndOnOutTicked;
      private int ticksNotBrewing;

      public BrewStand(TileEntityBrewingStand brewingStandTile) {
         this.brewingStandTile = brewingStandTile;
         this.positionVecCentered = new Vec3d(this.brewingStandTile.getPos()).addVector(0.5, 0.5, 0.5);
      }

      public TileEntityBrewingStand getTile() {
         return this.brewingStandTile;
      }

      public int getPower() {
         return this.power;
      }

      public void updateOnTick() {
         if (this.updatedBrewTimeOutTicks > 0) {
            this.updatedBrewTimeOutTicks--;
         }

         if (this.updatedBrewTimeOutTicks == 0 && this.brewingStatus) {
            this.updateOnBrewingProcessEnded();
         }

         if (this.ticksTimeOut > 0) {
            this.ticksTimeOut--;
         }

         if (this.isBrewing()) {
            if (this.updatedBrewTimeOutTicks == 0) {
               if (this.waitTicksEndOnOutTicked >= PotionBrewer.this.maxWaitTicksAfterBrew()) {
                  this.brewingStatus = false;
               } else {
                  this.waitTicksEndOnOutTicked++;
               }
            }

            this.ticksTimeOut = 0;
            this.ticksNotBrewing = 0;
         } else {
            this.ticksNotBrewing++;
         }
      }

      public ItemStack getStackPower() {
         return this.stackPower;
      }

      public ItemStack getStackIngridient() {
         return this.stackIngridient;
      }

      public ItemStack getStackBottleInSlot(int bottleSlot) {
         switch (bottleSlot) {
            case 0:
               return this.stackBottle0;
            case 1:
               return this.stackBottle1;
            case 2:
               return this.stackBottle2;
            default:
               return this.stackBottle0;
         }
      }

      public ItemStack[] getStacksInBottleSlots() {
         return new ItemStack[]{this.stackBottle0, this.stackBottle1, this.stackBottle2};
      }

      public boolean updateStacksOfContainer(ContainerBrewingStand containerBrewingStand) {
         this.stacksCheckedOnInit = true;
         boolean anyChanged = false;
         if (this.stackPower == null || this.stackPower.getItem() != containerBrewingStand.inventorySlots.get(4).getStack().getItem()) {
            this.stackPower = containerBrewingStand.inventorySlots.get(4).getStack();
            anyChanged = true;
         }

         if (this.stackIngridient == null || this.stackIngridient.getItem() != containerBrewingStand.inventorySlots.get(3).getStack().getItem()) {
            this.stackIngridient = containerBrewingStand.inventorySlots.get(3).getStack();
            anyChanged = true;
         }

         if (this.stackBottle0 == null
            || this.stackBottle0.getItem() != containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[0]).getStack().getItem()) {
            this.stackBottle0 = containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[0]).getStack();
            anyChanged = true;
         }

         if (this.stackBottle1 == null
            || this.stackBottle1.getItem() != containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[1]).getStack().getItem()) {
            this.stackBottle1 = containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[1]).getStack();
            anyChanged = true;
         }

         if (this.stackBottle2 == null
            || this.stackBottle2.getItem() != containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[2]).getStack().getItem()) {
            this.stackBottle2 = containerBrewingStand.inventorySlots.get(PotionBrewer.this.SLOTS_BOTTLES[2]).getStack();
            anyChanged = true;
         }

         if (containerBrewingStand.tileBrewingStand != null) {
            this.power = containerBrewingStand.tileBrewingStand.getField(1);
         }

         return anyChanged;
      }

      public boolean setPowerStack(ItemStack i0) {
         boolean hasChanged = false;
         if (this.stackPower != i0) {
            this.stackPower = i0;
            hasChanged = true;
         }

         return hasChanged;
      }

      public boolean setIngridientStack(ItemStack i0) {
         boolean hasChanged = false;
         if (this.stackIngridient != i0) {
            this.stackIngridient = i0;
            hasChanged = true;
         }

         return hasChanged;
      }

      public boolean setBottleStacks(ItemStack i0, ItemStack i1, ItemStack i2) {
         boolean anyChanged = false;
         if (this.stackBottle0 != i0) {
            this.stackBottle0 = i0;
            anyChanged = true;
         }

         if (this.stackBottle1 != i1) {
            this.stackBottle1 = i1;
            anyChanged = true;
         }

         if (this.stackBottle2 != i2) {
            this.stackBottle2 = i2;
            anyChanged = true;
         }

         return anyChanged;
      }

      public void updateOnBrewingProcessStarted() {
         this.updatedBrewTimeOutTicks = PotionBrewer.this.getBrewingTicksMax();
         this.brewingStatus = true;
         this.waitTicksEndOnOutTicked = 0;
         this.ticksNotBrewing = 0;
      }

      public void updateOnBrewingProcessEnded() {
         this.updatedBrewTimeOutTicks = 0;
      }

      public boolean onBrewingEndReceived() {
         if (this.brewingStatus) {
            if (this.power > 0) {
               this.power--;
            }

            this.brewingStatus = false;
            this.ticksNotBrewing = 0;
            return true;
         } else {
            return false;
         }
      }

      public void updateCurrentPotionEffectToBrew(PotionEffect set) {
         if (set != null) {
            this.targetPotionEffect = set;
         }
      }

      public BlockPos getBlockPos() {
         return this.brewingStandTile.getPos();
      }

      public Vec3d getPositionVecCentered() {
         return this.positionVecCentered;
      }

      public double getDistance() {
         return Minecraft.player.getDistanceAtEye(this.positionVecCentered.xCoord, this.positionVecCentered.yCoord, this.positionVecCentered.zCoord);
      }

      public boolean isTargeted(double rangeTargeting) {
         return this.getDistance() < rangeTargeting;
      }

      public boolean canOpen(
         double openRange,
         boolean have3Bottles,
         boolean boostEffect,
         boolean splash,
         boolean brewStrength,
         boolean brewSpeed,
         boolean brewResistance,
         boolean brewHealing,
         boolean brewRegen
      ) {
         return !this.isBrewing()
            && this.ticksNotBrewing > PotionBrewer.this.waitForOpenTicksPostBrewing()
            && this.getDistance() < openRange
            && this.ticksTimeOut <= 0
            && (
               !this.isStacksCheckedOnInit()
                  || PotionBrewer.this.brewUtil
                     .getIfCanBrewingAnyEffect(
                        this, have3Bottles, this.getPower(), boostEffect, splash, brewStrength, brewSpeed, brewResistance, brewHealing, brewRegen
                     )
            );
      }

      public boolean alwaysUpdatedRemoveIf(double rangeHandling, boolean moduleActived) {
         return this.brewingStandTile == null
            || Module.mc.world == null
            || Module.mc.world.getLoadedTileEntityList().stream().noneMatch(tile -> this.brewingStandTile.getPos().getDistanceToBlockPos(tile.getPos()) == 0.0)
            || this.getDistance() > rangeHandling
            || !moduleActived && this.isStacksCheckedOnInit();
      }

      public boolean looksLikeAnotherBrewTile(TileEntityBrewingStand tile) {
         return tile.getPos().getDistanceToBlockPos(this.brewingStandTile.getPos()) == 0.0;
      }

      public boolean looksLikePosition(double x, double y, double z) {
         double threshold = 1.0;
         return MathUtils.getDifferenceOf((double)this.brewingStandTile.getX(), x) < threshold
            && MathUtils.getDifferenceOf((double)this.brewingStandTile.getY(), y) < threshold
            && MathUtils.getDifferenceOf((double)this.brewingStandTile.getZ(), z) < threshold;
      }

      public PotionEffect getTargetPotionEffect() {
         return this.targetPotionEffect;
      }

      public boolean isBrewing() {
         return this.brewingStatus;
      }

      public float getBrewingProcessPC(float partialTicks) {
         return MathUtils.clamp(1.0F - ((float)this.updatedBrewTimeOutTicks - partialTicks) / (float)PotionBrewer.this.getBrewingTicksMax(), 0.0F, 1.0F);
      }

      public void setTimeOutBlockingOpenAction() {
         this.ticksTimeOut = PotionBrewer.this.getTimeOutCheckTicks();
      }

      public boolean isStacksCheckedOnInit() {
         return this.stacksCheckedOnInit;
      }
   }
}
