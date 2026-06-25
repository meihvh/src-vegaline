package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.math.MathHelper;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiOptionSlider extends GuiButton {
   private float sliderValue;
   public boolean dragging;
   private final GameSettings.Options options;
   private final float minValue;
   private final float maxValue;
   final AnimationUtils anim = new AnimationUtils(0.0F, 0.0F, 0.1F);

   public GuiOptionSlider(int buttonId, int x, int y, GameSettings.Options optionIn) {
      this(buttonId, x, y, optionIn, 0.0F, 1.0F);
   }

   public GuiOptionSlider(int buttonId, int x, int y, GameSettings.Options optionIn, float minValueIn, float maxValue) {
      super(buttonId, x, y, 150, 20, "");
      this.sliderValue = 1.0F;
      this.options = optionIn;
      this.minValue = minValueIn;
      this.maxValue = maxValue;
      Minecraft minecraft = Minecraft.getMinecraft();
      this.sliderValue = optionIn.normalizeValue(minecraft.gameSettings.getOptionFloatValue(optionIn));
      this.displayString = minecraft.gameSettings.getKeyBinding(optionIn);
   }

   @Override
   protected int getHoverState(boolean mouseOver) {
      return 0;
   }

   @Override
   protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
      if (this.visible) {
         if (this.dragging) {
            this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
            float f = this.options.denormalizeValue(this.sliderValue);
            mc.gameSettings.setOptionFloatValue(this.options, f);
            this.sliderValue = this.options.normalizeValue(f);
            this.displayString = mc.gameSettings.getKeyBinding(this.options);
         }

         if (Panic.stop) {
            mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)), this.yPosition, 0, 66, 4, 20);
            this.drawTexturedModalRect(this.xPosition + (int)(this.sliderValue * (float)(this.width - 8)) + 4, this.yPosition, 196, 66, 4, 20);
         } else {
            this.anim.to = !this.hovered && !this.dragging ? 0.0F : 1.0F;
            int c = ColorUtils.getOverallColorFrom(ColorUtils.getColor(60, 80, 120, 45), ColorUtils.getColor(60, 80, 120, 180), this.anim.getAnim());
            float ext = 4.0F;
            float sliderVal = this.sliderValue * ((float)this.width - ext * 2.0F - ext) + ext;
            float x = (float)this.xPosition + ext;
            float x2 = (float)this.xPosition + sliderVal + ext;
            float y = (float)(this.yPosition + 4);
            float y2 = (float)(this.yPosition + 16);
            float round = MathUtils.clamp(this.dragging ? 0.5F : 2.0F, 0.0F, MathUtils.getDifferenceOf(x, x2) / 2.0F);
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               x, y, x2, y2, 0.5F + 1.5F * this.anim.getAnim(), this.dragging ? 5.0F : 3.0F, c, c, c, c, true, true, true
            );
         }
      }
   }

   @Override
   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      if (super.mousePressed(mc, mouseX, mouseY)) {
         this.sliderValue = (float)(mouseX - (this.xPosition + 4)) / (float)(this.width - 8);
         this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0F, 1.0F);
         mc.gameSettings.setOptionFloatValue(this.options, this.options.denormalizeValue(this.sliderValue));
         this.displayString = mc.gameSettings.getKeyBinding(this.options);
         this.dragging = true;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY) {
      this.dragging = false;
   }
}
