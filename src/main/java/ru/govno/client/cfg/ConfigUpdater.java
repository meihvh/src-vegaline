package ru.govno.client.cfg;

import com.google.gson.JsonObject;

public interface ConfigUpdater {
   JsonObject save();

   boolean load(JsonObject var1);
}
