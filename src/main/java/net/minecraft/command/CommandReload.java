package net.minecraft.command;

import net.minecraft.server.MinecraftServer;

public class CommandReload extends CommandBase {
   @Override
   public String getCommandName() {
      return "reload";
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 3;
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.reload.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length > 0) {
         throw new WrongUsageException("commands.reload.usage");
      } else {
         server.func_193031_aM();
         notifyCommandListener(sender, this, "commands.reload.success", new Object[0]);
      }
   }
}
