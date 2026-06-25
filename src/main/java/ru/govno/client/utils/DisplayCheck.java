package ru.govno.client.utils;

import org.lwjgl.opengl.Display;

public class DisplayCheck {
   private static boolean active;
   private static boolean visible;
   private static boolean current;
   private static boolean fullscreen;
   private static Thread thread;
   private static int recreateReach;
   private static final TimerHelper timerDelay = new TimerHelper();

   public static void updateChecks() {
      try {
         if (timerDelay.hasReached(100.0F)) {
            boolean debug = false;
            if (++recreateReach > 50 || thread == null || !thread.isAlive() || thread.isInterrupted()) {
               if (debug) {
                  System.out.println("DisplayCheck хочет создать себе новый поток");
               }

               String threadName = "addition threads DisplayCheck(recreateReach="
                  + recreateReach
                  + ") reason="
                  + (
                     thread == null
                        ? "thread is null"
                        : (!thread.isAlive() ? "thread is dead or recursion" : (thread.isInterrupted() ? "thread interrupted" : "unknown"))
                  );
               if (debug) {
                  if (thread == null) {
                     System.out.println("DisplayCheck проверил поток который был создан - отсутствует");
                  } else if (!thread.isAlive()) {
                     System.out.println("DisplayCheck проверил поток который был создан - завершил работу и сдох");
                  } else if (thread.isInterrupted()) {
                     System.out.println("DisplayCheck проверил поток который был создан - не смог завершить работу и сдох");
                  }
               }

               thread = new Thread(() -> {
                  try {
                     active = Display.isActive();
                     visible = Display.isVisible();
                     if (Display.isCurrent()) {
                        current = true;
                     }

                     fullscreen = Display.isFullscreen();
                     if (debug) {
                        System.out.println("DisplayCheck смог обновить данные в потоке: {active, visible, current, fullscreen}");
                     }
                  } catch (Exception var2x) {
                     var2x.printStackTrace();
                  }
               });
               thread.setName(threadName);
               if (debug) {
                  System.out.println("DisplayCheck создал себе новый поток (" + threadName + ")");
               }

               thread.setPriority(1);
               if (debug) {
                  System.out.println("DisplayCheck установил Thread.MIN_PRIORITY своему потоку");
               }

               thread.setDaemon(false);
               if (debug) {
                  System.out.println("DisplayCheck установил daemon(false) своему потоку");
               }

               recreateReach = 0;
            }

            timerDelay.reset();
            if (thread != null) {
               if (debug) {
                  System.out.println("DisplayCheck успешно запустил поток и выполнил задачу");
               }

               thread.start();
               if (debug) {
                  System.out.println("DisplayCheck перевёл свой поток в режим ожидания перед повтором вызова текущего потока");
               }
            }
         }
      } catch (Exception var2) {
         System.out.println("DisplayCheck не смог запустить свой поток");
         var2.printStackTrace();
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static boolean isVisible() {
      return visible;
   }

   public static boolean isCurrent() {
      return current;
   }

   public static boolean isFullscreen() {
      return fullscreen;
   }
}
