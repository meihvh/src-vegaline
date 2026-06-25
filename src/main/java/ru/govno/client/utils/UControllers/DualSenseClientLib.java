package ru.govno.client.utils.UControllers;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Vec2f;
import org.apache.commons.io.FileUtils;

public class DualSenseClientLib {
   @Nullable
   private static Map<String, String> lastData = new HashMap<>();

   public static DualSenseClientLib.DualSenseLib instance() {
      return DualSenseClientLib.DualSenseLib.INSTANCE;
   }

   public static Map<String, String> getLastData() {
      return lastData;
   }

   public static Map<String, String> tryCallData() {
      if (DualSenseClientLib.DualSenseLib.INSTANCE == null) {
         return lastData;
      } else {
         Map<String, String> data = new HashMap<>();

         try {
            int count = DualSenseClientLib.DualSenseLib.INSTANCE.DS_Scan();
            if (count == 0) {
               lastData.clear();
               return lastData;
            }

            byte[] buffer = new byte[4096];
            int len = DualSenseClientLib.DualSenseLib.INSTANCE.DS_GetMostActiveState(buffer, buffer.length);
            if (len > 0) {
               String json = new String(buffer, 0, len);
               if (json == null || json.length() < 3) {
                  return lastData;
               }

               json = json.substring(1, json.length() - 1);

               for (String all : json.split(",")) {
                  String[] dataSplits = all.split(":");
                  String name = dataSplits[0];
                  String value = dataSplits[1];
                  data.put(name.replace("\"", ""), value.replace("\"", ""));
               }
            }
         } catch (Exception var12) {
            var12.printStackTrace();
         }

         lastData = data;
         return data;
      }
   }

   public static Vec2f valueOfV(String from) {
      if (!from.contains("notmatch") && from.contains("&")) {
         String[] split = from.split("&");
         return new Vec2f(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
      } else {
         return null;
      }
   }

   public static float valueOfF(String from) {
      return Float.parseFloat(from);
   }

   public static boolean valueOfB(String from) {
      return Boolean.parseBoolean(from);
   }

   public interface DualSenseLib extends Library {
      DualSenseClientLib.DualSenseLib INSTANCE = Native.loadLibrary(
         FileUtils.getUserDirectory().getAbsolutePath() + "\\Appdata\\Roaming\\VEGA.NCO\\vlDualsenseHid.vlapo", DualSenseClientLib.DualSenseLib.class
      );

      int DS_Scan();

      void DS_Disconnect();

      int DS_GetState(int var1, byte[] var2, int var3);

      int DS_GetMostActiveState(byte[] var1, int var2);

      int DS_IsEdge(int var1);

      int DS_SetLightBar(int var1, int var2, int var3, int var4);

      int DS_SetPlayerLEDs(int var1, int var2);

      int DS_SetLightBarAndLEDs(int var1, int var2, int var3, int var4, int var5);

      int DS_SetLightBarAndPlayer1(int var1, int var2, int var3, int var4);

      int DS_SetLightBarAndPlayer2(int var1, int var2, int var3, int var4);

      int DS_SetLightBarAndPlayer3(int var1, int var2, int var3, int var4);

      int DS_SetLightBarAndPlayer4(int var1, int var2, int var3, int var4);

      int DS_SetLightBarAndPlayer5(int var1, int var2, int var3, int var4);

      int DS_SendHapticsAudio(int var1, byte[] var2, int var3);

      int DS_SendSpeakerAudio(int var1, byte[] var2, int var3, int var4);

      int DS_SendHapticsAndSpeakerAudio(int var1, byte[] var2, int var3, byte[] var4, int var5, int var6);
   }
}
