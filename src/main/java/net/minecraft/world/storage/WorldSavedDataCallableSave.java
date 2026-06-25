package net.minecraft.world.storage;

public class WorldSavedDataCallableSave implements Runnable {
   private final WorldSavedData data;

   public WorldSavedDataCallableSave(WorldSavedData dataIn) {
      this.data = dataIn;
   }

   @Override
   public void run() {
      this.data.markDirty();
   }
}
