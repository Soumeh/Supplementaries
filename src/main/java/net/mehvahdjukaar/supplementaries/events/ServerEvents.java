package net.mehvahdjukaar.supplementaries.events;


import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.block.blocks.ClockBlock;
import net.mehvahdjukaar.supplementaries.block.blocks.RakedGravelBlock;
import net.mehvahdjukaar.supplementaries.block.blocks.RopeBlock;
import net.mehvahdjukaar.supplementaries.block.tiles.StatueBlockTile;
import net.mehvahdjukaar.supplementaries.client.renderers.entities.PicklePlayer;
import net.mehvahdjukaar.supplementaries.common.CommonUtil;
import net.mehvahdjukaar.supplementaries.compat.CompatHandler;
import net.mehvahdjukaar.supplementaries.compat.quark.QuarkPlugin;
import net.mehvahdjukaar.supplementaries.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.entities.ThrowableBrickEntity;
import net.mehvahdjukaar.supplementaries.items.CandyItem;
import net.mehvahdjukaar.supplementaries.network.NetworkHandler;
import net.mehvahdjukaar.supplementaries.network.SendLoginMessagePacket;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;


public class ServerEvents {

    //high priority event to override other wall lanterns
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlockHigh(PlayerInteractEvent.RightClickBlock event) {
        if (ServerConfigs.cached.WALL_LANTERN_HIGH_PRIORITY) {
            Player player = event.getPlayer();
            if (!player.isSpectator()) {
                ItemsOverrideHandler.tryHighPriorityOverride(event, event.getItemStack());
            }
        }
    }

    //block placement should stay low in priority to allow other more important mod interaction that use the event
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        if (player.isSpectator()) return;

        ItemStack stack = event.getItemStack();

        //others handled here
        if (ItemsOverrideHandler.tryPerformOverride(event, stack, false)) {
            return;
        }

        //empty hand behaviors
        //order matters here
        if (!player.isShiftKeyDown()) {

            InteractionHand hand = event.getHand();
            Item i = stack.getItem();
            Level world = event.getWorld();
            BlockPos pos = event.getPos();
            BlockState blockstate = world.getBlockState(pos);

            //directional cake conversion
            if (ServerConfigs.cached.DIRECTIONAL_CAKE && blockstate == Blocks.CAKE.defaultBlockState() &&
                    !(ServerConfigs.cached.DOUBLE_CAKE_PLACEMENT && i == Items.CAKE)) {
                world.setBlock(pos, ModRegistry.DIRECTIONAL_CAKE.get().defaultBlockState(), 4);
                BlockHitResult raytrace = new BlockHitResult(
                        new Vec3(pos.getX(), pos.getY(), pos.getZ()), event.getFace(), pos, false);

                event.setCanceled(true);
                event.setCancellationResult(blockstate.use(world, player, hand, raytrace));

                return;
            }

            //bell chains
            if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                if (ServerConfigs.cached.BELL_CHAIN) {
                    if (RopeBlock.findAndRingBell(world, pos, player, 0, s -> s.getBlock() instanceof ChainBlock && s.getValue(ChainBlock.AXIS) == Direction.Axis.Y)) {

                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.sidedSuccess(world.isClientSide));
                    }
                }
            }
        }


    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player playerIn = event.getPlayer();
        ItemStack itemstack = playerIn.getItemInHand(event.getHand());
        Item i = itemstack.getItem();
        Level worldIn = event.getWorld();

        //TODO: improve
        // ItemInteractionOverrideHandler.tryPerformOverride(event);

        if (ServerConfigs.cached.THROWABLE_BRICKS_ENABLED && CommonUtil.isBrick(i)) {

            worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (playerIn.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!worldIn.isClientSide) {
                ThrowableBrickEntity brickEntity = new ThrowableBrickEntity(worldIn, playerIn);
                brickEntity.setItem(itemstack);
                float pow = 0.7f;
                brickEntity.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 1.5F * pow, 1.0F * pow);
                worldIn.addFreshEntity(brickEntity);
            }

            if (!playerIn.getAbilities().instabuild) {
                itemstack.shrink(1);
            }

            //playerIn.swingArm(handIn);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(worldIn.isClientSide));
            return;

        }
        if (worldIn.isClientSide && ClientConfigs.cached.CLOCK_CLICK && i == Items.CLOCK) {
            ClockBlock.displayCurrentHour(worldIn, playerIn);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(worldIn.isClientSide));
        }

    }

    //raked gravel
    @SubscribeEvent
    public static void onHoeUsed(UseHoeEvent event) {
        UseOnContext context = event.getContext();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (ServerConfigs.cached.RAKED_GRAVEL) {
            if (world.getBlockState(pos).is(Blocks.GRAVEL)) {
                BlockState raked = ModRegistry.RAKED_GRAVEL.get().defaultBlockState();
                if (raked.canSurvive(world, pos)) {
                    world.setBlock(pos, RakedGravelBlock.getConnectedState(raked, world, pos, context.getHorizontalDirection()), 11);
                    world.playSound(context.getPlayer(), pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    event.setResult(Event.Result.ALLOW);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onAttachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        if (CompatHandler.quark) {
            QuarkPlugin.attachCapabilities(event);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            CandyItem.checkSweetTooth(event.player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()),
                    new SendLoginMessagePacket());
        } catch (Exception exception) {
            Supplementaries.LOGGER.warn("failed to end login message: " + exception);
        }
        //send in pickles
        PicklePlayer.PickleData.onPlayerLogin(event.getPlayer());

    }

    @SubscribeEvent
    public static void serverAboutToStart(final FMLServerAboutToStartEvent event) {
        StatueBlockTile.initializeSessionData(event.getServer());
    }


}
