package viamcp;

import java.io.File;
import ru.govno.client.utils.ViaSaver;
import ru.govno.client.utils.Versions.NewPhisicsFixes;
import ru.govno.client.utils.Versions.ViaInstalatorPanel;
import vialoadingbase.ViaLoadingBase;

public class ViaMCP {
   public static final int NATIVE_VERSION = 340;
   public static ViaMCP inst;
   private ViaInstalatorPanel viaPanel;

   public static ViaMCP INSTANCE() {
      return inst == null ? (inst = new ViaMCP()) : inst;
   }

   public static void create() {
      while (inst == null || inst.getViaPanel() == null) {
         new ViaMCP();
      }
   }

   public ViaMCP() {
      ViaLoadingBase.ViaLoadingBaseBuilder.create().runDirectory(new File("ViaMCP")).nativeVersion(340).onProtocolReload(comparableProtocolVersion -> {
         NewPhisicsFixes.updateNewVerionStatus(comparableProtocolVersion);
         NewPhisicsFixes.updateOldVerionStatus(comparableProtocolVersion);
         new ViaSaver().save();
      }).build();
      this.viaPanel = new ViaInstalatorPanel(15.0F, 15.0F);
      inst = this;
   }

   public ViaInstalatorPanel getViaPanel() {
      return this.viaPanel == null ? (this.viaPanel = new ViaInstalatorPanel(15.0F, 15.0F)) : this.viaPanel;
   }
}
