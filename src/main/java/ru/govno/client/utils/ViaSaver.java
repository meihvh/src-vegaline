package ru.govno.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import vialoadingbase.ViaLoadingBase;
import viamcp.ViaMCP;

public class ViaSaver {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File saver = new File(Minecraft.getMinecraft().mcDataDir + "/saves/configurations/", "versioncache.sav");

   public void save() {
      ProtocolVersion protocolVersion = null;

      try {
         JsonObject jsonObject = new JsonObject();
         protocolVersion = ViaMCP.INSTANCE().getViaPanel().getCurrentProtocol();
         jsonObject.addProperty("versionName", protocolVersion.getName());
         jsonObject.addProperty("protocolVer", protocolVersion.getVersion());
         FileWriter fileWriter = new FileWriter(this.saver);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         bufferedWriter.write(this.GSON.toJson((JsonElement)jsonObject));
         fileWriter.flush();
         bufferedWriter.flush();
         fileWriter.close();
         bufferedWriter.close();
      } catch (IOException var8) {
         var8.printStackTrace();
      } finally {
         System.out.println(protocolVersion == null ? "Minecraft version isn`t was saved" : "Minecraft version " + protocolVersion.getName() + " was saved");
      }
   }

   public int load(int nativeVer) {
      String verName = null;
      int versionId = nativeVer;

      try {
         FileReader fileReader = new FileReader(this.saver);
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         JsonObject jsonObject = this.GSON.fromJson(bufferedReader, JsonObject.class);
         fileReader.close();
         bufferedReader.close();
         if (jsonObject == null) {
            System.out.println("Minecraft version isn`t was loaded");
            return nativeVer;
         } else {
            verName = jsonObject.get("versionName").getAsString();
            if (verName == null) {
               System.out.println("Minecraft version isn`t was loaded");
               return nativeVer;
            } else if (jsonObject.get("protocolVer") == null) {
               System.out.println("Minecraft version isn`t was loaded");
               return versionId;
            } else {
               versionId = jsonObject.get("protocolVer").getAsInt();
               ProtocolVersion versionProtocol = ProtocolVersion.getClosest(verName);
               if (versionProtocol == null) {
                  System.out.println("Minecraft version isn`t was loaded");
                  return versionId;
               } else {
                  ViaLoadingBase.getInstance().reload(versionProtocol);
                  System.out.println("Minecraft version " + verName + " was loaded");
                  return versionProtocol.getVersion();
               }
            }
         }
      } catch (IOException var8) {
         var8.printStackTrace();
         return nativeVer;
      }
   }
}
