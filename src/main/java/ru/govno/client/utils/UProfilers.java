package ru.govno.client.utils;

import com.sun.management.OperatingSystemMXBean;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import ru.govno.client.Client;
import ru.govno.client.newfont.Fonts;
import ru.govno.client.utils.Math.FrameCounter;
import ru.govno.client.utils.Math.MathUtils;
import ru.govno.client.utils.Render.ColorUtils;
import ru.govno.client.utils.Render.RenderUtils;

public class UProfilers {
   private final List<UProfiler> profilers = new ArrayList<>();
   private boolean enabledProfilers = false;
   private boolean enabledMonitoring = false;
   private boolean enabledProperties = false;
   private final UProfiler DEFAULT_PROFILER = UProfiler.build(10000000000L, "NONE");
   private long delay;
   private final FrameCounter frameCounter = FrameCounter.build();
   private long lastReachedMS0;
   private long lastReachedMS1;
   private final ArrayList<String> pcProperties = new ArrayList<>();
   private final ArrayList<String> tempListPCDataUpdated0 = new ArrayList<>();
   private final ArrayList<String> tempListPCDataUpdated1 = new ArrayList<>();
   private GpuLoadGetter gpuLoadGetter;

   public boolean isEnabled() {
      return this.enabledProfilers || this.enabledMonitoring || this.enabledProperties;
   }

   public boolean isEnabledProfilers() {
      return this.enabledProfilers;
   }

   public boolean isEnabledMonitoring() {
      return this.enabledMonitoring;
   }

   public boolean isEnabledProperties() {
      return this.enabledProperties;
   }

   public long getDelay() {
      return this.delay;
   }

   private UProfilers(long delayProfilers, String... profilersToAdd) {
      for (String profilerName : profilersToAdd) {
         this.addProfiler(delayProfilers, profilerName);
      }

      this.delay = delayProfilers;
   }

   public static UProfilers build(long delayProfilers, String... profilersToAdd) {
      return new UProfilers(delayProfilers, profilersToAdd);
   }

   public void addProfiler(long delayProfilers, String profilerName) {
      this.profilers.add(UProfiler.build(delayProfilers, profilerName));
   }

   public UProfiler getProfiler(String profileName) {
      UProfiler current = this.profilers.stream().filter(profiler -> profiler.getNameProfiler().equalsIgnoreCase(profileName)).findAny().orElse(null);
      return current == null ? this.DEFAULT_PROFILER : current;
   }

   public List<UProfiler> getProfilers() {
      return this.profilers;
   }

   public UProfiler getProfiler(int index) {
      return this.profilers.get(index) == null ? this.DEFAULT_PROFILER : this.profilers.get(index);
   }

   public void start(boolean profilersEnable, boolean monitoringEnable, boolean propertiesEnabled) {
      if (profilersEnable) {
         for (UProfiler profiler : this.profilers) {
            profiler.setPaused(false);
         }

         this.enabledProfilers = true;
      }

      if (monitoringEnable) {
         this.enabledMonitoring = true;
      }

      if (propertiesEnabled) {
         this.enabledProperties = true;
      }
   }

   public void stop(boolean profilersDisable, boolean monitoringDisable, boolean propertiesDisable) {
      if (profilersDisable) {
         for (UProfiler profiler : this.profilers) {
            profiler.setPaused(true);
            profiler.cleanup();
         }

         this.enabledProfilers = false;
      }

      if (monitoringDisable) {
         this.enabledMonitoring = false;
      }

      if (propertiesDisable) {
         this.enabledProperties = false;
      }
   }

   public void setDelays(long delayProfilers) {
      for (UProfiler profiler : this.profilers) {
         profiler.setUpdateDelay(delayProfilers);
      }

      this.delay = delayProfilers;
   }

   public void setSearch(String search) {
      this.profilers.forEach(profiler -> profiler.setSearch(search));
   }

