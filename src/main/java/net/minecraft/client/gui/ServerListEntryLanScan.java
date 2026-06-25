package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class ServerListEntryLanScan implements GuiListExtended.IGuiListEntry {
   private final Minecraft mc = Minecraft.getMinecraft();

   @Override
   public void func_192634_a(
      int p_192634_1_,
      int p_192634_2_,
      int p_192634_3_,
      int p_192634_4_,
      int p_192634_5_,
      int p_192634_6_,
      int p_192634_7_,
      boolean p_192634_8_,
      float p_192634_9_
   ) {
      int i = p_192634_3_ + p_192634_5_ / 2 - this.mc.fontRendererObj.FONT_HEIGHT / 2;
      this.mc
         .fontRendererObj
         .drawString(
            I18n.format("lanServer.scanning"),
            (float)(GuiScreen.width / 2 - this.mc.fontRendererObj.getStringWidth(I18n.format("lanServer.scanning")) / 2),
            (double)i,
            16777215
         );

      String s = switch ((int)(Minecraft.getSystemTime() / 300L % 4L)) {
         default -> "O o o";
         case 1, 3 -> "o O o";
         case 2 -> "o o O";
      };
      this.mc
         .fontRendererObj
         .drawString(
            s, (float)(GuiScreen.width / 2 - this.mc.fontRendererObj.getStringWidth(s) / 2), (double)(i + this.mc.fontRendererObj.FONT_HEIGHT), 8421504
         );
   }

   @Override
   public void func_192633_a(int p_192633_1_, int p_192633_2_, int p_192633_3_, float p_192633_4_) {
   }

   @Override
   public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
      return false;
   }

   @Override
   public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
   }
}
