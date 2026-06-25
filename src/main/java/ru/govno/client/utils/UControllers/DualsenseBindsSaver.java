package ru.govno.client.utils.UControllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class DualsenseBindsSaver {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File saver = new File(Minecraft.getMinecraft().mcDataDir + "/saves/configurations/", "dualsensecache.sav");

   private static String indexPrefix(int index) {
      return "Bind_№" + index;
   }

   public void save(List<DualsenseBindAction> dxBindActionList) {
      try {
         JsonObject jsonObject = new JsonObject();
         int index = 0;

         for (DualsenseBindAction dxBindAction : dxBindActionList) {
            jsonObject.addProperty(indexPrefix(index), dxBindAction.getAllDataString());
            index++;
         }

         FileWriter fileWriter = new FileWriter(this.saver);
         BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         bufferedWriter.write(this.GSON.toJson((JsonElement)jsonObject));
         fileWriter.flush();
         bufferedWriter.flush();
         fileWriter.close();
         bufferedWriter.close();
      } catch (IOException var6) {
         var6.printStackTrace();
      }
   }

   public List<DualsenseBindAction> load(List<DualsenseBindAction> dxBindActionListPrev) {
      try {
         FileReader fileReader = new FileReader(this.saver);
         BufferedReader bufferedReader = new BufferedReader(fileReader);
         JsonObject jsonObject = this.GSON.fromJson(bufferedReader, JsonObject.class);
         fileReader.close();
         bufferedReader.close();
         if (jsonObject == null) {
            return new ArrayList<>();
         } else {
            List<DualsenseBindAction> getedDxActionBinds = new ArrayList<>();

            JsonElement data;
            for (int index = 0; index < 1000 && (data = jsonObject.get(indexPrefix(index))) != null; index++) {
               DualsenseBindAction dxBindAction = DualsenseBindAction.asConfigLine(data.getAsString());
               if (dxBindAction != null) {
                  getedDxActionBinds.add(dxBindAction);
               }
            }

            return getedDxActionBinds;
         }
      } catch (IOException var9) {
         var9.printStackTrace();
         return dxBindActionListPrev;
      }
   }
}
