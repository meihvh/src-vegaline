package ru.govno.client.trial;

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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;

public class TrialStatsSaver {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final File saver = new File(new File(Minecraft.getMinecraft().mcDataDir, "/saves/configurations"), "trialstats.sav");
   private final Map<String, Boolean> booleansToSave = new HashMap<>();
   private TrialStatsSaver.SaveBoolContainer saveBoolContainer;

   public Map<String, Boolean> getBooleansAtInitTrialTime() {
      return this.booleansToSave;
   }

   private TrialStatsSaver() {
   }

   public static TrialStatsSaver create() {
      return new TrialStatsSaver();
   }

   public TrialStatsSaver addSavableBool(String booleanName, boolean value) {
      this.booleansToSave.put(booleanName, value);
      return this;
   }

   public TrialStatsSaver build() {
      this.saveBoolContainer = new TrialStatsSaver.SaveBoolContainer(this.booleansToSave);
      return this;
   }

   public TrialStatsSaver.SaveBoolContainer getSaveBoolContainer() {
      return this.saveBoolContainer;
   }

   public class SaveBoolContainer {
      private Map<String, Boolean> booleans;

      private SaveBoolContainer(Map<String, Boolean> booleans) {
         this.booleans = booleans;
      }

      public TrialStatsSaver.SaveBoolContainer saveValues(@Nullable Map<String, Boolean> booleans) {
         if (booleans != null) {
            this.booleans = booleans;
         }

         try {
            JsonObject jsonObject = new JsonObject();
            if (this.booleans.isEmpty()) {
               return this;
            }

            for (Entry<String, Boolean> entry : booleans.entrySet()) {
               jsonObject.addProperty(entry.getKey(), entry.getValue());
            }

            FileWriter fileWriter = new FileWriter(TrialStatsSaver.this.saver);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            TrialStatsSaver.this.GSON.toJson((JsonElement)jsonObject, bufferedWriter);
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.flush();
            fileWriter.close();
         } catch (IOException var5) {
            var5.printStackTrace();
         }

         return this;
      }

      public TrialStatsSaver.SaveBoolContainer saveValues() {
         return this.saveValues(null);
      }

      public TrialStatsSaver.SaveBoolContainer getPostLoad() {
         if (this.booleans.isEmpty()) {
            return this;
         } else {
            try {
               FileReader fileReader = new FileReader(TrialStatsSaver.this.saver);
               BufferedReader bufferedReader = new BufferedReader(fileReader);
               JsonObject jsonObject = TrialStatsSaver.this.GSON.fromJson(bufferedReader, JsonObject.class);
               bufferedReader.close();
               fileReader.close();
               if (jsonObject == null) {
                  return this;
               }

               for (Entry<String, Boolean> entry : this.booleans.entrySet()) {
                  if (jsonObject.has(entry.getKey())) {
                     entry.setValue(jsonObject.get(entry.getKey()).getAsBoolean());
                  }
               }
            } catch (IOException var6) {
               var6.printStackTrace();
            }

            return this;
         }
      }

      public boolean getSaved(String booleanName) {
         Boolean value = this.booleans.get(booleanName);
         return value != null && value;
      }

      public TrialStatsSaver.SaveBoolContainer setBoolean(String booleanName, boolean value) {
         if (this.booleans.get(booleanName) == null) {
            return this;
         } else {
            this.booleans.replace(booleanName, value);
            TrialStatsSaver.this.saveBoolContainer.saveValues();
            return this;
         }
      }

      public TrialStatsSaver.SaveBoolContainer fillSingleBoolean(boolean value) {
         for (Entry<String, Boolean> entry : this.booleans.entrySet()) {
            entry.setValue(value);
         }

         TrialStatsSaver.this.saveBoolContainer.saveValues();
         return this;
      }
   }
}
