package ru.govno.client.event.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import ru.govno.client.event.Event;

public class EventRenderBlock extends Event {
   private final IBlockState state;
   private final BlockPos pos;

   public EventRenderBlock(IBlockState state, BlockPos pos) {
      this.state = state;
      this.pos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
   }

   public IBlockState getState() {
      return this.state;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
