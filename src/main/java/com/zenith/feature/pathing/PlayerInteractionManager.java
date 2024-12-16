package com.zenith.feature.pathing;

import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.world.World;
import com.zenith.feature.world.raycast.EntityRaycastResult;
import com.zenith.mc.block.Block;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.block.BlockState;
import com.zenith.mc.block.BlockTags;
import com.zenith.mc.enchantment.EnchantmentData;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ToolTag;
import com.zenith.mc.item.ToolTier;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.zenith.Shared.CACHE;

public class PlayerInteractionManager {
    private int destroyBlockPosX = -1;
    private int destroyBlockPosY = -1;
    private int destroyBlockPosZ = -1;
    private @Nullable ItemStack destroyingItem = Container.EMPTY_STACK;
    private double destroyProgress;
    private double destroyTicks;
    private int destroyDelay;
    private boolean isDestroying;
    private final PlayerSimulation player;

    public PlayerInteractionManager(final PlayerSimulation playerSimulation) {
        this.player = playerSimulation;
    }

    private boolean sameDestroyTarget(final int x, final int y, final int z) {
        ItemStack itemStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        return x == this.destroyBlockPosX && y == this.destroyBlockPosY && z == this.destroyBlockPosZ
            && Objects.equals(this.destroyingItem, itemStack);
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public boolean startDestroyBlock(final int x, final int y, final int z, Direction face) {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            Proxy.getInstance().getClient().sendAsync(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    x, y, z,
                    face,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                )
            );
            this.destroyDelay = 5;
        } else if (!this.isDestroying || !this.sameDestroyTarget(x, y, z)) {
            if (this.isDestroying) {
                Proxy.getInstance().getClient().sendAsync(
                    new ServerboundPlayerActionPacket(
                        PlayerAction.CANCEL_DIGGING,
                        this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                        face,
                        CACHE.getPlayerCache().getSeqId().incrementAndGet()
                    )
                );
            }

            BlockState blockState = World.getBlockState(x, y, z);
            if (blockState.block() == BlockRegistry.AIR || blockBreakSpeed(blockState) < 1.0) {
                this.isDestroying = true;
                this.destroyBlockPosX = x;
                this.destroyBlockPosY = y;
                this.destroyBlockPosZ = z;
                this.destroyingItem = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                this.destroyProgress = 0.0;
                this.destroyTicks = 0.0F;
            }

            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    x, y, z,
                    face,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()));
        }

        return true;
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            Proxy.getInstance().getClient()
                .send(new ServerboundPlayerActionPacket(
                    PlayerAction.CANCEL_DIGGING,
                    this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                    Direction.DOWN,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
        }
        this.isDestroying = false;
        this.destroyProgress = 0;
        this.destroyTicks = 0;
        this.destroyDelay = 0;
    }

    public boolean continueDestroyBlock(final int x, final int y, final int z, Direction directionFacing) {
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        } else if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = 5;
            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DIGGING,
                    x, y, z,
                    directionFacing,
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
            return true;
        } else if (this.sameDestroyTarget(x, y, z)) {
            BlockState blockState = World.getBlockState(x, y, z);
            if (blockState.block() == BlockRegistry.AIR) {
                this.isDestroying = false;
                return false;
            } else {
                this.destroyProgress += blockBreakSpeed(blockState);
                ++this.destroyTicks;
                if (this.destroyProgress >= 1.0F) {
                    this.isDestroying = false;
                    Proxy.getInstance().getClient().send(
                        new ServerboundPlayerActionPacket(
                            PlayerAction.FINISH_DIGGING,
                            x, y, z,
                            directionFacing,
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.destroyDelay = 5;
                }
                return true;
            }
        } else {
            return this.startDestroyBlock(x, y, z, directionFacing);
        }
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0 ? (int)(this.destroyProgress * 10.0) : -1;
    }

    public double blockBreakSpeed(BlockState state) {
        double destroySpeed = state.block().destroySpeed();
        double toolFactor = hasCorrectToolForDrops(state) ? 30.0 : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(state);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public boolean hasCorrectToolForDrops(BlockState state) {
        if (!state.block().requiresCorrectToolForDrops()) return true;
        var item = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (item == Container.EMPTY_STACK) return false;
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        var blockTags = state.block().blockTags();
        if (!blockTags.contains(toolTag.type().getBlockTag())) return false;
        if (blockTags.contains(BlockTags.NEEDS_STONE_TOOL)) {
            return toolTag.tier() == ToolTier.STONE || toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_IRON_TOOL)) {
            return toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        return true;
    }

    public boolean matchingTool(ItemStack item, Block block) {
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        return block.blockTags().contains(toolTag.type().getBlockTag());
    }

    public int getEnchantmentLevel(ItemStack item, EnchantmentData enchantmentData) {
        if (item == Container.EMPTY_STACK) return 0;
        DataComponents dataComponents = item.getDataComponents();
        if (dataComponents == null) return 0;
        ItemEnchantments itemEnchantments = dataComponents.get(DataComponentType.ENCHANTMENTS);
        if (itemEnchantments == null) return 0;
        if (!itemEnchantments.getEnchantments().containsKey(enchantmentData.id())) return 0;
        return itemEnchantments.getEnchantments().get(enchantmentData.id());
    }

    public double getPlayerDestroySpeed(BlockState state) {
        double speed = 1.0;
        var mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (mainHandStack != Container.EMPTY_STACK) {
            if (matchingTool(mainHandStack, state.block())) {
                ItemData itemData = ItemRegistry.REGISTRY.get(mainHandStack.getId());
                ToolTag toolTag = itemData.toolTag();
                speed = toolTag.tier().getSpeed();
            }
        }

        if (speed > 1.0) {
            float miningEfficiencyAttribute = player
                .getAttributeValue(AttributeType.Builtin.PLAYER_MINING_EFFICIENCY, 0);
            speed += miningEfficiencyAttribute;
        }

        boolean hasDigSpeedEffect = false;
        int hasteAmplifier = 0;
        var hasteEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.HASTE);
        if (hasteEffect != null) {
            hasDigSpeedEffect = true;
            hasteAmplifier = hasteEffect.getAmplifier();
        }
        int conduitPowerAmplifier = 0;
        var conduitPowerEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.CONDUIT_POWER);
        if (conduitPowerEffect != null) {
            hasDigSpeedEffect = true;
            conduitPowerAmplifier = conduitPowerEffect.getAmplifier();
        }

        if (hasDigSpeedEffect) {
            int digSpeedAmplification = Math.max(hasteAmplifier, conduitPowerAmplifier);
            speed *= 1.0 + (digSpeedAmplification + 1) * 0.2;
        }

        var miningFatigueEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.MINING_FATIGUE);

        if (miningFatigueEffect != null) {
            speed *= switch(miningFatigueEffect.getAmplifier()) {
                case 0 -> 0.3;
                case 1 -> 0.09;
                case 2 -> 0.0027;
                default -> 8.1E-4;
            };
        }

        speed *= player.getAttributeValue(AttributeType.Builtin.PLAYER_BLOCK_BREAK_SPEED, 1.0f);

        boolean isEyeInWater = World.isWater(
            World.getBlockAtBlockPos(
                MathHelper.floorI(player.getX()),
                MathHelper.floorI(player.getEyeY()),
                MathHelper.floorI(player.getZ())));
        if (isEyeInWater) {
            speed *= player.getAttributeValue(AttributeType.Builtin.PLAYER_SUBMERGED_MINING_SPEED, 0.2f);
        }

        if (!player.isOnGround()) {
            speed /= 5.0;
        }

        return speed;
    }

    public void attackEntity(final EntityRaycastResult entity) {
        Proxy.getInstance().getClient().sendAsync(new ServerboundInteractPacket(entity.entity().getEntityId(), InteractAction.ATTACK, false));
        Proxy.getInstance().getClient().sendAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }
}