   public void unsetSearch() {
      this.profilers.forEach(profiler -> profiler.unsetSearch());
   }

   public boolean anySearching() {
      return this.profilers.stream().anyMatch(profiler -> profiler.isSearching());
   }

   public boolean isSearching(UProfiler profiler) {
      return profiler.isSearching();
   }

   public String getSearch(UProfiler profiler) {
      return profiler.getSearch();
   }

   public void drawIn2D(ScaledResolution sr) {
      if ((this.enabledProfilers || this.enabledMonitoring || this.enabledProperties)
         && !this.profilers.isEmpty()
         && !Minecraft.getMinecraft().gameSettings.showDebugInfo) {
         GL11.glTranslatef(0.0F, 0.0F, 1000.0F);
         GL11.glEnable(3553);
         float x = 5.0F;
         float y = x;

         for (UProfiler profiler : this.profilers) {
            y += profiler.drawingResultsHeight(x, y, 0.5F);
         }

         if (this.enabledMonitoring) {
            this.frameCounter.renderThreadRead((int)MathUtils.clamp(this.frameCounter.getFps() / 3.33333F, 5.0, 240.0));
            String counterData = this.frameCounter.getFpsString(true)
               + ", "
               + this.frameCounter.getAverageFpsString(true)
               + ", "
               + this.frameCounter.getMinFpsString(true)
               + ", "
               + this.frameCounter.getLatencyString(true);
            float fpsPC = (float)MathUtils.clamp(this.frameCounter.getFps() / 200.0, 0.0, 1.0);
            float offX = 10.0F;
            float strW = (float)Minecraft.getMinecraft().fontRendererObj.getStringWidth(counterData);
            Minecraft.getMinecraft()
               .fontRendererObj
               .drawStringWithShadow(
                  counterData,
                  (float)sr.getScaledWidth() - x - offX - strW,
                  x,
                  ColorUtils.getOverallColorFrom(ColorUtils.getColor(255, 40, 40), ColorUtils.getColor(100, 255, 255), fpsPC)
               );
            float offXLines = this.frameCounter.getLatencyWindowWidth();
            this.frameCounter.drawLatencyLine((float)sr.getScaledWidth() - x - offXLines, x + 12.0F);
         }

         if (this.enabledProperties) {
            this.pcProperties.clear();
            this.pcProperties.addAll(this.updatedPCData());
            y = (float)sr.getScaledHeight() / 2.0F - (float)this.pcProperties.size() * 7.5F;
            int indexLine = 0;
            int gray = -10197916;
            int black = -16777216;

            for (String data : this.pcProperties) {
               float strW = Fonts.noise_14.getStringWidth(data) + 4.0F;
               float xPos = (float)sr.getScaledWidth() - x - strW - 2.0F;
               float yPos = y + 15.0F * (float)indexLine;
               RenderUtils.drawRect((double)(xPos - 1.0F), (double)(yPos - 1.0F), (double)(xPos + strW + 1.0F), (double)(yPos + 11.0F), -1);
               RenderUtils.drawRect((double)(xPos - 0.5F), (double)(yPos - 0.5F), (double)(xPos + strW + 0.5F), (double)(yPos + 10.5F), black);
               RenderUtils.drawRect((double)xPos, (double)yPos, (double)(xPos + strW), (double)(yPos + 10.0F), gray);
               Fonts.noise_14.drawString(data, xPos + 2.0F, yPos + 3.0F, black);
               indexLine++;
            }
         }

         if (this.enabledProfilers && this.anySearching()) {
            int yExpand = 80;
            int bgExtY = 2;
            int bgExtX = 4;
            int bgH = 14;
            int resultsCount = 0;

            for (UProfiler profiler : this.profilers) {
               if (profiler.isSearching()) {
                  for (String s : profiler.dataShowed()) {
                     if (s.toLowerCase().contains(profiler.getSearch().toLowerCase()) && !s.startsWith("~Sum")) {
                        resultsCount++;
                     }
                  }
               }
            }

            String title = "Results of search by contain '" + this.getSearch(this.profilers.get(0)) + "': (" + resultsCount + ")";
            int bgCol = ColorUtils.getColor(30, 105, 30);
            float titleW = Fonts.noise_24.getStringWidth(title);
            float titleX1 = (float)sr.getScaledWidth() / 2.0F - titleW / 2.0F;
            RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
               titleX1 - (float)bgExtX,
               (float)(yExpand - bgExtY),
               titleX1 + titleW + (float)bgExtX,
               (float)(yExpand + bgH + bgExtY),
               3.0F,
               1.0F,
               bgCol,
               bgCol,
               bgCol,
               bgCol,
               false,
               true,
               true
            );
            bgCol = ColorUtils.getColor(40);
            Fonts.noise_24
               .drawStringWithShadow(
                  title,
                  (float)sr.getScaledWidth() / 2.0F - Fonts.noise_24.getStringWidth(title) / 2.0F,
                  (float)yExpand + 1.0F,
                  ColorUtils.getColor(100, 255, 100)
               );
            yExpand = (int)((float)yExpand + 25.0F);

            for (UProfiler profilerx : this.profilers) {
               if (profilerx.isSearching()) {
                  int lastI = 0;

                  for (String sx : profilerx.dataShowed()) {
                     if (sx.toLowerCase().contains(profilerx.getSearch().toLowerCase()) && !sx.startsWith("~Sum")) {
                        float strW = Fonts.noise_24.getStringWidth(sx);
                        float textX1 = (float)sr.getScaledWidth() / 2.0F - strW / 2.0F;
                        GL11.glDisable(2929);
                        RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                           textX1 - (float)bgExtX,
                           (float)(yExpand - bgExtY),
                           textX1 + strW + (float)bgExtX,
                           (float)(yExpand + bgH + bgExtY),
                           3.0F,
                           1.0F,
                           bgCol,
                           bgCol,
                           bgCol,
                           bgCol,
                           false,
                           true,
                           true
                        );
                        Fonts.noise_24.drawString(sx, textX1, (float)yExpand + 1.0F, -1);
                        GL11.glEnable(2929);
                        yExpand += 20;
                        if ((float)yExpand >= (float)sr.getScaledHeight() / 3.0F) {
                           int lastI2 = 0;
                           int var38 = 8;
                           int var32 = 1;
                           int var35 = 2;

                           for (String s1 : profilerx.dataShowed()) {
                              if (s1.toLowerCase().contains(profilerx.getSearch().toLowerCase()) && !s1.startsWith("~Sum")) {
                                 if (lastI < lastI2) {
                                    strW = Fonts.noise_14.getStringWidth(s1);
                                    textX1 = (float)sr.getScaledWidth() / 2.0F - strW / 2.0F;
                                    bgCol = ColorUtils.getColor(40);
                                    GL11.glDisable(2929);
                                    RenderUtils.drawRoundedFullGradientShadowFullGradientRoundedFullGradientRectWithBloomBool(
                                       textX1 - (float)var35,
                                       (float)(yExpand - var32),
                                       textX1 + strW + (float)var35,
                                       (float)(yExpand + var38 + var32),
                                       3.0F,
                                       1.0F,
                                       bgCol,
                                       bgCol,
                                       bgCol,
                                       bgCol,
                                       false,
                                       true,
                                       true
                                    );
                                    Fonts.noise_14.drawString(s1, textX1, (float)yExpand + 1.0F, -1);
                                    GL11.glEnable(2929);
                                    yExpand += 13;
                                 }

                                 if (++lastI2 > 10) {
                                    break;
                                 }
                              }
                           }

                           bgH = 14;
                           bgExtY = 2;
                           bgExtX = 4;
                           break;
                        }

                        if (++lastI > 10) {
                           break;
                        }
                     }
                  }
               }
            }
         }

