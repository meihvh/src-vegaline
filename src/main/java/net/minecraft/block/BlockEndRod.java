package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockEndRod extends BlockDirectional {
   protected static final AxisAlignedBB END_ROD_VERTICAL_AABB = new AxisAlignedBB(0.375, 0.0, 0.375, 0.625, 1.0, 0.625);
   protected static final AxisAlignedBB END_ROD_NS_AABB = new AxisAlignedBB(0.375, 0.375, 0.0, 0.625, 0.625, 1.0);
   protected static final AxisAlignedBB END_ROD_EW_AABB = new AxisAlignedBB(0.0, 0.375, 0.375, 1.0, 0.625, 0.625);

   protected BlockEndRod() {
      super(Material.CIRCUITS);
      this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
      this.setCreativeTab(CreativeTabs.DECORATIONS);
   }

   @Override
   public IBlockState withRotation(IBlockState state, Rotation rot) {
      return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
   }

   @Override
   public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
      return state.withProperty(FACING, mirrorIn.mirror(state.getValue(FACING)));
   }

   @Override
   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      switch (state.getValue(FACING).getAxis()) {
         case X:
         default:
            return END_ROD_EW_AABB;
         case Z:
            return END_ROD_NS_AABB;
         case Y:
            return END_ROD_VERTICAL_AABB;
      }
   }

   @Override
   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   @Override
   public boolean isFullCube(IBlockState state) {
      return false;
   }

   @Override
   public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
      return true;
   }

   @Override
   public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState iblockstate = worldIn.getBlockState(pos.offset(facing.getOpposite()));
      if (iblockstate.getBlock() == Blocks.END_ROD) {
         EnumFacing enumfacing = iblockstate.getValue(FACING);
         if (enumfacing == facing) {
            return this.getDefaultState().withProperty(FACING, facing.getOpposite());
         }
      }

      return this.getDefaultState().withProperty(FACING, facing);
   }

   @Override
   public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
      EnumFacing enumfacing = stateIn.getValue(FACING);
      double d0 = (double)pos.getX() + 0.55 - (double)(rand.nextFloat() * 0.1F);
      double d1 = (double)pos.getY() + 0.55 - (double)(rand.nextFloat() * 0.1F);
      double d2 = (double)pos.getZ() + 0.55 - (double)(rand.nextFloat() * 0.1F);
      double d3 = (double)(0.4F - (rand.nextFloat() + rand.nextFloat()) * 0.4F);
      if (rand.nextInt(5) == 0) {
         worldIn.spawnParticle(
            EnumParticleTypes.END_ROD,
            d0 + (double)enumfacing.getFrontOffsetX() * d3,
            d1 + (double)enumfacing.getFrontOffsetY() * d3,
            d2 + (double)enumfacing.getFrontOffsetZ() * d3,
            rand.nextGaussian() * 0.005,
            rand.nextGaussian() * 0.005,
            rand.nextGaussian() * 0.005
         );
      }
   }

   @Override
   public BlockRenderLayer getBlockLayer() {
      return BlockRenderLayer.CUTOUT;
   }

   @Override
   public IBlockState getStateFromMeta(int meta) {
      IBlockState iblockstate = this.getDefaultState();
      return iblockstate.withProperty(FACING, EnumFacing.getFront(meta));
   }

   @Override
   public int getMetaFromState(IBlockState state) {
      return state.getValue(FACING).getIndex();
   }

   @Override
   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, FACING);
   }

   @Override
   public EnumPushReaction getMobilityFlag(IBlockState state) {
      return EnumPushReaction.NORMAL;
   }

   @Override
   public BlockFaceShape func_193383_a(IBlockAccess p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_) {
      return BlockFaceShape.UNDEFINED;
   }
}
