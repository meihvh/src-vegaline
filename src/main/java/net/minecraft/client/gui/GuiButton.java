package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.BloomUtil;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiButton extends Gui {
   protected static final ResourceLocation BUTTON_TEXTURES = new ResourceLocation("textures/gui/widgets.png");
   protected int width;
   protected int height;
   public int xPosition;
   public int yPosition;
   public String displayString;
   public int id;
   public boolean enabled;
   public boolean visible;
   protected boolean hovered;
   final AnimationUtils anim = new AnimationUtils(0.0F, 0.0F, 0.1F);
   private int opacity = 40;

   public GuiButton(int buttonId, int x, int y, String buttonText) {
      this(buttonId, x, y, 200, 20, buttonText);
   }

   public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
      this.width = 200;
      this.height = 20;
      this.enabled = true;
      this.visible = true;
      this.id = buttonId;
      this.xPosition = x;
      this.yPosition = y;
      this.width = widthIn;
      this.height = heightIn;
      this.displayString = buttonText;
   }

   public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, CFontRenderer font) {
      this.width = 200;
      this.height = 20;
      this.enabled = true;
      this.visible = true;
      this.id = buttonId;
      this.xPosition = x;
      this.yPosition = y;
      this.width = widthIn;
      this.height = heightIn;
      this.displayString = buttonText;
   }

   protected int getHoverState(boolean mouseOver) {
      int i = 1;
      if (!this.enabled) {
         i = 0;
      } else if (mouseOver) {
         i = 2;
      }

      return i;
   }

   public void func_191745_a(Minecraft p_191745_1_, int p_191745_2_, int p_191745_3_, float p_191745_4_) {
      if (this.visible) {
         if (!Panic.stop && ComfortUi.get.isBetterButtons()) {
            if (!this.visible) {
               return;
            }

            p_191745_1_.getTextureManager().bindTexture(BUTTON_TEXTURES);
            this.hovered = p_191745_2_ >= this.xPosition
               && p_191745_3_ >= this.yPosition
               && p_191745_2_ < this.xPosition + this.width
               && p_191745_3_ < this.yPosition + this.height;
            this.mouseDragged(p_191745_1_, p_191745_2_, p_191745_3_);
            float x = (float)this.xPosition;
            float y = (float)this.yPosition;
            float w = (float)this.width;
            float h = (float)this.height;
            float x2 = x + w;
            float y2 = y + h;
            this.anim.to = this.hovered ? 1.0F : 0.0F;
            float bgAlphaPC = MathUtils.clamp((0.35F + this.anim.getAnim() * 0.65F) * 1.01F, 0.0F, 1.0F);
            int colorise = ColorUtils.getColor(120, 140, 190);
            int bgC = ColorUtils.swapAlpha(colorise, 255.0F * (bgAlphaPC / 4.0F));
            int bgCOut = ColorUtils.swapAlpha(colorise, 255.0F * bgAlphaPC);
            float round = 2.0F + 3.5F * this.anim.getAnim();
            float radius = 0.25F + 2.0F * this.anim.getAnim();
            float ext = this.anim.getAnim();
            GlStateManager.blendFunc(770, 1);
            RenderUtils.drawRoundOutline(
               x + ext, y + ext, w - ext * 2.0F, h - ext * 2.0F, round, radius, bgC, bgCOut, new ScaledResolution(Minecraft.getMinecraft())
            );
            int texAlpha = (int)(125.0F + 130.0F * this.anim.getAnim());
            int textCol = ColorUtils.swapAlpha(ColorUtils.getFixedWhiteColor(), (float)texAlpha);
            CFontRenderer font = Fonts.mntsb_15;
            float texW = font.getStringWidth(this.displayString);
            font.drawString(this.displayString, x + w / 2.0F - texW / 2.0F, y + h / 2.0F + font.getHeight() / 4.0F - 2.5F, textCol);
            if (texAlpha > 140) {
               Runnable run = () -> font.drawString(this.displayString, x + w / 2.0F - texW / 2.0F, y + h / 2.0F + font.getHeight() / 4.0F - 2.5F, -1);
               BloomUtil.renderShadow(run, colorise, 1 + (int)(radius * 6.0F), 1, 2.7F * this.anim.getAnim(), false);
            }

            GlStateManager.blendFunc(770, 771);
         } else {
            FontRenderer fontrenderer = p_191745_1_.fontRendererObj;
            p_191745_1_.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = p_191745_2_ >= this.xPosition
               && p_191745_3_ >= this.yPosition
               && p_191745_2_ < this.xPosition + this.width
               && p_191745_3_ < this.yPosition + this.height;
            int i = this.getHoverState(this.hovered);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
               GlStateManager.SourceFactor.SRC_ALPHA,
               GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
               GlStateManager.SourceFactor.ONE,
               GlStateManager.DestFactor.ZERO
            );
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            this.drawTexturedModalRect(this.xPosition, this.yPosition, 0, 46 + i * 20, this.width / 2, this.height);
            this.drawTexturedModalRect(this.xPosition + this.width / 2, this.yPosition, 200 - this.width / 2, 46 + i * 20, this.width / 2, this.height);
            this.mouseDragged(p_191745_1_, p_191745_2_, p_191745_3_);
            int j = 14737632;
            if (!this.enabled) {
               j = 10526880;
            } else if (this.hovered) {
               j = 16777120;
            }

            this.drawCenteredString(fontrenderer, this.displayString, this.xPosition + this.width / 2, this.yPosition + (this.height - 8) / 2, j);
         }
      }
   }

   public void drawButton(Minecraft mc, int mouseX, int mouseY, float mouseButton) {
      if (this.visible) {
         mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
         if (this.hovered) {
            if (this.opacity < 40) {
               this.opacity++;
            }
         } else if (this.opacity > 22) {
            this.opacity--;
         }

         if (mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height) {
            boolean var10 = true;
         } else {
            boolean var10000 = false;
         }

         int color = ColorUtils.getColor(25, 25, 25, 73);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         int c1 = ColorUtils.getColor(0, 0, 0, 235);
         int c2 = ColorUtils.getColor(0, 180, 255);
         RenderUtils.fullRoundFG(
            (float)(this.xPosition - 2), (float)(this.yPosition - 1), (float)(this.width + 3), (float)(this.height + 1), 7.0F, c2, c2, c2, c2, false
         );
         RenderUtils.fullRoundFG((float)(this.xPosition - 1), (float)this.yPosition, (float)(this.width + 2), (float)this.height, 5.5F, c1, c1, c1, c1, false);
         this.mouseDragged(mc, mouseX, mouseY);
         String str = "Version �b" + this.displayString;
         Fonts.mntsb_18
            .drawString(
               str,
               (float)this.xPosition + (float)this.width / 2.0F - Fonts.mntsb_18.getStringWidth(str) / 2.0F,
               (float)this.yPosition + ((float)this.height - 2.0F) / 3.0F - 0.5F,
               -1
            );
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
      }
   }

   protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
   }

   public void mouseReleased(int mouseX, int mouseY) {
   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      return this.enabled
         && this.visible
         && mouseX >= this.xPosition
         && mouseY >= this.yPosition
         && mouseX < this.xPosition + this.width
         && mouseY < this.yPosition + this.height;
   }

   public boolean isMouseOver() {
      return this.hovered;
   }

   public void drawButtonForegroundLayer(int mouseX, int mouseY) {
   }

   public void playPressSound(SoundHandler soundHandlerIn) {
      soundHandlerIn.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public int getButtonWidth() {
      return this.width;
   }

   public void setWidth(int width) {
      this.width = width;
   }
}
