package ru.govno.client.utils.Command.impl;

import ru.govno.client.Client;
import ru.govno.client.utils.UProfiler;
import ru.govno.client.utils.Command.Command;

public class ProfilerToggler extends Command {
   public ProfilerToggler() {
      super("ProfilerToggler", new String[]{"profiler", "prof"});
   }

   public static boolean isNumeric(String str) {
      try {
         Long.parseLong(str);
         return true;
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   @Override
   public void onCommand(String[] args) {
      try {
         if (!args[1].equalsIgnoreCase("toggle") && !args[1].equalsIgnoreCase("t")) {
            if (!args[1].equalsIgnoreCase("proftoggle") && !args[1].equalsIgnoreCase("pt")) {
               if (!args[1].equalsIgnoreCase("montoggle") && !args[1].equalsIgnoreCase("mt")) {
                  if (!args[1].equalsIgnoreCase("infotoggle") && !args[1].equalsIgnoreCase("it")) {
                     if (args[1].equalsIgnoreCase("delay") && args.length == 3) {
                        if (isNumeric(args[2])) {
                           long delay = Long.parseLong(args[2]);
                           if (delay >= 0L && delay < 60000L) {
                              Client.uProfilers.setDelays(delay);
                              Client.msg("§7§lProfiler:§r §7Скорость провайлера теперь " + delay, false);
                           } else {
                              Client.msg("§7§lProfiler:§r §7Комманда написана неверно.", false);
                              Client.msg("§7§lProfiler:§r §7min delay 0, max delay 60000", false);
                           }
                        } else {
                           Client.msg("§7§lProfiler:§r §7Комманда написана неверно.", false);
                           Client.msg("§7§lProfiler:§r §7Скорость - delay (delayms)", false);
                        }
                     } else if ((args[1].equalsIgnoreCase("search") || args[1].equalsIgnoreCase("s")) && args.length == 3) {
                        if (args[2].length() > 0) {
                           Client.uProfilers.setSearch(args[2]);
                           Client.msg("§7§lProfiler:§r §7Искомое совпадение: " + args[2], false);
                        } else {
                           Client.uProfilers.unsetSearch();
                           Client.msg("§7§lProfiler:§r §7Искомое совпадение пустое.", false);
                           Client.msg("§7§lProfiler:§r §7min delay 0, max delay 60000", false);
                        }
                     } else if ((args[1].equalsIgnoreCase("stopsearch") || args[1].equalsIgnoreCase("ss")) && args.length == 2) {
                        if (Client.uProfilers.anySearching()) {
                           Client.uProfilers.unsetSearch();
                           Client.msg("§7§lProfiler:§r §7Поиск сброшен.", false);
                        } else {
                           Client.msg("§7§lProfiler:§r §7Поиск уже был сброшен.", false);
                        }
                     }
                  } else {
                     if (Client.uProfilers.isEnabledProperties()) {
                        Client.uProfilers.stop(false, false, true);
                     } else {
                        Client.uProfilers.start(false, false, true);
                     }

                     if (Client.uProfilers.isEnabledProperties()) {
                        Client.msg("§7§lProfiler:§r §aМониторинг системы загружен.", false);
                     } else {
                        Client.msg("§7§lProfiler:§r §cМониторинг системы остановлен.", false);
                     }
                  }
               } else {
                  if (Client.uProfilers.isEnabledMonitoring()) {
                     Client.uProfilers.stop(false, true, false);
                  } else {
                     Client.uProfilers.start(false, true, false);
                  }

                  if (Client.uProfilers.isEnabledMonitoring()) {
                     Client.msg("§7§lProfiler:§r §aСчётчик кадров применён.", false);
                  } else {
                     Client.msg("§7§lProfiler:§r §cСчётчик кадров отключен", false);
                  }
               }
            } else {
               if (Client.uProfilers.isEnabledProfilers()) {
                  Client.uProfilers.stop(true, false, false);
               } else {
                  Client.uProfilers.start(true, false, false);
               }

               if (Client.uProfilers.isEnabledProfilers()) {
                  for (UProfiler uProfiler : Client.uProfilers.getProfilers()) {
                     Client.msg("§7§lProfiler:§r §aПрофайлер открыт: " + uProfiler.getNameProfiler() + "§8 uProfiler", false);
                  }

                  Client.msg(
                     "§7§lProfiler:§r §aПрофайлеры применены: "
                        + Client.uProfilers.getProfilers().size()
                        + "шт - "
                        + Client.uProfilers.getDelay()
                        + " ms updating",
                     false
                  );
                  Client.msg("§7§lProfiler:§r §7Ожидание поиска.", false);
               } else {
                  Client.msg("§7§lProfiler:§r §cПрофайлеры: " + Client.uProfilers.getProfilers().size() + "шт выгружены.", false);
                  Client.uProfilers.unsetSearch();
                  Client.msg("§7§lProfiler:§r §7Поиск сброшен.", false);
               }
            }
         } else {
            if (Client.uProfilers.isEnabled()) {
               Client.uProfilers.stop(true, true, true);
            } else {
               Client.uProfilers.start(true, true, true);
            }

            if (Client.uProfilers.isEnabled()) {
               for (UProfiler uProfiler : Client.uProfilers.getProfilers()) {
                  Client.msg("§7§lProfiler:§r §aПрофайлер открыт: " + uProfiler.getNameProfiler() + "§8 uProfiler", false);
               }

               Client.msg(
                  "§7§lProfiler:§r §aПрофайлеры применены: "
                     + Client.uProfilers.getProfilers().size()
                     + "шт - "
                     + Client.uProfilers.getDelay()
                     + " ms updating",
                  false
               );
               Client.msg("§7§lProfiler:§r §aСчётчик кадров применён", false);
               Client.msg("§7§lProfiler:§r §aМониторинг системы загружен", false);
               Client.msg("§7§lProfiler:§r §7Ожидание поиска.", false);
            } else {
               Client.msg("§7§lProfiler:§r §cПрофайлеры: " + Client.uProfilers.getProfilers().size() + "шт выгружены.", false);
               Client.msg("§7§lProfiler:§r §cСчётчик кадров отключен", false);
               Client.msg("§7§lProfiler:§r §cМониторинг системы остановлен.", false);
               Client.uProfilers.unsetSearch();
               Client.msg("§7§lProfiler:§r §7Поиск сброшен.", false);
            }
         }
      } catch (Exception var4) {
         Client.msg("§7§lProfiler:§r §7Комманда написана неверно.", false);
         Client.msg("§7§lProfiler:§r §7Переключить все функции - toggle/t.", false);
         Client.msg("§7§lProfiler:§r §7Переключить профайлер - pt/proftoggle.", false);
         Client.msg("§7§lProfiler:§r §7Переключить мониторинг - mt/montoggle.", false);
         Client.msg("§7§lProfiler:§r §7Переключить инфо - it/infotoggle.", false);
         Client.msg("§7§lProfiler:§r §7Скорость - delay (delayms)", false);
         Client.msg("§7§lProfiler:§r §7Поиск по совпадеиям - search/s (contain)", false);
         Client.msg("§7§lProfiler:§r §7Обнулить поиск - stopsearch/ss", false);
         var4.fillInStackTrace();
      }
   }
}
