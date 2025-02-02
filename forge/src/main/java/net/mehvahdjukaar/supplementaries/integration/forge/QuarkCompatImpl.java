package net.mehvahdjukaar.supplementaries.integration.forge;

import net.mehvahdjukaar.moonlight.api.block.IBlockHolder;
import net.mehvahdjukaar.supplementaries.common.block.blocks.BambooSpikesBlock;
import net.mehvahdjukaar.supplementaries.common.block.tiles.BambooSpikesBlockTile;
import net.mehvahdjukaar.supplementaries.common.items.JarItem;
import net.mehvahdjukaar.supplementaries.common.items.SackItem;
import net.mehvahdjukaar.supplementaries.integration.CompatHandler;
import net.mehvahdjukaar.supplementaries.reg.ModDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import vazkii.quark.addons.oddities.item.BackpackItem;
import vazkii.quark.base.module.ModuleLoader;
import vazkii.quark.content.automation.module.JukeboxAutomationModule;
import vazkii.quark.content.automation.module.PistonsMoveTileEntitiesModule;
import vazkii.quark.content.building.block.WoodPostBlock;
import vazkii.quark.content.building.module.VerticalSlabsModule;
import vazkii.quark.content.tweaks.module.DoubleDoorOpeningModule;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class QuarkCompatImpl {

    //this should have been implemented in the post block updateShape method
    public static @Nullable BlockState updateWoodPostShape(BlockState post, Direction facing, BlockState facingState) {
        if (post.getBlock() instanceof WoodPostBlock) {
            Direction.Axis axis = post.getValue(WoodPostBlock.AXIS);
            if (facing.getAxis() != axis) {
                boolean chain = (facingState.getBlock() instanceof ChainBlock &&
                        facingState.getValue(BlockStateProperties.AXIS) == facing.getAxis());
                return post.setValue(WoodPostBlock.CHAINED[facing.ordinal()], chain);
            }
        }
        return null;
    }

    public static InteractionResult tryCaptureTater(JarItem jarItem, UseOnContext context) {
        return InteractionResult.PASS;
    }

    public static boolean isDoubleDoorEnabled() {
        return ModuleLoader.INSTANCE.isModuleEnabled(DoubleDoorOpeningModule.class);
    }

    public static boolean canMoveBlockEntity(BlockState state) {
        return !PistonsMoveTileEntitiesModule.shouldMoveTE(true, state);
    }

    public static int getSacksInBackpack(ItemStack stack) {
        int j = 0;
        if (stack.getItem() instanceof BackpackItem) {
            LazyOptional<IItemHandler> handlerOpt = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            if (handlerOpt.isPresent()) {
                IItemHandler handler = handlerOpt.orElse(null);
                for (int i = 0; i < handler.getSlots(); ++i) {
                    ItemStack slotItem = handler.getStackInSlot(i);
                    if (slotItem.getItem() instanceof SackItem) {
                        CompoundTag tag = stack.getTag();
                        if (tag != null && tag.contains("BlockEntityTag")) {
                            j++;
                        }
                    }
                }
            }
        }
        return j;
    }

    public static boolean isVerticalSlabEnabled() {
        return ModuleLoader.INSTANCE.isModuleEnabled(VerticalSlabsModule.class);
    }


    //--------bamboo spikes-------

    //called by mixin code

    public static void tickPiston(Level level, BlockPos pos, AABB pistonBB, boolean sameDir, BlockEntity movingTile) {
        List<Entity> list = level.getEntities(null, pistonBB);
        for (Entity entity : list) {
            if (entity instanceof Player player && player.isCreative()) return;
            if (entity instanceof LivingEntity livingEntity && entity.isAlive()) {
                AABB entityBB = entity.getBoundingBox();
                if (pistonBB.intersects(entityBB)) {
                    //apply potions using quark moving tiles
                    if (CompatHandler.quark) {
                        //get tile
                        if (getMovingBlockEntity(pos, level) instanceof BambooSpikesBlockTile tile) {
                            //apply effects
                            if (tile.interactWithEntity(livingEntity, level)) {
                                //change blockstate if empty
                                if (movingTile instanceof IBlockHolder te) {
                                    //remove last charge and set new blockState
                                    BlockState state = te.getHeldBlock();
                                    if (state.getBlock() instanceof BambooSpikesBlock) {
                                        te.setHeldBlock(state.setValue(BambooSpikesBlock.TIPPED, false));
                                    }
                                }
                            }
                            //update tile entity in its list
                            updateMovingTile(pos, level, tile);
                        }
                    }
                    entity.hurt(ModDamageSources.SPIKE_DAMAGE, sameDir ? 3 : 1);
                }
            }
        }
    }

    private static Field MOVEMENTS = null;

    private static void updateMovingTile(BlockPos pos, Level world, BlockEntity tile) {
        //not very nice of me to change its private fields :/
        try {
            //Class c = Class.forName("vazkii.quark.content.automation.module.PistonsMoveTileEntitiesModule");
            if (MOVEMENTS == null) {
                MOVEMENTS = ObfuscationReflectionHelper.findField(PistonsMoveTileEntitiesModule.class, "movements");
            }

            Object o = MOVEMENTS.get(null);
            if (o instanceof WeakHashMap) {
                WeakHashMap<Level, Map<BlockPos, CompoundTag>> movements = (WeakHashMap<Level, Map<BlockPos, CompoundTag>>) o;
                if (movements.containsKey(world)) {
                    Map<BlockPos, CompoundTag> worldMovements = movements.get(world);
                    if (worldMovements.containsKey(pos)) {
                        worldMovements.remove(pos);
                        worldMovements.put(pos, tile.saveWithFullMetadata());
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static BlockEntity getMovingBlockEntity(BlockPos pos, Level level) {
        return PistonsMoveTileEntitiesModule.getMovement(level, pos);
    }

    public static boolean isJukeboxModuleOn() {
      return  ModuleLoader.INSTANCE.isModuleEnabled(JukeboxAutomationModule.class);
    }

    public static void setup() {


    }


}
