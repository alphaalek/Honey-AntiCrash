package me.alek.honey.netty.tag.component.visitor;

import com.github.retrooper.packetevents.protocol.color.DyeColor;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.*;
import com.github.retrooper.packetevents.protocol.item.instrument.Instrument;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.util.protocol.NetProtocol;

public class CommonComponentMapInVisitor extends ComponentMapVisitor.In.Adapter {

    protected final ItemType type;
    protected final NetProtocol protocol;
    protected final NetProtocol protocolCreated;

    public CommonComponentMapInVisitor(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
        this.protocol = protocol;
        this.protocolCreated = protocolCreated;
        this.type = type;
    }

    @Override
    public ItemType type() {
        return this.type;
    }

    @Override
    public NetProtocol protocol() {
        return this.protocol;
    }

    @Override
    public NetProtocol protocolCreated() {
        return this.protocolCreated;
    }

    @Override
    public void visitFinal(ComponentMap map) {
        if (map.has(ComponentTypes.FOOD) && !map.has(ComponentTypes.CONSUMABLE) && !this.type.hasAttribute(ItemTypes.ItemAttribute.EDIBLE)) {
            throw new IllegalStateException("Illegal food component");
        }

        if (map.has(ComponentTypes.MAX_DAMAGE) && map.has(ComponentTypes.MAX_STACK_SIZE) && map.get(ComponentTypes.MAX_STACK_SIZE).get() != 1) {
            throw new IllegalStateException("Illegal combination of max_damage and max_stack_size components");
        }
    }

    @Override
    public void visitBannerPatterns(BannerLayers val) {
        if (       !this.type.equals(ItemTypes.SHIELD)
                && !this.type.equals(ItemTypes.BLACK_BANNER)
                && !this.type.equals(ItemTypes.BLUE_BANNER)
                && !this.type.equals(ItemTypes.BROWN_BANNER)
                && !this.type.equals(ItemTypes.GRAY_BANNER)
                && !this.type.equals(ItemTypes.CYAN_BANNER)
                && !this.type.equals(ItemTypes.GREEN_BANNER)
                && !this.type.equals(ItemTypes.LIME_BANNER)
                && !this.type.equals(ItemTypes.LIGHT_BLUE_BANNER)
                && !this.type.equals(ItemTypes.LIGHT_BLUE_BANNER)
                && !this.type.equals(ItemTypes.MAGENTA_BANNER)
                && !this.type.equals(ItemTypes.ORANGE_BANNER)
                && !this.type.equals(ItemTypes.PINK_BANNER)
                && !this.type.equals(ItemTypes.PURPLE_BANNER)
                && !this.type.equals(ItemTypes.RED_BANNER)
                && !this.type.equals(ItemTypes.WHITE_BANNER)
                && !this.type.equals(ItemTypes.YELLOW_BANNER)) {
            throw new IllegalStateException("Illegal banner_patterns component");
        }
    }

    @Override
    public void visitBaseColor(DyeColor val) {
        if (!this.type.equals(ItemTypes.SHIELD)) {
            throw new IllegalStateException("Illegal base_color component");
        }
    }

    @Override
    public void visitBees(ItemBees val) {
        if (!this.type.equals(ItemTypes.BEEHIVE) && !this.type.equals(ItemTypes.BEE_NEST)) {
            throw new IllegalStateException("Illegal bees component");
        }
    }

    @Override
    public void visitBucketEntityData(CompoundTag val) {
        if (       !this.type.equals(ItemTypes.COD_BUCKET)
                && !this.type.equals(ItemTypes.AXOLOTL_BUCKET)
                && !this.type.equals(ItemTypes.PUFFERFISH_BUCKET)
                && !this.type.equals(ItemTypes.SALMON_BUCKET)
                && !this.type.equals(ItemTypes.TADPOLE_BUCKET)
                && !this.type.equals(ItemTypes.TROPICAL_FISH_BUCKET)) {
            throw new IllegalStateException("Illegal bucket_entity_data component");
        }
    }

