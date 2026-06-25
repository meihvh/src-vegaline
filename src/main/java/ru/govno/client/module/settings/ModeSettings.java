package ru.govno.client.module.settings;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import ru.govno.client.module.Module;

public class ModeSettings extends Settings {
   public String[] modes;
   public String currentMode;

   public ModeSettings(String name, String currentMode, Module module, String[] modes) {
      this.name = name;
      this.currentMode = currentMode;
      this.module = module;
      this.modes = modes;
   }

   public ModeSettings(String name, String currentMode, Module module, String[] modes, Supplier<Boolean> visible) {
      this.name = name;
      this.currentMode = currentMode;
      this.module = module;
      this.modes = modes;
      this.visible = visible;
   }

   public void setMode(String value) {
      this.currentMode = value;
   }

   public String getMode() {
      return this == null ? "" : this.currentMode;
   }

   public void syncAsyncToNewStringArray(String[] newModes) {
      if (newModes != null && newModes.length != 0) {
         CompletableFuture.runAsync(() -> {
            boolean doSet = false;
            if (newModes.length != this.modes.length) {
               doSet = true;
            } else {
               for (int i = 0; i < (newModes.length > this.modes.length ? newModes.length : this.modes.length); i++) {
                  if (newModes.length < i || this.modes.length < i) {
                     doSet = true;
                     break;
                  }

                  String i0 = newModes[i];
                  String i1 = this.modes[i];
                  if (i0 == null || !i0.equals(i1)) {
                     doSet = true;
                  }
               }
            }

            if (doSet) {
               this.modes = newModes;
               if (Arrays.stream(this.modes).noneMatch(mode -> mode.equalsIgnoreCase(this.getMode()))) {
                  this.setMode(newModes[0]);
               }
            }
         });
      }
   }

   public int getModeIndex() {
      for (int i = 0; i < this.modes.length; i++) {
         if (this.modes[i].equalsIgnoreCase(this.getMode())) {
            return i;
         }
      }

      return 0;
   }

   public void setModeIndex(int index) {
      this.setMode(this.modes[index % this.modes.length]);
   }

   public int setNextModeOrder() {
      int next = this.getModeIndex() + 1;
      if (next == this.modes.length) {
         next = 0;
      }

      this.setMode(this.modes[next]);
      return this.getModeIndex();
   }

   public int setPrevModeOrder() {
      int prev = this.getModeIndex() - 1;
      if (prev < 0) {
         prev = this.modes.length - 1;
      }

      this.setMode(this.modes[prev]);
      return this.getModeIndex();
   }
}
