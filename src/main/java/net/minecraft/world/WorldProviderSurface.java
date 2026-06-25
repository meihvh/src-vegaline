package net.minecraft.world;

public class WorldProviderSurface extends WorldProvider {
   @Override
   public DimensionType getDimensionType() {
      return DimensionType.OVERWORLD;
   }

   @Override
   public boolean canDropChunk(int x, int z) {
      return !this.worldObj.isSpawnChunk(x, z);
   }
}
