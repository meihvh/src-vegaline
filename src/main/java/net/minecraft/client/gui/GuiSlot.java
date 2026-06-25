package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public abstract class GuiSlot {
   protected final Minecraft mc;
   protected int width;
   protected int height;
   protected int top;
   protected int bottom;
   protected int right;
   protected int left;
   protected final int slotHeight;
   private int scrollUpButtonID;
   private int scrollDownButtonID;
   protected int mouseX;
   protected int mouseY;
   protected boolean centerListVertically = true;
   protected int initialClickY = -2;
   protected float scrollMultiplier;
   private final AnimationUtils smoothScroll = new AnimationUtils(0.0F, 0.0F, 0.02F);
   private final AnimationUtils smoothScroll2 = new AnimationUtils(0.0F, 0.0F, 0.02F);
   public float amountScroll;
   protected int selectedElement = -1;
   protected long lastClicked;
   protected boolean visible = true;
   protected boolean showSelectionBox = true;
   protected boolean hasListHeader;
   protected int headerPadding;
   private boolean enabled = true;
   private final long initTime = System.currentTimeMillis();

   private boolean isSmoothScroll() {
      return !Panic.stop && this.smoothScroll != null && this.smoothScroll2 != null && ComfortUi.get != null && ComfortUi.get.isSmoothMcScroll();
   }

   private void setupScrollAnimSpeed() {
      this.smoothScroll.speed = 0.04F + MathUtils.clamp(MathUtils.getDifferenceOf(this.smoothScroll.anim, this.smoothScroll2.to) / 60.0F, 0.0F, 1.0F) * 0.2F;
      this.smoothScroll2.speed = 0.05F;
   }

   public void addScrolled(float scroll) {
      if (!this.isSmoothScroll()) {
         this.amountScroll += scroll;
      } else {
         float scrollSpeed = (!(scroll > 0.0F) || !(this.smoothScroll.to >= (float)this.getMaxScroll()))
               && (!(scroll < 0.0F) || !(this.smoothScroll.to <= 0.0F))
            ? 5.0F
            : 0.5F;
         scroll *= scrollSpeed;
         this.smoothScroll2.setAnim(this.smoothScroll.to + scroll);
         this.smoothScroll.to += scroll;
      }
   }

   public void setScrolled(float scroll) {
      if (this.isSmoothScroll()) {
         this.smoothScroll2.to = scroll;
         this.smoothScroll.to = this.smoothScroll2.getAnim();
      } else {
         this.amountScroll = scroll;
      }
   }

   public void resetScrolled() {
      if (this.isSmoothScroll()) {
         this.smoothScroll.to = 0.0F;
      } else {
         this.amountScroll = 0.0F;
      }
   }

   public float getScrolled() {
      return this.amountScroll;
   }

   public GuiSlot(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
      this.mc = mcIn;
      this.width = width;
      this.height = height;
      this.top = topIn;
      this.bottom = bottomIn;
      this.slotHeight = slotHeightIn;
      this.left = 0;
      this.right = width;
   }

   public void setDimensions(int widthIn, int heightIn, int topIn, int bottomIn) {
      this.width = widthIn;
      this.height = heightIn;
      this.top = topIn;
      this.bottom = bottomIn;
      this.left = 0;
      this.right = widthIn;
   }

   public void func_193651_b(boolean p_193651_1_) {
      this.showSelectionBox = p_193651_1_;
   }

   protected void setHasListHeader(boolean hasListHeaderIn, int headerPaddingIn) {
      this.hasListHeader = hasListHeaderIn;
      this.headerPadding = headerPaddingIn;
      if (!hasListHeaderIn) {
         this.headerPadding = 0;
      }
   }

   protected abstract int getSize();

   protected abstract void elementClicked(int var1, boolean var2, int var3, int var4);

   protected abstract boolean isSelected(int var1);

   protected int getContentHeight() {
      return this.getSize() * this.slotHeight + this.headerPadding;
   }

   protected abstract void drawBackground();

   protected void func_192639_a(int p_192639_1_, int p_192639_2_, int p_192639_3_, float p_192639_4_) {
   }

   protected abstract void func_192637_a(int var1, int var2, int var3, int var4, int var5, int var6, float var7);

   protected void drawListHeader(int insideLeft, int insideTop, Tessellator tessellatorIn) {
   }

   protected void clickedHeader(int p_148132_1_, int p_148132_2_) {
   }

   protected void renderDecorations(int mouseXIn, int mouseYIn) {
   }

   public int getSlotIndexFromScreenCoords(int posX, int posY) {
      int i = this.left + this.width / 2 - this.getListWidth() / 2;
      int j = this.left + this.width / 2 + this.getListWidth() / 2;
      int k = posY - this.top - this.headerPadding + (int)this.getScrolled() - 4;
      int l = k / this.slotHeight;
      return posX < this.getScrollBarX() && posX >= i && posX <= j && l >= 0 && k >= 0 && l < this.getSize() ? l : -1;
   }

   public void registerScrollButtons(int scrollUpButtonIDIn, int scrollDownButtonIDIn) {
      this.scrollUpButtonID = scrollUpButtonIDIn;
      this.scrollDownButtonID = scrollDownButtonIDIn;
   }

   protected void bindAmountScrolled() {
      this.setScrolled(MathHelper.clamp(this.getScrolled(), 0.0F, (float)this.getMaxScroll()));
   }

   public int getMaxScroll() {
      return Math.max(0, this.getContentHeight() - (this.bottom - this.top - 4));
   }

   public float getAmountScrolled() {
      return this.getScrolled();
   }

   public boolean isMouseYWithinSlotBounds(int p_148141_1_) {
      return p_148141_1_ >= this.top && p_148141_1_ <= this.bottom && this.mouseX >= this.left && this.mouseX <= this.right;
   }

   public void scrollBy(int amount) {
      this.addScrolled((float)amount);
      this.bindAmountScrolled();
      this.initialClickY = -2;
   }

   public void actionPerformed(GuiButton button) {
      if (button.enabled) {
         if (button.id == this.scrollUpButtonID) {
            this.addScrolled(-((float)(this.slotHeight * 2 / 3)));
            this.initialClickY = -2;
            this.bindAmountScrolled();
         } else if (button.id == this.scrollDownButtonID) {
            this.addScrolled((float)(this.slotHeight * 2 / 3));
            this.initialClickY = -2;
            this.bindAmountScrolled();
         }
      }
   }

   protected void drawServers(int x1, int y1, int p_192638_3_, int p_192638_4_, float p_192638_5_) {
      int i = this.getSize();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();

      for (int index = 0; index < i; index++) {
         ServerData nullableServerData;
         boolean var10000;
         label46: {
            nullableServerData = null;
            if (this.mc.currentScreen instanceof GuiMultiplayer gui
               && gui.getServerList() != null
               && index < gui.getServerList().countServers()
               && (nullableServerData = gui.getServerList().getServerData(index)) != null
               && gui.getServerList().getServerData(index).isClientDataServer()
               && gui.getServerList().getServerData(index).hasClientData()) {
               var10000 = true;
               break label46;
            }

            var10000 = false;
         }

         boolean clientDrawing = var10000;
         int yElement = y1 + index * this.slotHeight + this.headerPadding;
         int l = this.slotHeight - 4;
         if (yElement > this.bottom || yElement + l < this.top) {
            this.func_192639_a(index, x1, yElement, p_192638_5_);
         }

         if (this.showSelectionBox && this.isSelected(index)) {
            int i1 = this.left + (this.width / 2 - this.getListWidth() / 2);
            int j1 = this.left + this.width / 2 + this.getListWidth() / 2;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableTexture2D();
            if (clientDrawing && nullableServerData != null) {
               try {
                  Client.ServerListPresetElement clientData = nullableServerData.getClientData();
                  int baseCol0 = clientData.getServerColor();
                  int baseColDark0 = ColorUtils.toDark(baseCol0, 0.1F);
                  int baseCol1 = clientData.getServerColor();
                  int baseColDark1 = ColorUtils.toDark(baseCol1, 0.8F);
                  int fade0 = ColorUtils.fadeColor(baseColDark0, baseCol0, 0.3F, 90 - yElement);
                  int fade1 = ColorUtils.fadeColor(baseColDark1, baseCol1, 0.3F, -yElement);
                  float expandXY = 2.5F + 1.5F * (float)MathUtils.easeInOutQuadWave((double)((float)(System.currentTimeMillis() % 2000L) / 2000.0F));
                  float shFullRound = 2.5F;
                  int iterations = 1
                     + (int)(MathUtils.clamp(1.0F - MathUtils.valWave01((float)(System.currentTimeMillis() % 3000L) / 3000.0F) * 4.0F, 0.0F, 1.0F) * 4.0F);
                  float expandStep = 4.0F;

                  for (int iter = 0; iter < iterations; iter++) {
                     float ciclePC01 = (float)iter / (float)iterations;
                     float aPC = Math.max(1.0F - ciclePC01, 0.0F);
                     aPC *= aPC;
                     float x = (float)x1 - expandXY - 0.5F;
                     float x2 = (float)j1 + expandXY - 1.5F;
                     float y = (float)yElement - expandXY - 0.5F;
                     float y2 = y + (float)l + 1.0F + expandXY * 2.0F;
                     expandXY += expandStep;
                     int fadeSH0 = ColorUtils.fadeColor(ColorUtils.fadeColor(-1, fade0, 0.8F), fade0, 0.2F);
                     fadeSH0 = ColorUtils.swapAlpha(fadeSH0, (float)ColorUtils.getAlphaFromColor(fadeSH0) * aPC);
                     int fadeSH1 = ColorUtils.fadeColor(ColorUtils.fadeColor(-1, fade1, 0.8F), fade1, 0.2F);
                     fadeSH1 = ColorUtils.swapAlpha(fadeSH1, (float)ColorUtils.getAlphaFromColor(fadeSH1) * aPC);
                     RenderUtils.drawOutsideAndInsideFullRoundedFullGradientShadowRectWithBloomBoolShadowsBoolChangeShadowSize(
                        x, y, x2, y2, shFullRound, shFullRound / 2.0F, shFullRound / 2.0F, fadeSH0, fadeSH1, fadeSH1, fadeSH0, true, true, true
                     );
                  }
               } catch (Exception var36) {
                  var36.printStackTrace();
               }
            } else {
               bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               bufferbuilder.pos((double)i1, (double)(yElement + l + 2), 0.0).tex(0.0, 1.0).color(128, 128, 128, 255).endVertex();
               bufferbuilder.pos((double)j1, (double)(yElement + l + 2), 0.0).tex(1.0, 1.0).color(128, 128, 128, 255).endVertex();
               bufferbuilder.pos((double)j1, (double)(yElement - 2), 0.0).tex(1.0, 0.0).color(128, 128, 128, 255).endVertex();
               bufferbuilder.pos((double)i1, (double)(yElement - 2), 0.0).tex(0.0, 0.0).color(128, 128, 128, 255).endVertex();
               bufferbuilder.pos((double)(i1 + 1), (double)(yElement + l + 1), 0.0).tex(0.0, 1.0).color(0, 0, 0, 255).endVertex();
               bufferbuilder.pos((double)(j1 - 1), (double)(yElement + l + 1), 0.0).tex(1.0, 1.0).color(0, 0, 0, 255).endVertex();
               bufferbuilder.pos((double)(j1 - 1), (double)(yElement - 1), 0.0).tex(1.0, 0.0).color(0, 0, 0, 255).endVertex();
               bufferbuilder.pos((double)(i1 + 1), (double)(yElement - 1), 0.0).tex(0.0, 0.0).color(0, 0, 0, 255).endVertex();
               tessellator.draw();
            }

            GlStateManager.enableTexture2D();
         }

         if (yElement >= this.top - this.slotHeight && yElement <= this.bottom) {
            this.func_192637_a(index, x1, yElement, l, p_192638_3_, p_192638_4_, p_192638_5_);
         }
      }
   }

   public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
      if (this.isSmoothScroll()) {
         this.setupScrollAnimSpeed();
         float scrolled = this.smoothScroll.getAnim();
         this.amountScroll = scrolled;
      }

      ScaledResolution sr = new ScaledResolution(this.mc);
      if (this.visible) {
         this.mouseX = mouseXIn;
         this.mouseY = mouseYIn;
         if (Panic.stop || Client.moduleManager == null || !ComfortUi.get.isScreensDarking() || this.mc.world == null) {
            this.drawBackground();
         }

         int i = this.getScrollBarX();
         int j = i + 6;
         this.bindAmountScrolled();
         GlStateManager.disableLighting();
         GlStateManager.disableFog();
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferbuilder = tessellator.getBuffer();
         if (Panic.stop || Client.moduleManager == null || !ComfortUi.get.isScreensDarking() || this.mc.world == null) {
            this.drawContainerBackground(tessellator);
         }

         int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
         int l = this.top + 4 - (int)this.getScrolled();
         if ((
               this.mc.currentScreen instanceof GuiMultiplayer
                  || this.mc.currentScreen instanceof GuiWorldSelection
                  || this.mc.currentScreen instanceof GuiConnecting
            )
            && !Panic.stop) {
            RenderUtils.drawScreenShaderBackground(sr, this.mouseX, this.mouseY);
         }

         if ((this.mc.currentScreen instanceof GuiMultiplayer || this.mc.currentScreen instanceof GuiWorldSelection) && !Panic.stop) {
            int bgColor = ColorUtils.getColor(0, 0, 0, 140);
            RenderUtils.drawAlphedRect(
               (double)(sr.getScaledWidth() / 2 - 170), 0.0, (double)(sr.getScaledWidth() / 2 + 170), (double)sr.getScaledHeight(), bgColor
            );
            RenderUtils.drawAlphedSideways(
               (double)(sr.getScaledWidth() / 2 - 200), 0.0, (double)(sr.getScaledWidth() / 2 - 170), (double)sr.getScaledHeight(), 0, bgColor
            );
            RenderUtils.drawAlphedSideways(
               (double)(sr.getScaledWidth() / 2 + 170), 0.0, (double)(sr.getScaledWidth() / 2 + 200), (double)sr.getScaledHeight(), bgColor, 0
            );
         }

         if (this.hasListHeader) {
            GL11.glEnable(3042);
            GL11.glEnable(3553);
            this.drawListHeader(k, l, tessellator);
         }

         GL11.glEnable(3042);
         GL11.glEnable(3553);
         this.drawServers(k, l, mouseXIn, mouseYIn, partialTicks);
         GlStateManager.disableDepth();
         if ((this.mc.currentScreen instanceof GuiMultiplayer || this.mc.currentScreen instanceof GuiWorldSelection) && !Panic.stop) {
            int bgColor2 = ColorUtils.getColor(0, 0, 0);
            RenderUtils.drawAlphedRect(
               (double)(sr.getScaledWidth() / 2 - 170),
               (double)(sr.getScaledHeight() - 50),
               (double)(sr.getScaledWidth() / 2 + 170),
               (double)sr.getScaledHeight(),
               bgColor2
            );
            RenderUtils.drawAlphedGradient(
               (double)(sr.getScaledWidth() / 2 - 170),
               (double)(sr.getScaledHeight() - 110),
               (double)(sr.getScaledWidth() / 2 + 170),
               (double)(sr.getScaledHeight() - 50),
               0,
               bgColor2
            );
            RenderUtils.drawAlphedRect((double)(sr.getScaledWidth() / 2 - 170), 0.0, (double)(sr.getScaledWidth() / 2 + 170), 30.0, bgColor2);
            RenderUtils.drawShadowRect((double)(sr.getScaledWidth() / 2 - 170), 0.0, (double)(sr.getScaledWidth() / 2 + 170), 30.0, 5);
            RenderUtils.drawShadowRect((double)(sr.getScaledWidth() / 2 - 170), 0.0, (double)(sr.getScaledWidth() / 2 + 170), 30.0, 15);
            RenderUtils.drawAlphedGradient((double)(sr.getScaledWidth() / 2 - 170), 30.0, (double)(sr.getScaledWidth() / 2 + 170), 70.0, bgColor2, 0);
            RenderUtils.drawShadowRect(
               (double)(sr.getScaledWidth() / 2 - 170),
               (double)(sr.getScaledHeight() - 50),
               (double)(sr.getScaledWidth() / 2 + 170),
               (double)sr.getScaledHeight(),
               5
            );
            RenderUtils.drawShadowRect(
               (double)(sr.getScaledWidth() / 2 - 170),
               (double)(sr.getScaledHeight() - 50),
               (double)(sr.getScaledWidth() / 2 + 170),
               (double)sr.getScaledHeight(),
               15
            );
         } else if (Panic.stop || Client.moduleManager == null || !ComfortUi.get.isScreensDarking() || this.mc.world == null) {
            this.overlayBackground(0, this.top, 255, 255);
            this.overlayBackground(this.bottom, this.height, 255, 255);
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
            GlStateManager.SourceFactor.ZERO,
            GlStateManager.DestFactor.ONE
         );
         GlStateManager.disableAlpha();
         GlStateManager.shadeModel(7425);
         GlStateManager.disableTexture2D();
         int i1 = 4;
         if (!(this.mc.currentScreen instanceof GuiMultiplayer) && !(this.mc.currentScreen instanceof GuiWorldSelection)) {
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)this.left, (double)(this.top + 4), 0.0).tex(0.0, 1.0).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos((double)this.right, (double)(this.top + 4), 0.0).tex(1.0, 1.0).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos((double)this.right, (double)this.top, 0.0).tex(1.0, 0.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)this.left, (double)this.top, 0.0).tex(0.0, 0.0).color(0, 0, 0, 255).endVertex();
            tessellator.draw();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)this.left, (double)this.bottom, 0.0).tex(0.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)this.right, (double)this.bottom, 0.0).tex(1.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)this.right, (double)(this.bottom - 4), 0.0).tex(1.0, 0.0).color(0, 0, 0, 0).endVertex();
            bufferbuilder.pos((double)this.left, (double)(this.bottom - 4), 0.0).tex(0.0, 0.0).color(0, 0, 0, 0).endVertex();
            tessellator.draw();
         }

         int j1 = this.getMaxScroll();
         if (j1 > 0) {
            int k1 = (this.bottom - this.top) * (this.bottom - this.top) / this.getContentHeight();
            k1 = MathHelper.clamp(k1, 32, this.bottom - this.top - 8);
            int l1 = (int)this.getScrolled() * (this.bottom - this.top - k1) / j1 + this.top;
            if (l1 < this.top) {
               l1 = this.top;
            }

            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)i, (double)this.bottom, 0.0).tex(0.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)j, (double)this.bottom, 0.0).tex(1.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)j, (double)this.top, 0.0).tex(1.0, 0.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)i, (double)this.top, 0.0).tex(0.0, 0.0).color(0, 0, 0, 255).endVertex();
            tessellator.draw();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)i, (double)(l1 + k1), 0.0).tex(0.0, 1.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)j, (double)(l1 + k1), 0.0).tex(1.0, 1.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)j, (double)l1, 0.0).tex(1.0, 0.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)i, (double)l1, 0.0).tex(0.0, 0.0).color(128, 128, 128, 255).endVertex();
            tessellator.draw();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)i, (double)(l1 + k1 - 1), 0.0).tex(0.0, 1.0).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos((double)(j - 1), (double)(l1 + k1 - 1), 0.0).tex(1.0, 1.0).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos((double)(j - 1), (double)l1, 0.0).tex(1.0, 0.0).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos((double)i, (double)l1, 0.0).tex(0.0, 0.0).color(192, 192, 192, 255).endVertex();
            tessellator.draw();
         }

         this.renderDecorations(mouseXIn, mouseYIn);
         GlStateManager.enableTexture2D();
         GlStateManager.shadeModel(7424);
         GlStateManager.enableAlpha();
         GlStateManager.disableBlend();
      }
   }

   public void handleMouseInput() {
      if (this.isMouseYWithinSlotBounds(this.mouseY)) {
         if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState() && this.mouseY >= this.top && this.mouseY <= this.bottom) {
            int i = (this.width - this.getListWidth()) / 2;
            int j = (this.width + this.getListWidth()) / 2;
            int k = this.mouseY - this.top - this.headerPadding + (int)this.getScrolled() - 4;
            int l = k / this.slotHeight;
            if (l < this.getSize() && this.mouseX >= i && this.mouseX <= j && l >= 0 && k >= 0) {
               this.elementClicked(l, false, this.mouseX, this.mouseY);
               this.selectedElement = l;
            } else if (this.mouseX >= i && this.mouseX <= j && k < 0) {
               this.clickedHeader(this.mouseX - i, this.mouseY - this.top + (int)this.getScrolled() - 4);
            }
         }

         if (!Mouse.isButtonDown(0) || !this.getEnabled()) {
            this.initialClickY = -1;
         } else if (this.initialClickY == -1) {
            boolean flag1 = true;
            if (this.mouseY >= this.top && this.mouseY <= this.bottom) {
               int j2 = (this.width - this.getListWidth()) / 2;
               int k2 = (this.width + this.getListWidth()) / 2;
               int l2 = this.mouseY - this.top - this.headerPadding + (int)this.getScrolled() - 4;
               int i1 = l2 / this.slotHeight;
               if (i1 < this.getSize() && this.mouseX >= j2 && this.mouseX <= k2 && i1 >= 0 && l2 >= 0) {
                  boolean flag = i1 == this.selectedElement && Minecraft.getSystemTime() - this.lastClicked < 250L;
                  this.elementClicked(i1, flag, this.mouseX, this.mouseY);
                  this.selectedElement = i1;
                  this.lastClicked = Minecraft.getSystemTime();
               } else if (this.mouseX >= j2 && this.mouseX <= k2 && l2 < 0) {
                  this.clickedHeader(this.mouseX - j2, this.mouseY - this.top + (int)this.getScrolled() - 4);
                  flag1 = false;
               }

               int i3 = this.getScrollBarX();
               int j1 = i3 + 6;
               if (this.mouseX >= i3 && this.mouseX <= j1) {
                  this.scrollMultiplier = -1.0F;
                  int k1 = this.getMaxScroll();
                  if (k1 < 1) {
                     k1 = 1;
                  }

                  int l1 = (int)((float)((this.bottom - this.top) * (this.bottom - this.top)) / (float)this.getContentHeight());
                  l1 = MathHelper.clamp(l1, 32, this.bottom - this.top - 8);
                  this.scrollMultiplier = this.scrollMultiplier / ((float)(this.bottom - this.top - l1) / (float)k1);
               } else {
                  this.scrollMultiplier = 1.0F;
               }

               if (flag1) {
                  this.initialClickY = this.mouseY;
               } else {
                  this.initialClickY = -2;
               }
            } else {
               this.initialClickY = -2;
            }
         } else if (this.initialClickY >= 0) {
            this.addScrolled(-((float)(this.mouseY - this.initialClickY) * this.scrollMultiplier));
            this.initialClickY = this.mouseY;
         }

         int i2 = Mouse.getEventDWheel();
         if (i2 != 0) {
            if (i2 > 0) {
               i2 = -1;
            } else if (i2 < 0) {
               i2 = 1;
            }

            this.addScrolled((float)(i2 * this.slotHeight / 2));
         }
      }
   }

   public void setEnabled(boolean enabledIn) {
      this.enabled = enabledIn;
   }

   public boolean getEnabled() {
      return this.enabled;
   }

   public int getListWidth() {
      return 220;
   }

   protected void func_192638_a(int p_192638_1_, int p_192638_2_, int p_192638_3_, int p_192638_4_, float p_192638_5_) {
      int i = this.getSize();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();

      for (int j = 0; j < i; j++) {
         int k = p_192638_2_ + j * this.slotHeight + this.headerPadding;
         int l = this.slotHeight - 4;
         if (k > this.bottom || k + l < this.top) {
            this.func_192639_a(j, p_192638_1_, k, p_192638_5_);
         }

         if (this.showSelectionBox && this.isSelected(j)) {
            int i1 = this.left + (this.width / 2 - this.getListWidth() / 2);
            int j1 = this.left + this.width / 2 + this.getListWidth() / 2;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableTexture2D();
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            bufferbuilder.pos((double)i1, (double)(k + l + 2), 0.0).tex(0.0, 1.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)j1, (double)(k + l + 2), 0.0).tex(1.0, 1.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)j1, (double)(k - 2), 0.0).tex(1.0, 0.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)i1, (double)(k - 2), 0.0).tex(0.0, 0.0).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos((double)(i1 + 1), (double)(k + l + 1), 0.0).tex(0.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)(j1 - 1), (double)(k + l + 1), 0.0).tex(1.0, 1.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)(j1 - 1), (double)(k - 1), 0.0).tex(1.0, 0.0).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos((double)(i1 + 1), (double)(k - 1), 0.0).tex(0.0, 0.0).color(0, 0, 0, 255).endVertex();
            tessellator.draw();
            GlStateManager.enableTexture2D();
         }

         if (k >= this.top - this.slotHeight && k <= this.bottom) {
            this.func_192637_a(j, p_192638_1_, k, l, p_192638_3_, p_192638_4_, p_192638_5_);
         }
      }
   }

   protected int getScrollBarX() {
      return this.width / 2 + 124;
   }

   protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferbuilder = tessellator.getBuffer();
      this.mc.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      float f = 32.0F;
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      bufferbuilder.pos((double)this.left, (double)endY, 0.0).tex(0.0, (double)((float)endY / 32.0F)).color(64, 64, 64, endAlpha).endVertex();
      bufferbuilder.pos((double)(this.left + this.width), (double)endY, 0.0)
         .tex((double)((float)this.width / 32.0F), (double)((float)endY / 32.0F))
         .color(64, 64, 64, endAlpha)
         .endVertex();
      bufferbuilder.pos((double)(this.left + this.width), (double)startY, 0.0)
         .tex((double)((float)this.width / 32.0F), (double)((float)startY / 32.0F))
         .color(64, 64, 64, startAlpha)
         .endVertex();
      bufferbuilder.pos((double)this.left, (double)startY, 0.0).tex(0.0, (double)((float)startY / 32.0F)).color(64, 64, 64, startAlpha).endVertex();
      tessellator.draw();
   }

   public void setSlotXBoundsFromLeft(int leftIn) {
      this.left = leftIn;
      this.right = leftIn + this.width;
   }

   public int getSlotHeight() {
      return this.slotHeight;
   }

   protected void drawContainerBackground(Tessellator p_drawContainerBackground_1_) {
      GL11.glEnable(3042);
      GL11.glEnable(3553);
      BufferBuilder bufferbuilder = p_drawContainerBackground_1_.getBuffer();
      this.mc.getTextureManager().bindTexture(Gui.OPTIONS_BACKGROUND);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      float f = 32.0F;
      bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      bufferbuilder.pos((double)this.left, (double)this.bottom, 0.0)
         .tex((double)((float)this.left / 32.0F), (double)((float)(this.bottom + (int)this.getScrolled()) / 32.0F))
         .color(32, 32, 32, 255)
         .endVertex();
      bufferbuilder.pos((double)this.right, (double)this.bottom, 0.0)
         .tex((double)((float)this.right / 32.0F), (double)((float)(this.bottom + (int)this.getScrolled()) / 32.0F))
         .color(32, 32, 32, 255)
         .endVertex();
      bufferbuilder.pos((double)this.right, (double)this.top, 0.0)
         .tex((double)((float)this.right / 32.0F), (double)((float)(this.top + (int)this.getScrolled()) / 32.0F))
         .color(32, 32, 32, 255)
         .endVertex();
      bufferbuilder.pos((double)this.left, (double)this.top, 0.0)
         .tex((double)((float)this.left / 32.0F), (double)((float)(this.top + (int)this.getScrolled()) / 32.0F))
         .color(32, 32, 32, 255)
         .endVertex();
      p_drawContainerBackground_1_.draw();
   }
}
