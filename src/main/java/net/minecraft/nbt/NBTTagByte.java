package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class NBTTagByte extends NBTPrimitive {
   private byte data;

   NBTTagByte() {
   }

   public NBTTagByte(byte data) {
      this.data = data;
   }

   @Override
   void write(DataOutput output) throws IOException {
      output.writeByte(this.data);
   }

   @Override
   void read(DataInput input, int depth, NBTSizeTracker sizeTracker) throws IOException {
      sizeTracker.read(72L);
      this.data = input.readByte();
   }

   @Override
   public byte getId() {
      return 1;
   }

   @Override
   public String toString() {
      return this.data + "b";
   }

   public NBTTagByte copy() {
      return new NBTTagByte(this.data);
   }

   @Override
   public boolean equals(Object p_equals_1_) {
      return super.equals(p_equals_1_) && this.data == ((NBTTagByte)p_equals_1_).data;
   }

   @Override
   public int hashCode() {
      return super.hashCode() ^ this.data;
   }

   @Override
   public long getLong() {
      return (long)this.data;
   }

   @Override
   public int getInt() {
      return this.data;
   }

   @Override
   public short getShort() {
      return (short)this.data;
   }

   @Override
   public byte getByte() {
      return this.data;
   }

   @Override
   public double getDouble() {
      return (double)this.data;
   }

   @Override
   public float getFloat() {
      return (float)this.data;
   }
}
