package ru.govno.client.friendsystem;

import java.util.ArrayList;
import java.util.List;

public class FriendManager {
   private final List<Friend> friends = new ArrayList<>();

   public void addFriend(String name) {
      this.friends.add(new Friend(name));
   }

   public boolean isFriend(String name) {
      return this.friends.stream().anyMatch(paramFriend -> paramFriend.getName().equalsIgnoreCase(name));
   }

   public String isFriendDisplayReturn(String name) {
      Friend friend = this.friends.stream().filter(paramFriend -> name.contains(paramFriend.getName())).findAny().orElse(null);
      return friend == null ? null : friend.getName();
   }

   public void removeFriend(String name) {
      for (Friend friend : this.getFriends()) {
         if (friend.getName().equalsIgnoreCase(name)) {
            this.friends.remove(friend);
            break;
         }
      }
   }

   public void clearFriends() {
      this.friends.clear();
   }

   public List<Friend> getFriends() {
      return this.friends;
   }

   public Friend getFriend(String name) {
      return this.friends.stream().filter(paramFriend -> paramFriend.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }
}
