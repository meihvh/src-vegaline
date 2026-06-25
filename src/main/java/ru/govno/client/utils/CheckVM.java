package ru.govno.client.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class CheckVM {
   public static String result;

   public static boolean hasAny() {
      result = "";
      if (biosCheck()) {
         result = result + "C-i0";
      }

      if (cpuCheck()) {
         result = result + "C-i1";
      }

      if (regCheck()) {
         result = result + "C-i2";
      }

      if (dataNameCheck()) {
         result = result + "C-i3";
      }

      if (anynonwincmd()) {
         result = result + "C-i4";
      }

      return !result.isEmpty();
   }

   private static boolean biosCheck() {
      try {
         String command = "wmic bios get manufacturer";
         Process process = Runtime.getRuntime().exec(command);
         BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

         String line;
         while ((line = reader.readLine()) != null) {
            if (line.contains("VMware") || line.contains("VirtualBox") || line.contains("QEMU")) {
               return true;
            }
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private static boolean cpuCheck() {
      try {
         String command = "wmic cpu get caption";
         Process process = Runtime.getRuntime().exec(command);
         BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

         String line;
         while ((line = reader.readLine()) != null) {
            if (line.contains("VMware") || line.contains("VirtualBox") || line.contains("Xen")) {
               return true;
            }
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private static boolean regCheck() {
      try {
         String command = "reg query HKLM\\HARDWARE\\DESCRIPTION\\System";
         Process process = Runtime.getRuntime().exec(command);
         BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

         String line;
         while ((line = reader.readLine()) != null) {
            if (line.contains("VMware") || line.contains("VirtualBox") || line.contains("Xen")) {
               return true;
            }
         }
      } catch (Exception var4) {
      }

      return false;
   }

   private static boolean macidCheck() {
      try {
         Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

         while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            byte[] mac = networkInterface.getHardwareAddress();
            if (mac != null) {
               String macAddress = String.format("%02X:%02X:%02X:%02X:%02X:%02X", mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
               if (macAddress.startsWith("00:05:69")
                  || macAddress.startsWith("00:1C:14")
                  || macAddress.startsWith("00:0C:29")
                  || macAddress.startsWith("00:50:56")) {
                  return true;
               }
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      return false;
   }

   private static boolean dataNameCheck() {
      String n = System.getProperty("os.name").toLowerCase();
      String a = System.getProperty("os.arch").toLowerCase();
      String v = System.getProperty("java.vm.vendor").toLowerCase();
      return n.contains("vm") || a.contains("vm") || v.contains("vmware") || v.contains("virtualbox") || v.contains("graalvm");
   }

   private static boolean anynonwincmd() {
      try {
         return new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor() != 0;
      } catch (Exception var1) {
         return false;
      }
   }
}
