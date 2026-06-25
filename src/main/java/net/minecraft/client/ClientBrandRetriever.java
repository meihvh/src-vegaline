package net.minecraft.client;

import ru.govno.client.Client;
import ru.govno.client.module.modules.Bypass;
import ru.govno.client.utils.Command.impl.Panic;
import viamcp.ViaMCP;

public class ClientBrandRetriever {
   public static boolean doHideTrueName() {
      return !Panic.stop && (Bypass.get == null || !Bypass.get.ClientSpoof.getBool());
   }

   public static String getTrueClientName() {
      return Client.name + " " + getVersionStr() + "(" + Client.version + ")";
   }

   public static String getFalseClientPrefix() {
      return "Minecraft " + getVersionStr() + " ";
   }

   public static String getVersionStr() {
      String str = ViaMCP.INSTANCE().getViaPanel().INSTANCE.getCurrentProtocol().getName();
      str = str.replace("1.20/", "").replace("/1", "").replace("/2", "").replace("/3", "").replace("/4", "").replace("/5", "").replace(".x", "");
      return str.replace("1.16.4", "1.16.5").replace("1.8.x", "1.8.9");
   }

   public static String getReleaseTypePrefix() {
      String str = "";
      if (doHideTrueName()) {
         str = "ForgeOptifine ";
      } else {
         String versionStr = ViaMCP.INSTANCE().getViaPanel().INSTANCE.getCurrentProtocol().getName();
         switch (versionStr) {
            case "1.13":
            case "1.17":
               str = "Optifine ";
               break;
            case "1.15":
            case "1.15.1":
            case "1.16":
            case "1.20.2":
               str = "";
               break;
            default:
               str = "ForgeOptifine ";
         }
      }

      return str;
   }

   public static String getVersionSuffix() {
      String versionName = ViaMCP.INSTANCE().getViaPanel().INSTANCE.getCurrentProtocol().getName();
      String str = "";

      return switch (versionName) {
         case "1.7.2-1.7.5", "1.7.6-1.7.10" -> "";
         case "1.8.x" -> "/fml/Forge";
         case "1.9", "1.9.1", "1.9.2", "1.9.3/4" -> "/fml,forge/Forge";
         case "1.10.x", "1.11", "1.11/2", "1.12", "1.12.1", "1.12.2" -> "/fml,forge/Forge";
         case "1.13", "1.13.1", "1.13.2", "1.14", "1.14.1", "1.14.2", "1.14.3" -> "/vanilla/modified";
         case "1.14.4" -> "/forge/modified";
         case "1.15", "1.15.1" -> "/vanilla";
         case "1.15.2" -> "/forge/modified";
         case "1.16" -> "/vanilla";
         case "1.16.1", "1.16.2", "1.16.3", "1.16.4/5" -> "/forge/modified";
         case "1.17" -> "/vanilla/modified";
         case "1.17.1" -> "/forge/modified";
         case "1.18", "1.18.1" -> "/forge/modified";
         case "1.19", "1.19.1/2", "1.19.3", "1.19.4" -> "/forge/modified";
         case "1.20/1.20.1" -> "/forge/modified";
         case "1.20.2" -> "/vanilla";
         default -> "/fml,forge,Forge";
      };
   }

   public static String getClientModName() {
      return getReleaseTypePrefix() + getVersionStr() + getVersionSuffix();
   }

   public static String getDebugVersionString() {
      return doHideTrueName() ? getTrueClientName() : getFalseClientPrefix() + (getVersionSuffix().isEmpty() ? "" : "(" + getClientModName() + ")");
   }
}
