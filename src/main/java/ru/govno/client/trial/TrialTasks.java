package ru.govno.client.trial;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import ru.govno.client.Client;
import ru.govno.client.trial.Ui.WindowManager;

public class TrialTasks {
   private final TrialStatsSaver configurator = TrialStatsSaver.create()
      .addSavableBool("trialOpenClickGui", true)
      .addSavableBool("trialOpenModuleClickGui", true)
      .addSavableBool("trialBindModuleClickGui", true)
      .addSavableBool("trialScrollClickGui", true)
      .addSavableBool("trialUseCommands", true)
      .build();
   private TrialStatsSaver.SaveBoolContainer save = this.configurator.getSaveBoolContainer().getPostLoad();
   private final List<Trial> trials = new ArrayList<>();
   private final WindowManager windowManager = new WindowManager();
   public static TrialTasks instance;

   public WindowManager getWindowManager() {
      return this.windowManager;
   }

   private void addTrial(
      Runnable onTrialFinalized, Supplier<Boolean> trialStartIf, String trialLabel, String[] trialMessage, String[] buttonLabels, Runnable[] buttonActions
   ) {
      this.trials.add(new Trial(onTrialFinalized, trialStartIf, trialLabel, trialMessage, buttonLabels, buttonActions));
   }

   private TrialTasks() {
      instance = this;
   }

   private void resetTrialsStats() {
      this.save = this.save.saveValues(this.configurator.getBooleansAtInitTrialTime()).getPostLoad();
   }

   public static void stopAllTrials() {
      instance.save = instance.save.fillSingleBoolean(false).getPostLoad();
   }

   public static TrialTasks startup() {
      return new TrialTasks();
   }

   public void updateTasksStatus() {
      this.save = this.save.saveValues();
   }

   public void updateTrial(String trialName, Runnable onStartTrial, Supplier<Boolean> startIf) {
      this.resetTrialsStats();
      if (this.save.getSaved(trialName) && onStartTrial != null && startIf.get()) {
         onStartTrial.run();
         byte var5 = -1;
         switch (trialName.hashCode()) {
            case -480306893:
               if (trialName.equals("trialOpenClickGui")) {
                  var5 = 0;
               }
            default:
               switch (var5) {
                  case 0:
                     this.addTrial(
                        () -> {
                           Client.msg("Молодец ёбаныйдалбоёб228!", true);
                           Client.msg("Ты смог открыть меню, вау ничосе!", true);
                        },
                        () -> Minecraft.player != null && Minecraft.player.ticksExisted > 100,
                        "Открой меню клиента",
                        new String[]{"Сейчас у меню кнопка бинда это (LCONTROL)", "нажми LCONTROL что-бы открыть меню"},
                        null,
                        null
                     );
                  default:
                     this.save.setBoolean(trialName, false);
                     this.updateTasksStatus();
               }
         }
      }
   }
}
