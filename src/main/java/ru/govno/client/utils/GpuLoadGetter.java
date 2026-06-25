package ru.govno.client.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;
import ru.govno.client.utils.Math.MathUtils;

public class GpuLoadGetter {
   private int prevLoad0100;
   private int load0100;
   private boolean isSupported;
   private final ru.govno.client.utils.Math.TimerHelper updateLoadGpuTimer = ru.govno.client.utils.Math.TimerHelper.TimerHelperReseted();

   private GpuLoadGetter() {
      this.isSupported = this.getBrandGpuCurrentOGLContext() != GpuLoadGetter.BrandGpu.UNSUPPORTED;
   }

   public static GpuLoadGetter build() {
      return new GpuLoadGetter();
   }

   public static GpuLoadGetter buildWhileNull(@Nullable GpuLoadGetter nullableGpuLoadGetter) {
      return nullableGpuLoadGetter == null ? new GpuLoadGetter() : nullableGpuLoadGetter;
   }

   private long updateLoadGpuDelay() {
      return 1000L;
   }

   public int getLoad(boolean update, boolean interpolated) {
      if (update) {
         this.updateLoad();
      }

      return interpolated
         ? (int)MathUtils.lerp(
            (float)this.prevLoad0100, (float)this.load0100, Math.min((float)this.updateLoadGpuTimer.getTime() / (float)this.updateLoadGpuDelay() * 7.5F, 1.0F)
         )
         : this.load0100;
   }

   public String getLoadString(boolean update, boolean interpolated, boolean append) {
      int load = this.getLoad(update, interpolated);
      return append ? "Gpu load (" + load + "%)" : String.valueOf(load);
   }

   public boolean isSupported() {
      this.updateLoad();
      return this.isSupported;
   }

   private void updateLoad() {
      try {
         if (!this.updateLoadGpuTimer.hasReached((double)this.updateLoadGpuDelay())) {
            return;
         }

         this.updateLoadGpuTimer.reset();
         GpuLoadGetter.BrandGpu brandGpu;
         if (!(this.isSupported = (brandGpu = this.getBrandGpuCurrentOGLContext()) != GpuLoadGetter.BrandGpu.UNSUPPORTED)) {
            return;
         }

         CompletableFuture.runAsync(() -> {
            int load = this.getLoadGpu0100WhileCan(brandGpu);
            if (load == -1) {
               this.load0100 = 0;
               this.prevLoad0100 = 0;
               this.isSupported = false;
            } else {
               this.prevLoad0100 = this.load0100;
               this.load0100 = load;
            }
         });
      } catch (Exception var2) {
         var2.printStackTrace();
         Sys.alert("Unknown err:", "-bo6");
      }
   }

   private int getLoadGpu0100WhileCan(GpuLoadGetter.BrandGpu brandGpu) {
      switch (brandGpu) {
         case NVIDIA:
            try {
               Process process = Runtime.getRuntime().exec(brandGpu.getCommandGetLoad());
               if (process != null) {
                  BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                  if (stdInput != null) {
                     stdInput.readLine();
                     return Integer.parseInt(stdInput.readLine().replace(" %", ""));
                  }
               }
            } catch (Exception var4) {
               return -1;
            }
         default:
            return -1;
         case UNSUPPORTED:
            return -1;
      }
   }

   private GpuLoadGetter.BrandGpu getBrandGpuCurrentOGLContext() {
      try {
         String gpuDisplayName = GL11.glGetString(7937) + ", Driver " + GL11.glGetString(7936);
         if (gpuDisplayName.toLowerCase().contains("nvidia")) {
            return GpuLoadGetter.BrandGpu.NVIDIA;
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      return GpuLoadGetter.BrandGpu.UNSUPPORTED;
   }

   private static enum BrandGpu {
      UNSUPPORTED(null),
      NVIDIA("nvidia-smi --query-gpu=utilization.gpu --format=csv");

      String commandGetLoad;

      private BrandGpu(String commandGetLoad) {
         this.commandGetLoad = commandGetLoad;
      }

      public String getCommandGetLoad() {
         return this.commandGetLoad;
      }
   }
}