    @Override
    public void visitBundleContents(BundleContents val) {
        if (       !this.type.equals(ItemTypes.BUNDLE)
                && !this.type.equals(ItemTypes.BROWN_BUNDLE)
                && !this.type.equals(ItemTypes.BLACK_BUNDLE)
                && !this.type.equals(ItemTypes.BLUE_BUNDLE)
                && !this.type.equals(ItemTypes.CYAN_BUNDLE)
                && !this.type.equals(ItemTypes.GRAY_BUNDLE)
                && !this.type.equals(ItemTypes.GREEN_BUNDLE)
                && !this.type.equals(ItemTypes.LIGHT_BLUE_BUNDLE)
                && !this.type.equals(ItemTypes.LIGHT_GRAY_BUNDLE)
                && !this.type.equals(ItemTypes.LIME_BUNDLE)
                && !this.type.equals(ItemTypes.MAGENTA_BUNDLE)
                && !this.type.equals(ItemTypes.ORANGE_BUNDLE)
                && !this.type.equals(ItemTypes.PINK_BUNDLE)
                && !this.type.equals(ItemTypes.RED_BUNDLE)
                && !this.type.equals(ItemTypes.WHITE_BUNDLE)
                && !this.type.equals(ItemTypes.YELLOW_BUNDLE)
                && !this.type.equals(ItemTypes.PURPLE_BUNDLE)) {
            throw new IllegalStateException("Illegal bundle_contents component");
        }
    }

    @Override
    public void visitChargedProjectiles(ChargedProjectiles val) {
        if (!this.type.equals(ItemTypes.CROSSBOW)) {
            throw new IllegalStateException("Illegal charged_projectiles component");
        }
    }

    @Override
    public void visitDebugStickState(DebugStickState val) {
        if (!this.type.equals(ItemTypes.DEBUG_STICK)) {
            throw new IllegalStateException("Illegal debug_stick component");
        }
    }

    @Override
    public void visitDeathProtection(ItemDeathProtection val) {
        if (!this.type.equals(ItemTypes.TOTEM_OF_UNDYING)) {
            throw new IllegalStateException("Illegal death_protection component");
        }
    }

    @Override
    public void visitDyedColor(ItemDyeColor val) {
        if (       !this.type.equals(ItemTypes.LEATHER_BOOTS)
                && !this.type.equals(ItemTypes.LEATHER_LEGGINGS)
                && !this.type.equals(ItemTypes.LEATHER_CHESTPLATE)
                && !this.type.equals(ItemTypes.LEATHER_HELMET)
                && !this.type.equals(ItemTypes.LEATHER_HORSE_ARMOR)) {
            throw new IllegalStateException("Illegal dyed_color component");
        }
    }

    @Override
    public void visitFireworkExplosion(FireworkExplosion val) {
        if (!this.type.equals(ItemTypes.FIREWORK_STAR)) {
            throw new IllegalStateException("Illegal firework_explosion component");
        }
    }

    @Override
    public void visitFireworks(ItemFireworks val) {
        if (!this.type.equals(ItemTypes.FIREWORK_ROCKET)) {
            throw new IllegalStateException("Illegal fireworks component");
        }
    }

    @Override
    public void visitInstrument(Instrument val) {
        if (!this.type.equals(ItemTypes.GOAT_HORN)) {
            throw new IllegalStateException("Illegal instrument component");
        }
    }

    @Override
    public void visitLodestoneTracker(LodestoneTracker val) {
        if (!this.type.equals(ItemTypes.COMPASS)) {
            throw new IllegalStateException("Illegal lodestone_tracker component");
        }
    }

    @Override
    public void visitMapId(int val) {
        if (!this.type.equals(ItemTypes.MAP) && !this.type.equals(ItemTypes.FILLED_MAP)) {
            throw new IllegalStateException("Illegal map component");
        }
    }

    @Override
    public void visitMapColor(int val) {
        if (!this.type.equals(ItemTypes.MAP) && !this.type.equals(ItemTypes.FILLED_MAP)) {
            throw new IllegalStateException("Illegal map component");
        }
    }

    @Override
    public void visitMapDecorations(ItemMapDecorations val) {
        if (!this.type.equals(ItemTypes.MAP) && !this.type.equals(ItemTypes.FILLED_MAP)) {
            throw new IllegalStateException("Illegal map component");
        }
    }

    @Override
    public void visitOmniousBottleAmplifier(int val) {
        if (!this.type.equals(ItemTypes.OMINOUS_BOTTLE)) {
            throw new IllegalStateException("Illegal ominous_bottle_amplifier component");
        }
    }

