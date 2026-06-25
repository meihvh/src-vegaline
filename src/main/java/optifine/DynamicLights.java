package optifine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DynamicLights {
   public static final Map<Integer, DynamicLight> mapDynamicLights = new HashMap<>();
   private static long timeUpdateMs = 0L;
   private static final double MAX_DIST = 7.5;
   private static final double MAX_DIST_SQ = 56.25;
   private static final int LIGHT_LEVEL_MAX = 15;
   private static final int LIGHT_LEVEL_FIRE = 15;
   private static final int LIGHT_LEVEL_BLAZE = 10;
   private static final int LIGHT_LEVEL_MAGMA_CUBE = 8;
   private static final int LIGHT_LEVEL_MAGMA_CUBE_CORE = 13;
   private static final int LIGHT_LEVEL_GLOWSTONE_DUST = 8;
   private static final int LIGHT_LEVEL_PRISMARINE_CRYSTALS = 8;
   private static final DataParameter<ItemStack> PARAMETER_ITEM_STACK = new DataParameter<>(6, DataSerializers.OPTIONAL_ITEM_STACK);
   private static final List<Entity> tempEntities = new ArrayList<>();
   private static final Set<DynamicLight> customLights = new HashSet<>();

   public static void entityAdded(Entity p_entityAdded_0_, RenderGlobal p_entityAdded_1_) {
   }

   public static void entityRemoved(Entity p_entityRemoved_0_, RenderGlobal p_entityRemoved_1_) {
      synchronized (mapDynamicLights) {
         DynamicLight dynamiclight = mapDynamicLights.remove(net.minecraft.util.IntegerCache.getInteger(p_entityRemoved_0_.getEntityId()));
         if (dynamiclight != null) {
            dynamiclight.updateLitChunks(p_entityRemoved_1_);
         }
      }
   }

   public static void update(RenderGlobal p_update_0_) {
      long i = System.currentTimeMillis();
      if (i >= timeUpdateMs + 50L) {
         timeUpdateMs = i;
         synchronized (mapDynamicLights) {
            updateMapDynamicLights(p_update_0_);
            if (mapDynamicLights.size() > 0) {
               for (DynamicLight dynamiclight : mapDynamicLights.values()) {
                  dynamiclight.update(p_update_0_);
               }
            }
         }
      }
   }

   private static void updateMapDynamicLights(RenderGlobal p_updateMapDynamicLights_0_) {
      World world = p_updateMapDynamicLights_0_.getWorld();
      if (world != null) {
         for (Entity entity : world.getLoadedEntityList()) {
            int i = getLightLevel(entity);
            if (i > 0) {
               Integer integer = net.minecraft.util.IntegerCache.getInteger(entity.getEntityId());
               DynamicLight dynamiclight = mapDynamicLights.get(integer);
               if (dynamiclight == null) {
                  dynamiclight = new DynamicLight(entity);
                  mapDynamicLights.put(integer, dynamiclight);
               }
            } else {
               Integer integer1 = net.minecraft.util.IntegerCache.getInteger(entity.getEntityId());
               DynamicLight dynamiclight1 = mapDynamicLights.remove(integer1);
               if (dynamiclight1 != null) {
                  dynamiclight1.updateLitChunks(p_updateMapDynamicLights_0_);
               }
            }
         }

         synchronized (customLights) {
            Set<DynamicLight> lights = controlCustomLightUpdate();
            lights.forEach(dynamiclight -> dynamiclight.updateCustom(p_updateMapDynamicLights_0_));
            customLights.forEach(light -> light.updateLitChunks(p_updateMapDynamicLights_0_));
         }
      }
   }

   public static void addSingleCustomLightOneTick(double x, double y, double z, float lightPC01, float minContrastToMax) {
      addSingleCustomLightOneTick(new Vec3d(x, y, z), lightPC01, minContrastToMax);
   }

   public static void addSingleCustomLightOneTick(Vec3d pos, float lightPC01, float minContrastToMax) {
      synchronized (customLights) {
          Vec3d finalPos = pos;
          DynamicLight tryFindInList = customLights.stream()
            .filter(
               light -> light.isCustomLight()
                     && light.position.xCoord == finalPos.xCoord
                     && light.position.zCoord == finalPos.zCoord
                     && light.light015 == Math.min(lightPC01, 1.0F) * 15.0F
                     && light.strengthPC01 == minContrastToMax
            )
            .findFirst()
            .orElse(null);
         if (tryFindInList != null) {
            tryFindInList.timeAlive.reset();
         } else {
            if (Minecraft.getMinecraft().world != null) {
               BlockPos bp = new BlockPos(pos);
               IBlockState state = Minecraft.getMinecraft().world.getBlockState(bp);
               if (state != null && !state.getMaterial().blocksLight()) {
                  for (int i = 0; i < 2; i++) {
                     state = Minecraft.getMinecraft().world.getBlockState(bp.down(i));
                     if (state != null && state.getMaterial().blocksMovement()) {
                        pos = new Vec3d(pos.xCoord, pos.yCoord, pos.zCoord);
                        pos.yCoord -= (double)i;
                        break;
                     }
                  }
               }
            }

            customLights.add(DynamicLight.createCustomDynamicLightForTick(pos, lightPC01, minContrastToMax));
         }
      }
   }

   private static Set<DynamicLight> controlCustomLightUpdate() {
      synchronized (customLights) {
         customLights.removeIf(DynamicLight::toBeRemovedIfCustom);
      }

      return customLights;
   }

   public static int getCombinedLight(BlockPos p_getCombinedLight_0_, int p_getCombinedLight_1_) {
      double d0 = getLightLevel(p_getCombinedLight_0_);
      return getCombinedLight(d0, p_getCombinedLight_1_);
   }

   public static int getCombinedLight(Entity p_getCombinedLight_0_, int p_getCombinedLight_1_) {
      double d0 = (double)getLightLevel(p_getCombinedLight_0_);
      return getCombinedLight(d0, p_getCombinedLight_1_);
   }

   public static int getCombinedLight(double p_getCombinedLight_0_, int p_getCombinedLight_2_) {
      if (p_getCombinedLight_0_ > 0.0) {
         int i = (int)(p_getCombinedLight_0_ * 16.0);
         int j = p_getCombinedLight_2_ & 0xFF;
         if (i > j) {
            p_getCombinedLight_2_ &= -256;
            p_getCombinedLight_2_ |= i;
         }
      }

      return p_getCombinedLight_2_;
   }

   public static double getLightLevel(BlockPos p_getLightLevel_0_) {
      double d0 = 0.0;
      synchronized (mapDynamicLights) {
         for (DynamicLight dynamiclight : mapDynamicLights.values()) {
            int i = dynamiclight.getLastLightLevel();
            if (i > 0) {
               double d1 = dynamiclight.getLastPosX();
               double d2 = dynamiclight.getLastPosY();
               double d3 = dynamiclight.getLastPosZ();
               double d4 = (double)p_getLightLevel_0_.getX() - d1;
               double d5 = (double)p_getLightLevel_0_.getY() - d2;
               double d6 = (double)p_getLightLevel_0_.getZ() - d3;
               double d7 = d4 * d4 + d5 * d5 + d6 * d6;
               if (dynamiclight.isUnderwater() && !Config.isClearWater()) {
                  i = Config.limit(i - 2, 0, 15);
                  d7 *= 2.0;
               }

               if (d7 <= 56.25) {
                  double d8 = Math.sqrt(d7);
                  double d9 = 1.0 - d8 / 7.5;
                  double d10 = d9 * (double)i;
                  if (d10 > d0) {
                     d0 = d10;
                  }
               }
            }
         }
      }

      synchronized (customLights) {
         for (DynamicLight customLight : customLights.stream().toList()) {
            float lightLevel015 = customLight.light015;
            if (lightLevel015 > 0.0F) {
               float strengthPC01 = customLight.strengthPC01;
               double posX = customLight.position.xCoord;
               double posY = customLight.position.yCoord;
               double posZ = customLight.position.zCoord;
               double diffX = (double)p_getLightLevel_0_.getX() - posX;
               double diffY = (double)p_getLightLevel_0_.getY() - posY;
               double diffZ = (double)p_getLightLevel_0_.getZ() - posZ;
               double dstSQRT = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
               float maxDistance16MAX = ru.govno.client.utils.Math.MathUtils.lerp(1.5F, 10.0F, strengthPC01);
               if (dstSQRT <= (double)maxDistance16MAX) {
                  float dstPC01 = ru.govno.client.utils.Math.MathUtils.lerp(
                     (float)Math.min((dstSQRT - 1.5) / (double)maxDistance16MAX, 1.0), 0.0F, strengthPC01
                  );
                  double brightPC = Math.min(
                     1.0 - ru.govno.client.utils.Math.MathUtils.easeOutCubic((double)dstPC01) * (double)(1.0F - (0.2F + strengthPC01 * 0.8F)), 1.0
                  );
                  double limitedBrightToMax = brightPC * (double)lightLevel015;
                  if (limitedBrightToMax > d0) {
                     d0 = limitedBrightToMax;
                  }
               }
            }
         }
      }

      return Config.limit(d0, 0.0, 15.0);
   }

   public static int getLightLevel(ItemStack p_getLightLevel_0_) {
      if (p_getLightLevel_0_ == null) {
         return 0;
      } else {
         Item item = p_getLightLevel_0_.getItem();
         if (item instanceof ItemBlock itemblock) {
            Block block = itemblock.getBlock();
            if (block != null) {
               return block.getLightValue(block.getDefaultState());
            }
         }

         if (item == Items.LAVA_BUCKET) {
            return Blocks.LAVA.getLightValue(Blocks.LAVA.getDefaultState());
         } else if (item == Items.BLAZE_ROD || item == Items.BLAZE_POWDER) {
            return 10;
         } else if (item == Items.GLOWSTONE_DUST) {
            return 8;
         } else if (item == Items.PRISMARINE_CRYSTALS) {
            return 8;
         } else if (item == Items.MAGMA_CREAM) {
            return 8;
         } else {
            return item == Items.NETHER_STAR ? Blocks.BEACON.getLightValue(Blocks.BEACON.getDefaultState()) / 2 : 0;
         }
      }
   }

   public static int getLightLevel(Entity p_getLightLevel_0_) {
      if (p_getLightLevel_0_ == Config.getMinecraft().getRenderViewEntity() && !Config.isDynamicHandLight()) {
         return 0;
      } else {
         if (p_getLightLevel_0_ instanceof EntityPlayer entityplayer && entityplayer.isSpectator()) {
            return 0;
         }

         if (p_getLightLevel_0_.isBurning()) {
            return 15;
         } else if (p_getLightLevel_0_ instanceof EntityFireball) {
            return 15;
         } else if (p_getLightLevel_0_ instanceof EntityTNTPrimed) {
            return 15;
         } else if (p_getLightLevel_0_ instanceof EntityBlaze entityblaze) {
            return entityblaze.isCharged() ? 15 : 10;
         } else if (p_getLightLevel_0_ instanceof EntityMagmaCube entitymagmacube) {
            return (double)entitymagmacube.squishFactor > 0.6 ? 13 : 8;
         } else {
            if (p_getLightLevel_0_ instanceof EntityCreeper entitycreeper && (double)entitycreeper.getCreeperFlashIntensity(0.0F) > 0.001) {
               return 15;
            }

            if (p_getLightLevel_0_ instanceof EntityLivingBase entitylivingbase) {
               ItemStack itemstack3 = entitylivingbase.getHeldItemMainhand();
               int i = getLightLevel(itemstack3);
               ItemStack itemstack1 = entitylivingbase.getHeldItemOffhand();
               int j = getLightLevel(itemstack1);
               ItemStack itemstack2 = entitylivingbase.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
               int k = getLightLevel(itemstack2);
               int l = Math.max(i, j);
               return Math.max(l, k);
            } else if (p_getLightLevel_0_ instanceof EntityItem entityitem) {
               ItemStack itemstack = getItemStack(entityitem);
               return getLightLevel(itemstack);
            } else {
               return 0;
            }
         }
      }
   }

   public static void removeLights(RenderGlobal p_removeLights_0_) {
      synchronized (mapDynamicLights) {
         Collection<DynamicLight> collection = mapDynamicLights.values();
         Iterator iterator = collection.iterator();

         while (iterator.hasNext()) {
            DynamicLight dynamiclight = (DynamicLight)iterator.next();
            iterator.remove();
            dynamiclight.updateLitChunks(p_removeLights_0_);
         }
      }
   }

   public static void clear() {
      synchronized (mapDynamicLights) {
         mapDynamicLights.clear();
      }
   }

   public static int getCount() {
      synchronized (mapDynamicLights) {
         return mapDynamicLights.size();
      }
   }

   public static ItemStack getItemStack(EntityItem p_getItemStack_0_) {
      return p_getItemStack_0_.getDataManager().get(PARAMETER_ITEM_STACK);
   }
}
