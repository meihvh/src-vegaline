package ru.govno.client.utils.UControllers;

import java.util.Arrays;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.utils.MacroMngr.Macros;

public class DualsenseBindAction {
   public String actionDisplay;

   private DualsenseBindAction(String actionDisplay) {
      this.actionDisplay = actionDisplay;
   }

   public static DualsenseBindAction asModuleBind(Module module, DualsenseController.SenseButton button) {
      return new DualsenseBindAction("moduleBind-" + module.getName() + "_button-" + button.getName());
   }

   public static DualsenseBindAction asMacrosBind(Macros macros, DualsenseController.SenseButton button) {
      return new DualsenseBindAction("macroBind-" + macros.getName() + "_macroCommand-" + macros.getMassage() + "_button-" + button.getName());
   }

   public static DualsenseBindAction asConfigLine(String lineValue) {
      return lineValue != null && (lineValue.startsWith("moduleBind-") || lineValue.startsWith("macroBind-")) ? new DualsenseBindAction(lineValue) : null;
   }

   public String getAllDataString() {
      return this.actionDisplay;
   }

   public String getNameDisplay() {
      Module tryGetModule = this.tryGetModule();
      if (tryGetModule != null) {
         return tryGetModule.getName();
      } else {
         Macros tryGetMacros = this.tryGetMacros();
         return tryGetMacros != null ? tryGetMacros.getName() : null;
      }
   }

   public void executeAction() {
      try {
         Module module = this.tryGetModule();
         if (module != null) {
            DualsenseController.SenseButton button = this.tryGetBindButton();
            if (button != null && button.isPressedForBind()) {
               module.toggle();
            }
         } else {
            Macros macros = this.tryGetMacros();
            if (macros != null) {
               DualsenseController.SenseButton button = this.tryGetBindButton();
               if (button != null && button.isPressedForBind()) {
                  macros.use();
                  ClientTune.get.playUseMacros();
               }
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public DualsenseController.SenseButton tryGetBindButton() {
      try {
         if (this.actionDisplay.startsWith("moduleBind-")) {
            String buttonName = this.actionDisplay.split("_")[1].substring("button-".length());
            DualsenseController.SenseButton button = Arrays.stream(DualsenseController.SenseButton.values())
               .filter(btn -> btn.getName().equalsIgnoreCase(buttonName))
               .findAny()
               .orElse(null);
            if (button != null) {
               return button;
            }
         } else if (this.actionDisplay.startsWith("macroBind-")) {
            String buttonName = this.actionDisplay.split("_")[2].substring("button-".length());
            DualsenseController.SenseButton button = Arrays.stream(DualsenseController.SenseButton.values())
               .filter(btn -> btn.getName().equalsIgnoreCase(buttonName))
               .findAny()
               .orElse(null);
            if (button != null) {
               return button;
            }
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      return null;
   }

   public Module tryGetModule() {
      try {
         if (this.actionDisplay.startsWith("moduleBind-")) {
            String moduleName = this.actionDisplay.substring("moduleBind-".length()).split("_")[0];
            if (Client.moduleManager != null) {
               Module module = Client.moduleManager.getModule(moduleName);
               if (module != null) {
                  return module;
               }
            }
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      return null;
   }

   public Macros tryGetMacros() {
      try {
         if (this.actionDisplay.startsWith("macroBind-")) {
            String macrosName = this.actionDisplay.substring("macroBind-".length()).split("_")[0];
            if (Client.macrosManager != null) {
               Macros macros = Client.macrosManager.getMacrosList().stream().filter(mc -> mc.getName().contains(macrosName)).findAny().orElse(null);
               if (macros != null) {
                  return macros;
               }
            }
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      return null;
   }

   public static void testSout() {
   }
}
