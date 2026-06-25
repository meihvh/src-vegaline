package net.minecraft.client.gui;

import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiDownloadTerrain extends GuiScreen {
   @Override
   public void initGui() {
      this.buttonList.clear();
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      GL11.glEnable(3042);
      GL11.glEnable(3553);
      ScaledResolution sr = new ScaledResolution(this.mc);
      if (!Panic.stop) {
         RenderUtils.drawScreenShaderBackground(sr, mouseX, mouseY);
      } else {
         this.drawBackground(0);
      }

      this.drawCenteredString(this.fontRendererObj, I18n.format("multiplayer.downloadingTerrain"), width / 2, height / 2 - 50, 16777215);
      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }
}
