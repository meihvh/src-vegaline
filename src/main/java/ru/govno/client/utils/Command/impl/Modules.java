package ru.govno.client.utils.Command.impl;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import ru.govno.client.Client;
import ru.govno.client.module.Module;
import ru.govno.client.module.modules.ClientTune;
import ru.govno.client.module.settings.BoolSettings;
import ru.govno.client.module.settings.ColorSettings;
import ru.govno.client.module.settings.FloatSettings;
import ru.govno.client.module.settings.ModeSettings;
import ru.govno.client.module.settings.Settings;
import ru.govno.client.utils.MusicHelper;
import ru.govno.client.utils.Command.Command;
import ru.govno.client.utils.Render.ColorUtils;

public class Modules extends Command {
   private static final Minecraft mc = Minecraft.getMinecraft();

   public Modules() {
      super("Modules", new String[]{"ss", "setsetting", "module", "m"});
   }

   private static String grabbingSplitM() {
      return ">";
   }

   private static String grabbingSplitS() {
      return ",";
   }

   private static String grabbingSplitV() {
      return ":";
   }

   private String grabAllSettings(Module module) {
      if (module == null) {
         return "";
      } else {
         List<Settings> settings = module.getSettings();
         if (settings.size() == 0) {
            return "";
         } else {
            String out = module.getName() + grabbingSplitM();
            int index = 0;
            int indexV = 0;
            int indexVMax = (int)settings.stream().filter(Settings::isVisible).count();

            for (Settings s : settings) {
               if (s.isVisible()) {
                  String set = index + grabbingSplitV();
                  if (s instanceof BoolSettings v) {
                     set = set + (v.getBool() ? "1" : "0");
                  } else if (s instanceof FloatSettings v) {
                     float f = v.getFloat();
                     set = set + (f == (float)((int)f) ? String.valueOf((int)f) : String.valueOf(f));
                  } else if (s instanceof ModeSettings v) {
                     set = set + v.getModeIndex();
                  } else if (s instanceof ColorSettings v) {
                     set = set + v.getCol();
                  }

                  out = out + (indexV >= indexVMax - 1 ? set : set + grabbingSplitS());
                  indexV++;
               }

               index++;
            }

            return out;
         }
      }
   }

   private static String getGrabbedModuleName(String grabbedSetsString) {
      String moduleName = grabbedSetsString.split(grabbingSplitM())[0];
      Module findModule = Client.moduleManager.getModule(moduleName);
      return findModule != null ? findModule.getName() : moduleName;
   }

