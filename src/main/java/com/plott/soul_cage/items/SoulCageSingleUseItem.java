package com.plott.soul_cage.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.TypedActionResult;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.plott.soul_cage.SoulCageMod;

public class SoulCageSingleUseItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    public SoulCageSingleUseItem(Item.Settings settings) {
        super(settings);
    }

    public ActionResult captureEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!player.getWorld().isClient) {
            String entityId = EntityType.getId(target.getType()).toString();
            LOGGER.info("Attempting to capture entity: " + entityId);

            if (SoulCageMod.enableWhitelist && !SoulCageMod.whitelist.contains(entityId)) {
                LOGGER.info("Capture failed: " + entityId + " is not in the whitelist.");
                return ActionResult.FAIL;
            }

            if (SoulCageMod.enableBlacklist && SoulCageMod.blacklist.contains(entityId)) {
                LOGGER.info("Capture failed: " + entityId + " is in the blacklist.");
                return ActionResult.FAIL;
            }

            if (player.getItemCooldownManager().isCoolingDown(this)) {
                LOGGER.info("Capture failed: Item is on cooldown.");
                return ActionResult.PASS;
            }

            NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

            if (nbt.contains("CapturedEntity")) {
                LOGGER.info("Capture failed: This SoulCage already contains a captured mob.");
                return ActionResult.FAIL;
            }

            try {
                NbtCompound entityTag = new NbtCompound();
                target.writeNbt(entityTag);

                nbt.putString("CapturedEntityType", entityId);
                nbt.put("CapturedEntity", entityTag);

                target.remove(Entity.RemovalReason.DISCARDED);

                LOGGER.info("Entity captured successfully with data: " + entityTag);

                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

                player.getItemCooldownManager().set(this, 20);

                return ActionResult.SUCCESS;
            } catch (Exception e) {
                LOGGER.error("Error capturing entity: " + e.getMessage());
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }

    // Release the captured mob and destroy the SoulCage after use
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

        // Check if player is holding a SoulCage with cooldown active
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            LOGGER.info("Use failed: Item is on cooldown.");
            return TypedActionResult.pass(stack);
        }

        if (nbt != null && nbt.contains("CapturedEntity") && nbt.contains("CapturedEntityType")) {
            if (!world.isClient) {
                try {
                    // Retrieve entity data
                    NbtCompound entityTag = nbt.getCompound("CapturedEntity");
                    String entityTypeString = nbt.getString("CapturedEntityType");
                    LOGGER.info("Releasing entity of type: " + entityTypeString);

                    Optional<EntityType<?>> optionalEntityType = Registries.ENTITY_TYPE
                            .getOrEmpty(Identifier.of(entityTypeString));
                    if (optionalEntityType.isPresent()) {
                        EntityType<?> entityType = optionalEntityType.get();
                        LivingEntity entity = (LivingEntity) entityType.create(world);
                        if (entity != null) {
                            entity.readNbt(entityTag);

                            // Use player's raycast method to find where they are looking
                            HitResult hitResult = player.raycast(5.0D, 1.0F, false); // 5 blocks distance

                            if (hitResult.getType() == HitResult.Type.BLOCK) {
                                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                                BlockPos targetPos = blockHitResult.getBlockPos().offset(blockHitResult.getSide());

                                // Set entity position to the location where the player is looking
                                entity.refreshPositionAndAngles(
                                        targetPos.getX() + 0.5,
                                        targetPos.getY(),
                                        targetPos.getZ() + 0.5,
                                        player.getYaw(),
                                        player.getPitch());

                                world.spawnEntity(entity); // Spawn the entity
                                LOGGER.info("Entity released successfully at " + targetPos.toShortString());

                                // Destroy the SoulCage after successful release by returning an empty stack
                                player.getItemCooldownManager().set(this, 20); // Apply cooldown
                                return TypedActionResult.success(ItemStack.EMPTY);
                            } else {
                                LOGGER.info("No valid block found to release entity.");
                            }
                        } else {
                            LOGGER.error("Failed to create entity from captured data.");
                        }
                    } else {
                        LOGGER.error("Entity type not found in registry: " + entityTypeString);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error releasing entity: " + e.getMessage());
                }

                // Apply a cooldown to prevent immediate re-triggering
                player.getItemCooldownManager().set(this, 20); // 20 ticks = 1 second cooldown

                return TypedActionResult.success(stack);
            }
        } else {
            LOGGER.info("No entity to release.");
        }

        return TypedActionResult.pass(stack);
    }

    // Tooltip to indicate if a mob is stored in the SoulCage
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt != null && nbt.contains("CapturedEntityType")) {
            String entityTypeString = nbt.getString("CapturedEntityType");
            tooltip.add(Text.literal("Captured Mob: " + entityTypeString));
        } else {
            tooltip.add(Text.literal("No Mob Captured"));
        }
    }
}
