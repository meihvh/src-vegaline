package ru.govno.client.utils;

import java.util.Arrays;
import ru.govno.client.Client;
import ru.govno.client.utils.Command.impl.Panic;
import ru.govno.client.utils.Discord.Discord;
import ru.govno.client.utils.Discord.DiscordEventHandlers;
import ru.govno.client.utils.Discord.DiscordRichPresence;
import ru.govno.client.utils.Discord.helpers.RPCButton;
import viamcp.ViaMCP;

public class DiscordRP {
   private boolean running = true;
   private long created = 0L;
   private Discord discord;
   private DiscordRichPresence.Builder builder;
   private DiscordEventHandlers.Builder handlersBuilder;
   private DiscordRichPresence discordRPC;
   private String firstLine;
   private String secondLine;
   public static String fiio = "TrafimLine";
   public static String avatar = null;

   public void start() {
      if (System.getProperty("os.name").startsWith("Windows")) {
         this.created = System.currentTimeMillis() / 1000L;
         this.discord = Discord.INSTANCE;
         if (this.discord != null) {
            this.builder = new DiscordRichPresence.Builder();
            this.builder = this.builder.setStartTimestamp(this.created);
            this.builder = this.builder.setLargeImage("large").setSmallImage("large2");
            this.builder = this.builder
               .setButtons(
                  Arrays.asList(
                     RPCButton.create("Посмотреть", "https://discord.gg/9c33Yz7R2X"),
                     RPCButton.create("Купить на FunPay", "https://funpay.com/lots/offer?id=44666969")
                  )
               );
            this.discordRPC = this.builder.build();
            this.handlersBuilder = new DiscordEventHandlers.Builder();
            this.discord.Discord_Initialize("1054570317726617711", this.handlersBuilder.build(), true, "");
            this.update("Загрузка...", "Ещё чуть-чуть");
         }
      }
   }

   public void shutdown() {
      if (System.getProperty("os.name").startsWith("Windows")) {
         this.running = false;
         this.discord.Discord_ClearPresence();
         this.discord.Discord_Shutdown();
      }
   }

   public void refresh() {
      if (!Panic.stop && this.discordRPC != null && System.getProperty("os.name").startsWith("Windows")) {
         if (this.firstLine != null && this.secondLine != null && ViaMCP.INSTANCE() != null) {
            String mainImageString = Client.name.replace("00", "").trim();
            mainImageString = Client.nameCut + " " + Client.releaseType + " v092 " + mainImageString.replace(Client.nameCut, "");
            String miniImageString = "v" + ViaMCP.INSTANCE().getViaPanel().getCurrentProtocol().getName().replace("4/5", "5") + " UID: " + Client.staticAddress;
            this.builder = this.builder.setStartTimestamp(this.created);
            this.builder = this.builder.setLargeImage("large", mainImageString).setSmallImage("large2", miniImageString);
            this.builder = this.builder
               .setButtons(
                  Arrays.asList(
                     RPCButton.create("Посмотреть", "https://discord.gg/9c33Yz7R2X"),
                     RPCButton.create("Купить на FunPay", "https://funpay.com/lots/offer?id=44666969")
                  )
               );
            this.builder = this.builder.setDetails(this.firstLine);
            this.builder = this.builder.setState(this.secondLine + this.mods());
            this.discordRPC = this.builder.build();
            this.discord.Discord_UpdatePresence(this.discordRPC);
         }
      }
   }

   private String mods() {
      return Client.moduleManager != null
         ? " | Моды: " + Client.moduleManager.getEnabledModulesCount() + "/" + Client.moduleManager.getModuleList().size()
         : "";
   }

   public void update(String firstLine, String secondLine) {
      if (System.getProperty("os.name").startsWith("Windows")) {
         this.firstLine = firstLine;
         this.secondLine = secondLine;
         String mainImageString = Client.name.replace("00", "").trim();
         mainImageString = Client.nameCut + " " + Client.releaseType + " v092 " + mainImageString.replace(Client.nameCut, "");
         String miniImageString = "v" + ViaMCP.INSTANCE().getViaPanel().getCurrentProtocol().getName().replace("4/5", "5") + " UID: " + Client.staticAddress;
         this.builder = this.builder.setStartTimestamp(this.created);
         this.builder = this.builder.setLargeImage("large", mainImageString).setSmallImage("large2", miniImageString);
         this.builder = this.builder
            .setButtons(
               Arrays.asList(
                  RPCButton.create("Посмотреть", "https://discord.gg/9c33Yz7R2X"),
                  RPCButton.create("Купить на FunPay", "https://funpay.com/lots/offer?id=44666969")
               )
            );
         this.builder = this.builder.setDetails(this.firstLine);
         this.builder = this.builder.setState(this.secondLine + this.mods());
         this.discordRPC = this.builder.build();
         this.discord.Discord_UpdatePresence(this.discordRPC);
      }
   }
}
