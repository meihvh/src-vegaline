package net.minecraft.command.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameType;

public class CommandPublishLocalServer extends CommandBase {
   @Override
   public String getCommandName() {
      return "publish";
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.publish.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      String s = server.shareToLAN(GameType.SURVIVAL, false);
      if (s != null) {
         notifyCommandListener(sender, this, "commands.publish.started", new Object[]{s});
      } else {
         notifyCommandListener(sender, this, "commands.publish.failed", new Object[0]);
      }
   }
}
