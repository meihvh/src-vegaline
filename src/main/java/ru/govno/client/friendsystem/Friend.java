package ru.govno.client.friendsystem;

public class Friend {
   private String name;
   private boolean linked;

   public Friend(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String paramString) {
      this.name = paramString;
   }

   public void setLinked(boolean link) {
      this.linked = link;
   }

   public boolean isLinked() {
      return this.linked;
   }
}
