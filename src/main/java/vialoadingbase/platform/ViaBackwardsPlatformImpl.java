package vialoadingbase.platform;

import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import java.io.File;
import java.util.logging.Logger;
import vialoadingbase.ViaLoadingBase;

public class ViaBackwardsPlatformImpl implements ViaBackwardsPlatform {
   private final File directory;

   public ViaBackwardsPlatformImpl(File directory) {
      this.init(this.directory = directory);
   }

   @Override
   public Logger getLogger() {
      return ViaLoadingBase.LOGGER;
   }

   @Override
   public boolean isOutdated() {
      return false;
   }

   @Override
   public void disable() {
   }

   @Override
   public File getDataFolder() {
      return this.directory;
   }
}