   private static boolean applyGrabbedSets(String grabbedSetsString) {
      String[] mSplit = grabbedSetsString.split(grabbingSplitM());
      String moduleName = mSplit[0];
      Module findModule = Client.moduleManager.getModule(moduleName);
      if (findModule == null) {
         return false;
      } else {
         grabbedSetsString = mSplit[1];
         String[] setsToApplyAll = grabbedSetsString.split(grabbingSplitS());
         if (setsToApplyAll.length == 0) {
            return false;
         } else {
            for (String setsToApply : setsToApplyAll) {
               try {
                  String[] settingData = setsToApply.split(grabbingSplitV());
                  if (settingData.length == 2) {
                     int index = Integer.parseInt(settingData[0]);
                     Settings setting = findModule.getSettings().get(index);
                     if (setting != null) {
                        String settingValueString = settingData[1];
                        if (setting instanceof BoolSettings v) {
                           v.setBool(settingValueString.equalsIgnoreCase("1"));
                        } else if (setting instanceof FloatSettings v) {
                           v.setFloat(Float.parseFloat(settingValueString));
                        } else if (setting instanceof ModeSettings v) {
                           v.setModeIndex(Integer.parseInt(settingValueString));
                        } else if (setting instanceof ColorSettings v) {
                           v.setCol(Integer.parseInt(settingValueString));
                        }
                     }
                  }
               } catch (NumberFormatException var17) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   @Override
   public void onCommand(String[] args) {
      try {
         if (args.length == 3 && args[1].equalsIgnoreCase("grab")) {
            Module findAnyModule = Client.moduleManager.getModule(args[2]);
            if (findAnyModule != null) {
               String tryGrab = this.grabAllSettings(findAnyModule);
               if (tryGrab.length() > 0) {
                  StringSelection selection = new StringSelection(tryGrab);
                  Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                  clipboard.setContents(selection, selection);
                  Client.msg("§f§lModules:§r §7активные настройки [§l" + findAnyModule.getName() + "§r§7] §7скопированы в буфер обмена", false);
                  return;
               }

               Client.msg("§2§lModules:§r §7настройки модуля [§l" + args[2] + "§r§7] §7скопировать не удалось", false);
               return;
            }

            Client.msg("§2§lModules:§r §7модуля с именем [§l" + args[2] + "§r§7] §7не существует", false);
         }

         if (args.length == 3 && args[1].equalsIgnoreCase("apply") && args[2].length() > 1) {
            if (applyGrabbedSets(args[2])) {
               Client.msg("§f§lModules:§r §7настройки из строки применены к модулю [§l" + getGrabbedModuleName(args[2]) + "§r§7] ", false);
               return;
            }

            Client.msg("§2§lModules:§r §7строку с настройками применить невозможно", false);
         }

         for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replace("%%", "&");
         }

         if (args[1].equalsIgnoreCase("reload")) {
            MusicHelper.playSound("hidecl.wav", 0.3F);
            Panic.restart();
            Client.msg("§6§lClient:§r §7Все системы возобновлены.", false);
         } else if (Client.moduleManager.getModule(args[1]) == null) {
            Client.msg("§2§lModules:§r §7модуля с именем [§l" + args[1] + "§r§7] §7не существует", false);
         } else {
            Module module = Client.moduleManager.getModule(args[1]);
            if (!args[2].equalsIgnoreCase("enable") && !args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("+")) {
               if (!args[2].equalsIgnoreCase("disable") && !args[2].equalsIgnoreCase("off") && !args[2].equalsIgnoreCase("-")) {
                  if (!args[2].equalsIgnoreCase("toggle") && !args[2].equalsIgnoreCase("tog")) {
                     Settings setting = null;

                     for (Settings set : module.settings) {
                        String num = set.getName();
                        String num2 = set.getName().toLowerCase();
                        String num3 = set.getName().toUpperCase();
                        if (num.equalsIgnoreCase(args[2].replace("_", " "))
                           || num2.equalsIgnoreCase(args[2].replace("_", " "))
                           || num3.equalsIgnoreCase(args[2].replace("_", " "))) {
                           setting = set;
                        }
                     }

                     if (setting == null) {
                        Client.msg("§f§lModules:§r §7настройки с именем [§l" + args[2].replace("_", " ") + "§r§7] §7не существует", false);
                     } else if (setting instanceof BoolSettings boolSet) {
                        boolean correct = args[3].equalsIgnoreCase("on")
                           || args[3].equalsIgnoreCase("true")
                           || args[3].equalsIgnoreCase("+")
                           || args[3].equalsIgnoreCase("off")
                           || args[3].equalsIgnoreCase("false")
                           || args[3].equalsIgnoreCase("-")
                           || args[3].equalsIgnoreCase("toggle")
                           || args[3].equalsIgnoreCase("tog");
                        if (!correct) {
                           if (args.length == 5 && args[3].equalsIgnoreCase("bind")) {
                              int keyBind = Keyboard.getKeyIndex(args[4].toLowerCase().trim());
                              if (keyBind != 0) {
                                 if (keyBind == boolSet.getBind()) {
                                    Client.msg(
                                       "§f§lModules:§r §7чек [§l"
                                          + setting.getName()
                                          + "§r§7] мода [§l"
                                          + module.getName()
                                          + "§r§7] уже забинжен на эту клавишу.",
                                       false
                                    );
                                 } else {
                                    boolSet.setBind(keyBind);
                                    ClientTune.get.playGuiModuleBindSong(true);
                                    Client.msg(
                                       "§f§lModules:§r §7чек [§l"
                                          + setting.getName()
                                          + "§r§7] мода [§l"
                                          + module.getName()
                                          + "§r§7] забинжен на "
                                          + Keyboard.getKeyName(keyBind)
                                          + ".",
                                       false
                                    );
                                 }

                                 return;
                              }
                           } else if (args.length == 4 && args[3].equalsIgnoreCase("unbind")) {
                              if (boolSet.getBind() == 0) {
                                 Client.msg(
                                    "§f§lModules:§r §7чек [§l" + setting.getName() + "§r§7] мода [§l" + module.getName() + "§r§7] уже разбинжен.", false
                                 );
                              } else {
                                 boolSet.setBind(0);
                                 ClientTune.get.playGuiModuleBindSong(false);
                                 Client.msg("§f§lModules:§r §7чек [§l" + setting.getName() + "§r§7] мода [§l" + module.getName() + "§r§7] разбинжен.", false);
                              }

                              return;
                           }

                           Client.msg("§f§lModules:§r §7Комманда написана неверно.", false);
                           Client.msg("§f§lModules:§r §7чек: ss [§lNAME§r§7] [§lCheck§r§7] [§lon/true/+/off/false/-/toggle/tog§r§7]", false);
                        } else {
                           boolean act = args[3].equalsIgnoreCase("on")
                              || args[3].equalsIgnoreCase("true")
                              || args[3].equalsIgnoreCase("+")
                              || !args[3].equalsIgnoreCase("off")
                                 && !args[3].equalsIgnoreCase("false")
                                 && !args[3].equalsIgnoreCase("-")
                                 && (args[3].equalsIgnoreCase("toggle") || args[3].equalsIgnoreCase("tog")) != boolSet.getBool();
                           if (boolSet.getBool() == act) {
                              Client.msg(
                                 "§f§lModules:§r §7настройка [§l"
                                    + setting.getName()
                                    + "§r§7] §7у "
                                    + module.getName()
                                    + " уже "
                                    + (boolSet.getBool() ? "включена" : "выключена"),
                                 false
                              );
                           } else {
                              boolSet.setBool(act);
                              Client.msg(
                                 "§f§lModules:§r §7настройка [§l"
                                    + setting.getName()
                                    + "§r§7] §7у "
                                    + module.getName()
                                    + " теперь "
                                    + (boolSet.getBool() ? "включена" : "выключена"),
                                 false
                              );
                              ClientTune.get.playGuiScreenCheckBox(!boolSet.getBool());
                           }
                        }
                     } else if (setting instanceof ModeSettings modeSet) {
                        String mod = modeSet.currentMode;
                        boolean correct = false;
                        String newMode = null;

                        for (String mode : modeSet.modes) {
                           String numMode2 = mode.toLowerCase();
                           String numMode3 = mode.toUpperCase();
                           boolean change = args.length < 5 || !modeSet.getMode().equalsIgnoreCase(mode);
                           if ((args[3].equalsIgnoreCase(mode) || args[3].equalsIgnoreCase(numMode2) || args[3].equalsIgnoreCase(numMode3)) && change) {
                              newMode = mode;
                           }

                           if (args.length == 5
                              && change
                              && (args[4].equalsIgnoreCase(mode) || args[4].equalsIgnoreCase(numMode2) || args[4].equalsIgnoreCase(numMode3))) {
                              newMode = mode;
                           }
                        }

                        if (newMode == null) {
                           Client.msg("§f§lModules:§r §7мода с именем [§l" + args[3] + "§r§7] §7не существует", false);
                           Client.msg("§f§lModules:§r §7спиок сущесвующих модов:", false);
                           int number = 0;

                           for (String mode : modeSet.modes) {
                              Client.msg("§f§lModules:§r §7мод №" + ++number + ": " + mode, false);
                           }
                        } else if (modeSet.getMode() == newMode) {
                           Client.msg("§f§lModules:§r §7мод [§l" + setting.getName() + "§r§7]  уже является [§l" + newMode + "§r§7] §7", false);
                           ClientTune.get.playGuiScreenChangeModeSong(false);
                        } else {
                           modeSet.setMode(newMode);
                           Client.msg("§f§lModules:§r §7мод [§l" + setting.getName() + "§r§7]  теперь является [§l" + newMode + "§r§7] §7", false);
                           ClientTune.get.playGuiScreenChangeModeSong(true);
                        }
                     } else if (!(setting instanceof ColorSettings colorSet)) {
                        if (setting instanceof FloatSettings floatSet) {
                           float flot = floatSet.getFloat();
                           Float val = Float.parseFloat(args[3]);
                           if (val.isNaN()) {
                              Client.msg("§f§lModules:§r §7Комманда написана неверно.", false);
                              Client.msg("§f§lModules:§r §7вы ввели недопустимое значение для этой настройки", false);
                              Client.msg("§f§lModules:§r §7слайдер: ss [§lNAME§r§7] [§lSlider§r§7] [§lValue§r§7]", false);
                           } else {
                              String value = val + "";
                              if (val == (float)((int)val.floatValue()) && value.endsWith(".0")) {
                                 value = value.replace(".0", "");
                              }

                              if (floatSet.getFloat() == val) {
                                 Client.msg(
                                    "§f§lModules:§r §7настройка [§l" + setting.getName() + "§r§7] §7у " + module.getName() + " уже равна " + value, false
                                 );
                              } else {
                                 floatSet.setFloat(val);
                                 Client.msg(
                                    "§f§lModules:§r §7настройка [§l" + setting.getName() + "§r§7] §7у " + module.getName() + " теперь равна " + value, false
                                 );
                              }
                           }
                        } else {
                           Client.msg("§f§lModules:§r §7технические шоколадки (невозможно узнать тип настройки модуля :/)", false);
                        }
                     } else {
                        int col = colorSet.getCol();
                        int red = ColorUtils.getRedFromColor(col);
                        int green = ColorUtils.getGreenFromColor(col);
                        int blue = ColorUtils.getBlueFromColor(col);
                        int alpha = ColorUtils.getAlphaFromColor(col);
                        boolean rgba = args.length == 7
                           && this.isColorValue(args[3])
                           && this.isColorValue(args[4])
                           && this.isColorValue(args[5])
                           && this.isColorValue(args[6]);
                        boolean rgb = args.length == 6 && this.isColorValue(args[3]) && this.isColorValue(args[4]) && this.isColorValue(args[5]);
                        boolean brightAlpha = args.length == 5 && this.isColorValue(args[3]) && this.isColorValue(args[4]);
                        boolean bright = args.length == 4 && this.isColorValue(args[3]);
                        boolean isPreset = this.isColor(args[3]);
                        int preset = this.getColorValue(args[3]);
                        if (rgba) {
                           red = Integer.parseInt(args[3]);
                           green = Integer.parseInt(args[4]);
                           blue = Integer.parseInt(args[5]);
                           alpha = Integer.parseInt(args[6]);
                        } else if (rgb) {
                           red = Integer.parseInt(args[3]);
                           green = Integer.parseInt(args[4]);
                           blue = Integer.parseInt(args[5]);
                           alpha = 255;
                        } else if (brightAlpha) {
                           red = Integer.parseInt(args[3]);
                           green = Integer.parseInt(args[3]);
                           blue = Integer.parseInt(args[3]);
                           alpha = Integer.parseInt(args[4]);
                        } else if (bright) {
                           red = Integer.parseInt(args[3]);
                           green = Integer.parseInt(args[3]);
                           blue = Integer.parseInt(args[3]);
                           alpha = 255;
                        } else if (isPreset) {
                           red = ColorUtils.getRedFromColor(preset);
                           green = ColorUtils.getGreenFromColor(preset);
                           blue = ColorUtils.getBlueFromColor(preset);
                           alpha = ColorUtils.getAlphaFromColor(preset);
                        } else {
                           int c = Integer.parseInt(args[3]);
                           red = ColorUtils.getRedFromColor(c);
                           green = ColorUtils.getGreenFromColor(c);
                           blue = ColorUtils.getBlueFromColor(c);
                           alpha = ColorUtils.getAlphaFromColor(c);
                        }

                        col = ColorUtils.getColor(red, green, blue, alpha);
                        if (col > Integer.MAX_VALUE || col < Integer.MIN_VALUE) {
                           Client.msg("§f§lModules:§r §7Комманда написана неверно.", false);
                           Client.msg("§f§lModules:§r §7цвет не является допустимым к применению§7", false);
                           Client.msg("§f§lModules:§r §7цвет: ss [§lNAME§r§7] [§lColor§r§7] [§lrgba/rgb/ba/b/integer§r§7]", false);
                           return;
                        }

                        String rgbaSee = (this.getColorName(col) != null ? this.getColorName(col) : "red:" + red + ", green:" + green + ", blue:" + blue)
                           + (alpha != 255 && alpha != 0 ? ", alpha:" + alpha : "");
                        if (colorSet.getCol() == col) {
                           Client.msg("§f§lModules:§r §7цвет [§l" + setting.getName() + "§r§7]  уже равен [§l" + rgbaSee + "§r§7] §7", false);
                        } else {
                           colorSet.setCol(col);
                           Client.msg("§f§lModules:§r §7цвет [§l" + setting.getName() + "§r§7]  теперь равен [§l" + rgbaSee + "§r§7] §7", false);
                        }
                     }
                  } else {
                     module.toggle(!module.actived);
                     Client.msg("§f§lModules:§r §7модуль [§l" + args[1] + "§r§7] §7теперь " + (module.actived ? "включен" : "выключен"), false);
                  }
               } else if (module.actived) {
                  module.toggle(false);
                  Client.msg("§f§lModules:§r §7модуль [§l" + args[1] + "§r§7] §7теперь выключен", false);
               } else {
                  Client.msg("§f§lModules:§r §7модуль [§l" + args[1] + "§r§7] §7уже выключен", false);
               }
            } else if (!module.actived) {
               module.toggle(true);
               Client.msg("§f§lModules:§r §7модуль [§l" + args[1] + "§r§7] §7теперь включен", false);
            } else {
               Client.msg("§f§lModules:§r §7модуль [§l" + args[1] + "§r§7] §7уже включен", false);
            }
         }
      } catch (Exception var20) {
         Client.msg("§f§lModules:§r §7Комманда написана неверно.", false);
         Client.msg("§f§lModules:§r §7использовать: ss/setsetting/module/m", false);
         Client.msg("§f§lModules:§r §7перезагрузить всё: reload", false);
         Client.msg("§f§lModules:§r §7включить: [§lNAME§r§7] true/on/+", false);
         Client.msg("§f§lModules:§r §7выключить: [§lNAME§r§7] false/off/-", false);
         Client.msg("§f§lModules:§r §7слайдер: [§lNAME§r§7] [§lSlider§r§7] [§lValue§r§7]", false);
         Client.msg("§f§lModules:§r §7чек: [§lNAME§r§7] [§lCheck§r§7] [§ltrue/+/off/-/toggle/tog/bind [key]/unbind§r§7]", false);
         Client.msg("§f§lModules:§r §7моды: [§lNAME§r§7] [§lModes§r§7] [§lSelected/variant1 variant2§r§7]", false);
         Client.msg("§f§lModules:§r §7цвет: [§lNAME§r§7] [§lColor§r§7] [§lrgba/rgb/ba/b/int§r§7]", false);
         Client.msg("§f§lModules:§r §7копировать настройки: grab [§lNAME§r§7]", false);
         Client.msg("§f§lModules:§r §7загрузить настройки: apply [§lDATA§r§7]", false);
         var20.printStackTrace();
      }
   }

   int getColorValue(String text) {
      int c = 1111111111;
      if (text.equalsIgnoreCase("black")) {
         c = ColorUtils.getColor(0, 0, 0);
      } else if (text.equalsIgnoreCase("white")) {
         int var4 = -1;
      } else if (text.equalsIgnoreCase("red")) {
         c = ColorUtils.getColor(255, 0, 0);
      } else if (text.equalsIgnoreCase("green")) {
         c = ColorUtils.getColor(0, 255, 0);
      } else if (text.equalsIgnoreCase("blue")) {
         c = ColorUtils.getColor(0, 0, 255);
      } else if (text.equalsIgnoreCase("gray")) {
         c = ColorUtils.getColor(127, 127, 127);
      }

      return 1111111111;
   }

   String getColorName(int color) {
      if (color == ColorUtils.getColor(0, 0, 0, 0)) {
         return "none";
      } else if (color == ColorUtils.getColor(0, 0, 0)) {
         return "black";
      } else if (color == -1) {
         return "white";
      } else if (color == ColorUtils.getColor(255, 0, 0)) {
         return "red";
      } else if (color == ColorUtils.getColor(0, 255, 0)) {
         return "green";
      } else if (color == ColorUtils.getColor(0, 0, 255)) {
         return "blue";
      } else {
         return color == ColorUtils.getColor(127, 127, 127) ? "gray" : null;
      }
   }

   boolean isColor(String text) {
      return this.getColorValue(text) != 1111111111;
   }

   boolean isColorValue(String text) {
      int val = 1111111111;

      try {
         val = Integer.parseInt(text);
      } catch (Exception var4) {
      }

      return val != 1111111111 && val >= 0 && val <= 255;
   }
}
