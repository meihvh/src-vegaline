package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.gui.advancements.GuiScreenAdvancements;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.world.GameType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.module.modules.ComfortUi;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Render.AnimationUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiIngameMenu extends GuiScreen {
   private int saveStep;
   private int visibleTime;
   public static boolean respawnKey = false;
   public static GameType gamemode = GameType.SURVIVAL;
   private final AnimationUtils scale = new AnimationUtils(1.1F, 1.0F, 0.1F);

   @Override
   public void initGui() {
      Keyboard.enableRepeatEvents(false);
      this.saveStep = 0;
      this.buttonList.clear();
      int i = -16;
      int j = 98;
      this.buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 120 + -16, I18n.format("menu.returnToMenu")));
      if (!Panic.stop) {
         if (respawnKey) {
            this.buttonList.add(new GuiButton(1337, width / 2 - 100, height / 4 + 120 + -16 + 24 + 24, I18n.format("deathScreen.respawn")));
         }

         if (ComfortUi.get != null && ComfortUi.get.isAddClientButtons()) {
            this.buttonList
               .add(new GuiButton(8, width / 2 - 100, height / 4 + 120 + -16 + 24, this.mc.isSingleplayer() ? I18n.format("menu.multiplayer") : "Перезайти"));
         }
      }

      if (!this.mc.isIntegratedServerRunning()) {
         this.buttonList.get(0).displayString = I18n.format("menu.disconnect");
      }

      this.buttonList.add(new GuiButton(4, width / 2 - 100, height / 4 + 24 + -16, I18n.format("menu.returnToGame")));
      this.buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 96 + -16, 98, 20, I18n.format("menu.options")));
      GuiButton guibutton = this.addButton(new GuiButton(7, width / 2 + 2, height / 4 + 96 + -16, 98, 20, I18n.format("menu.shareToLan")));
      guibutton.enabled = this.mc.isSingleplayer() && !this.mc.getIntegratedServer().getPublic();
      this.buttonList.add(new GuiButton(5, width / 2 - 100, height / 4 + 48 + -16, 98, 20, I18n.format("gui.advancements")));
      this.buttonList.add(new GuiButton(6, width / 2 + 2, height / 4 + 48 + -16, 98, 20, I18n.format("gui.stats")));
      super.initGui();
   }

   @Override
   public void actionPerformed(GuiButton button) throws IOException {
      switch (button.id) {
         case 0:
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
            break;
         case 1:
            if (!Panic.stop) {
               Client.configManager.saveConfig("Default");
            }

            boolean flag = this.mc.isIntegratedServerRunning();
            boolean flag1 = this.mc.isConnectedToRealms();
            button.enabled = false;
            if (Minecraft.getMinecraft().getConnection() != null && Minecraft.getMinecraft().getConnection().getNetworkManager() != null) {
               this.mc.world.sendQuittingDisconnectingPacket();
            }

            this.mc.loadWorld(null);
            if (flag) {
               this.mc.displayGuiScreen(new GuiMainMenu());
            } else if (flag1) {
               RealmsBridge realmsbridge = new RealmsBridge();
               realmsbridge.switchToRealms(new GuiMainMenu());
            } else {
               this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
            }
         case 3:
         default:
            break;
         case 4:
            if (!Panic.stop) {
               Client.configManager.saveConfig("Default");
            }

            this.mc.displayGuiScreen(null);
            this.mc.setIngameFocus();
            break;
         case 5:
            this.mc.displayGuiScreen(new GuiScreenAdvancements(Minecraft.player.connection.func_191982_f()));
            break;
         case 6:
            this.mc.displayGuiScreen(new GuiStats(this, Minecraft.player.getStatFileWriter()));
            break;
         case 7:
            this.mc.displayGuiScreen(new GuiShareToLan(this));
            break;
         case 8:
            if (!Panic.stop) {
               Client.configManager.saveConfig("Default");
            }

            if (this.mc.isSingleplayer()) {
               this.mc.displayGuiScreen(new GuiMultiplayer(this));
            } else if (this.mc.getCurrentServerData() != null) {
               GuiDisconnected.lastServer = this.mc.getCurrentServerData().serverIP;
               this.mc.world.sendQuittingDisconnectingPacket("Перезаход на сервер " + GuiDisconnected.lastServer);
               GuiDisconnected.does = true;
            }
            break;
         case 1337:
            if (Minecraft.player == null) {
               return;
            }

            Minecraft.player.respawnPlayer();
            respawnKey = false;
            Minecraft.player.closeScreen();
            this.mc.playerController.setGameType(gamemode);
            Minecraft.player.capabilities.isFlying = false;
      }
   }

   @Override
   public void updateScreen() {
      super.updateScreen();
      this.visibleTime++;
   }

   public float lerp(float start, float end, float step) {
      return start + step * (end - start);
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      float scaled = (double)this.scale.anim < 1.001 ? 1.0F : this.scale.getAnim();
      GL11.glPushMatrix();
      ScaledResolution sr = new ScaledResolution(this.mc);
      float scaledWidth = (float)sr.getScaledWidth();
      float scaledHeight = (float)sr.getScaledHeight();
      if (!Panic.stop && ComfortUi.get.isAnimPauseScreen()) {
         RenderUtils.customScaledObject2D(0.0F, 0.0F, scaledWidth, scaledHeight * 0.75F, scaled);
      } else {
         this.drawDefaultBackground();
      }

      this.drawCenteredString(this.fontRendererObj, I18n.format("menu.game"), width / 2, 40, 16777215);
      super.drawScreen(mouseX, mouseY, partialTicks);
      GL11.glPopMatrix();
   }
}
