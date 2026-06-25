package vialoadingbase.platform;

import com.viaversion.viarewind.ViaRewindConfig;
import com.viaversion.viarewind.api.ViaRewindPlatform;
import java.io.File;
import java.util.logging.Logger;
import vialoadingbase.ViaLoadingBase;

public class ViaRewindPlatformImpl implements ViaRewindPlatform {
   public ViaRewindPlatformImpl(File directory) {
      ViaRewindConfig config = new ViaRewindConfig(new File(directory, "viarewind.yml"));
      config.reloadConfig();
      this.init(config);
   }

   @Override
   public Logger getLogger() {
      return ViaLoadingBase.LOGGER;
   }
}
