package ru.govno.client.trial.Ui;

import java.util.ArrayList;
import java.util.List;

public class WindowManager {
   public static WindowManager instance;
   private final List<Window> windows;

   public WindowManager() {
      instance = this;
      this.windows = new ArrayList<>();
   }

   public void addWindow(Window window) {
      this.windows.add(window);
   }

   public void renderWindows() {
      if (!this.windows.isEmpty()) {
         Window currentWindow = this.windows.get(0);
         if (currentWindow != null) {
            currentWindow.render();
         }

         this.windows.removeIf(window -> window.closed);
      }
   }
}
