package ru.govno.client.trial.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import ru.govno.client.newfont.CFontRenderer;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Render.RenderUtils;

public class Window {
   private final String title;
   private final String[] texts;
   private final float x;
   private final float y;
   private final float x2;
   private final float y2;
   private boolean shouldToClose;
   public boolean closed;
   private final List<Button> buttons = new ArrayList<>();

   public void close() {
      this.shouldToClose = true;
   }

   public static CFontRenderer standardTitleFont() {
      return Fonts.comfortaaRegular_22;
   }

   public static CFontRenderer standardTextFont() {
      return Fonts.comfortaaRegular_14;
   }

   public static float standardOffset() {
      return 2.0F;
   }

   public Window(String title, String[] texts, String[] buttonLabels, Runnable[] buttonActions) {
      int buttonsToAddCount = buttonLabels.length < buttonActions.length ? buttonLabels.length : buttonActions.length;
      float calcWidth = standardTitleFont().getStringWidth(title);
      float calcHeight = standardTitleFont().getHeight() + standardOffset() * 2.0F;
      float allButtonWidth = (float)IntStream.range(0, buttonsToAddCount)
            .mapToDouble(ix -> (double)(new Button(buttonLabels[ix], 0.0F, 0.0F, null, false).getWidth() + standardOffset()))
            .sum()
         - standardOffset();
      if (calcWidth > allButtonWidth) {
         calcWidth = allButtonWidth;
      }

      for (String text : texts) {
         float textW = standardTextFont().getStringWidth(text);
         if (textW + standardOffset() < calcWidth) {
            calcWidth = textW + standardOffset();
         }

         calcHeight += standardTextFont().getHeight() + standardOffset();
      }

      if (buttonsToAddCount > 0) {
         calcHeight += (float)IntStream.range(0, buttonsToAddCount)
            .mapToDouble(ix -> (double)new Button(buttonLabels[ix], 0.0F, 0.0F, null, false).getHeight())
            .max()
            .getAsDouble();
      }

      this.title = title;
      this.texts = texts;
      ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
      this.x = (float)sr.getScaledWidth() / 2.0F - calcWidth / 2.0F;
      this.y = (float)sr.getScaledHeight() / 2.0F - calcHeight / 2.0F;
      this.x2 = (float)sr.getScaledWidth() / 2.0F + calcWidth / 2.0F;
      this.y2 = (float)sr.getScaledHeight() / 2.0F + calcHeight / 2.0F;
      float buttonsMoveRight = (
            this.x2
               - this.x
               - (float)IntStream.range(0, buttonsToAddCount)
                  .mapToDouble(ix -> (double)(new Button(buttonLabels[ix], 0.0F, 0.0F, null, false).getWidth() + standardOffset()))
                  .sum()
               - standardOffset()
         )
         / 2.0F;
      float buttonsMoveUp = (float)IntStream.range(0, buttonsToAddCount)
            .mapToDouble(ix -> (double)new Button(buttonLabels[ix], 0.0F, 0.0F, null, false).getHeight())
            .max()
            .getAsDouble()
         + standardOffset();
      float buttonX = this.x + buttonsMoveRight + standardOffset();
      float buttonY = this.y2 - buttonsMoveUp;

      for (int i = 0; i < buttonsToAddCount; i++) {
         Button tempButton = new Button(buttonLabels[i], buttonX, buttonY, buttonActions[i], false);
         this.buttons.add(tempButton);
         buttonX += tempButton.getWidth() + standardOffset();
         buttonY += tempButton.getHeight() + standardOffset();
      }
   }

   public void render() {
      RenderUtils.drawRect((double)this.x, (double)this.y, (double)this.x2, (double)this.y2, Integer.MIN_VALUE);
      float centerX = this.x + (this.x2 - this.x) / 2.0F;
      float textYTemp;
      standardTitleFont().drawCenteredString(this.title, centerX, textYTemp = this.y + 7.0F, Integer.MAX_VALUE);
      textYTemp += standardTitleFont().getHeight();

      for (String text : this.texts) {
         standardTextFont().drawCenteredString(text, centerX, textYTemp += standardTextFont().getHeight() + 2.0F, Integer.MAX_VALUE);
      }

      this.buttons.forEach(Button::render);
   }
}
