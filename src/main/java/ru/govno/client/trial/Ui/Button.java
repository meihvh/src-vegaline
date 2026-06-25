package ru.govno.client.trial.Ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Render.RenderUtils;

public class Button {
   private float x;
   private float y;
   private final float x2;
   private final float y2;
   private final String label;
   private boolean mouseHolded;
   private final boolean lockOnClick;
   private boolean locked;
   private final Runnable onClick;

   private static CFontRenderer standartLabelFont() {
      return Fonts.comfortaa_18;
   }

   private static float standartExpand() {
      return 5.0F;
   }

   public float getWidth() {
      return standartExpand() * 2.0F + standartLabelFont().getStringWidth(this.label);
   }

   public float getHeight() {
      return standartExpand() * 2.0F + standartLabelFont().getHeight();
   }

   public Button(String label, float x, float y, Runnable onClick, boolean lockOnClick) {
      this.label = label;
      this.x = x;
      this.y = y;
      float w = this.getWidth();
      float h = this.getHeight();
      this.x -= w / 2.0F;
      this.x2 = this.x + w;
      this.y -= h / 2.0F;
      this.y2 = this.y + h;
      this.onClick = onClick;
      this.lockOnClick = lockOnClick;
   }

   private void updateMouseClick(boolean hovered) {
      boolean pressed = hovered && Mouse.isButtonDown(0);
      if (pressed) {
         if (!this.mouseHolded) {
            if (this.onClick != null && !this.locked) {
               this.onClick.run();
            }

            this.locked = this.lockOnClick;
         }

         this.mouseHolded = true;
      } else {
         this.mouseHolded = false;
      }
   }

   public void render() {
      this.updateMouseClick(this.isHovered());
      RenderUtils.drawRect((double)this.x, (double)this.y, (double)this.x2, (double)this.y2, Integer.MIN_VALUE);
      standartLabelFont().drawString(this.label, this.x + standartExpand(), this.y2 + standartExpand(), Integer.MAX_VALUE);
   }

   private boolean isHovered() {
      return Minecraft.getMinecraft().currentScreen != null
         && (float)GuiScreen.staticMouseX >= this.x
         && (float)GuiScreen.staticMouseY >= this.y
         && (float)GuiScreen.staticMouseX < this.x2
         && (float)GuiScreen.staticMouseY < this.y2;
   }
}
