package ru.govno.client.utils.UControllers;

import java.util.ArrayList;
import java.util.List;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.utils.MacroMngr.Macros;

public class DualsenseBindHandler {
   private List<DualsenseBindAction> binds;
   private final DualsenseBindsSaver saver = new DualsenseBindsSaver();

   public DualsenseBindHandler() {
      this.binds = new ArrayList<>();
      this.binds = this.saver.load(this.binds);
   }

   public void onPressSenseButtonBindsExecute(DualsenseController.SenseButton button) {
      DualsenseBindAction dualsenseBindAction = this.getAsButton(button);
      if (dualsenseBindAction != null && button != DualsenseSettings.bindsTriggerActivator()) {
         dualsenseBindAction.executeAction();
      }
   }

   public void toggleBindToList(DualsenseController.SenseButton button, Module module) {
      DualsenseBindAction findAnyBind = this.getAsModule(module);
      if (findAnyBind != null) {
         boolean isConcurentBind = findAnyBind.tryGetBindButton() == button;
         this.binds.removeIf(bindAction -> bindAction.tryGetModule() == module);
         this.onPostUpdateBindList();
         if (isConcurentBind) {
            ClientTune.get.playGuiModuleBindSong(false);
            return;
         }
      }

      if (button != DualsenseSettings.bindsTriggerActivator()) {
         this.binds.add(DualsenseBindAction.asModuleBind(module, button));
         this.onPostUpdateBindList();
         ClientTune.get.playGuiModuleBindSong(true);
      }
   }

   public void toggleBindToList(DualsenseController.SenseButton button, Macros macros) {
      DualsenseBindAction findAnyBind = this.getAsMacros(macros);
      if (findAnyBind != null) {
         boolean isConcurentBind = findAnyBind.tryGetBindButton() == button;
         this.binds.removeIf(bindAction -> bindAction.tryGetMacros() == macros);
         this.onPostUpdateBindList();
         if (isConcurentBind) {
            ClientTune.get.playGuiModuleBindSong(false);
            return;
         }
      }

      if (button != DualsenseSettings.bindsTriggerActivator()) {
         this.binds.add(DualsenseBindAction.asMacrosBind(macros, button));
         this.onPostUpdateBindList();
         ClientTune.get.playGuiModuleBindSong(true);
      }
   }

   public DualsenseBindAction getAsButton(DualsenseController.SenseButton button) {
      return this.binds.stream().filter(bindAction -> bindAction.tryGetBindButton() == button).findFirst().orElse(null);
   }

   public List<DualsenseBindAction> getAllAsButton(DualsenseController.SenseButton button) {
      return this.binds.stream().filter(bindAction -> bindAction.tryGetBindButton() == button).toList();
   }

   public DualsenseBindAction getAsButtonAndModule(DualsenseController.SenseButton button, Module module) {
      if (button == null) {
         return null;
      } else {
         return module == null
            ? this.binds
               .stream()
               .filter(bindAction -> bindAction.tryGetModule() != null)
               .filter(bindAction -> bindAction.tryGetBindButton() == button)
               .findFirst()
               .orElse(null)
            : this.binds
               .stream()
               .filter(bindAction -> bindAction.tryGetModule() != null && bindAction.tryGetModule() != null)
               .filter(
                  bindAction -> bindAction.tryGetBindButton().getName().equalsIgnoreCase(button.getName())
                        && bindAction.tryGetModule().getName().equalsIgnoreCase(module.getName())
               )
               .findFirst()
               .orElse(null);
      }
   }

   public DualsenseBindAction getAsModule(Module module) {
      return module == null
         ? null
         : this.binds
            .stream()
            .filter(bindAction -> bindAction.tryGetModule() != null)
            .filter(bindAction -> bindAction.tryGetModule().getName().equalsIgnoreCase(module.getName()))
            .findFirst()
            .orElse(null);
   }

   public DualsenseBindAction getAsMacros(Macros macros) {
      return macros == null
         ? null
         : this.binds
            .stream()
            .filter(bindAction -> bindAction.tryGetMacros() != null)
            .filter(bindAction -> bindAction.tryGetMacros().getName().equalsIgnoreCase(macros.getName()))
            .findFirst()
            .orElse(null);
   }

   public DualsenseController.SenseButton getDualsenseBindButton(Module fromModule) {
      DualsenseBindAction dxBindAction = this.binds
         .stream()
         .filter(bindAction -> bindAction.tryGetModule() != null)
         .filter(bindAction -> bindAction.tryGetModule().getName().equalsIgnoreCase(fromModule.getName()))
         .findFirst()
         .orElse(null);
      return dxBindAction == null ? null : dxBindAction.tryGetBindButton();
   }

   public DualsenseController.SenseButton getDualsenseBindButton(Macros fromMacros) {
      DualsenseBindAction dxBindAction = this.binds
         .stream()
         .filter(bindAction -> bindAction.tryGetMacros() != null)
         .filter(bindAction -> bindAction.tryGetMacros().getName().equalsIgnoreCase(fromMacros.getName()))
         .findFirst()
         .orElse(null);
      return dxBindAction == null ? null : dxBindAction.tryGetBindButton();
   }

   private void onPostUpdateBindList() {
      this.saver.save(this.binds);
   }

   public List<DualsenseBindAction> getBinds() {
      return this.binds;
   }

   public DualsenseBindsSaver getBindsSaver() {
      return this.saver;
   }
}
