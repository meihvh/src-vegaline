package net.minecraft.command;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class CommandShowSeed extends CommandBase {
   @Override
   public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
      return server.isSinglePlayer() || super.checkPermission(server, sender);
   }

   @Override
   public String getCommandName() {
      return "seed";
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 2;
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.seed.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      World world = (World)(sender instanceof EntityPlayer ? ((EntityPlayer)sender).world : server.worldServerForDimension(0));
      sender.addChatMessage(new TextComponentTranslation("commands.seed.success", world.getSeed()));
   }
}
