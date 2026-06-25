package net.minecraft.client.multiplayer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.govno.client.utils.ClientRP;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Render.RenderUtils;

public class GuiConnecting extends GuiScreen {
   private final long initTime = System.currentTimeMillis();
   private static final AtomicInteger CONNECTION_ID = new AtomicInteger(0);
   private static final Logger LOGGER = LogManager.getLogger();
   private NetworkManager networkManager;
   private boolean cancel;
   private final GuiScreen previousGuiScreen;

   public GuiConnecting(GuiScreen parent, Minecraft mcIn, ServerData serverDataIn) {
      this.mc = mcIn;
      this.previousGuiScreen = parent;
      ServerAddress serveraddress = ServerAddress.fromString(serverDataIn.serverIP);
      mcIn.loadWorld(null);
      mcIn.setServerData(serverDataIn);
      this.connect(serveraddress.getIP(), serveraddress.getPort());
      GuiDisconnected.lastServer = serveraddress.getIP();
   }

   public GuiConnecting(GuiScreen parent, Minecraft mcIn, String hostName, int port) {
      this.mc = mcIn;
      this.previousGuiScreen = parent;
      mcIn.loadWorld(null);
      this.connect(hostName, port);
   }

   private void connect(final String ip, final int port) {
      LOGGER.info("Connecting to {}, {}", ip, port);
      (new Thread("Server Connector #" + CONNECTION_ID.incrementAndGet()) {
            @Override
            public void run() {
               InetAddress inetaddress = null;

               try {
                  if (GuiConnecting.this.cancel) {
                     return;
                  }

                  if (ip != null && InetAddress.getByName(ip) != null) {
                     inetaddress = InetAddress.getByName(ip);
                  }

                  GuiConnecting.this.networkManager = NetworkManager.createNetworkManagerAndConnect(
                     inetaddress, port, GuiConnecting.this.mc.gameSettings.isUsingNativeTransport()
                  );
                  GuiConnecting.this.networkManager
                     .setNetHandler(new NetHandlerLoginClient(GuiConnecting.this.networkManager, GuiConnecting.this.mc, GuiConnecting.this.previousGuiScreen));
                  GuiConnecting.this.networkManager.sendPacket(new C00Handshake(ip, port, EnumConnectionState.LOGIN));
                  GuiConnecting.this.networkManager.sendPacket(new CPacketLoginStart(GuiConnecting.this.mc.getSession().getProfile()));
                  String ipS = ip + (port != 25565 ? ":" + port : "").replace("localhost", "LocalServer");

                  try {
                     String[] pointSplits = ipS.split(".");
                     switch (pointSplits.length) {
                        case 1:
                        case 2:
                           ipS = pointSplits[0];
                           break;
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                           ipS = pointSplits[1];
                     }
                  } catch (Exception var5) {
                     ipS = ipS.replace(".ru", "")
                        .replace("ovh.", "")
                        .replace(".cc.", "")
                        .replace(".space", "")
                        .replace("mc.", "")
                        .replace("play.", "")
                        .replace(".ru", "")
                        .replace(".com", "")
                        .replace(".net", "")
                        .replace(".su", "")
                        .replace(".eu", "")
                        .replace("creative.", "")
                        .replace(".xyz", "")
                        .replace(".org", "")
                        .replace(".gg", "")
                        .replace(".me", "")
                        .replace("join.", "")
                        .replace(".fun", "");
                  }

                  ClientRP.getInstance().getDiscordRP().update("Играет на " + ipS, "В онлайне");
               } catch (UnknownHostException var6) {
                  if (GuiConnecting.this.cancel) {
                     return;
                  }

                  GuiConnecting.LOGGER.error("Couldn't connect to server", (Throwable)var6);
                  GuiConnecting.this.mc
                     .displayGuiScreen(
                        new GuiDisconnected(
                           GuiConnecting.this.previousGuiScreen, "connect.failed", new TextComponentTranslation("disconnect.genericReason", "Unknown host")
                        )
                     );
               } catch (Exception var7) {
                  if (GuiConnecting.this.cancel) {
                     return;
                  }

                  GuiConnecting.LOGGER.error("Couldn't connect to server", (Throwable)var7);
                  String s = var7.toString();
                  if (inetaddress != null) {
                     String s1 = inetaddress + ":" + port;
                     s = s.replaceAll(s1, "");
                  }

                  GuiConnecting.this.mc
                     .displayGuiScreen(
                        new GuiDisconnected(GuiConnecting.this.previousGuiScreen, "connect.failed", new TextComponentTranslation("disconnect.genericReason", s))
                     );
               }
            }
         })
         .start();
   }

   @Override
   public void updateScreen() {
      if (this.networkManager != null) {
         if (this.networkManager.isChannelOpen()) {
            this.networkManager.processReceivedPackets();
         } else {
            this.networkManager.checkDisconnected();
         }
      }
   }

   @Override
   protected void keyTyped(char typedChar, int keyCode) throws IOException {
   }

   @Override
   public void initGui() {
      this.buttonList.clear();
      this.buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120 + 12, I18n.format("gui.cancel")));
   }

   @Override
   public void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 0) {
         this.cancel = true;
         if (this.networkManager != null) {
            this.networkManager.closeChannel(new TextComponentString("Aborted"));
         }

         this.mc.displayGuiScreen(this.previousGuiScreen);
      }
   }

   @Override
   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      ScaledResolution sr = new ScaledResolution(this.mc);
      if (!Panic.stop) {
         RenderUtils.drawScreenShaderBackground(sr, mouseX, mouseY);
      } else {
         this.drawDefaultBackground();
      }

      if (this.networkManager == null) {
         this.drawCenteredString(this.fontRendererObj, I18n.format("connect.connecting"), width / 2, height / 2 - 70, 16777215);
      } else {
         this.drawCenteredString(this.fontRendererObj, I18n.format("connect.authorizing"), width / 2, height / 2 - 70, 16777215);
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
   }
}
