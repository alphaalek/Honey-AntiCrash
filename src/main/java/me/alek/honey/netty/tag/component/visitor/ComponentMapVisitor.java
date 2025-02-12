package me.alek.honey.netty.tag.component.visitor;

import com.github.retrooper.packetevents.protocol.color.DyeColor;
import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.builtin.item.*;
import com.github.retrooper.packetevents.protocol.item.instrument.Instrument;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Dummy;
import me.alek.honey.netty.codec.CodecVisitor;
import me.alek.honey.netty.codec.PacketDataSerializer;
import me.alek.honey.netty.tag.FallbackDefaultCodecException;
import me.alek.honey.netty.tag.component.ComponentMap;
import me.alek.honey.netty.tag.nbt.CompoundTag;
import me.alek.honey.util.protocol.NetProtocol;
import net.kyori.adventure.text.Component;

public interface ComponentMapVisitor extends CodecVisitor {

    abstract class Adapter implements ComponentMapVisitor {

        @Override
        public void visitAbsent(ComponentType<?> type) {

        }

        @Override
        public void visitFinal(ComponentMap map) {

        }

        @Override
        public void visitCustomData(CompoundTag val) {

        }

        @Override
        public void visitMaxStackSize(int val) {

        }

        @Override
        public void visitMaxDamage(int val) {

        }

        @Override
        public void visitDamage(int val) {

        }

        @Override
        public void visitUnbreakable(boolean val) {

        }

        @Override
        public void visitCustomName(Component val) {

        }

        @Override
        public void visitItemName(Component val) {

        }

        @Override
        public void visitLore(ItemLore val) {

        }

        @Override
        public void visitRarity(ItemRarity val) {

        }

        @Override
        public void visitEnchantments(ItemEnchantments val) {

        }

        @Override
        public void visitCanPlaceOn(ItemAdventurePredicate val) {

        }

        @Override
        public void visitCanBreak(ItemAdventurePredicate val) {

        }

        @Override
        public void visitAttributeModifiers(ItemAttributeModifiers val) {

        }

        @Override
        public void visitCustomModelDataLists(ItemCustomModelData val) {

        }

        @Override
        public void visitCustomModelData(int val) {

        }

        @Override
        public void visitHideAdditionalTooltip(Dummy val) {

        }

        @Override
        public void visitHideTooltip(Dummy val) {

        }

        @Override
        public void visitRepairCost(Dummy val) {

        }

        @Override
        public void visitCreativeSlotLock(Dummy val) {

        }

        @Override
        public void visitEnchantmentGlintOverride(Dummy val) {

        }

        @Override
        public void visitIntagibleProjectiles(Dummy val) {

        }

        @Override
        public void visitFood(FoodProperties val) {

        }

        @Override
        public void visitFireResistant(Dummy val) {

        }

        @Override
        public void visitTool(ItemTool val) {

        }

        @Override
        public void visitStoredEnchantments(ItemEnchantments val) {

        }

        @Override
        public void visitDyedColor(ItemDyeColor val) {

        }

        @Override
        public void visitMapColor(int val) {

        }

        @Override
        public void visitMapId(int val) {

        }

        @Override
        public void visitMapDecorations(ItemMapDecorations val) {

        }

        @Override
        public void visitMapPostProcessing(ItemMapPostProcessingState val) {

        }

        @Override
        public void visitChargedProjectiles(ChargedProjectiles val) {

        }

        @Override
        public void visitBundleContents(BundleContents val) {

        }

        @Override
        public void visitPotionContents(ItemPotionContents val) {

        }

        @Override
        public void visitSuspiciousStewEffects(SuspiciousStewEffects val) {

        }

        @Override
        public void visitWritableBookContent(WritableBookContent val) {

        }

        @Override
        public void visitWrittenBookContent(WrittenBookContent val) {

        }

        @Override
        public void visitTrim(ArmorTrim val) {

        }

        @Override
        public void visitDebugStickState(DebugStickState val) {

        }

        @Override
        public void visitEntityData(CompoundTag val) {

        }

        @Override
        public void visitBucketEntityData(CompoundTag val) {

        }

        @Override
        public void visitBlockEntityData(CompoundTag val) {

        }

        @Override
        public void visitInstrument(Instrument val) {

        }

