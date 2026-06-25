package ru.govno.client.utils.UControllers;

import ru.govno.client.module.modules.DualsenseSupport;

public class DualsenseSettings {
   public static boolean useDualsenseForGaming() {
      return DualsenseSupport.canUseDualsenseGamepadForGaming();
   }

   public static float inDeadZoneSticks() {
      return DualsenseSupport.getInDeadZoneAdaptive();
   }

   public static String showPanelMode() {
      return DualsenseSupport.getUiShowMode();
   }

   public static String moveStickMode() {
      return DualsenseSupport.getMoveMode();
   }

   public static DualsenseController.SenseButton bindsTriggerActivator() {
      return DualsenseController.SenseButton.R1;
   }

   public static DualsenseController.SenseButton[] buttonsWhichIgnoreBindsActiveState() {
      return new DualsenseController.SenseButton[]{DualsenseController.SenseButton.CROSS};
   }

   public static DualsenseController.SenseButton bindingSelectorInterOrSelectButton() {
      return DualsenseController.SenseButton.CROSS;
   }

   public static DualsenseController.SenseButton bindingSelectorExitButton() {
      return DualsenseController.SenseButton.CIRCLE;
   }

   public static DualsenseController.SenseButton[] bindsSettingsModeComboActivator() {
      return new DualsenseController.SenseButton[]{DualsenseController.SenseButton.R1, DualsenseController.SenseButton.PS};
   }

   public static String[] bindingGuideTexts() {
      return new String[]{
         "для активации биндов или биндинга в панели зажмите кнопку " + bindsTriggerActivator().getName() + " на вашем dualsense,",
         "для открытия панели настройки нажмите "
            + bindsSettingsModeComboActivator()[0].getName()
            + "+"
            + bindsSettingsModeComboActivator()[1].getName()
            + " на вашем dualsense,"
      };
   }
}
