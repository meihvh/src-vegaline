package ru.govno.client.utils.URender;

import net.minecraft.client.renderer.BufferBuilder;

public class UVertex {
   public float x;
   public float y;
   public float z;
   public float u;
   public float v;
   public int c;
   public boolean d3;
   public boolean t;

   private UVertex(float x, float y, float z, float u, float v, boolean d, boolean t, int c) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.u = u;
      this.v = v;
      this.d3 = d;
      this.t = t;
      this.c = c;
   }

   public static UVertex vertex(float x, float y, float z, float u, float v, int c) {
      return new UVertex(x, y, z, u, v, true, true, c);
   }

   public static UVertex vertex(float x, float y, float z, int c) {
      return new UVertex(x, y, z, 0.0F, 0.0F, true, false, c);
   }

   public static UVertex vertex(float x, float y, float u, float v, int c) {
      return new UVertex(x, y, 0.0F, u, v, false, true, c);
   }

   public static UVertex vertex(float x, float y, int c) {
      return new UVertex(x, y, 0.0F, 0.0F, 0.0F, false, false, c);
   }

   public int getColor() {
      return this.c;
   }

   public void doGl() {
      UGL.color(this.c);
      if (this.t) {
         UGL.tex(this.u, this.v);
         if (this.d3) {
            UGL.vert(this.x, this.y, this.z);
         } else {
            UGL.vert(this.x, this.y, this.z);
         }
      } else {
         if (this.d3) {
            UGL.vert(this.x, this.y, this.z);
         } else {
            UGL.vert(this.x, this.y);
         }
      }
   }

   public void doMcVBO(BufferBuilder buffer) {
      if (this.t) {
         if (this.d3) {
            buffer.pos((double)this.x, (double)this.y, (double)this.z).tex((double)this.u, (double)this.v).color(this.c).endVertex();
         } else {
            buffer.pos((double)this.x, (double)this.y).tex((double)this.u, (double)this.v).color(this.c).endVertex();
         }
      } else {
         if (this.d3) {
            buffer.pos((double)this.x, (double)this.y, (double)this.z).color(this.c).endVertex();
         } else {
            buffer.pos((double)this.x, (double)this.y).color(this.c).endVertex();
         }
      }
   }
}
