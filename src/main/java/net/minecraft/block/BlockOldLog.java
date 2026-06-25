package net.minecraft.block;

import com.google.common.base.Predicate;
import javax.annotation.Nullable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockOldLog extends BlockLog {
   public static final PropertyEnum<BlockPlanks.EnumType> VARIANT = PropertyEnum.create(
      "variant", BlockPlanks.EnumType.class, new Predicate<BlockPlanks.EnumType>() {
         public boolean apply(@Nullable BlockPlanks.EnumType p_apply_1_) {
            return p_apply_1_.getMetadata() < 4;
         }
      }
   );

   public BlockOldLog() {
      this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT, BlockPlanks.EnumType.OAK).withProperty(LOG_AXIS, BlockLog.EnumAxis.Y));
   }

   @Override
   public MapColor getMapColor(IBlockState state, IBlockAccess p_180659_2_, BlockPos p_180659_3_) {
      BlockPlanks.EnumType blockplanks$enumtype = state.getValue(VARIANT);
      switch ((BlockLog.EnumAxis)state.getValue(LOG_AXIS)) {
         case X:
         case Z:
         case NONE:
         default:
            switch (blockplanks$enumtype) {
               case OAK:
               default:
                  return BlockPlanks.EnumType.SPRUCE.getMapColor();
               case SPRUCE:
                  return BlockPlanks.EnumType.DARK_OAK.getMapColor();
               case BIRCH:
                  return MapColor.QUARTZ;
               case JUNGLE:
                  return BlockPlanks.EnumType.SPRUCE.getMapColor();
            }
         case Y:
            return blockplanks$enumtype.getMapColor();
      }
   }

   @Override
   public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> tab) {
      tab.add(new ItemStack(this, 1, BlockPlanks.EnumType.OAK.getMetadata()));
      tab.add(new ItemStack(this, 1, BlockPlanks.EnumType.SPRUCE.getMetadata()));
      tab.add(new ItemStack(this, 1, BlockPlanks.EnumType.BIRCH.getMetadata()));
      tab.add(new ItemStack(this, 1, BlockPlanks.EnumType.JUNGLE.getMetadata()));
   }

   @Override
   public IBlockState getStateFromMeta(int meta) {
      IBlockState iblockstate = this.getDefaultState().withProperty(VARIANT, BlockPlanks.EnumType.byMetadata((meta & 3) % 4));

      return switch (meta & 12) {
         case 0 -> iblockstate.withProperty(LOG_AXIS, BlockLog.EnumAxis.Y);
         case 4 -> iblockstate.withProperty(LOG_AXIS, BlockLog.EnumAxis.X);
         case 8 -> iblockstate.withProperty(LOG_AXIS, BlockLog.EnumAxis.Z);
         default -> iblockstate.withProperty(LOG_AXIS, BlockLog.EnumAxis.NONE);
      };
   }

   @Override
   public int getMetaFromState(IBlockState state) {
      int i = 0;
      i |= state.getValue(VARIANT).getMetadata();
      switch ((BlockLog.EnumAxis)state.getValue(LOG_AXIS)) {
         case X:
            i |= 4;
            break;
         case Z:
            i |= 8;
            break;
         case NONE:
            i |= 12;
      }

      return i;
   }

   @Override
   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, VARIANT, LOG_AXIS);
   }

   @Override
   protected ItemStack getSilkTouchDrop(IBlockState state) {
      return new ItemStack(Item.getItemFromBlock(this), 1, state.getValue(VARIANT).getMetadata());
   }

   @Override
   public int damageDropped(IBlockState state) {
      return state.getValue(VARIANT).getMetadata();
   }
}
