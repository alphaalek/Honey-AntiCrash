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
import me.alek.honey.netty.tag.component.visitor.ComponentVisitor;
import me.alek.honey.netty.tag.component.visitor.InVisitorOperator;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public interface WrapperComponentType<T> extends InVisitorOperator<T> {

    Map<ComponentType<?>, WrapperComponentType<?>> REGISTRY = new HashMap<>();

    WrapperComponentType<CompoundTag> CUSTOM_DATA = define(ComponentTypes.CUSTOM_DATA, ComponentVisitor.In::visitAtCustomData, ComponentVisitor::visitCustomData);
    WrapperComponentType<Integer> MAX_STACK_SIZE = define(ComponentTypes.MAX_STACK_SIZE, ComponentVisitor.In::visitAtMaxStackSize, ComponentVisitor::visitMaxStackSize);
    WrapperComponentType<Integer> MAX_DAMAGE = define(ComponentTypes.MAX_DAMAGE, ComponentVisitor.In::visitAtMaxDamage, ComponentVisitor::visitMaxDamage);
    WrapperComponentType<Integer> DAMAGE = define(ComponentTypes.DAMAGE, ComponentVisitor.In::visitAtDamage, ComponentVisitor::visitDamage);
    WrapperComponentType<Boolean> UNBREAKABLE = define(ComponentTypes.UNBREAKABLE, ComponentVisitor.In::visitAtUnbreakable, ComponentVisitor::visitUnbreakable);
    WrapperComponentType<Component> CUSTOM_NAME = define(ComponentTypes.CUSTOM_NAME, ComponentVisitor.In::visitAtCustomName, ComponentVisitor::visitCustomName);
    WrapperComponentType<Component> ITEM_NAME = define(ComponentTypes.ITEM_NAME, ComponentVisitor.In::visitAtItemName, ComponentVisitor::visitItemName);
    WrapperComponentType<ItemLore> LORE = define(ComponentTypes.LORE, ComponentVisitor.In::visitAtLore, ComponentVisitor::visitLore);
    WrapperComponentType<ItemRarity> RARITY = define(ComponentTypes.RARITY, ComponentVisitor.In::visitAtRarity, ComponentVisitor::visitRarity);
    WrapperComponentType<ItemEnchantments> ENCHANTMENTS = define(ComponentTypes.ENCHANTMENTS, ComponentVisitor.In::visitAtEnchantments, ComponentVisitor::visitEnchantments);
    WrapperComponentType<ItemAdventurePredicate> CAN_PLACE_ON = define(ComponentTypes.CAN_PLACE_ON, ComponentVisitor.In::visitAtCanPlaceOn, ComponentVisitor::visitCanPlaceOn);
    WrapperComponentType<ItemAdventurePredicate> CAN_BREAK = define(ComponentTypes.CAN_BREAK, ComponentVisitor.In::visitAtCanBreak, ComponentVisitor::visitCanBreak);
    WrapperComponentType<ItemAttributeModifiers> ATTRIBUTE_MODIFIERS = define(ComponentTypes.ATTRIBUTE_MODIFIERS, ComponentVisitor.In::visitAtAttributeModifiers, ComponentVisitor::visitAttributeModifiers);
    WrapperComponentType<ItemCustomModelData> CUSTOM_MODEL_DATA_LISTS = define(ComponentTypes.CUSTOM_MODEL_DATA_LISTS, ComponentVisitor.In::visitAtCustomModelDataLists, ComponentVisitor::visitCustomModelDataLists);
    @Deprecated
    WrapperComponentType<Integer> CUSTOM_MODEL_DATA = define(ComponentTypes.CUSTOM_MODEL_DATA, ComponentVisitor.In::visitAtCustomModelData, ComponentVisitor::visitCustomModelData);
    WrapperComponentType<Dummy> HIDE_ADDITIONAL_TOOLTIP = define(ComponentTypes.HIDE_ADDITIONAL_TOOLTIP, ComponentVisitor.In::visitAtHideAdditionalTooltip, ComponentVisitor::visitHideAdditionalTooltip);
    WrapperComponentType<Dummy> HIDE_TOOLTIP = define(ComponentTypes.HIDE_TOOLTIP, ComponentVisitor.In::visitAtHideTooltip, ComponentVisitor::visitHideTooltip);
    WrapperComponentType<Dummy> REPAIR_COST = define(ComponentTypes.REPAIR_COST, ComponentVisitor.In::visitAtRepairCost, ComponentVisitor::visitRepairCost);
    WrapperComponentType<Dummy> CREATIVE_SLOT_LOCK = define(ComponentTypes.CREATIVE_SLOT_LOCK, ComponentVisitor.In::visitAtCreativeSlotLock, ComponentVisitor::visitCreativeSlotLock);
    WrapperComponentType<Dummy> ENCHANTMENT_GLINT_OVERRIDE = define(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, ComponentVisitor.In::visitAtEnchantmentGlintOverride, ComponentVisitor::visitEnchantmentGlintOverride);
    WrapperComponentType<Dummy> INTANGIBLE_PROJECTILE = define(ComponentTypes.INTANGIBLE_PROJECTILE, ComponentVisitor.In::visitAtIntagibleProjectiles, ComponentVisitor::visitIntagibleProjectiles);
    WrapperComponentType<FoodProperties> FOOD = define(ComponentTypes.FOOD, ComponentVisitor.In::visitAtFood, ComponentVisitor::visitFood);
    WrapperComponentType<Dummy> FIRE_RESISTANT = define(ComponentTypes.FIRE_RESISTANT, ComponentVisitor.In::visitAtFireResistant, ComponentVisitor::visitFireResistant);
    WrapperComponentType<ItemTool> TOOL = define(ComponentTypes.TOOL, ComponentVisitor.In::visitAtTool, ComponentVisitor::visitTool);
    WrapperComponentType<ItemEnchantments> STORED_ENCHANTMENTS = define(ComponentTypes.STORED_ENCHANTMENTS, ComponentVisitor.In::visitAtStoredEnchantments, ComponentVisitor::visitStoredEnchantments);
    WrapperComponentType<ItemDyeColor> DYED_COLOR = define(ComponentTypes.DYED_COLOR, ComponentVisitor.In::visitAtDyedColor, ComponentVisitor::visitDyedColor);
    WrapperComponentType<Integer> MAP_COLOR = define(ComponentTypes.MAP_COLOR, ComponentVisitor.In::visitAtMapColor, ComponentVisitor::visitMapColor);
    WrapperComponentType<Integer> MAP_ID = define(ComponentTypes.MAP_ID, ComponentVisitor.In::visitAtMapId, ComponentVisitor::visitMapId);
    WrapperComponentType<ItemMapDecorations> MAP_DECORATIONS = define(ComponentTypes.MAP_DECORATIONS, ComponentVisitor.In::visitAtMapDecorations, ComponentVisitor::visitMapDecorations);
    WrapperComponentType<ItemMapPostProcessingState> MAP_POST_PROCESSING = define(ComponentTypes.MAP_POST_PROCESSING, ComponentVisitor.In::visitAtMapPostProcessing, ComponentVisitor::visitMapPostProcessing);
    WrapperComponentType<ChargedProjectiles> CHARGED_PROJECTILES = define(ComponentTypes.CHARGED_PROJECTILES, ComponentVisitor.In::visitAtChargedProjectiles, ComponentVisitor::visitChargedProjectiles);
    WrapperComponentType<BundleContents> BUNDLE_CONTENTS = define(ComponentTypes.BUNDLE_CONTENTS, ComponentVisitor.In::visitAtBundleContents, ComponentVisitor::visitBundleContents);
    WrapperComponentType<ItemPotionContents> POTION_CONTENTS = define(ComponentTypes.POTION_CONTENTS, ComponentVisitor.In::visitAtPotionContents, ComponentVisitor::visitPotionContents);
    WrapperComponentType<SuspiciousStewEffects> SUSPICIOUS_STEW_EFFECTS = define(ComponentTypes.SUSPICIOUS_STEW_EFFECTS, ComponentVisitor.In::visitAtSuspiciousStewEffects, ComponentVisitor::visitSuspiciousStewEffects);
    WrapperComponentType<WritableBookContent> WRITABLE_BOOK_CONTENT = define(ComponentTypes.WRITABLE_BOOK_CONTENT, ComponentVisitor.In::visitAtWritableBookContent, ComponentVisitor::visitWritableBookContent);
    WrapperComponentType<WrittenBookContent> WRITTEN_BOOK_CONTENT = define(ComponentTypes.WRITTEN_BOOK_CONTENT, ComponentVisitor.In::visitAtWrittenBookContent, ComponentVisitor::visitWrittenBookContent);
    WrapperComponentType<ArmorTrim> TRIM = define(ComponentTypes.TRIM, ComponentVisitor.In::visitAtTrim, ComponentVisitor::visitTrim);
    WrapperComponentType<DebugStickState> DEBUG_STICK_STATE = define(ComponentTypes.DEBUG_STICK_STATE, ComponentVisitor.In::visitAtDebugStickState, ComponentVisitor::visitDebugStickState);
    WrapperComponentType<CompoundTag> ENTITY_DATA = define(ComponentTypes.ENTITY_DATA, ComponentVisitor.In::visitAtEntityData, ComponentVisitor::visitEntityData);
    WrapperComponentType<CompoundTag> BUCKET_ENTITY_DATA = define(ComponentTypes.BUCKET_ENTITY_DATA, ComponentVisitor.In::visitAtBucketEntityData, ComponentVisitor::visitBucketEntityData);
    WrapperComponentType<CompoundTag> BLOCK_ENTITY_DATA = define(ComponentTypes.BLOCK_ENTITY_DATA, ComponentVisitor.In::visitAtBlockEntityData, ComponentVisitor::visitBlockEntityData);
    WrapperComponentType<Instrument> INSTRUMENT = define(ComponentTypes.INSTRUMENT, ComponentVisitor.In::visitAtInstrument, ComponentVisitor::visitInstrument);
    WrapperComponentType<Integer> OMINOUS_BOTTLE_AMPLIFIER = define(ComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, ComponentVisitor.In::visitAtOmniousBottleAmplifier, ComponentVisitor::visitOmniousBottleAmplifier);
    WrapperComponentType<ItemRecipes> RECIPES = define(ComponentTypes.RECIPES, ComponentVisitor.In::visitAtRecipes, ComponentVisitor::visitRecipes);
    WrapperComponentType<LodestoneTracker> LODESTONE_TRACKER = define(ComponentTypes.LODESTONE_TRACKER, ComponentVisitor.In::visitAtLodestoneTracker, ComponentVisitor::visitLodestoneTracker);
    WrapperComponentType<FireworkExplosion> FIREWORK_EXPLOSION = define(ComponentTypes.FIREWORK_EXPLOSION, ComponentVisitor.In::visitAtFireworkExplosion, ComponentVisitor::visitFireworkExplosion);
    WrapperComponentType<ItemFireworks> FIREWORKS = define(ComponentTypes.FIREWORKS, ComponentVisitor.In::visitAtFireworks, ComponentVisitor::visitFireworks);
    WrapperComponentType<ItemProfile> PROFILE = define(ComponentTypes.PROFILE, ComponentVisitor.In::visitAtProfile, ComponentVisitor::visitProfile);
    WrapperComponentType<ResourceLocation> NOTE_BLOCK_SOUND = define(ComponentTypes.NOTE_BLOCK_SOUND, ComponentVisitor.In::visitAtNoteBlockSound, ComponentVisitor::visitNoteBlockSound);
    WrapperComponentType<BannerLayers> BANNER_PATTERNS = define(ComponentTypes.BANNER_PATTERNS, ComponentVisitor.In::visitAtBannerPatterns, ComponentVisitor::visitBannerPatterns);
    WrapperComponentType<DyeColor> BASE_COLOR = define(ComponentTypes.BASE_COLOR, ComponentVisitor.In::visitAtBaseColor, ComponentVisitor::visitBaseColor);
    WrapperComponentType<PotDecorations> POT_DECORATIONS = define(ComponentTypes.POT_DECORATIONS, ComponentVisitor.In::visitAtPotDecorations, ComponentVisitor::visitPotDecorations);
    WrapperComponentType<ItemContainerContents> CONTAINER = define(ComponentTypes.CONTAINER, ComponentVisitor.In::visitAtContainer, ComponentVisitor::visitContainer);
    WrapperComponentType<ItemBlockStateProperties> BLOCK_STATE = define(ComponentTypes.BLOCK_STATE, ComponentVisitor.In::visitAtBlockState, ComponentVisitor::visitBlockState);
    WrapperComponentType<ItemBees> BEES = define(ComponentTypes.BEES, ComponentVisitor.In::visitAtBees, ComponentVisitor::visitBees);
    WrapperComponentType<ItemLock> LOCK = define(ComponentTypes.LOCK, ComponentVisitor.In::visitAtLock, ComponentVisitor::visitLock);
    WrapperComponentType<ItemContainerLoot> CONTAINER_LOOT = define(ComponentTypes.CONTAINER_LOOT, ComponentVisitor.In::visitAtContainerLoot, ComponentVisitor::visitContainerLoot);
    WrapperComponentType<ItemJukeboxPlayable> JUKEBOX_PLAYABLE = define(ComponentTypes.JUKEBOX_PLAYABLE, ComponentVisitor.In::visitAtJukeboxPlayable, ComponentVisitor::visitJukeboxPlayable);
    WrapperComponentType<ItemConsumable> CONSUMABLE = define(ComponentTypes.CONSUMABLE, ComponentVisitor.In::visitAtConsumable, ComponentVisitor::visitConsumable);
    WrapperComponentType<ItemUseRemainder> USE_REMAINDER = define(ComponentTypes.USE_REMAINDER, ComponentVisitor.In::visitAtUseRemainder, ComponentVisitor::visitUseRemainder);
    WrapperComponentType<ItemUseCooldown> USE_COOLDOWN = define(ComponentTypes.USE_COOLDOWN, ComponentVisitor.In::visitAtUseCooldown, ComponentVisitor::visitUseCooldown);
    WrapperComponentType<ItemEnchantable> ENCHANTABLE = define(ComponentTypes.ENCHANTABLE, ComponentVisitor.In::visitAtEnchantable, ComponentVisitor::visitEnchantable);
    WrapperComponentType<ItemRepairable> REPAIRABLE = define(ComponentTypes.REPAIRABLE, ComponentVisitor.In::visitAtRepairable, ComponentVisitor::visitRepairable);
    WrapperComponentType<ItemModel> ITEM_MODEL = define(ComponentTypes.ITEM_MODEL, ComponentVisitor.In::visitAtItemModel, ComponentVisitor::visitItemModel);
    WrapperComponentType<ItemDamageResistant> DAMAGE_RESISTANT = define(ComponentTypes.DAMAGE_RESISTANT, ComponentVisitor.In::visitAtDamageResistant, ComponentVisitor::visitDamageResistant);
    WrapperComponentType<ItemEquippable> EQUIPPABLE = define(ComponentTypes.EQUIPPABLE, ComponentVisitor.In::visitAtEquippable, ComponentVisitor::visitEquippable);
    WrapperComponentType<Dummy> GLIDER = define(ComponentTypes.GLIDER, ComponentVisitor.In::visitAtGlider, ComponentVisitor::visitGlider);
    WrapperComponentType<ItemDeathProtection> DEATH_PROTECTION = define(ComponentTypes.DEATH_PROTECTION, ComponentVisitor.In::visitAtDeathProtection, ComponentVisitor::visitDeathProtection);
    WrapperComponentType<ItemTooltipStyle> TOOLTIP_STYLE = define(ComponentTypes.TOOLTIP_STYLE, ComponentVisitor.In::visitAtTooltipStyle, ComponentVisitor::visitTooltipStyle);

    static <T> WrapperComponentType<T> define(ComponentType<?> type, InVisitorOperator<T> operator, BiConsumer<ComponentVisitor, T> consumer) {
        WrapperComponentType<T> wrapperComponentType = of(type, operator, consumer);
        REGISTRY.put(type, wrapperComponentType);
        return wrapperComponentType;
    }

    static <T> WrapperComponentType<T> of(ComponentType<?> type, InVisitorOperator<T> operator, BiConsumer<ComponentVisitor, T> consumer) {
        return new WrapperComponentType<T>() {

            @Override
            public T visitAt(ComponentVisitor.In in, PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                return operator.visitAt(in, serializer);
            }

            @Override
            public ComponentType<?> delegate() {
                return type;
            }

            @Override
            public void visit(ComponentVisitor visitor, T data) {
                consumer.accept(visitor, data);
            }
        };
    }

    ComponentType<?> delegate();

    void visit(ComponentVisitor visitor, T data);
}
