package net.minecraft.client.gui;

import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import ru.govno.client.module.modules.Respawn;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Math.TimerHelper;

public class GuiGameOver extends GuiScreen {
   TimerHelper timer = new TimerHelper();
   private int enableButtonsTimer;
   private final ITextComponent causeOfDeath;
   private float smoothAlpha;
   private int deathX;
   private int deathY;
   private int deathZ;

   public GuiGameOver(@Nullable ITextComponent p_i46598_1_) {
      this.causeOfDeath = p_i46598_1_;
   }

   @Override
   public void initGui() {
      this.buttonList.clear();
      this.enableButtonsTimer = 0;
      if (this.mc.currentScreen instanceof GuiGameOver && Minecraft.player.deathTime == 0 && !Respawn.get.actived) {
         this.timer.reset();
         this.smoothAlpha = 0.0F;
         this.deathX = (int)Minecraft.player.posX;
         this.deathY = (int)Minecraft.player.posY;
         this.deathZ = (int)Minecraft.player.posZ;
      }

      if (this.mc.world.getWorldInfo().isHardcoreModeEnabled()) {
         this.buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 262, I18n.format("deathScreen.spectate")));
         this.buttonList
            .add(
               new GuiButton(
                  1, width / 2 - 100, height / 4 + 286, I18n.format("deathScreen." + (this.mc.isIntegratedServerRunning() ? "deleteWorld" : "leaveServer"))
               )
            );
      } else {
         this.buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 262, I18n.format("deathScreen.respawn")));
         this.buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 286, I18n.format("deathScreen.titleScreen")));
         if (!Panic.stop) {
            this.buttonList.add(new GuiButton(228, width / 2 - 100, height / 4 + 286 + 24, I18n.format("deathScreen.spectate")));
         }

         if (this.mc.getSession() == null) {
            this.buttonList.get(1).enabled = false;
         }
      }

      if (!this.mc.gameSettings.ofFastRender && !this.mc.entityRenderer.isShaderActive() && !Panic.stop) {
         this.mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/desaturate.json"));
      }

      for (GuiButton guibutton : this.buttonList) {
         guibutton.enabled = false;
      }
   }

   @Override
   protected void keyTyped(char typedChar, int keyCode) throws IOException {
   }

   @Override
   public void actionPerformed(GuiButton button) throws IOException {
      switch (button.id) {
         case 0:
            Minecraft.player.respawnPlayer();
            this.mc.displayGuiScreen(null);
            break;
         case 1:
            if (this.mc.world.getWorldInfo().isHardcoreModeEnabled()) {
               this.mc.displayGuiScreen(new GuiMainMenu());
            } else {
               GuiYesNo guiyesno = new GuiYesNo(
                  this, I18n.format("deathScreen.quit.confirm"), "", I18n.format("deathScreen.titleScreen"), I18n.format("deathScreen.respawn"), 0
               );
               this.mc.displayGuiScreen(guiyesno);
               guiyesno.setButtonDelay(0);
            }
            break;
         case 228:
            GuiIngameMenu.respawnKey = true;
            Minecraft.player.setDead(false);
            Minecraft.player.setHealth(20.0F);
            Minecraft.player.capabilities.isFlying = true;
            Minecraft.player.closeScreen();
            this.mc.playerController.setGameType(GameType.SPECTATOR);
            Minecraft.player.noClip = true;
      }
   }

   @Override
   public void confirmClicked(boolean result, int id) {
      if (result) {
         if (this.mc.world != null) {
            this.mc.world.sendQuittingDisconnectingPacket();
         }

         this.mc.loadWorld(null);
         this.mc.displayGuiScreen(new GuiMainMenu());
      } else {
         Minecraft.player.respawnPlayer();
         this.mc.displayGuiScreen(null);
      }
   }

   @Override
   public void onGuiClosed() {
      if (this.mc.entityRenderer.isShaderActive()) {
         this.mc.entityRenderer.theShaderGroup = null;
      }
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      boolean flag = this.mc.world.getWorldInfo().isHardcoreModeEnabled();
      boolean smooth = false;
      if (this.timer.hasReached(2300.0) && !Panic.stop) {
         String coords = this.deathX + " / " + this.deathY + " / " + this.deathZ;
         Fonts.comfortaaRegular_18
            .drawString(coords, (float)(width / 2) - Fonts.comfortaaRegular_18.getStringWidth(coords) / 2.0F, (float)(height / 2) + 20.0F, -1);
      }

      GlStateManager.pushMatrix();
      GlStateManager.scale(2.0F, 2.0F, 2.0F);
      this.drawCenteredString(this.fontRendererObj, I18n.format(flag ? "deathScreen.title.hardcore" : "deathScreen.title"), width / 2 / 2, 30, 16777215);
      GlStateManager.popMatrix();
      if (this.causeOfDeath != null) {
         this.drawCenteredString(this.fontRendererObj, this.causeOfDeath.getFormattedText(), width / 2, 85, 16777215);
      }

      this.drawCenteredString(
         this.fontRendererObj, I18n.format("deathScreen.score") + ": " + TextFormatting.YELLOW + Minecraft.player.getScore(), width / 2, 100, 16777215
      );
      if (this.causeOfDeath != null && mouseY > 85 && mouseY < 85 + this.fontRendererObj.FONT_HEIGHT) {
         ITextComponent itextcomponent = this.getClickedComponentAt(mouseX);
         if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null) {
            this.handleComponentHover(itextcomponent, mouseX, mouseY);
         }
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   @Nullable
   public ITextComponent getClickedComponentAt(int p_184870_1_) {
      if (this.causeOfDeath == null) {
         return null;
      } else {
         int i = this.mc.fontRendererObj.getStringWidth(this.causeOfDeath.getFormattedText());
         int j = width / 2 - i / 2;
         int k = width / 2 + i / 2;
         int l = j;
         if (p_184870_1_ >= j && p_184870_1_ <= k) {
            for (ITextComponent itextcomponent : this.causeOfDeath) {
               l += this.mc
                  .fontRendererObj
                  .getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(itextcomponent.getUnformattedComponentText(), false));
               if (l > p_184870_1_) {
                  return itextcomponent;
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   @Override
   public boolean doesGuiPauseGame() {
      return false;
   }

   @Override
   public void updateScreen() {
      super.updateScreen();
      this.enableButtonsTimer++;
      if (this.enableButtonsTimer == 20) {
         for (GuiButton guibutton : this.buttonList) {
            guibutton.enabled = true;
         }
      }
   }
}