        @Override
        public void visitOmniousBottleAmplifier(int val) {

        }

        @Override
        public void visitRecipes(ItemRecipes val) {

        }

        @Override
        public void visitLodestoneTracker(LodestoneTracker val) {

        }

        @Override
        public void visitFireworkExplosion(FireworkExplosion val) {

        }

        @Override
        public void visitFireworks(ItemFireworks val) {

        }

        @Override
        public void visitProfile(ItemProfile val) {

        }

        @Override
        public void visitNoteBlockSound(ResourceLocation val) {

        }

        @Override
        public void visitBannerPatterns(BannerLayers val) {

        }

        @Override
        public void visitBaseColor(DyeColor val) {

        }

        @Override
        public void visitPotDecorations(PotDecorations val) {

        }

        @Override
        public void visitContainer(ItemContainerContents val) {

        }

        @Override
        public void visitBlockState(ItemBlockStateProperties val) {

        }

        @Override
        public void visitBees(ItemBees val) {

        }

        @Override
        public void visitLock(ItemLock val) {

        }

        @Override
        public void visitContainerLoot(ItemContainerLoot val) {

        }

        @Override
        public void visitJukeboxPlayable(ItemJukeboxPlayable val) {

        }

        @Override
        public void visitConsumable(ItemConsumable val) {

        }

        @Override
        public void visitUseRemainder(ItemUseRemainder val) {

        }

        @Override
        public void visitUseCooldown(ItemUseCooldown val) {

        }

        @Override
        public void visitEnchantable(ItemEnchantable val) {

        }

        @Override
        public void visitRepairable(ItemRepairable val) {

        }

        @Override
        public void visitItemModel(ItemModel val) {

        }

        @Override
        public void visitDamageResistant(ItemDamageResistant val) {

        }

        @Override
        public void visitEquippable(ItemEquippable val) {

        }

        @Override
        public void visitGlider(Dummy val) {

        }

        @Override
        public void visitDeathProtection(ItemDeathProtection val) {

        }

        @Override
        public void visitTooltipStyle(ItemTooltipStyle val) {

        }
    }

    void visitAbsent(ComponentType<?> type);

    void visitFinal(ComponentMap map);

    void visitCustomData(CompoundTag val);

    void visitMaxStackSize(int val);

    void visitMaxDamage(int val);

    void visitDamage(int val);

    void visitUnbreakable(boolean val);

    void visitCustomName(Component val);

    void visitItemName(Component val);

    void visitLore(ItemLore val);

    void visitRarity(ItemRarity val);

    void visitEnchantments(ItemEnchantments val);

    void visitCanPlaceOn(ItemAdventurePredicate val);

    void visitCanBreak(ItemAdventurePredicate val);

    void visitAttributeModifiers(ItemAttributeModifiers val);

    void visitCustomModelDataLists(ItemCustomModelData val);

    void visitCustomModelData(int val);

    void visitHideAdditionalTooltip(Dummy val);

    void visitHideTooltip(Dummy val);

    void visitRepairCost(Dummy val);

    void visitCreativeSlotLock(Dummy val);

    void visitEnchantmentGlintOverride(Dummy val);

    void visitIntagibleProjectiles(Dummy val);

    void visitFood(FoodProperties val);

    void visitFireResistant(Dummy val);

    void visitTool(ItemTool val);

    void visitStoredEnchantments(ItemEnchantments val);

    void visitDyedColor(ItemDyeColor val);

    void visitMapColor(int val);

    void visitMapId(int val);

    void visitMapDecorations(ItemMapDecorations val);

    void visitMapPostProcessing(ItemMapPostProcessingState val);

    void visitChargedProjectiles(ChargedProjectiles val);

    void visitBundleContents(BundleContents val);

    void visitPotionContents(ItemPotionContents val);

    void visitSuspiciousStewEffects(SuspiciousStewEffects val);

    void visitWritableBookContent(WritableBookContent val);

    void visitWrittenBookContent(WrittenBookContent val);

    void visitTrim(ArmorTrim val);

    void visitDebugStickState(DebugStickState val);

    void visitEntityData(CompoundTag val);

    void visitBucketEntityData(CompoundTag val);

    void visitBlockEntityData(CompoundTag val);

