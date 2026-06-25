package net.minecraft.command;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class RecipeCommand extends CommandBase {
   @Override
   public String getCommandName() {
      return "recipe";
   }

   @Override
   public int getRequiredPermissionLevel() {
      return 2;
   }

   @Override
   public String getCommandUsage(ICommandSender sender) {
      return "commands.recipe.usage";
   }

   @Override
   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length < 2) {
         throw new WrongUsageException("commands.recipe.usage");
      } else {
         boolean flag = "give".equalsIgnoreCase(args[0]);
         boolean flag1 = "take".equalsIgnoreCase(args[0]);
         if (!flag && !flag1) {
            throw new WrongUsageException("commands.recipe.usage");
         } else {
            for (EntityPlayerMP entityplayermp : func_193513_a(server, sender, args[1])) {
               if ("*".equals(args[2])) {
                  if (flag) {
                     entityplayermp.func_192021_a(this.func_192556_d());
                     notifyCommandListener(sender, this, "commands.recipe.give.success.all", new Object[]{entityplayermp.getName()});
                  } else {
                     entityplayermp.func_192022_b(this.func_192556_d());
                     notifyCommandListener(sender, this, "commands.recipe.take.success.all", new Object[]{entityplayermp.getName()});
                  }
               } else {
                  IRecipe irecipe = CraftingManager.func_193373_a(new ResourceLocation(args[2]));
                  if (irecipe == null) {
                     throw new CommandException("commands.recipe.unknownrecipe", args[2]);
                  }

                  if (irecipe.func_192399_d()) {
                     throw new CommandException("commands.recipe.unsupported", args[2]);
                  }

                  List<IRecipe> list = Lists.newArrayList(irecipe);
                  if (flag == entityplayermp.func_192037_E().func_193830_f(irecipe)) {
                     String s = flag ? "commands.recipe.alreadyHave" : "commands.recipe.dontHave";
                     throw new CommandException(s, entityplayermp.getName(), irecipe.getRecipeOutput().getDisplayName());
                  }

                  if (flag) {
                     entityplayermp.func_192021_a(list);
                     notifyCommandListener(
                        sender, this, "commands.recipe.give.success.one", new Object[]{entityplayermp.getName(), irecipe.getRecipeOutput().getDisplayName()}
                     );
                  } else {
                     entityplayermp.func_192022_b(list);
                     notifyCommandListener(
                        sender, this, "commands.recipe.take.success.one", new Object[]{irecipe.getRecipeOutput().getDisplayName(), entityplayermp.getName()}
                     );
                  }
               }
            }
         }
      }
   }

   private List<IRecipe> func_192556_d() {
      return Lists.newArrayList(CraftingManager.field_193380_a);
   }

   @Override
   public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
      if (args.length == 1) {
         return getListOfStringsMatchingLastWord(args, new String[]{"give", "take"});
      } else if (args.length == 2) {
         return getListOfStringsMatchingLastWord(args, server.getAllUsernames());
      } else {
         return args.length == 3 ? getListOfStringsMatchingLastWord(args, CraftingManager.field_193380_a.getKeys()) : Collections.emptyList();
      }
   }
}
