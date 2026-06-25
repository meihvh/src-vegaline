package ru.govno.client.utils.URender;

import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class UGL {
   private static boolean inBegin;
   private static ResourceLocation lastBindedTexture;
   private static float lineWidth = 1.0F;
   private static float pointSize = 1.0F;

   public static void setAlphaMin(float value) {
      GL11.glAlphaFunc(516, value);
   }

   public static float r(int c) {
      return (float)(c >> 16 & 0xFF) / 255.0F;
   }

   public static float g(int c) {
      return (float)(c >> 8 & 0xFF) / 255.0F;
   }

   public static float b(int c) {
      return (float)(c & 0xFF) / 255.0F;
   }

   public static float a(int c) {
      return (float)(c >> 24 & 0xFF) / 255.0F;
   }

   public static boolean isOn(int func) {
      return GL11.glIsEnabled(func);
   }

   public static void toggle(int func, boolean active) {
      if (active) {
         enable(func);
      } else {
         disable(func);
      }
   }

   public static void enable(int func) {
      if (!isOn(func)) {
         GL11.glEnable(func);
      }
   }

   public static void disable(int func) {
      if (isOn(func)) {
         GL11.glDisable(func);
      }
   }

   public static void shade(boolean enabled) {
      GL11.glShadeModel(enabled ? 7425 : 7424);
   }

   public static void shade(List<UVertex> vertices) {
      shade(new HashSet<>(vertices.stream().map(UVertex::getColor).toList()).size() != 1);
   }

   public static void blend(boolean bloom) {
      GL11.glBlendFunc(770, bloom ? 1 : 771);
   }

   public static void hint(int hint, int type) {
      GL11.glHint(hint, type);
   }

   public static void vert(float x, float y) {
      GL11.glVertex2f(x, y);
   }

   public static void vert(float x, float y, float z) {
      GL11.glVertex3f(x, y, z);
   }

   public static void vert(double x, double y) {
      GL11.glVertex2d(x, y);
   }

   public static void vert(double x, double y, double z) {
      GL11.glVertex3d(x, y, z);
   }

   public static void vert(float x, float y, int c) {
      if (inBegin) {
         color(c);
         GL11.glVertex2f(x, y);
      }
   }

   public static void vert(float x, float y, float z, int c) {
      if (inBegin) {
         color(c);
         GL11.glVertex3f(x, y, z);
      }
   }

   public static void vert(double x, double y, int c) {
      if (inBegin) {
         color(c);
         GL11.glVertex2d(x, y);
      }
   }

   public static void vert(double x, double y, double z, int c) {
      if (inBegin) {
         color(c);
         GL11.glVertex3d(x, y, z);
      }
   }

   public static void verts(List<UVertex> vertices) {
      if (inBegin) {
         vertices.forEach(UVertex::doGl);
      }
   }

   public static void color(float r, float g, float b, float a) {
      GL11.glColor4f(r, g, b, a);
   }

   public static void color(int c) {
      color(r(c), g(c), b(c), a(c));
   }

   public static void begin(int mode) {
      GL11.glBegin(mode);
      inBegin = true;
   }

   public static void end() {
      GL11.glEnd();
      inBegin = false;
   }

   public static void trans(float x, float y) {
      GL11.glTranslatef(x, y, 0.0F);
   }

   public static void trans(float x, float y, float z) {
      GL11.glTranslatef(x, y, z);
   }

   public static void scale(float x, float y, float z) {
      GL11.glScalef(x, y, z);
   }

   public static void scale(float x, float y) {
      GL11.glScalef(x, y, 1.0F);
   }

   public static void tex(float x, float y) {
      GL11.glTexCoord2f(x, y);
   }

   public static void scaleAt(float x, float y, float z, float scale) {
      trans(x, y, z);
      scale(scale, scale, scale);
      trans(-x, -y, -z);
   }

   public static void scaleAt(float x, float y, float scale) {
      trans(x, y);
      scale(scale, scale);
      trans(-x, -y);
   }

   public static boolean isTextured() {
      return isOn(3553);
   }

   public static void depth(boolean enabled) {
      toggle(2929, enabled);
   }

   public static void mask(boolean enabled) {
      GL11.glDepthMask(enabled);
   }

   public static void cull(boolean enabled) {
      toggle(2884, enabled);
   }

   public static void bindTex(ResourceLocation location) {
      if ((lastBindedTexture == null || lastBindedTexture != location) && location != null) {
         TextureManager manager = Minecraft.getMinecraft().getTextureManager();
         ITextureObject itextureobject = manager.getMapTextureObjects().get(location);
         if (itextureobject == null) {
            itextureobject = new SimpleTexture(location);
            manager.loadTexture(location, itextureobject);
         }

         TextureUtil.bindTexture(itextureobject.getGlTextureId());
         lastBindedTexture = location;
      }
   }

   public static ResourceLocation getLastBindedTexture() {
      return lastBindedTexture;
   }

   public static void setLineWidth(float lineWidth1) {
      lineWidth = lineWidth1;
      GL11.glLineWidth(lineWidth1);
   }

   public static void setPointSize(float pointSize1) {
      pointSize = pointSize1;
      GL11.glPointSize(pointSize1);
   }

   public static void resetLineWidth() {
      lineWidth = 1.0F;
      GL11.glLineWidth(1.0F);
   }

   public static void resetPointSize() {
      pointSize = 1.0F;
      GL11.glPointSize(1.0F);
   }

   public static float getLastLineWidth() {
      return lineWidth;
   }

   public static float getLastPointSize() {
      return pointSize;
   }

   public static void setAntialiasing(int begin, boolean enable) {
      switch (begin) {
         case 0:
            toggle(2832, enable);
            break;
         case 1:
         case 2:
         case 3:
            toggle(2848, enable);
            hint(3154, enable ? 4354 : 4352);
            break;
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
            toggle(2881, enable);
            hint(3155, enable ? 4354 : 4352);
      }
   }

   public static void resetColor() {
      GL11.glColor3b((byte)1, (byte)1, (byte)1);
   }
}