    void visitInstrument(Instrument val);

    void visitOmniousBottleAmplifier(int val);

    void visitRecipes(ItemRecipes val);

    void visitLodestoneTracker(LodestoneTracker val);

    void visitFireworkExplosion(FireworkExplosion val);

    void visitFireworks(ItemFireworks val);

    void visitProfile(ItemProfile val);

    void visitNoteBlockSound(ResourceLocation val);

    void visitBannerPatterns(BannerLayers val);

    void visitBaseColor(DyeColor val);

    void visitPotDecorations(PotDecorations val);

    void visitContainer(ItemContainerContents val);

    void visitBlockState(ItemBlockStateProperties val);

    void visitBees(ItemBees val);

    void visitLock(ItemLock val);

    void visitContainerLoot(ItemContainerLoot val);

    void visitJukeboxPlayable(ItemJukeboxPlayable val);

    void visitConsumable(ItemConsumable val);

    void visitUseRemainder(ItemUseRemainder val);

    void visitUseCooldown(ItemUseCooldown val);

    void visitEnchantable(ItemEnchantable val);

    void visitRepairable(ItemRepairable val);

    void visitItemModel(ItemModel val);

    void visitDamageResistant(ItemDamageResistant val);

    void visitEquippable(ItemEquippable val);

    void visitGlider(Dummy val);

    void visitDeathProtection(ItemDeathProtection val);

    void visitTooltipStyle(ItemTooltipStyle val);

    interface In extends ComponentMapVisitor {

        static In createDefault(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            return new Adapter() {
                @Override
                public ItemType type() {
                    return type;
                }

                @Override
                public NetProtocol protocol() {
                    return protocol;
                }

                @Override
                public NetProtocol protocolCreated() {
                    return protocolCreated;
                }
            };
        }

        abstract class Adapter extends ComponentMapVisitor.Adapter implements In {

            @Override
            public void visitAt(PacketDataSerializer serializer, int present, int absent) {

            }