         GL11.glTranslatef(0.0F, 0.0F, -1000.0F);
      }
   }

   public ArrayList<String> updatedPCData() {
      if (this.lastReachedMS1 < System.currentTimeMillis()) {
         try {
            this.tempListPCDataUpdated1.clear();
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double jvmCpuLoad = osBean.getProcessCpuLoad();
            this.tempListPCDataUpdated1.add("JVM_cpu_load: " + String.format("%.2f", jvmCpuLoad * 100.0) + "%");
            this.tempListPCDataUpdated1
               .add(
                  "Sys_dram_load: "
                     + String.format(
                        "%2d%% %03d/%02dMB",
                        (osBean.getTotalMemorySize() - osBean.getFreeMemorySize()) * 100L / osBean.getTotalMemorySize(),
                        (osBean.getTotalMemorySize() - osBean.getFreeMemorySize()) / 1024L / 1024L,
                        osBean.getTotalMemorySize() / 1024L / 1024L
                     )
               );
            this.gpuLoadGetter = GpuLoadGetter.buildWhileNull(this.gpuLoadGetter);
            if (this.gpuLoadGetter.isSupported()) {
               this.tempListPCDataUpdated1.add("JVM_gpu_load: " + this.gpuLoadGetter.getLoad(true, false) + "%");
            }
         } catch (Exception var14) {
            var14.printStackTrace();
         }

         this.lastReachedMS1 = System.currentTimeMillis() + 2000L;
      }

      if (this.lastReachedMS0 < System.currentTimeMillis()) {
         try {
            this.tempListPCDataUpdated0.clear();
            String jvmString = System.getProperty("java.vendor")
               + "/"
               + String.format("Java: %s %dbit", System.getProperty("java.version"), Minecraft.getMinecraft().isJava64bit() ? 64 : 32);
            String recommendedVM = "17.0.6";
            if (Client.badJavaVersion) {
               jvmString = jvmString + TextFormatting.DARK_RED + " (not recommended java version, update your java to " + recommendedVM + " version)";
            }

            this.tempListPCDataUpdated0.add("Current_JVM: " + jvmString);
            long maxMem = Runtime.getRuntime().maxMemory();
            long totalMem = Runtime.getRuntime().totalMemory();
            long freeMem = Runtime.getRuntime().freeMemory();
            this.tempListPCDataUpdated0
               .add(
                  "JVM_dram_load: "
                     + String.format("%2d%% %03d/%03dMB", (totalMem - freeMem) * 100L / maxMem, (totalMem - freeMem) / 1024L / 1024L, maxMem / 1024L / 1024L)
               );
            this.tempListPCDataUpdated0.add("JVM_started_OS: " + System.getProperty("os.name") + " | ver: " + System.getProperty("os.version"));
            this.tempListPCDataUpdated0.add("PC_CPU: " + String.format("%s", OpenGlHelper.getCpu()).trim());
            this.tempListPCDataUpdated0.add("PC_GPU: " + GL11.glGetString(7937));
            this.tempListPCDataUpdated0.add("VGA_Driver: " + GL11.glGetString(7936));
            GraphicsDevice firstDisplay = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int pixWidth = firstDisplay.getDefaultConfiguration().getBounds().width;
            int pixHeight = firstDisplay.getDefaultConfiguration().getBounds().height;
            int refreshRate = firstDisplay.getDisplayMode().getRefreshRate();
            this.tempListPCDataUpdated0.add("Display_Standard: " + pixWidth + "x" + pixHeight + " (" + refreshRate + "hz)");
         } catch (Exception var13) {
            var13.printStackTrace();
         }

         this.lastReachedMS0 = System.currentTimeMillis() + 50L;
      }

      ArrayList<String> finalList = new ArrayList<>();
      finalList.addAll(this.tempListPCDataUpdated1);
      finalList.addAll(this.tempListPCDataUpdated0);
      return finalList;
   }
}