    @Override
    public void visitPotDecorations(PotDecorations val) {
        if (!this.type.equals(ItemTypes.DECORATED_POT)) {
            throw new IllegalStateException("Illegal pot_decorations component");
        }
    }

    @Override
    public void visitPotionContents(ItemPotionContents val) {
        if (       !this.type.equals(ItemTypes.POTION)
                && !this.type.equals(ItemTypes.SPLASH_POTION)
                && !this.type.equals(ItemTypes.LINGERING_POTION)
                && !this.type.equals(ItemTypes.TIPPED_ARROW)) {
            throw new IllegalStateException("Illegal potion_contents component");
        }
    }

    @Override
    public void visitProfile(ItemProfile val) {
        if (!this.type.equals(ItemTypes.PLAYER_HEAD)) {
            throw new IllegalStateException("Illegal profile component");
        }
    }

    @Override
    public void visitRecipes(ItemRecipes val) {
        if (!this.type.equals(ItemTypes.KNOWLEDGE_BOOK)) {
            throw new IllegalStateException("Illegal recipes component");
        }
    }

    @Override
    public void visitStoredEnchantments(ItemEnchantments val) {
        if (!this.type.equals(ItemTypes.ENCHANTED_BOOK)) {
            throw new IllegalStateException("Illegal stored_enchantments component");
        }
    }

    @Override
    public void visitSuspiciousStewEffects(SuspiciousStewEffects val) {
        if (!this.type.equals(ItemTypes.SUSPICIOUS_STEW)) {
            throw new IllegalStateException("Illegal suspicious_stew_effects component");
        }
    }

    @Override
    public void visitTrim(ArmorTrim val) {
        if (       !this.type.equals(ItemTypes.LEATHER_BOOTS)
                && !this.type.equals(ItemTypes.LEATHER_LEGGINGS)
                && !this.type.equals(ItemTypes.LEATHER_CHESTPLATE)
                && !this.type.equals(ItemTypes.LEATHER_HELMET)
                && !this.type.equals(ItemTypes.IRON_BOOTS)
                && !this.type.equals(ItemTypes.IRON_LEGGINGS)
                && !this.type.equals(ItemTypes.IRON_CHESTPLATE)
                && !this.type.equals(ItemTypes.IRON_HELMET)
                && !this.type.equals(ItemTypes.CHAINMAIL_BOOTS)
                && !this.type.equals(ItemTypes.CHAINMAIL_LEGGINGS)
                && !this.type.equals(ItemTypes.CHAINMAIL_CHESTPLATE)
                && !this.type.equals(ItemTypes.CHAINMAIL_HELMET)
                && !this.type.equals(ItemTypes.GOLDEN_BOOTS)
                && !this.type.equals(ItemTypes.GOLDEN_LEGGINGS)
                && !this.type.equals(ItemTypes.GOLDEN_CHESTPLATE)
                && !this.type.equals(ItemTypes.GOLDEN_HELMET)
                && !this.type.equals(ItemTypes.DIAMOND_BOOTS)
                && !this.type.equals(ItemTypes.DIAMOND_LEGGINGS)
                && !this.type.equals(ItemTypes.DIAMOND_CHESTPLATE)
                && !this.type.equals(ItemTypes.DIAMOND_HELMET)
                && !this.type.equals(ItemTypes.NETHERITE_BOOTS)
                && !this.type.equals(ItemTypes.NETHERITE_LEGGINGS)
                && !this.type.equals(ItemTypes.NETHERITE_CHESTPLATE)
                && !this.type.equals(ItemTypes.NETHERITE_HELMET)) {
            throw new IllegalStateException("Illegal trim component");
        }
    }

    @Override
    public void visitWritableBookContent(WritableBookContent val) {
        if (!this.type.equals(ItemTypes.WRITABLE_BOOK)) {
            throw new IllegalStateException("Illegal writable_book_content component");
        }
    }

    @Override
    public void visitWrittenBookContent(WrittenBookContent val) {
        if (!this.type.equals(ItemTypes.WRITTEN_BOOK)) {
            throw new IllegalStateException("Illegal written_book_content component");
        }
    }
}
