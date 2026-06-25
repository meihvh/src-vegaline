package ru.govno.client.trial;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import ru.govno.client.trial.Ui.Window;
import ru.govno.client.trial.Ui.WindowManager;

public class Trial {
   private final Runnable onTrialFinalized;
   private boolean trialWasFinish;
   private boolean trialWasStarted;
   private final Supplier<Boolean> trialStartIf;
   private final String trialLabel;
   private final String[] trialMessage;
   private Window window;
   private final String[] buttonLabels;
   private final Runnable[] buttonActions;

   public Trial(
      Runnable onTrialFinalized, Supplier<Boolean> trialStartIf, String trialLabel, String[] trialMessage, String[] buttonLabels, Runnable[] buttonActions
   ) {
      this.onTrialFinalized = onTrialFinalized;
      this.trialStartIf = trialStartIf;
      this.trialLabel = trialLabel;
      this.trialMessage = trialMessage;
      this.buttonLabels = buttonLabels;
      this.buttonActions = buttonActions;
   }

   private void start() {
      List<String> buttonLabelsList = Arrays.asList(this.buttonLabels);
      List<Runnable> buttonActionsList = Arrays.asList(this.buttonActions);
      buttonLabelsList.add("Пропустить");
      buttonActionsList.add(() -> this.stop());
      buttonLabelsList.add("Завершить всё");
      buttonActionsList.add(() -> {
         TrialTasks.stopAllTrials();
         this.stop();
      });
      WindowManager.instance
         .addWindow(this.window = new Window(this.trialLabel, this.trialMessage, (String[])buttonLabelsList.toArray(), (Runnable[])buttonActionsList.toArray()));
   }

   public void update() {
      if (this.trialStartIf.get() && !this.trialWasStarted) {
         this.start();
         this.trialWasStarted = true;
      }
   }

   private void stop() {
      this.trialWasFinish = true;
      this.window.close();
   }
}
