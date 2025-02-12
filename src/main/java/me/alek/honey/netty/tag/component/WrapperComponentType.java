package me.alek.honey.netty.tag.component;

import com.github.retrooper.packetevents.protocol.color.DyeColor;
import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.*;
import com.github.retrooper.packetevents.protocol.item.instrument.Instrument;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Dummy;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.component.visitor.ComponentMapVisitor;
import me.alek.honey.netty.tag.component.visitor.InVisitorOperator;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public interface WrapperComponentType<T> extends InVisitorOperator<T> {

    Map<ComponentType<?>, WrapperComponentType<?>> REGISTRY = new HashMap<>();

    WrapperComponentType<CompoundTag> CUSTOM_DATA = define(ComponentTypes.CUSTOM_DATA, ComponentMapVisitor.In::visitAtCustomData, ComponentMapVisitor::visitCustomData);
    WrapperComponentType<Integer> MAX_STACK_SIZE = define(ComponentTypes.MAX_STACK_SIZE, ComponentMapVisitor.In::visitAtMaxStackSize, ComponentMapVisitor::visitMaxStackSize);
    WrapperComponentType<Integer> MAX_DAMAGE = define(ComponentTypes.MAX_DAMAGE, ComponentMapVisitor.In::visitAtMaxDamage, ComponentMapVisitor::visitMaxDamage);
    WrapperComponentType<Integer> DAMAGE = define(ComponentTypes.DAMAGE, ComponentMapVisitor.In::visitAtDamage, ComponentMapVisitor::visitDamage);
    WrapperComponentType<Boolean> UNBREAKABLE = define(ComponentTypes.UNBREAKABLE, ComponentMapVisitor.In::visitAtUnbreakable, ComponentMapVisitor::visitUnbreakable);
    WrapperComponentType<Component> CUSTOM_NAME = define(ComponentTypes.CUSTOM_NAME, ComponentMapVisitor.In::visitAtCustomName, ComponentMapVisitor::visitCustomName);
    WrapperComponentType<Component> ITEM_NAME = define(ComponentTypes.ITEM_NAME, ComponentMapVisitor.In::visitAtItemName, ComponentMapVisitor::visitItemName);
    WrapperComponentType<ItemLore> LORE = define(ComponentTypes.LORE, ComponentMapVisitor.In::visitAtLore, ComponentMapVisitor::visitLore);
    WrapperComponentType<ItemRarity> RARITY = define(ComponentTypes.RARITY, ComponentMapVisitor.In::visitAtRarity, ComponentMapVisitor::visitRarity);
    WrapperComponentType<ItemEnchantments> ENCHANTMENTS = define(ComponentTypes.ENCHANTMENTS, ComponentMapVisitor.In::visitAtEnchantments, ComponentMapVisitor::visitEnchantments);
    WrapperComponentType<ItemAdventurePredicate> CAN_PLACE_ON = define(ComponentTypes.CAN_PLACE_ON, ComponentMapVisitor.In::visitAtCanPlaceOn, ComponentMapVisitor::visitCanPlaceOn);
    WrapperComponentType<ItemAdventurePredicate> CAN_BREAK = define(ComponentTypes.CAN_BREAK, ComponentMapVisitor.In::visitAtCanBreak, ComponentMapVisitor::visitCanBreak);
    WrapperComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = define(ComponentTypes.ATTRIBUTE_MODIFIERS, ComponentMapVisitor.In::visitAtAttributeModifiers, ComponentMapVisitor::visitAttributeModifiers);
    WrapperComponentType<ItemCustomModelData> CUSTOM_MODEL_DATA_LISTS = define(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, ComponentMapVisitor.In::visitAtCustomModelDataLists, ComponentMapVisitor::visitCustomModelDataLists);
    @Deprecated
    WrapperComponentType<Integer> CUSTOM_MODEL_DATA = define(ComponentTypes.CUSTOM_MODEL_DATA, ComponentMapVisitor.In::visitAtCustomModelData, ComponentMapVisitor::visitCustomModelData);
    WrapperComponentType<Dummy> HIDE_ADDITIONAL_TOOLTIP = define(ComponentTypes.HIDE_ADDITIONAL_TOOLTIP, ComponentMapVisitor.In::visitAtHideAdditionalTooltip, ComponentMapVisitor::visitHideAdditionalTooltip);
    WrapperComponentType<Dummy> HIDE_TOOLTIP = define(ComponentTypes.HIDE_TOOLTIP, ComponentMapVisitor.In::visitAtHideTooltip, ComponentMapVisitor::visitHideTooltip);
    WrapperComponentType<Dummy> REPAIR_COST = define(ComponentTypes.REPAIR_COST, ComponentMapVisitor.In::visitAtRepairCost, ComponentMapVisitor::visitRepairCost);
    WrapperComponentType<Dummy> CREATIVE_SLOT_LOCK = define(ComponentTypes.CREATIVE_SLOT_LOCK, ComponentMapVisitor.In::visitAtCreativeSlotLock, ComponentMapVisitor::visitCreativeSlotLock);
    WrapperComponentType<Dummy> ENCHANTMENT_GLINT_OVERRIDE = define(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, ComponentMapVisitor.In::visitAtEnchantmentGlintOverride, ComponentMapVisitor::visitEnchantmentGlintOverride);
    WrapperComponentType<Dummy> INTANGIBLE_PROJECTILE = define(ComponentTypes.INTANGIBLE_PROJECTILE, ComponentMapVisitor.In::visitAtIntagibleProjectiles, ComponentMapVisitor::visitIntagibleProjectiles);
    WrapperComponentType<FoodProperties> FOOD = define(ComponentTypes.FOOD, ComponentMapVisitor.In::visitAtFood, ComponentMapVisitor::visitFood);
    WrapperComponentType<Dummy> FIRE_RESISTANT = define(ComponentTypes.FIRE_RESISTANT, ComponentMapVisitor.In::visitAtFireResistant, ComponentMapVisitor::visitFireResistant);
    WrapperComponentType<ItemTool> TOOL = define(ComponentTypes.TOOL, ComponentMapVisitor.In::visitAtTool, ComponentMapVisitor::visitTool);
    WrapperComponentType<ItemEnchantments> STORED_ENCHANTMENTS = define(ComponentTypes.STORED_ENCHANTMENTS, ComponentMapVisitor.In::visitAtStoredEnchantments, ComponentMapVisitor::visitStoredEnchantments);
    WrapperComponentType<ItemDyeColor> DYED_COLOR = define(ComponentTypes.DYED_COLOR, ComponentMapVisitor.In::visitAtDyedColor, ComponentMapVisitor::visitDyedColor);
    WrapperComponentType<Integer> MAP_COLOR = define(ComponentTypes.MAP_COLOR, ComponentMapVisitor.In::visitAtMapColor, ComponentMapVisitor::visitMapColor);
    WrapperComponentType<Integer> MAP_ID = define(ComponentTypes.MAP_ID, ComponentMapVisitor.In::visitAtMapId, ComponentMapVisitor::visitMapId);
    WrapperComponentType<ItemMapDecorations> MAP_DECORATIONS = define(ComponentTypes.MAP_DECORATIONS, ComponentMapVisitor.In::visitAtMapDecorations, ComponentMapVisitor::visitMapDecorations);
    WrapperComponentType<ItemMapPostProcessingState> MAP_POST_PROCESSING = define(ComponentTypes.MAP_POST_PROCESSING, ComponentMapVisitor.In::visitAtMapPostProcessing, ComponentMapVisitor::visitMapPostProcessing);
    WrapperComponentType<ChargedProjectiles> CHARGED_PROJECTILES = define(ComponentTypes.CHARGED_PROJECTILES, ComponentMapVisitor.In::visitAtChargedProjectiles, ComponentMapVisitor::visitChargedProjectiles);
    WrapperComponentType<BundleContents> BUNDLE_CONTENTS = define(ComponentTypes.BUNDLE_CONTENTS, ComponentMapVisitor.In::visitAtBundleContents, ComponentMapVisitor::visitBundleContents);
    WrapperComponentType<ItemPotionContents> POTION_CONTENTS = define(ComponentTypes.POTION_CONTENTS, ComponentMapVisitor.In::visitAtPotionContents, ComponentMapVisitor::visitPotionContents);
    WrapperComponentType<SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS = define(ComponentTypes.SUSPICIOUS_STEW_EFFECTS, ComponentMapVisitor.In::visitAtSuspiciousStewEffects, ComponentMapVisitor::visitSuspiciousStewEffects);
    WrapperComponentType<WritableBookContent> WRITABLE_BOOK_CONTENT = define(ComponentTypes.WRITABLE_BOOK_CONTENT, ComponentMapVisitor.In::visitAtWritableBookContent, ComponentMapVisitor::visitWritableBookContent);
    WrapperComponentType<WrittenBookContent> WRITTEN_BOOK_CONTENT = define(ComponentTypes.WRITTEN_BOOK_CONTENT, ComponentMapVisitor.In::visitAtWrittenBookContent, ComponentMapVisitor::visitWrittenBookContent);
    WrapperComponentType<ArmorTrim> TRIM = define(ComponentTypes.TRIM, ComponentMapVisitor.In::visitAtTrim, ComponentMapVisitor::visitTrim);
    WrapperComponentType<DebugStickState> DEBUG_STICK_STATE = define(ComponentTypes.DEBUG_STICK_STATE, ComponentMapVisitor.In::visitAtDebugStickState, ComponentMapVisitor::visitDebugStickState);
    WrapperComponentType<CompoundTag> ENTITY_DATA = define(ComponentTypes.ENTITY_DATA, ComponentMapVisitor.In::visitAtEntityData, ComponentMapVisitor::visitEntityData);
    WrapperComponentType<CompoundTag> BUCKET_ENTITY_DATA = define(ComponentTypes.BUCKET_ENTITY_DATA, ComponentMapVisitor.In::visitAtBucketEntityData, ComponentMapVisitor::visitBucketEntityData);
    WrapperComponentType<CompoundTag> BLOCK_ENTITY_DATA = define(ComponentTypes.BLOCK_ENTITY_DATA, ComponentMapVisitor.In::visitAtBlockEntityData, ComponentMapVisitor::visitBlockEntityData);
    WrapperComponentType<Instrument> INSTRUMENT = define(ComponentTypes.INSTRUMENT, ComponentMapVisitor.In::visitAtInstrument, ComponentMapVisitor::visitInstrument);
    WrapperComponentType<Integer> OMINOUS_BOTTLE_AMPLIFIER = define(ComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, ComponentMapVisitor.In::visitAtOmniousBottleAmplifier, ComponentMapVisitor::visitOmniousBottleAmplifier);
    WrapperComponentType<ItemRecipes> RECIPES = define(ComponentTypes.RECIPES, ComponentMapVisitor.In::visitAtRecipes, ComponentMapVisitor::visitRecipes);
    WrapperComponentType<LodestoneTracker> LODESTONE_TRACKER = define(ComponentTypes.LODESTONE_TRACKER, ComponentMapVisitor.In::visitAtLodestoneTracker, ComponentMapVisitor::visitLodestoneTracker);
    WrapperComponentType<FireworkExplosion> FIREWORK_EXPLOSION = define(ComponentTypes.FIREWORK_EXPLOSION, ComponentMapVisitor.In::visitAtFireworkExplosion, ComponentMapVisitor::visitFireworkExplosion);
    WrapperComponentType<ItemFireworks> FIREWORKS = define(ComponentTypes.FIREWORKS, ComponentMapVisitor.In::visitAtFireworks, ComponentMapVisitor::visitFireworks);
    WrapperComponentType<ItemProfile> PROFILE = define(ComponentTypes.PROFILE, ComponentMapVisitor.In::visitAtProfile, ComponentMapVisitor::visitProfile);
    WrapperComponentType<ResourceLocation> NOTE_BLOCK_SOUND = define(ComponentTypes.NOTE_BLOCK_SOUND, ComponentMapVisitor.In::visitAtNoteBlockSound, ComponentMapVisitor::visitNoteBlockSound);
    WrapperComponentType<BannerLayers> BANNER_PATTERNS = define(ComponentTypes.BANNER_PATTERNS, ComponentMapVisitor.In::visitAtBannerPatterns, ComponentMapVisitor::visitBannerPatterns);
    WrapperComponentType<DyeColor> BASE_COLOR = define(ComponentTypes.BASE_COLOR, ComponentMapVisitor.In::visitAtBaseColor, ComponentMapVisitor::visitBaseColor);
    WrapperComponentType<PotDecorations> POT_DECORATIONS = define(ComponentTypes.POT_DECORATIONS, ComponentMapVisitor.In::visitAtPotDecorations, ComponentMapVisitor::visitPotDecorations);
    WrapperComponentType<ItemContainerContents> CONTAINER = define(ComponentTypes.CONTAINER, ComponentMapVisitor.In::visitAtContainer, ComponentMapVisitor::visitContainer);
    WrapperComponentType<ItemBlockStateProperties> BLOCK_STATE = define(ComponentTypes.BLOCK_STATE, ComponentMapVisitor.In::visitAtBlockState, ComponentMapVisitor::visitBlockState);
    WrapperComponentType<ItemBees> BEES = define(ComponentTypes.BEES, ComponentMapVisitor.In::visitAtBees, ComponentMapVisitor::visitBees);
    WrapperComponentType<ItemLock> LOCK = define(ComponentTypes.LOCK, ComponentMapVisitor.In::visitAtLock, ComponentMapVisitor::visitLock);
    WrapperComponentType<ItemContainerLoot> CONTAINER_LOOT = define(ComponentTypes.CONTAINER_LOOT, ComponentMapVisitor.In::visitAtContainerLoot, ComponentMapVisitor::visitContainerLoot);
    WrapperComponentType<ItemJukeboxPlayable> JUKEBOX_PLAYABLE = define(ComponentTypes.JUKEBOX_PLAYABLE, ComponentMapVisitor.In::visitAtJukeboxPlayable, ComponentMapVisitor::visitJukeboxPlayable);
    WrapperComponentType<ItemConsumable> CONSUMABLE = define(ComponentTypes.CONSUMABLE, ComponentMapVisitor.In::visitAtConsumable, ComponentMapVisitor::visitConsumable);
    WrapperComponentType<ItemUseRemainder> USE_REMAINDER = define(ComponentTypes.USE_REMAINDER, ComponentMapVisitor.In::visitAtUseRemainder, ComponentMapVisitor::visitUseRemainder);
    WrapperComponentType<ItemUseCooldown> USE_COOLDOWN = define(ComponentTypes.USE_COOLDOWN, ComponentMapVisitor.In::visitAtUseCooldown, ComponentMapVisitor::visitUseCooldown);
    WrapperComponentType<ItemEnchantable> ENCHANTABLE = define(ComponentTypes.ENCHANTABLE, ComponentMapVisitor.In::visitAtEnchantable, ComponentMapVisitor::visitEnchantable);
    WrapperComponentType<ItemRepairable> REPAIRABLE = define(ComponentTypes.REPAIRABLE, ComponentMapVisitor.In::visitAtRepairable, ComponentMapVisitor::visitRepairable);
    WrapperComponentType<ItemModel> ITEM_MODEL = define(ComponentTypes.ITEM_MODEL, ComponentMapVisitor.In::visitAtItemModel, ComponentMapVisitor::visitItemModel);
    WrapperComponentType<ItemDamageResistant> DAMAGE_RESISTANT = define(ComponentTypes.DAMAGE_RESISTANT, ComponentMapVisitor.In::visitAtDamageResistant, ComponentMapVisitor::visitDamageResistant);
    WrapperComponentType<ItemEquippable> EQUIPPABLE = define(ComponentTypes.EQUIPPABLE, ComponentMapVisitor.In::visitAtEquippable, ComponentMapVisitor::visitEquippable);
    WrapperComponentType<Dummy> GLIDER = define(ComponentTypes.GLIDER, ComponentMapVisitor.In::visitAtGlider, ComponentMapVisitor::visitGlider);
    WrapperComponentType<ItemDeathProtection> DEATH_PROTECTION = define(ComponentTypes.DEATH_PROTECTION, ComponentMapVisitor.In::visitAtDeathProtection, ComponentMapVisitor::visitDeathProtection);
    WrapperComponentType<ItemTooltipStyle> TOOLTIP_STYLE = define(ComponentTypes.TOOLTIP_STYLE, ComponentMapVisitor.In::visitAtTooltipStyle, ComponentMapVisitor::visitTooltipStyle);

    static <T> WrapperComponentType<T> define(ComponentType<?> type, InVisitorOperator<T> operator, BiConsumer<ComponentMapVisitor, T> consumer) {
        WrapperComponentType<T> wrapperComponentType = of(type, operator, consumer);
        REGISTRY.put(type, wrapperComponentType);
        return wrapperComponentType;
    }

    static <T> WrapperComponentType<T> of(ComponentType<?> type, InVisitorOperator<T> operator, BiConsumer<ComponentMapVisitor, T> consumer) {
        return new WrapperComponentType<T>() {

            @Override
            public T visitAt(ComponentMapVisitor.In in, PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                return operator.visitAt(in, serializer);
            }

            @Override
            public ComponentType<?> delegate() {
                return type;
            }

            @Override
            public void visit(ComponentMapVisitor visitor, T data) {
                consumer.accept(visitor, data);
            }
        };
    }

    ComponentType<?> delegate();

    void visit(ComponentMapVisitor visitor, T data);
}
