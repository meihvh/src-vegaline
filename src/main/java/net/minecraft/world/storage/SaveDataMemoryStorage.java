package net.minecraft.world.storage;

import javax.annotation.Nullable;

public class SaveDataMemoryStorage extends MapStorage {
   public SaveDataMemoryStorage() {
      super((ISaveHandler)null);
   }

   @Nullable
   @Override
   public WorldSavedData getOrLoadData(Class<? extends WorldSavedData> clazz, String dataIdentifier) {
      return this.loadedDataMap.get(dataIdentifier);
   }

   @Override
   public void setData(String dataIdentifier, WorldSavedData data) {
      this.loadedDataMap.put(dataIdentifier, data);
   }

   @Override
   public void saveAllData() {
   }

   @Override
   public int getUniqueDataId(String key) {
      return 0;
   }
}
