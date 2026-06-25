package net.minecraft.world;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

public class NextTickListEntry implements Comparable<NextTickListEntry> {
   private static long nextTickEntryID;
   private final Block block;
   public final BlockPos position;
   public long scheduledTime;
   public int priority;
   private final long tickEntryID;

   public NextTickListEntry(BlockPos positionIn, Block blockIn) {
      this.tickEntryID = nextTickEntryID++;
      this.position = positionIn.toImmutable();
      this.block = blockIn;
   }

   @Override
   public boolean equals(Object p_equals_1_) {
      return !(p_equals_1_ instanceof NextTickListEntry nextticklistentry)
         ? false
         : this.position.equals(nextticklistentry.position) && Block.isEqualTo(this.block, nextticklistentry.block);
   }

   @Override
   public int hashCode() {
      return this.position.hashCode();
   }

   public NextTickListEntry setScheduledTime(long scheduledTimeIn) {
      this.scheduledTime = scheduledTimeIn;
      return this;
   }

   public void setPriority(int priorityIn) {
      this.priority = priorityIn;
   }

   public int compareTo(NextTickListEntry p_compareTo_1_) {
      if (this.scheduledTime < p_compareTo_1_.scheduledTime) {
         return -1;
      } else if (this.scheduledTime > p_compareTo_1_.scheduledTime) {
         return 1;
      } else if (this.priority != p_compareTo_1_.priority) {
         return this.priority - p_compareTo_1_.priority;
      } else if (this.tickEntryID < p_compareTo_1_.tickEntryID) {
         return -1;
      } else {
         return this.tickEntryID > p_compareTo_1_.tickEntryID ? 1 : 0;
      }
   }

   @Override
   public String toString() {
      return Block.getIdFromBlock(this.block) + ": " + this.position + ", " + this.scheduledTime + ", " + this.priority + ", " + this.tickEntryID;
   }

   public Block getBlock() {
      return this.block;
   }
}
