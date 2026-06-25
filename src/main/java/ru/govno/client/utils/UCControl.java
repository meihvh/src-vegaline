package ru.govno.client.utils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class UCControl {
   private UCControl.ActivationInfo activationInfo;
   private final String splitPrefixAndFieldData = ":>>";
   private final String splitFieldAndData = "===";
   private static final String PREFIX_ADDITION_FIELDS = "ADDS";
   private static final String PREFIX_ACTIVATIONINFO_FIELDS = "ACT";
   private static final String field_canBeUpdateVersion = "field_canBeUpdateVersion";
   private static final String field_forceLimitFPS = "field_forceLimitFPS";
   private static final String field_lockedModules = "field_lockedModules";
   private boolean canBeUpdateVersion;
   private int forceLimitFPS;
   private List<String> lockedModules;

   private String getSPrefixMessage(String sMessage) {
      return sMessage.split(":>>")[0];
   }

   private String getSFieldMessage(String sMessage) {
      return sMessage.split(":>>".split("===")[0])[1];
   }

   private String getSDataMessage(String sMessage) {
      return sMessage.split(":>>".split("===")[1])[1];
   }

   private boolean getSDataBooleanMessage(String sMessage) {
      return sMessage.split(":>>".split("===")[1])[1].equalsIgnoreCase("true");
   }

   private int getSDataIntegerMessage(String sMessage) {
      try {
         return Integer.parseInt(sMessage.split(":>>")[1]);
      } catch (Exception var3) {
         var3.printStackTrace();
         return Integer.MAX_VALUE;
      }
   }

   private UCControl.ActivationInfo.DateActivation getSDataDateActivationMessage(String sMessage) {
      try {
         String dateFullString = sMessage.split(":>>".split("===")[1])[1];
         String[] dateIntegersStrings = dateFullString.split(":");
         int day = Integer.parseInt(dateIntegersStrings[0]);
         int month = Integer.parseInt(dateIntegersStrings[1]);
         int year = Integer.parseInt(dateIntegersStrings[2]);
         return new UCControl.ActivationInfo.DateActivation(day, month, year);
      } catch (Exception var7) {
         var7.fillInStackTrace();
         return null;
      }
   }

   private UCControl.ActivationInfo.ActivationStatus getSDataActivationStatusMessage(String sMessage) {
      String var2 = sMessage.split(":>>".split("===")[1])[1];
      switch (var2) {
         case "not_activated":
            return UCControl.ActivationInfo.ActivationStatus.NOT_ACTIVATED;
         case "activated":
            return UCControl.ActivationInfo.ActivationStatus.ACTIVATED;
         case "banned":
            return UCControl.ActivationInfo.ActivationStatus.BANNED;
         case "permanent_banned":
            return UCControl.ActivationInfo.ActivationStatus.PERMANENT_BANNED;
         case "banned_and_break_system":
            return UCControl.ActivationInfo.ActivationStatus.BANNED_AND_BREAK_SYSTEM;
         default:
            return null;
      }
   }

   private UCControl.ActivationInfo.ActivationMode getSDataActivationModeMessage(String sMessage) {
      String var2 = sMessage.split(":>>".split("===")[1])[1];
      switch (var2) {
         case "free":
            return UCControl.ActivationInfo.ActivationMode.FREE;
         case "media_tiktok":
            return UCControl.ActivationInfo.ActivationMode.MEDIA_TIKTOK;
         case "media_youtube":
            return UCControl.ActivationInfo.ActivationMode.MEDIA_YOUTUBE;
         case "media_override":
            return UCControl.ActivationInfo.ActivationMode.MEDIA_OVERRIDE;
         case "purchased":
            return UCControl.ActivationInfo.ActivationMode.PURCHASED;
         case "friend":
            return UCControl.ActivationInfo.ActivationMode.FRIEND;
         case "administrator":
            return UCControl.ActivationInfo.ActivationMode.ADMINISTRATOR;
         default:
            return null;
      }
   }

   private List<String> getSDataStringListMessage(String sMessage) {
      try {
         String[] massiveStrings = sMessage.split(":>>".split("===")[1])[1].split(",");
         return Arrays.asList(massiveStrings);
      } catch (Exception var3) {
         var3.fillInStackTrace();
         return null;
      }
   }

   private boolean findAndSetActivationInfo(String sMessage) {
      if (this.getSPrefixMessage(sMessage).equalsIgnoreCase("ACT")) {
         if (this.activationInfo == null) {
            this.activationInfo = new UCControl.ActivationInfo();
         }

         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("activationStatus")) {
            this.activationInfo.activationStatus = this.getSDataActivationStatusMessage(sMessage);
            return true;
         }

         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("dateEndingSubscription")) {
            this.activationInfo.dateEndingSubscription = this.getSDataDateActivationMessage(sMessage);
            return true;
         }

         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("activationMode")) {
            this.activationInfo.activationMode = this.getSDataActivationModeMessage(sMessage);
            return true;
         }
      }

      return false;
   }

   private boolean findAndSetAdditionFields(String sMessage) {
      if (this.getSPrefixMessage(sMessage).equalsIgnoreCase("ADDS")) {
         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("field_canBeUpdateVersion")) {
            this.canBeUpdateVersion = this.getSDataBooleanMessage(sMessage);
            return true;
         }

         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("field_forceLimitFPS")) {
            this.forceLimitFPS = this.getSDataIntegerMessage(sMessage);
            return true;
         }

         if (this.getSFieldMessage(sMessage).equalsIgnoreCase("field_lockedModules")) {
            this.lockedModules = this.getSDataStringListMessage(sMessage);
            return true;
         }
      }

      return false;
   }

   private class ActivationInfo {
      static final String field_activationStatus = "activationStatus";
      static final String field_dateEndingSubscription = "dateEndingSubscription";
      static final String field_activationMode = "activationMode";
      UCControl.ActivationInfo.ActivationStatus activationStatus;
      UCControl.ActivationInfo.DateActivation dateEndingSubscription;
      UCControl.ActivationInfo.ActivationMode activationMode;

      public ActivationInfo(
         UCControl.ActivationInfo.ActivationStatus activationStatus,
         UCControl.ActivationInfo.DateActivation dateEndingSubscription,
         UCControl.ActivationInfo.ActivationMode activationMode
      ) {
         this.activationStatus = activationStatus;
         this.dateEndingSubscription = dateEndingSubscription;
         this.activationMode = activationMode;
      }

      public ActivationInfo() {
      }

      static enum ActivationMode {
         FREE("free"),
         MEDIA_TIKTOK("media_tiktok"),
         MEDIA_YOUTUBE("media_youtube"),
         MEDIA_OVERRIDE("media_override"),
         PURCHASED("purchased"),
         FRIEND("friend"),
         ADMINISTRATOR("administrator");

         String name;

         private ActivationMode(String name) {
            this.name = name;
         }
      }

      static enum ActivationStatus {
         NOT_ACTIVATED("not_activated"),
         ACTIVATED("activated"),
         BANNED("banned"),
         PERMANENT_BANNED("permanent_banned"),
         BANNED_AND_BREAK_SYSTEM("banned_and_break_system");

         String name;

         private ActivationStatus(String name) {
            this.name = name;
         }
      }

      static class DateActivation {
         int day;
         int month;
         int year;

         DateActivation(int day, int month, int year) {
            this.day = day;
            this.month = month;
            this.year = year;
         }

         public boolean isOutDatedToPC() {
            Date datePC = Calendar.getInstance().getTime();
            float datePCYearDelta = (float)datePC.getYear() + ((float)datePC.getMonth() + (float)datePC.getDay() / 30.0F) / 12.0F;
            float dateYearDelta = (float)this.year + (float)(this.month + this.day) / 30.0F / 12.0F;
            return datePCYearDelta > dateYearDelta;
         }
      }
   }
}
