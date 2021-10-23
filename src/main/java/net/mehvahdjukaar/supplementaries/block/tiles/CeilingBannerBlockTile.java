package net.mehvahdjukaar.supplementaries.block.tiles;

import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;


public class CeilingBannerBlockTile extends BlockEntity implements Nameable {
    @Nullable
    private Component name;
    @Nullable
    private DyeColor baseColor = DyeColor.WHITE;
    @Nullable
    private ListTag itemPatterns;
    private boolean receivedData;
    @Nullable
    private List<Pair<BannerPattern, DyeColor>> patterns;

    public CeilingBannerBlockTile(BlockPos pos, BlockState state) {
        this(pos, state, DyeColor.WHITE);
    }

    public CeilingBannerBlockTile(BlockPos pos, BlockState state, DyeColor color) {
        super(ModRegistry.CEILING_BANNER_TILE.get(), pos, state);
        this.baseColor = color;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : new TranslatableComponent("block.minecraft.banner");
    }

    @Nullable
    public Component getCustomName() {
        return this.name;
    }

    public void setCustomName(Component p_213136_1_) {
        this.name = p_213136_1_;
    }

    @Override
    public CompoundTag save(CompoundTag p_189515_1_) {
        super.save(p_189515_1_);
        if (this.itemPatterns != null) {
            p_189515_1_.put("Patterns", this.itemPatterns);
        }

        if (this.name != null) {
            p_189515_1_.putString("CustomName", Component.Serializer.toJson(this.name));
        }

        return p_189515_1_;
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("CustomName", 8)) {
            this.name = Component.Serializer.fromJson(pTag.getString("CustomName"));
        }

        if (this.hasLevel()) {
            this.baseColor = ((AbstractBannerBlock) this.getBlockState().getBlock()).getColor();
        } else {
            this.baseColor = null;
        }

        this.itemPatterns = pTag.getList("Patterns", 10);
        this.patterns = null;
        this.receivedData = true;
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(this.worldPosition, 6, this.getUpdateTag());
    }

    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    public List<Pair<BannerPattern, DyeColor>> getPatterns() {
        if (this.patterns == null && this.receivedData) {
            this.patterns = BannerBlockEntity.createPatterns(this.getBaseColor(this::getBlockState), this.itemPatterns);
        }

        return this.patterns;
    }

    public ItemStack getItem(BlockState state) {
        ItemStack itemstack = new ItemStack(BannerBlock.byColor(this.getBaseColor(() -> state)));
        if (this.itemPatterns != null && !this.itemPatterns.isEmpty()) {
            itemstack.getOrCreateTagElement("BlockEntityTag").put("Patterns", this.itemPatterns.copy());
        }

        if (this.name != null) {
            itemstack.setHoverName(this.name);
        }

        return itemstack;
    }

    public DyeColor getBaseColor(Supplier<BlockState> blockStateSupplier) {
        if (this.baseColor == null) {
            this.baseColor = ((AbstractBannerBlock) blockStateSupplier.get().getBlock()).getColor();
        }

        return this.baseColor;
    }
}

