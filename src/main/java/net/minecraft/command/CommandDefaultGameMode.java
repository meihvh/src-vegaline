package net.minecraft.command;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;

public class CommandDefaultGameMode extends CommandGameMode {
   @Override
   public String getCommandName() {
      return "defaultgamemode";
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.defaultgamemode.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length <= 0) {
         throw new WrongUsageException("commands.defaultgamemode.usage");
      } else {
         GameType gametype = this.getGameModeFromCommand(sender, args[0]);
         this.setDefaultGameType(gametype, server);
         notifyCommandListener(sender, this, "commands.defaultgamemode.success", new Object[]{new TextComponentTranslation("gameMode." + gametype.getName())});
      }
   }

   protected void setDefaultGameType(GameType gameType, MinecraftServer server) {
      server.setGameType(gameType);
      if (server.getForceGamemode()) {
         for (EntityPlayerMP entityplayermp : server.getPlayerList().getPlayerList()) {
            entityplayermp.setGameType(gameType);
         }
      }
   }
}
