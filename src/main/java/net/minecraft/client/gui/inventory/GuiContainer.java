package net.minecraft.client.gui.inventory;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.Bypass;
import ru.govno.client.module.modules.ChestStealer;
import ru.govno.client.module.modules.ClientColors;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.module.modules.InvWalk;
import ru.govno.client.module.modules.OffHand;
import ru.govno.client.module.modules.ProContainer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Math.TimerHelper;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public abstract class GuiContainer extends GuiScreen {
   public static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");
   protected int xSize = 176;
   protected int ySize = 166;
   public Container inventorySlots;
   protected int guiLeft;
   protected int guiTop;
   private Slot theSlot;
   public Slot clickedSlot;
   public static int setedClickedSlot;
   private boolean isRightMouseClick;
   public static ItemStack draggedStack = ItemStack.field_190927_a;
   private int touchUpX;
   private int touchUpY;
   private Slot returningStackDestSlot;
   private long returningStackTime;
   private ItemStack returningStack = ItemStack.field_190927_a;
   private Slot currentDragTargetSlot;
   private long dragItemDropDelay;
   protected final Set<Slot> dragSplittingSlots = Sets.newHashSet();
   protected boolean dragSplitting;
   private int dragSplittingLimit;
   private int dragSplittingButton;
   private boolean ignoreMouseUp;
   private int dragSplittingRemnant;
   private long lastClickTime;
   private Slot lastClickSlot;
   private int lastClickButton;
   private boolean doubleClick;
   private ItemStack shiftClickedSlot = ItemStack.field_190927_a;
   public static final AnimationUtils inter = new AnimationUtils(0.0F, 0.0F, 0.1F);
   public boolean colose = false;
   boolean drop = false;
   boolean loadEC = false;
   private final List<GuiContainer.Particle> particles = new ArrayList<>();
   AnimationUtils extButton = new AnimationUtils(0.0F, 0.0F, 0.075F);
   static int stacksPerTick = 0;
   static TimerHelper timePassDrop = new TimerHelper();
   int mx;
   int my;

   public GuiContainer(Container inventorySlotsIn) {
      this.inventorySlots = inventorySlotsIn;
      this.ignoreMouseUp = true;
   }

   @Override
   public void initGui() {
      super.initGui();
      inter.speed = 0.125F;
      Minecraft.player.openContainer = this.inventorySlots;
      this.guiLeft = (width - this.xSize) / 2;
      this.guiTop = (height - this.ySize) / 2;
      inter.setAnim(!Panic.stop && ComfortUi.get.isContainerAnim() ? 0.0F : 1.0F);
      inter.to = 1.0F;
      this.colose = false;
      this.drop = false;
      this.mx = 0;
      this.my = 0;
      InvWalk.inInitScreen(this);
      ClientTune.get.playGuiContannerOpenOrCloseSong(true);
      if (ComfortUi.get.isInvParticles()) {
         ScaledResolution sr = new ScaledResolution(this.mc);

         for (int i = 0; (float)i < (float)(sr.getScaledWidth() * sr.getScaledHeight()) / 1080.0F / 2.0F; i++) {
            this.particles.add(new GuiContainer.Particle(sr));
         }
      } else if (!this.particles.isEmpty()) {
         this.particles.clear();
      }
   }

   float[] dropButtonPos(boolean isEC) {
      ScaledResolution sr = new ScaledResolution(this.mc);
      float w = Fonts.neverlose500_16.getStringWidth(isEC ? "Выложить всё" : "Выкинуть всё") / 2.0F + 6.0F;
      float x1 = (float)(sr.getScaledWidth() / 2) - w;
      float y1 = (float)(sr.getScaledHeight() / 2 - 105 - (isEC ? 30 : 0));
      float x2 = x1 + w * 2.0F;
      float y2 = y1 + 18.0F;
      return new float[]{x1, y1, x2, y2};
   }

   boolean invIsNoEmpty() {
      boolean hasStack = false;

      for (int i1 = 0; i1 < this.inventorySlots.inventorySlots.size(); i1++) {
         Slot slot = this.inventorySlots.inventorySlots.get(i1);
         if (slot.getHasStack()) {
            hasStack = true;
         }
      }

      return hasStack;
   }

   boolean canHasDropButton() {
      return !Panic.stop && this.invIsNoEmpty();
   }

   void drawButton(boolean hover, boolean isEC) {
      float[] pos = this.dropButtonPos(isEC);
      float ext = hover && !this.colose ? 1.5F : 0.0F;
      this.extButton.to = ext;
      ext = this.extButton.getAnim();
      float x1 = pos[0] - ext;
      float y1 = pos[1] - ext;
      float x2 = pos[2] + ext;
      float y2 = pos[3] + ext;
      String str = isEC ? "Выложить всё" : "Выкинуть всё";
      float aPC = GuiInventory.containerAlpha;
      aPC *= aPC;
      float w = x1 + (x2 - x1) / 2.0F;
      int cli1 = ClientColors.getColorQ(1, aPC);
      int cli2 = ClientColors.getColorQ(2, aPC);
      int cli3 = ClientColors.getColorQ(3, aPC);
      int cli4 = ClientColors.getColorQ(4, aPC);
      int cc1 = ColorUtils.swapAlpha(cli1, (float)RenderUtils.alpha(cli1) * aPC / 5.0F);
      int cc2 = ColorUtils.swapAlpha(cli2, (float)RenderUtils.alpha(cli2) * aPC / 5.0F);
      int cc3 = ColorUtils.swapAlpha(cli3, (float)RenderUtils.alpha(cli3) * aPC / 5.0F);
      int cc4 = ColorUtils.swapAlpha(cli4, (float)RenderUtils.alpha(cli4) * aPC / 5.0F);
      RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBool(x1, y1, x2, y2, 4.0F, cli1, cli2, cli3, cli4, true);
      RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
         x1, y1, x2, y2, 4.0F, 1.0F, cc1, cc2, cc3, cc4, true, true, true
      );
      if (255.0F * aPC >= 26.0F) {
         Fonts.neverlose500_16
            .drawStringWithShadow(
               str,
               x1 / 2.0F + w / 2.0F - Fonts.neverlose500_16.getStringWidth(str) / 4.0F + 2.0F + ext * 0.5F,
               y1 + (y2 - y1) / 2.0F - 2.0F,
               ColorUtils.swapAlpha(
                  ColorUtils.getOverallColorFrom(
                     ColorUtils.getFixedWhiteColor(),
                     ColorUtils.getOverallColorFrom(ColorUtils.getOverallColorFrom(cli1, cli2), ColorUtils.getOverallColorFrom(cli3, cli4)),
                     0.3333F
                  ),
                  255.0F * aPC
               )
            );
      }

      GL11.glEnable(2929);
      GL11.glDisable(2896);
      GL11.glDepthMask(true);
   }

   boolean isHoverButton(int mouseX, int mouseY, boolean isEC) {
      float[] pos = this.dropButtonPos(isEC);
      return RenderUtils.isHovered((float)mouseX, (float)mouseY, pos[0], pos[1], pos[2] - pos[0], pos[3] - pos[1]);
   }

   void droperInventory() {
      if (this.drop) {
         int maxPerTick = 2;
         boolean hasItem = false;

         for (int i1 = 0; i1 < this.inventorySlots.inventorySlots.size(); i1++) {
            Slot slot = this.inventorySlots.inventorySlots.get(i1);
            if (slot.getHasStack() && slot.getStack().getItem() != Items.air) {
               hasItem = true;
               if (stacksPerTick <= maxPerTick) {
                  if (timePassDrop.hasReached(50.0)) {
                     this.mc.playerController.windowClick(Minecraft.player.inventoryContainer.windowId, slot.slotNumber, 1, ClickType.THROW, Minecraft.player);
                     stacksPerTick++;
                  }
               } else {
                  stacksPerTick = 0;
                  if (true) {
                     timePassDrop.reset();
                  }
               }
            }
         }

         if (hasItem) {
            return;
         }

         stacksPerTick = 0;
         this.drop = false;
         this.colose = true;
         inter.to = 0.0F;
      }
   }

   void loadChest() {
      if (this.loadEC) {
         int maxPerTick = 3;
         boolean hasItem = false;
         if (Minecraft.player.openContainer != null && Minecraft.player.openContainer instanceof ContainerChest) {
            for (int i1 = 0; i1 < this.inventorySlots.inventorySlots.size(); i1++) {
               if (this.inventorySlots.inventorySlots.get(i1) != null) {
                  Slot slot = this.inventorySlots.inventorySlots.get(i1);
                  if (slot != null && slot.inventory instanceof InventoryPlayer && slot.getHasStack() && slot.getStack().getItem() != Items.air) {
                     hasItem = true;
                     boolean pcm = slot.getStack().stackSize == 1;
                     if (stacksPerTick <= maxPerTick) {
                        if (timePassDrop.hasReached(50.0) && slot != null) {
                           this.mc
                              .playerController
                              .windowClick(Minecraft.player.openContainer.windowId, slot.slotNumber, 1, ClickType.QUICK_MOVE, Minecraft.player);
                           stacksPerTick++;
                        }
                     } else {
                        stacksPerTick = 0;
                        if (true) {
                           timePassDrop.reset();
                        }
                     }
                  }
               }
            }
         }

         if (hasItem) {
            return;
         }

         stacksPerTick = 0;
         this.loadEC = false;
         this.colose = true;
         inter.setAnim(!Panic.stop && ComfortUi.get.isContainerAnim() ? 1.0F : 0.0F);
         inter.to = 0.0F;
      }
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      GuiInventory.containerAlpha = inter.getAnim();
      float scale = (float)MathUtils.easeInOutQuad((double)GuiInventory.containerAlpha);
      scale = Math.min(scale * 1.7F, 1.0F);
      if (scale != 1.0F) {
         RenderUtils.customScaledObject2D(0.0F, 0.0F, (float)width, (float)height, 0.95F + 0.05F * scale);
      }

      if (this.colose && (double)inter.getAnim() < 0.1) {
         Minecraft.player.closeScreen();
      }

      this.mx = mouseX;
      this.my = mouseY;
      if (!this.particles.isEmpty() && !Panic.stop) {
         float lpSCFactor = ScaledResolution.lpSCFactor();
         RenderUtils.glRenderStart();
         GL11.glEnable(2832);
         GL11.glDisable(3008);
         GL11.glDepthMask(false);
         float r = 3.0F;
         float a = GuiInventory.containerAlpha;
         ScaledResolution sr = new ScaledResolution(this.mc);
         GL11.glPointSize(r * lpSCFactor);
         GL11.glColor3f(1.0F, 1.0F, 1.0F);
         int index = 0;
         RenderUtils.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);

         for (GuiContainer.Particle particle : this.particles) {
            RenderUtils.buffer
               .pos(
                  (double)((particle.x + (float)mouseX / 15.0F) % (float)sr.getScaledWidth()),
                  (double)((particle.y + (float)mouseY / 7.5F) % (float)sr.getScaledHeight())
               )
               .color(1.0F, 1.0F, 1.0F, a * ((float)index % 16.0F + 1.0F) / 16.0F)
               .endVertex();
            index++;
         }

         RenderUtils.tessellator.draw();
         GL11.glBlendFunc(770, 32772);
         index = 0;

         for (GuiContainer.Particle particle : this.particles) {
            particle.updateVertex(GuiInventory.containerAlpha / 1.8F, 1.0F + (float)index / (float)this.particles.size() * 2.0F, 5000L, index);
            index++;
         }

         RenderUtils.buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);
         index = 0;

         for (GuiContainer.Particle particle : this.particles) {
            RenderUtils.buffer
               .pos(
                  (double)((particle.x + (float)mouseX / 15.0F) % (float)sr.getScaledWidth()),
                  (double)((particle.y + (float)mouseY / 7.5F) % (float)sr.getScaledHeight())
               )
               .color(1.0F, 1.0F, 1.0F, a * ((float)index % 16.0F + 1.0F) / 16.0F)
               .endVertex();
            index++;
         }

         RenderUtils.tessellator.draw();
         GL11.glBlendFunc(770, 771);
         GL11.glPointSize(1.0F);
         GL11.glEnable(3008);
         GL11.glDepthMask(true);
         GlStateManager.resetColor();
         RenderUtils.glRenderStop();
      }

      if (this.canHasDropButton() && ComfortUi.get.isAddClientButtons()) {
         this.drawButton(this.isHoverButton(mouseX, mouseY, this.isECCH()), this.isECCH());
      }

      this.droperInventory();
      this.loadChest();
      GlStateManager.pushMatrix();
      int i = this.guiLeft;
      int j = this.guiTop;
      GlStateManager.enableAlpha();
      GlStateManager.enableBlend();
      this.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
      GlStateManager.disableRescaleNormal();
      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      super.drawScreen(mouseX, mouseY, partialTicks);
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)i, (float)j, 0.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableRescaleNormal();
      this.theSlot = null;
      int k = 240;
      int l = 240;
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableBlend();
      boolean notify = !Panic.stop && ChestStealer.get.isActived() && ChestStealer.get.HasStealNotify.getBool() && this instanceof GuiInventory;

      for (int i1 = 0; i1 < this.inventorySlots.inventorySlots.size(); i1++) {
         Slot slot = this.inventorySlots.inventorySlots.get(i1);
         if (slot.canBeHovered()) {
            if (notify && ChestStealer.get.stackIsLastLooted(slot)) {
               int x = slot.xDisplayPosition;
               int y = slot.yDisplayPosition;
               int outCol = ColorUtils.getColor(160, 255, 160, 255.0F * GuiInventory.containerAlpha);
               int shCol = ColorUtils.getColor(50, 255, 50, 255.0F * GuiInventory.containerAlpha);
               float spreadAnim = (float)MathUtils.easeInOutQuadWave(
                  (double)((float)((System.currentTimeMillis() + (long)(slot.slotNumber * 72)) % 200L) / 200.0F)
               );
               int bgCol = ColorUtils.getColor(90, 255, 90, 90.0F * GuiInventory.containerAlpha * spreadAnim);
               GL11.glPushMatrix();
               GL11.glTranslated((-0.5 + Math.random()) * (double)spreadAnim, (-0.5 + Math.random()) * (double)spreadAnim, 0.0);
               this.drawSlot(slot);
               GL11.glPopMatrix();
               GlStateManager.disableLighting();
               GlStateManager.disableDepth();
               GlStateManager.colorMask(true, true, true, false);
               RenderUtils.drawLightContureRectSmooth(
                  (double)((float)x + 2.0F), (double)((float)y + 2.0F), (double)((float)x + 14.0F), (double)((float)y + 14.0F), outCol
               );
               RenderUtils.drawRoundedFullGradientOutsideShadow(
                  (float)x + 1.0F,
                  (float)y + 1.0F,
                  (float)x + 15.0F,
                  (float)y + 15.0F,
                  1.0F,
                  GuiInventory.containerAlpha + 2.0F * spreadAnim,
                  shCol,
                  shCol,
                  shCol,
                  shCol,
                  true
               );
               RenderUtils.drawAlphedRectWithBloom(
                  (double)((float)x + 3.0F), (double)((float)y + 3.0F), (double)((float)x + 13.0F), (double)((float)y + 13.0F), bgCol, true
               );
               GlStateManager.colorMask(true, true, true, true);
               GlStateManager.enableLighting();
               GlStateManager.enableDepth();
               GlStateManager.enableBlend();
            } else {
               this.drawSlot(slot);
            }
         }

         if (this.isMouseOverSlot(slot, mouseX, mouseY) && slot.canBeHovered()) {
            if (ProContainer.get.actived && ProContainer.get.ScrollItems.getBool() && Mouse.hasWheel()) {
               int whell = Mouse.getDWheel();
               if (this.mc.currentScreen instanceof GuiInventory && whell > 0) {
                  int craftnumber = -10001;
                  if ((
                        Minecraft.player.inventory.getStackInSlot(1).getItem() != slot.getStack().getItem()
                           || Minecraft.player.inventory.getStackInSlot(1) == null
                           || Minecraft.player.inventory.getStackInSlot(1).getCount2() >= Minecraft.player.inventory.getStackInSlot(1).getMaxStackSize()
                     )
                     && Minecraft.player.inventory.getStackInSlot(1).getItem() != Items.air
                     && Minecraft.player.inventory.getStackInSlot(1) != null) {
                     if ((
                           Minecraft.player.inventory.getStackInSlot(2).getItem() != slot.getStack().getItem()
                              || Minecraft.player.inventory.getStackInSlot(2) == null
                              || Minecraft.player.inventory.getStackInSlot(2).getCount2() >= Minecraft.player.inventory.getStackInSlot(1).getMaxStackSize()
                        )
                        && Minecraft.player.inventory.getStackInSlot(2).getItem() != Items.air
                        && Minecraft.player.inventory.getStackInSlot(2) != null) {
                        if ((
                              Minecraft.player.inventory.getStackInSlot(3).getItem() != slot.getStack().getItem()
                                 || Minecraft.player.inventory.getStackInSlot(3) == null
                                 || Minecraft.player.inventory.getStackInSlot(3).getCount2() >= Minecraft.player.inventory.getStackInSlot(1).getMaxStackSize()
                           )
                           && Minecraft.player.inventory.getStackInSlot(3).getItem() != Items.air
                           && Minecraft.player.inventory.getStackInSlot(3) != null) {
                           if (Minecraft.player.inventory.getStackInSlot(4).getItem() == slot.getStack().getItem()
                                 && Minecraft.player.inventory.getStackInSlot(4) != null
                                 && Minecraft.player.inventory.getStackInSlot(4).getCount2() < Minecraft.player.inventory.getStackInSlot(1).getMaxStackSize()
                              || Minecraft.player.inventory.getStackInSlot(4).getItem() == Items.air
                              || Minecraft.player.inventory.getStackInSlot(4) == null) {
                              craftnumber = 4;
                           }
                        } else {
                           craftnumber = 3;
                        }
                     } else {
                        craftnumber = 2;
                     }
                  } else {
                     craftnumber = 1;
                  }

                  if (craftnumber != -10001 && slot.getStack().getItem() != Items.air) {
                     if (slot.canBeHovered() && slot.slotNumber == craftnumber) {
                        this.handleMouseClick(this.inventorySlots.inventorySlots.get(craftnumber), craftnumber, 0, ClickType.PICKUP);
                        this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.PICKUP);
                        this.handleMouseClick(
                           slot, slot.slotNumber, Keyboard.isKeyDown(this.mc.gameSettings.keyBindSneak.getKeyCode()) ? 0 : 1, ClickType.QUICK_MOVE
                        );
                        this.handleMouseClick(this.inventorySlots.inventorySlots.get(craftnumber), craftnumber, 0, ClickType.PICKUP);
                     } else if (slot.getStack().getItem() != Items.air) {
                        this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.PICKUP);
                        this.handleMouseClick(
                           this.inventorySlots.inventorySlots.get(craftnumber),
                           craftnumber,
                           Keyboard.isKeyDown(this.mc.gameSettings.keyBindSneak.getKeyCode()) ? 0 : 1,
                           ClickType.PICKUP
                        );
                        this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.PICKUP);
                     }
                  }
               } else if (slot.getStack().getItem() != Items.air && whell < 0 && !(this.mc.currentScreen instanceof GuiContainerCreative)) {
                  this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.PICKUP);
                  this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.PICKUP);
                  this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.QUICK_MOVE);
                  this.handleMouseClick(slot, slot.slotNumber, 0, ClickType.PICKUP);
               }
            }

            if (Mouse.isButtonDown(0)
               && isShiftKeyDown()
               && ProContainer.get.actived
               && ProContainer.get.MouseTweaks.getBool()
               && this.mc.currentScreen != null
               && !(slot.getStack().getItem() instanceof ItemAir)) {
               this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.QUICK_MOVE);
            }
         }

         if (this.isMouseOverSlot(slot, mouseX, mouseY) && slot.canBeHovered()) {
            this.theSlot = slot;
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            int j1 = slot.xDisplayPosition;
            int k1 = slot.yDisplayPosition;
            GlStateManager.colorMask(true, true, true, false);
            this.drawGradientRect(
               j1,
               k1,
               j1 + 16,
               k1 + 16,
               ColorUtils.swapAlpha(-2130706433, (float)ColorUtils.getAlphaFromColor(-2130706433) * inter.anim),
               ColorUtils.swapAlpha(-2130706433, (float)ColorUtils.getAlphaFromColor(-2130706433) * inter.anim)
            );
            GlStateManager.colorMask(true, true, true, true);
            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            GlStateManager.enableBlend();
         }
      }

      RenderHelper.disableStandardItemLighting();
      this.drawGuiContainerForegroundLayer(mouseX, mouseY);
      RenderHelper.enableGUIStandardItemLighting();
      InventoryPlayer inventoryplayer = Minecraft.player.inventory;
      ItemStack itemstack = draggedStack.isEmpty() ? inventoryplayer.getItemStack() : draggedStack;
      if (!itemstack.isEmpty()) {
         int j2 = 8;
         int k2 = draggedStack.isEmpty() ? 8 : 16;
         String s = null;
         if (!draggedStack.isEmpty() && this.isRightMouseClick) {
            itemstack = itemstack.copy();
            itemstack.func_190920_e(MathHelper.ceil((float)itemstack.getCount2() / 2.0F));
         } else if (this.dragSplitting && this.dragSplittingSlots.size() > 1) {
            itemstack = itemstack.copy();
            itemstack.func_190920_e(this.dragSplittingRemnant);
            if (itemstack.isEmpty()) {
               s = TextFormatting.YELLOW + "0";
            }
         }

         this.drawItemStack(itemstack, mouseX - i - 8, mouseY - j - k2, s);
      }

      if (!this.returningStack.isEmpty()) {
         float f = (float)(Minecraft.getSystemTime() - this.returningStackTime) / 100.0F;
         if (f >= 1.0F) {
            f = 1.0F;
            this.returningStack = ItemStack.field_190927_a;
         }

         int l2 = this.returningStackDestSlot.xDisplayPosition - this.touchUpX;
         int i3 = this.returningStackDestSlot.yDisplayPosition - this.touchUpY;
         int l1 = this.touchUpX + (int)((float)l2 * f);
         int i2 = this.touchUpY + (int)((float)i3 * f);
         this.drawItemStack(this.returningStack, l1, i2, null);
      }

      GlStateManager.popMatrix();
      GlStateManager.enableLighting();
      GlStateManager.enableDepth();
      RenderHelper.enableStandardItemLighting();
      GlStateManager.popMatrix();
   }

   protected void func_191948_b(int p_191948_1_, int p_191948_2_) {
      if (Minecraft.player.inventory.getItemStack().isEmpty() && this.theSlot != null && this.theSlot.getHasStack()) {
         this.renderToolTip(this.theSlot.getStack(), p_191948_1_, p_191948_2_);
      }
   }

   private void drawItemStack(ItemStack stack, int x, int y, String altText) {
      GlStateManager.translate(0.0F, 0.0F, 32.0F);
      this.zLevel = 200.0F;
      this.itemRender.zLevel = 200.0F;
      this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
      this.itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, stack, x, y - (draggedStack.isEmpty() ? 0 : 8), altText);
      this.zLevel = 0.0F;
      this.itemRender.zLevel = 0.0F;
   }

   protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
   }

   protected abstract void drawGuiContainerBackgroundLayer(float var1, int var2, int var3);

   private void drawSlot(Slot slotIn) {
      int i = slotIn.xDisplayPosition;
      int j = slotIn.yDisplayPosition;
      ItemStack itemstack = slotIn.getStack();
      boolean flag = false;
      boolean flag1 = slotIn == this.clickedSlot && !draggedStack.isEmpty() && !this.isRightMouseClick;
      ItemStack itemstack1 = Minecraft.player.inventory.getItemStack();
      String s = null;
      if (slotIn == this.clickedSlot && !draggedStack.isEmpty() && this.isRightMouseClick && !itemstack.isEmpty()) {
         itemstack = itemstack.copy();
         itemstack.func_190920_e(itemstack.getCount2() / 2);
      } else if (this.dragSplitting && this.dragSplittingSlots.contains(slotIn) && !itemstack1.isEmpty()) {
         if (this.dragSplittingSlots.size() == 1) {
            return;
         }

         if (Container.canAddItemToSlot(slotIn, itemstack1, true) && this.inventorySlots.canDragIntoSlot(slotIn)) {
            itemstack = itemstack1.copy();
            flag = true;
            Container.computeStackSize(
               this.dragSplittingSlots, this.dragSplittingLimit, itemstack, slotIn.getStack().isEmpty() ? 0 : slotIn.getStack().getCount2()
            );
            int k = Math.min(itemstack.getMaxStackSize(), slotIn.getItemStackLimit(itemstack));
            if (itemstack.getCount2() > k) {
               s = TextFormatting.YELLOW.toString() + k;
               itemstack.func_190920_e(k);
            }
         } else {
            this.dragSplittingSlots.remove(slotIn);
            this.updateDragSplitting();
         }
      }

      this.zLevel = 100.0F;
      this.itemRender.zLevel = 100.0F;
      String s1;
      if (itemstack.isEmpty() && slotIn.canBeHovered() && (s1 = slotIn.getSlotTexture()) != null) {
         TextureAtlasSprite textureatlassprite = this.mc.getTextureMapBlocks().getAtlasSprite(s1);
         GlStateManager.disableLighting();
         this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
         float scale = !ComfortUi.get.isContainerAnim() ? 1.0F : MathUtils.clamp(inter.getAnim() * 1.25F, 0.01F, 1.0F);
         if (!Panic.stop && scale != 1.0F) {
            RenderUtils.customScaledObject2D((float)i, (float)j, 16.0F, 16.0F, scale);
         }

         RenderUtils.glRenderStart();
         GL11.glEnable(3553);
         this.drawTexturedModalRect(i, j, textureatlassprite, 16, 16);
         RenderUtils.glRenderStop();
         if (!Panic.stop && scale != 1.0F) {
            RenderUtils.customScaledObject2D((float)i, (float)j, 16.0F, 16.0F, 1.0F / scale);
         }

         GlStateManager.enableLighting();
         flag1 = true;
      }

      if (!flag1) {
         if (flag) {
            drawRect(i, (double)j, (double)(i + 16), (double)(j + 16), -2130706433);
         }

         GlStateManager.enableDepth();
         float scalex = !ComfortUi.get.isContainerAnim()
            ? 1.0F
            : (
               MathUtils.getDifferenceOf(0.0F, inter.getAnim()) < 0.1F
                  ? 0.0F
                  : (MathUtils.getDifferenceOf(1.0F, inter.getAnim()) < 0.1F ? 1.0F : inter.getAnim())
            );
         if (!Panic.stop && scalex != 1.0F && itemstack != draggedStack) {
            GlStateManager.pushMatrix();
            RenderUtils.customScaledObject2D((float)i, (float)j, 16.0F, 16.0F, scalex);
         }

         this.itemRender.renderItemAndEffectIntoGUI(Minecraft.player, itemstack, i, j);
         if (!Panic.stop) {
            ProContainer.get.injectPostDrawStack(itemstack, slotIn.slotNumber, (float)i, (float)j, GuiInventory.containerAlpha);
         }

         this.itemRender.renderItemOverlayIntoGUI(this.fontRendererObj, itemstack, i, j, s);
         if (!Panic.stop && scalex != 1.0F && itemstack != draggedStack) {
            RenderUtils.customScaledObject2D((float)i, (float)j, 16.0F, 16.0F, 1.0F / scalex);
            GlStateManager.popMatrix();
         }

         if (!Panic.stop && ProContainer.get.isActived() && ProContainer.get.ShowSlotNumbers.getBool()) {
            GL11.glDepthRange(0.0, 0.01);
            int c = ColorUtils.getColor(255, (int)(255.0F * inter.anim * inter.anim));
            if (ColorUtils.getAlphaFromColor(c) >= 33) {
               Fonts.mntsb_7.drawString("[" + slotIn.slotNumber + "]", (float)(i + 1), (float)(j + 4), c);
            }

            GL11.glDepthRange(0.0, 1.0);
         }
      }

      this.itemRender.zLevel = 0.0F;
      this.zLevel = 0.0F;
   }

   private void updateDragSplitting() {
      ItemStack itemstack = Minecraft.player.inventory.getItemStack();
      if (!itemstack.isEmpty() && this.dragSplitting) {
         if (this.dragSplittingLimit == 2) {
            this.dragSplittingRemnant = itemstack.getMaxStackSize();
         } else {
            this.dragSplittingRemnant = itemstack.getCount2();

            for (Slot slot : this.dragSplittingSlots) {
               ItemStack itemstack1 = itemstack.copy();
               ItemStack itemstack2 = slot.getStack();
               int i = itemstack2.isEmpty() ? 0 : itemstack2.getCount2();
               Container.computeStackSize(this.dragSplittingSlots, this.dragSplittingLimit, itemstack1, i);
               int j = Math.min(itemstack1.getMaxStackSize(), slot.getItemStackLimit(itemstack1));
               if (itemstack1.getCount2() > j) {
                  itemstack1.func_190920_e(j);
               }

               this.dragSplittingRemnant = this.dragSplittingRemnant - (itemstack1.getCount2() - i);
            }
         }
      }
   }

   private Slot getSlotAtPosition(int x, int y) {
      for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
         Slot slot = this.inventorySlots.inventorySlots.get(i);
         if (this.isMouseOverSlot(slot, x, y) && slot.canBeHovered()) {
            return slot;
         }
      }

      return null;
   }

   private boolean isECCH() {
      return !(this instanceof GuiInventory);
   }

   @Override
   public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (mouseButton == 0 && this.canHasDropButton() && ComfortUi.get.isAddClientButtons() && this.isHoverButton(mouseX, mouseY, this.isECCH())) {
         if (this.isECCH()) {
            this.loadEC = true;
         } else {
            this.drop = true;
         }
      }

      setedClickedSlot = -1000;
      boolean flag = mouseButton == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100;
      Slot slot = this.getSlotAtPosition(mouseX, mouseY);
      long i = Minecraft.getSystemTime();
      this.doubleClick = this.lastClickSlot == slot && i - this.lastClickTime < 250L && this.lastClickButton == mouseButton;
      this.ignoreMouseUp = false;
      if (mouseButton == 0 || mouseButton == 1 || flag) {
         int j = this.guiLeft;
         int k = this.guiTop;
         boolean flag1 = this.func_193983_c(mouseX, mouseY, j, k);
         int l = -1;
         if (slot != null) {
            l = slot.slotNumber;
         }

         if (flag1) {
            l = -999;
         }

         if (this.mc.gameSettings.touchscreen && flag1 && Minecraft.player.inventory.getItemStack().isEmpty()) {
            this.mc.displayGuiScreen(null);
            return;
         }

         if (l != -1) {
            if (this.mc.gameSettings.touchscreen) {
               if (slot != null && slot.getHasStack()) {
                  this.clickedSlot = slot;
                  draggedStack = ItemStack.field_190927_a;
                  this.isRightMouseClick = mouseButton == 1;
               } else {
                  this.clickedSlot = null;
               }
            } else if (!this.dragSplitting) {
               if (Minecraft.player.inventory.getItemStack().isEmpty()) {
                  if (mouseButton == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100) {
                     this.handleMouseClick(slot, l, mouseButton, ClickType.CLONE);
                  } else {
                     boolean flag2 = l != -999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
                     ClickType clicktype = ClickType.PICKUP;
                     if (flag2) {
                        this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack().copy() : ItemStack.field_190927_a;
                        clicktype = ClickType.QUICK_MOVE;
                     } else if (l == -999) {
                        clicktype = ClickType.THROW;
                     }

                     if (clicktype == ClickType.PICKUP) {
                        setedClickedSlot = l;
                     }

                     this.handleMouseClick(slot, l, mouseButton, clicktype);
                  }

                  this.ignoreMouseUp = true;
               } else {
                  this.dragSplitting = true;
                  this.dragSplittingButton = mouseButton;
                  this.dragSplittingSlots.clear();
                  if (mouseButton == 0) {
                     this.dragSplittingLimit = 0;
                  } else if (mouseButton == 1) {
                     this.dragSplittingLimit = 1;
                  } else if (mouseButton == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100) {
                     this.dragSplittingLimit = 2;
                  }
               }
            }
         }
      }

      this.lastClickSlot = slot;
      this.lastClickTime = i;
      this.lastClickButton = mouseButton;
   }

   protected boolean func_193983_c(int p_193983_1_, int p_193983_2_, int p_193983_3_, int p_193983_4_) {
      return p_193983_1_ < p_193983_3_ || p_193983_2_ < p_193983_4_ || p_193983_1_ >= p_193983_3_ + this.xSize || p_193983_2_ >= p_193983_4_ + this.ySize;
   }

   @Override
   protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
      Slot slot = this.getSlotAtPosition(mouseX, mouseY);
      ItemStack itemstack = Minecraft.player.inventory.getItemStack();
      if (this.clickedSlot != null && this.mc.gameSettings.touchscreen) {
         if (clickedMouseButton == 0 || clickedMouseButton == 1) {
            if (draggedStack.isEmpty()) {
               if (slot != this.clickedSlot && !this.clickedSlot.getStack().isEmpty()) {
                  draggedStack = this.clickedSlot.getStack().copy();
               }
            } else if (draggedStack.getCount2() > 1 && slot != null && Container.canAddItemToSlot(slot, draggedStack, false)) {
               long i = Minecraft.getSystemTime();
               if (this.currentDragTargetSlot == slot) {
                  if (i - this.dragItemDropDelay > 500L) {
                     this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, ClickType.PICKUP);
                     this.handleMouseClick(slot, slot.slotNumber, 1, ClickType.PICKUP);
                     this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, 0, ClickType.PICKUP);
                     this.dragItemDropDelay = i + 750L;
                     draggedStack.func_190918_g(1);
                  }
               } else {
                  this.currentDragTargetSlot = slot;
                  this.dragItemDropDelay = i;
               }
            }
         }
      } else if (this.dragSplitting
         && slot != null
         && !itemstack.isEmpty()
         && (itemstack.getCount2() > this.dragSplittingSlots.size() || this.dragSplittingLimit == 2)
         && Container.canAddItemToSlot(slot, itemstack, true)
         && slot.isItemValid(itemstack)
         && this.inventorySlots.canDragIntoSlot(slot)) {
         this.dragSplittingSlots.add(slot);
         this.updateDragSplitting();
      }
   }

   @Override
   protected void mouseReleased(int mouseX, int mouseY, int state) {
      Slot slot = this.getSlotAtPosition(mouseX, mouseY);
      int i = this.guiLeft;
      int j = this.guiTop;
      boolean flag = this.func_193983_c(mouseX, mouseY, i, j);
      int k = -1;
      if (slot != null) {
         k = slot.slotNumber;
      }

      if (flag) {
         k = -999;
      }

      if (this.doubleClick && slot != null && state == 0 && this.inventorySlots.canMergeSlot(ItemStack.field_190927_a, slot)) {
         if (isShiftKeyDown()) {
            if (!this.shiftClickedSlot.isEmpty()) {
               for (Slot slot2 : this.inventorySlots.inventorySlots) {
                  if (slot2 != null
                     && slot2.canTakeStack(Minecraft.player)
                     && slot2.getHasStack()
                     && slot2.inventory == slot.inventory
                     && Container.canAddItemToSlot(slot2, this.shiftClickedSlot, true)) {
                     this.handleMouseClick(slot2, slot2.slotNumber, state, ClickType.QUICK_MOVE);
                  }
               }
            }
         } else {
            this.handleMouseClick(slot, k, state, ClickType.PICKUP_ALL);
         }

         this.doubleClick = false;
         this.lastClickTime = 0L;
      } else {
         if (this.dragSplitting && this.dragSplittingButton != state) {
            this.dragSplitting = false;
            this.dragSplittingSlots.clear();
            this.ignoreMouseUp = true;
            return;
         }

         if (this.ignoreMouseUp) {
            this.ignoreMouseUp = false;
            return;
         }

         if (this.clickedSlot != null && this.mc.gameSettings.touchscreen) {
            if (state == 0 || state == 1) {
               if (draggedStack.isEmpty() && slot != this.clickedSlot) {
                  draggedStack = this.clickedSlot.getStack();
               }

               boolean flag2 = Container.canAddItemToSlot(slot, draggedStack, false);
               if (k != -1 && !draggedStack.isEmpty() && flag2) {
                  this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, state, ClickType.PICKUP);
                  this.handleMouseClick(slot, k, 0, ClickType.PICKUP);
                  if (Minecraft.player.inventory.getItemStack().isEmpty()) {
                     this.returningStack = ItemStack.field_190927_a;
                  } else {
                     this.handleMouseClick(this.clickedSlot, this.clickedSlot.slotNumber, state, ClickType.PICKUP);
                     this.touchUpX = mouseX - i;
                     this.touchUpY = mouseY - j;
                     this.returningStackDestSlot = this.clickedSlot;
                     this.returningStack = draggedStack;
                     this.returningStackTime = Minecraft.getSystemTime();
                  }
               } else if (!draggedStack.isEmpty()) {
                  this.touchUpX = mouseX - i;
                  this.touchUpY = mouseY - j;
                  this.returningStackDestSlot = this.clickedSlot;
                  this.returningStack = draggedStack;
                  this.returningStackTime = Minecraft.getSystemTime();
               }

               draggedStack = ItemStack.field_190927_a;
               this.clickedSlot = null;
            }
         } else if (this.dragSplitting && !this.dragSplittingSlots.isEmpty()) {
            this.handleMouseClick(null, -999, Container.getQuickcraftMask(0, this.dragSplittingLimit), ClickType.QUICK_CRAFT);

            for (Slot slot1 : this.dragSplittingSlots) {
               this.handleMouseClick(slot1, slot1.slotNumber, Container.getQuickcraftMask(1, this.dragSplittingLimit), ClickType.QUICK_CRAFT);
            }

            this.handleMouseClick(null, -999, Container.getQuickcraftMask(2, this.dragSplittingLimit), ClickType.QUICK_CRAFT);
         } else if (!Minecraft.player.inventory.getItemStack().isEmpty()) {
            if (state == this.mc.gameSettings.keyBindPickBlock.getKeyCode() + 100) {
               this.handleMouseClick(slot, k, state, ClickType.CLONE);
            } else {
               boolean flag1 = k != -999 && (Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54));
               if (flag1) {
                  this.shiftClickedSlot = slot != null && slot.getHasStack() ? slot.getStack().copy() : ItemStack.field_190927_a;
               }

               this.handleMouseClick(slot, k, state, flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
            }
         }
      }

      if (Minecraft.player.inventory.getItemStack().isEmpty()) {
         this.lastClickTime = 0L;
      }

      this.dragSplitting = false;
   }

   private boolean isMouseOverSlot(Slot slotIn, int mouseX, int mouseY) {
      return this.isPointInRegion(slotIn.xDisplayPosition, slotIn.yDisplayPosition, 16, 16, mouseX, mouseY);
   }

   protected boolean isPointInRegion(int rectX, int rectY, int rectWidth, int rectHeight, int pointX, int pointY) {
      int i = this.guiLeft;
      int j = this.guiTop;
      int var9;
      int var10;
      return (var9 = pointX - i) >= rectX - 1 && var9 < rectX + rectWidth + 1 && (var10 = pointY - j) >= rectY - 1 && var10 < rectY + rectHeight + 1;
   }

   public void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
      if (this.inventorySlots.windowId == 0
         && slotIn != null
         && ProContainer.get.isActived()
         && ProContainer.get.AutoArmor.getBool()
         && slotId >= 0
         && slotId <= 44) {
         ItemStack stack = Minecraft.player.inventory.getStackInSlot(slotId);
         if (stack != null) {
            boolean armorClickedEQ = slotIn.slotNumber >= 5
               && slotIn.slotNumber <= 8
               && Minecraft.player.inventory.armorItemInSlot(slotIn.slotNumber - 5).getItem() instanceof ItemArmor;
            boolean armorClickedDEQ = slotIn.slotNumber >= 9
               && slotIn.slotNumber <= 44
               && Minecraft.player.inventoryContainer.getSlot(slotIn.slotNumber).getStack().getItem() instanceof ItemArmor;
            if (armorClickedEQ) {
               ProContainer.autoArmorOFF = true;
            }

            if (armorClickedDEQ) {
               ProContainer.autoArmorOFF = false;
            }

            ProContainer.get.timer.reset();
         }
      }

      if (slotIn != null) {
         slotId = slotIn.slotNumber;
      }

      this.mc.playerController.windowClick(this.inventorySlots.windowId, slotId, mouseButton, type, Minecraft.player);
   }

   public boolean itemOne(int slotIn) {
      if (Minecraft.player.inventoryContainer.getSlot(slotIn) != null && Minecraft.player.inventoryContainer.getSlot(slotIn).getStack() != null) {
         ItemStack itemStack = Minecraft.player.inventoryContainer.getSlot(slotIn).getStack();
         return itemStack.stackSize == 1;
      } else {
         return false;
      }
   }

   @Override
   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      if (keyCode == this.mc.gameSettings.keyBindSwapHands.getKeyCode()
         && !Panic.stop
         && this.mc.currentScreen != null
         && this.mc.currentScreen instanceof GuiInventory
         && ProContainer.get.actived
         && ProContainer.get.QuickSwap.getBool()) {
         if (Minecraft.player.hasNewVersionMoves) {
            Slot hoveredSlot = null;

            for (Slot slot : this.inventorySlots.inventorySlots) {
               if (this.isMouseOverSlot(slot, this.mx, this.my)) {
                  hoveredSlot = slot;
                  break;
               }
            }

            if (hoveredSlot != null) {
               try {
                  this.mc.playerController.windowClick(0, hoveredSlot.slotNumber, 40, ClickType.SWAP, Minecraft.player);
               } catch (Exception var7) {
                  var7.printStackTrace();
               }
            }
         } else {
            int dragSlot = -1;

            for (int i1 = 0; i1 < this.inventorySlots.inventorySlots.size(); i1++) {
               Slot slotx = this.inventorySlots.inventorySlots.get(i1);
               if (this.isMouseOverSlot(slotx, this.mx, this.my) && slotx.canBeHovered()) {
                  dragSlot = i1;
               }
            }

            if (dragSlot != -1
               && dragSlot != 45
               && (
                  !(Minecraft.player.inventoryContainer.getSlot(dragSlot).getStack().getItem() instanceof ItemAir)
                     || !(Minecraft.player.inventoryContainer.getSlot(45).getStack().getItem() instanceof ItemAir)
               )) {
               boolean a = !Minecraft.player.getHeldItemOffhand().isEmpty();
               if (a) {
                  this.mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, Minecraft.player);
               }

               boolean aired = Minecraft.player.inventoryContainer.getSlot(dragSlot).getStack().getItem() instanceof ItemAir;
               if (Bypass.get.isAACWinClick()) {
                  int delay = 50;
                  this.mc
                     .playerController
                     .windowClickMemory(
                        0, dragSlot, !aired && dragSlot != -1 && this.itemOne(dragSlot) ? 1 : 0, ClickType.PICKUP, Minecraft.player, a ? delay * 2 : delay
                     );
                  int var13 = 75;
                  this.mc.playerController.windowClickMemory(0, 45, 0, ClickType.PICKUP, Minecraft.player, a ? var13 * 3 : var13 * 2);
               } else {
                  this.mc
                     .playerController
                     .windowClick(0, dragSlot, !aired && dragSlot != -1 && this.itemOne(dragSlot) ? 1 : 0, ClickType.PICKUP, Minecraft.player);
                  this.mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, Minecraft.player);
               }
            }
         }
      }

      if (keyCode == 1 || keyCode == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
         if (Panic.stop) {
            Minecraft.player.closeScreen();
         } else {
            this.colose = true;
            inter.to = 0.0F;
            inter.setAnim(!Panic.stop && ComfortUi.get.isContainerAnim() ? 1.0F : 0.0F);
            this.mc.setIngameFocus();
            Mouse.setGrabbed(true);
            this.drop = false;
         }

         if (ProContainer.get.isActived() && ProContainer.get.NoExitDrop.getBool() && setedClickedSlot != -1000 && this.lastClickSlot != null) {
            this.handleMouseClick(this.lastClickSlot, setedClickedSlot, 0, ClickType.PICKUP);
            setedClickedSlot = -1000;
         }

         if (Minecraft.player != null) {
            Minecraft.player.setSneaking(this.mc.gameSettings.keyBindSneak.isKeyDown());
         }
      }

      if (ProContainer.get.actived && ProContainer.get.CtrlRDroper.getBool() && Keyboard.isKeyDown(29) && keyCode == 19 && this.canHasDropButton()) {
         if (this.isECCH()) {
            this.loadEC = true;
         } else {
            this.drop = true;
         }
      }

      this.checkHotbarKeys(keyCode);
      if (this.theSlot != null && this.theSlot.getHasStack()) {
         if (keyCode == this.mc.gameSettings.keyBindPickBlock.getKeyCode()) {
            this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, 0, ClickType.CLONE);
         } else if (keyCode == this.mc.gameSettings.keyBindDrop.getKeyCode()) {
            this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, isCtrlKeyDown() ? 1 : 0, ClickType.THROW);
         }
      }
   }

   protected boolean checkHotbarKeys(int keyCode) {
      if (Minecraft.player.inventory.getItemStack().isEmpty() && this.theSlot != null) {
         for (int i = 0; i < 9; i++) {
            if (keyCode == this.mc.gameSettings.keyBindsHotbar[i].getKeyCode()) {
               this.handleMouseClick(this.theSlot, this.theSlot.slotNumber, i, ClickType.SWAP);
               if (this.theSlot.slotNumber == 45) {
                  OffHand.oldSlot = null;
               }

               return true;
            }
         }
      }

      return false;
   }

   @Override
   public void onGuiClosed() {
      this.mx = 0;
      this.my = 0;
      this.colose = false;
      this.drop = false;
      if (!Panic.stop && InvWalk.get.actived && InvWalk.get.MouseMove.getBool()) {
         Mouse.setGrabbed(true);
      }

      if (Minecraft.player != null) {
         this.inventorySlots.onContainerClosed(Minecraft.player);
      }

      if (Minecraft.player != null) {
         Minecraft.player.setSneaking(false);
      }

      ClientTune.get.playGuiContannerOpenOrCloseSong(false);
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }

   @Override
   public void updateScreen() {
      super.updateScreen();
      if (!Minecraft.player.isEntityAlive() || Minecraft.player.isDead) {
         Minecraft.player.closeScreen();
      }
   }

   class Particle {
      float x;
      float y;
      ScaledResolution sr;

      Particle(ScaledResolution sr) {
         this.x = (float)((double)sr.getScaledWidth() * Math.random());
         this.y = (float)((double)sr.getScaledHeight() * Math.random());
         this.sr = sr;
      }

      void updateVertex(float alphaPC, float speed, long timed, int index) {
         float n = (float)(
            (
                  -0.5
                     + MathUtils.easeInOutQuadWave(
                        (double)((float)((System.currentTimeMillis() + (long)((int)((float)((long)index * timed) / 8.0F))) % timed) / (float)timed)
                     )
               )
               * (double)speed
               / 2.0
         );
         this.x += n * n;
         this.y = (float)((System.currentTimeMillis() + (long)((int)((float)(index * this.sr.getScaledHeight()) / speed))) % timed)
            / (float)timed
            * speed
            * (float)this.sr.getScaledHeight();
         this.x = this.x % (float)this.sr.getScaledWidth();
         this.y = this.y % (float)this.sr.getScaledHeight();
      }
   }
}