            @Override
            public CompoundTag visitAtCustomData(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtMaxStackSize(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtMaxDamage(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtDamage(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public boolean visitAtUnbreakable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Component visitAtCustomName(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Component visitAtItemName(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemLore visitAtLore(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemRarity visitAtRarity(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemEnchantments visitAtEnchantments(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemAdventurePredicate visitAtCanPlaceOn(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemAdventurePredicate visitAtCanBreak(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemAttributeModifiers visitAtAttributeModifiers(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemCustomModelData visitAtCustomModelDataLists(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtCustomModelData(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtHideAdditionalTooltip(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtHideTooltip(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtRepairCost(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtCreativeSlotLock(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtEnchantmentGlintOverride(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtIntagibleProjectiles(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public FoodProperties visitAtFood(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtFireResistant(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemTool visitAtTool(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemEnchantments visitAtStoredEnchantments(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemDyeColor visitAtDyedColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtMapColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtMapId(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemMapDecorations visitAtMapDecorations(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemMapPostProcessingState visitAtMapPostProcessing(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ChargedProjectiles visitAtChargedProjectiles(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public BundleContents visitAtBundleContents(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemPotionContents visitAtPotionContents(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public SuspiciousStewEffects visitAtSuspiciousStewEffects(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public WritableBookContent visitAtWritableBookContent(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public WrittenBookContent visitAtWrittenBookContent(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ArmorTrim visitAtTrim(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public DebugStickState visitAtDebugStickState(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public CompoundTag visitAtEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public CompoundTag visitAtBucketEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public CompoundTag visitAtBlockEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Instrument visitAtInstrument(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public int visitAtOmniousBottleAmplifier(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemRecipes visitAtRecipes(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public LodestoneTracker visitAtLodestoneTracker(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public FireworkExplosion visitAtFireworkExplosion(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemFireworks visitAtFireworks(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemProfile visitAtProfile(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ResourceLocation visitAtNoteBlockSound(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public BannerLayers visitAtBannerPatterns(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public DyeColor visitAtBaseColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public PotDecorations visitAtPotDecorations(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemContainerContents visitAtContainer(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemBlockStateProperties visitAtBlockState(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemBees visitAtBees(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemLock visitAtLock(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemContainerLoot visitAtContainerLoot(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemJukeboxPlayable visitAtJukeboxPlayable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemConsumable visitAtConsumable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemUseRemainder visitAtUseRemainder(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemUseCooldown visitAtUseCooldown(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemEnchantable visitAtEnchantable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemRepairable visitAtRepairable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemModel visitAtItemModel(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemDamageResistant visitAtDamageResistant(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemEquippable visitAtEquippable(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public Dummy visitAtGlider(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemDeathProtection visitAtDeathProtection(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }

            @Override
            public ItemTooltipStyle visitAtTooltipStyle(PacketDataSerializer serializer) throws FallbackDefaultCodecException {
                throw new FallbackDefaultCodecException();
            }
        }

        void visitAt(PacketDataSerializer serializer, int present, int absent);

        CompoundTag visitAtCustomData(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtMaxStackSize(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtMaxDamage(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtDamage(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        boolean visitAtUnbreakable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Component visitAtCustomName(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Component visitAtItemName(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemLore visitAtLore(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemRarity visitAtRarity(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemEnchantments visitAtEnchantments(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemAdventurePredicate visitAtCanPlaceOn(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemAdventurePredicate visitAtCanBreak(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemAttributeModifiers visitAtAttributeModifiers(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemCustomModelData visitAtCustomModelDataLists(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        @Deprecated
        int visitAtCustomModelData(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtHideAdditionalTooltip(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtHideTooltip(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtRepairCost(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtCreativeSlotLock(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtEnchantmentGlintOverride(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtIntagibleProjectiles(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        FoodProperties visitAtFood(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtFireResistant(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemTool visitAtTool(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemEnchantments visitAtStoredEnchantments(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemDyeColor visitAtDyedColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtMapColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtMapId(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemMapDecorations visitAtMapDecorations(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemMapPostProcessingState visitAtMapPostProcessing(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ChargedProjectiles visitAtChargedProjectiles(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        BundleContents visitAtBundleContents(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemPotionContents visitAtPotionContents(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        SuspiciousStewEffects visitAtSuspiciousStewEffects(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        WritableBookContent visitAtWritableBookContent(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        WrittenBookContent visitAtWrittenBookContent(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ArmorTrim visitAtTrim(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        DebugStickState visitAtDebugStickState(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        CompoundTag visitAtEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        CompoundTag visitAtBucketEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        CompoundTag visitAtBlockEntityData(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Instrument visitAtInstrument(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        int visitAtOmniousBottleAmplifier(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemRecipes visitAtRecipes(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        LodestoneTracker visitAtLodestoneTracker(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        FireworkExplosion visitAtFireworkExplosion(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemFireworks visitAtFireworks(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemProfile visitAtProfile(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ResourceLocation visitAtNoteBlockSound(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        BannerLayers visitAtBannerPatterns(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        DyeColor visitAtBaseColor(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        PotDecorations visitAtPotDecorations(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemContainerContents visitAtContainer(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemBlockStateProperties visitAtBlockState(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemBees visitAtBees(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemLock visitAtLock(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemContainerLoot visitAtContainerLoot(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemJukeboxPlayable visitAtJukeboxPlayable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemConsumable visitAtConsumable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemUseRemainder visitAtUseRemainder(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemUseCooldown visitAtUseCooldown(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemEnchantable visitAtEnchantable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemRepairable visitAtRepairable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemModel visitAtItemModel(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemDamageResistant visitAtDamageResistant(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemEquippable visitAtEquippable(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        Dummy visitAtGlider(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemDeathProtection visitAtDeathProtection(PacketDataSerializer serializer) throws FallbackDefaultCodecException;

        ItemTooltipStyle visitAtTooltipStyle(PacketDataSerializer serializer) throws FallbackDefaultCodecException;
    }

    interface Out extends ComponentMapVisitor {

        abstract class Adapter extends ComponentMapVisitor.Adapter implements Out {

        }

        static Out createDefault(NetProtocol protocol, NetProtocol protocolCreated, ItemType type) {
            return new Adapter() {
                @Override
                public ItemType type() {
                    return type;
                }

                @Override
                public NetProtocol protocol() {
                    return protocol;
                }

                @Override
                public NetProtocol protocolCreated() {
                    return protocolCreated;
                }
            };
        }
    }
}
