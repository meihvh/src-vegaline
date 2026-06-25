package ru.govno.client.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import ru.govno.client.module.modules.Notifications;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class UProfiler {
   private final TimerHelper timer = new TimerHelper();
   private long calcDelay;
   private String search;
   private final String nameProfiler;
   private long nanoMS;
   private long tempMS;
   private boolean paused = true;
   private boolean pausedByDelay;
   private final List<UProfiler.Property> properties = new ArrayList<>();
   private final List<String> toShow = new ArrayList<>();

   public void setUpdateDelay(long updateDelay) {
      this.calcDelay = updateDelay;
   }

   public void setSearch(String search) {
      this.search = search;
   }

   public void unsetSearch() {
      this.search = null;
   }

   public boolean isSearching() {
      return this.search != null && !this.search.isEmpty();
   }

   public String getSearch() {
      return this.search;
   }

   private UProfiler(long delayCalc, String nameProfiler) {
      this.calcDelay = delayCalc;
      this.nameProfiler = nameProfiler;
   }

   public long getUpdateDelay() {
      return this.calcDelay;
   }

   public String getNameProfiler() {
      return this.nameProfiler;
   }

   public static UProfiler build(long delayCalc, String nameProfiler) {
      return new UProfiler(delayCalc, nameProfiler);
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public List<String> dataShowed() {
      return this.toShow;
   }

   public void startCalc() {
      if (!this.paused && !(this.pausedByDelay = !this.timer.hasReached((float)this.getUpdateDelay()))) {
         this.nanoMS = System.nanoTime();
         this.properties.clear();
      }
   }

   public void addObj(String name) {
      if (!this.paused && !this.pausedByDelay) {
         long nanos = System.nanoTime();
         this.properties.add(new UProfiler.Property(name, Math.max(nanos - (this.properties.isEmpty() ? this.nanoMS : this.tempMS) - 300L, 0L)));
         this.tempMS = nanos;
      }
   }

   public void endCalc(boolean sort) {
      if (!this.paused && !this.pausedByDelay) {
         if (sort && this.properties.size() > 1) {
            this.properties.sort(Comparator.comparingLong(UProfiler.Property::getNanoTime));
         }

         if (!this.toShow.isEmpty()) {
            this.toShow.clear();
         }

         float msSum = 0.0F;
         float msToFPSSum = 0.0F;

         for (UProfiler.Property property : this.properties) {
            float ms = (float)property.getNanoTime() / 1000000.0F;
            if (!(ms < 0.001F)) {
               msSum += ms;
               float msToFPS = ms * 1000.0F / 6.6666665F;
               msToFPSSum += msToFPS;
               String fpsLat = String.format("%.2f", msToFPS);
               fpsLat = fpsLat.replace("0.00", "0");
               fpsLat = fpsLat.replace("00", "0");
               fpsLat = fpsLat.equalsIgnoreCase("0") ? "" : TextFormatting.GRAY + " ~(-" + fpsLat + ")fps";
               this.toShow.add(property.getName() + " " + TextFormatting.WHITE + String.format("%.4f", ms) + "ms" + fpsLat);
               if (ms > 10.0F) {
                  System.out.println("LAG FOUND: " + property.getName() + " " + String.format("%.4f", ms) + "ms!");
                  Notifications.Notify.spawnNotify(
                     "LAG FOUND: " + property.getName() + " " + TextFormatting.RED + String.format("%.4f", ms) + "ms!", Notifications.type.STAFF
                  );
               }
            }
         }

         if (!this.toShow.isEmpty() && msSum > 0.005F) {
            String fpsLatSum = String.format("%.2f", msToFPSSum);
            fpsLatSum = fpsLatSum.replace("0.00", "0");
            fpsLatSum = fpsLatSum.replace("00", "0");
            fpsLatSum = fpsLatSum.equalsIgnoreCase("0") ? "" : TextFormatting.GRAY + " ~(-" + fpsLatSum + ")fps";
            this.toShow.add(0, "~Sum all delays ~" + String.format("%.4f", msSum) + "ms" + fpsLatSum);
         }

         this.timer.reset();
      }
   }

   public float drawingResultsHeight(float x, float y, float scale) {
      if (this.paused) {
         return 0.0F;
      } else {
         float h = 18.0F;
         x /= scale;
         y /= scale;
         GL11.glPushMatrix();
         GL11.glScaled((double)scale, (double)scale, 1.0);
         int bgCol = ColorUtils.getColor(0, 0, 0, 130);
         if (this.toShow.isEmpty()) {
            RenderUtils.drawRect(
               (double)x, (double)y, (double)(x + 2.0F + Fonts.minecraftia_16.getStringWidth("No results in " + this.nameProfiler)), (double)(y + 9.0F), bgCol
            );
            Fonts.minecraftia_16.drawString("No results in " + this.nameProfiler, x, y - 1.0F, ColorUtils.astolfoColorsCool(0, (int)y));
         } else {
            GL11.glScaled((double)(1.0F / scale), (double)(1.0F / scale), 1.0);
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow("Profiler " + this.nameProfiler, x, y - 1.0F, -1);
            GL11.glScaled((double)scale, (double)scale, 1.0);
            int index = 0;

            for (String info : this.toShow) {
               float iY = y + (float)index * 9.0F + 9.0F / scale;
               RenderUtils.drawRect(
                  (double)x,
                  (double)iY,
                  (double)(x + 2.0F + Fonts.minecraftia_16.getStringWidth(info)),
                  (double)(iY + 9.0F),
                  info.startsWith("~") ? ColorUtils.getColor(100, 33, 100) : bgCol
               );
               Fonts.minecraftia_16
                  .drawString(
                     info,
                     x + 1.0F,
                     iY + 1.0F,
                     info.startsWith("~")
                        ? ColorUtils.getColor(0, 255, 255)
                        : ColorUtils.getOverallColorFrom(
                           ColorUtils.getColor(155, 255, 155), ColorUtils.getColor(255, 100, 100), (float)index / (float)this.toShow.size()
                        )
                  );
               h += 9.0F * scale;
               index++;
            }
         }

         GL11.glPopMatrix();
         return h;
      }
   }

   public void cleanup() {
      this.toShow.clear();
      this.properties.clear();
      this.paused = true;
      this.unsetSearch();
   }

   private class Property {
      private final String name;
      private final long nanoTime;

      public Property(String name, long nanoTime) {
         this.name = name;
         this.nanoTime = nanoTime;
      }

      public String getName() {
         return this.name;
      }

      public long getNanoTime() {
         return this.nanoTime;
      }
   }
}
