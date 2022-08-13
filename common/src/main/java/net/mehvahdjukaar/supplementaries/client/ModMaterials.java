package net.mehvahdjukaar.supplementaries.client;

import net.mehvahdjukaar.moonlight.api.set.wood.WoodType;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.common.ModTextures;
import net.mehvahdjukaar.supplementaries.common.block.tiles.BookPileBlockTile;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Registry;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.level.block.entity.BannerPattern;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static net.mehvahdjukaar.supplementaries.reg.ClientRegistry.BANNER_SHEET;
import static net.minecraft.client.renderer.Sheets.SHULKER_SHEET;
import static net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;

public class ModMaterials {

    //materials
    public static final Material BLACKBOARD_OUTLINE = new Material(LOCATION_BLOCKS, ModTextures.BLACKBOARD_GRID);
    public static final Material BELLOWS_MATERIAL = new Material(LOCATION_BLOCKS, ModTextures.BELLOWS_TEXTURE);
    public static final Material BUBBLE_BLOCK_MATERIAL = new Material(LOCATION_BLOCKS, ModTextures.BUBBLE_BLOCK_TEXTURE);
    public static final Material BOOK_ENCHANTED_MATERIAL = new Material(SHULKER_SHEET, ModTextures.BOOK_ENCHANTED_TEXTURES);
    public static final Material BOOK_TOME_MATERIAL = new Material(SHULKER_SHEET, ModTextures.BOOK_TOME_TEXTURES);
    public static final Material BOOK_WRITTEN_MATERIAL = new Material(SHULKER_SHEET, ModTextures.BOOK_WRITTEN_TEXTURES);
    public static final Material BOOK_AND_QUILL_MATERIAL = new Material(SHULKER_SHEET, ModTextures.BOOK_AND_QUILL_TEXTURES);
    public static final Map<BookPileBlockTile.BookColor, Material> BOOK_MATERIALS = new HashMap<>();
    public static final Map<WoodType, Material> SIGN_POSTS_MATERIALS = new HashMap<>();
    public static final Map<BannerPattern, Material> FLAG_MATERIALS = new HashMap<>();

    //needs static initializer as models are loaded before client setup
    static {
        ModRegistry.SIGN_POST_ITEMS.forEach((wood, item) -> SIGN_POSTS_MATERIALS
                .put(wood, new Material(LOCATION_BLOCKS, Supplementaries.res("entity/sign_posts/" + Utils.getID(item).getPath()))));
    }

    //needs to run after textures but can't run too early because of banners
    public static void setup() {
        for (var e : ModTextures.FLAG_TEXTURES.entrySet()) {
            FLAG_MATERIALS.put(e.getKey(), new Material(BANNER_SHEET, e.getValue()));
        }

        for (var e : ModTextures.BOOK_TEXTURES.entrySet()) {
            BOOK_MATERIALS.put(e.getKey(), new Material(SHULKER_SHEET, e.getValue()));
        }
    }

    @Nullable
    public static Material getFlagMaterialForPatternItem(BannerPatternItem item) {
        var p = ITEM_TO_PATTERNS.get(item);
        if (p == null) {
            for (var j : Registry.BANNER_PATTERN.getTag(item.getBannerPattern()).get()) {
                ITEM_TO_PATTERNS.put(item, j.value());
                return FLAG_MATERIALS.get(j.value());
            }
            return null;
        } else return FLAG_MATERIALS.get(p);
    }

    private static final Map<BannerPatternItem, BannerPattern> ITEM_TO_PATTERNS = new HashMap<>();


}
