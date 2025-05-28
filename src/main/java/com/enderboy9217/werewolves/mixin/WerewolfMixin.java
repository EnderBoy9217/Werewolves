package com.enderboy9217.werewolves.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.enderboy9217.werewolves.EndersWerewolves.LOGGER;
import static com.enderboy9217.werewolves.EndersWerewolves.MOD_ID;

@Mixin(MinecraftServer.class)
public class WerewolfMixin {
	@Unique
	private boolean alreadyCheckedTonight = false;

	@Unique
	private boolean isFullMoon = false;

	@Unique
	private void playHowl(MinecraftServer server, ServerWorld world) {
		Predicate<WolfEntity> isUntamed = wolf -> !wolf.isTamed();
		List<WolfEntity> wolves = new ArrayList<>();

		for (Entity entity : world.iterateEntities()) {
			if (entity instanceof WolfEntity wolf && ( !wolf.isTamed() )) {
				wolves.add(wolf);
			}
		}

		Identifier soundIdentifier = new Identifier(MOD_ID, "wolf_howl");
		RegistryEntry<SoundEvent> soundEntry = Registries.SOUND_EVENT.getEntry(Registries.SOUND_EVENT.get(soundIdentifier));

		for (WolfEntity wolf : wolves) {
			double x = wolf.getX();
			double y = wolf.getY();
			double z = wolf.getZ();
			LOGGER.info("Playing howl");

			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				// Only play sound for players within 256 blocks
				if (player.squaredDistanceTo(wolf) < 256 * 256) {
					float pitch = 0.8f + (world.getRandom().nextFloat() * 0.2f); // Between 0.8 and 1.0
					PlaySoundS2CPacket packet = new PlaySoundS2CPacket(
							soundEntry,
							SoundCategory.NEUTRAL,
							x, y, z,
							3.0f, pitch,
							server.getOverworld().getRandom().nextLong()
					);

					player.networkHandler.sendPacket(packet);
				}
			}
		}
	}

	@Inject(method = "tickWorlds", at = @At("HEAD"))
	private void onNightTick(CallbackInfo info) {
		MinecraftServer server = (MinecraftServer) (Object) this;

		for (ServerWorld world : server.getWorlds()) {
			if (world.getDimension().hasSkyLight()) {
				long timeOfDay = world.getTimeOfDay() % 24000;
				if (!alreadyCheckedTonight && timeOfDay >= 13000) {
					isFullMoon = (world.getMoonPhase() == 0);
					if (isFullMoon) {
						LOGGER.info("Wolves will now be angered by the full moon.");
						alreadyCheckedTonight = true;

						// Play Howl Sound
						playHowl(server, world);
					}
					alreadyCheckedTonight = true;
				}
				if (isFullMoon && timeOfDay%20==0) {

					if (world.getDifficulty() != Difficulty.PEACEFUL) {

						Predicate<WolfEntity> isUntamed = wolf -> !wolf.isTamed();

						// For each player in the world
						for (PlayerEntity player : world.getPlayers()) {
							if (player.isCreative()) continue;

							BlockPos pos = player.getBlockPos();
							Box area = new Box(pos).expand(8); // 16^3 Region around the player

							List<WolfEntity> wolves = world.getEntitiesByClass(WolfEntity.class, area, isUntamed);
                            //LOGGER.info("Found {} wolves near player {}", wolves.size(), player.getEntityName());

							for (WolfEntity wolf : wolves) {
								if (wolf.getAngryAt() == player.getUuid()) {
									continue;
								}
								wolf.setTarget(player);
								wolf.setAngryAt(player.getUuid());
								wolf.setAngerTime(11000);
								wolf.setAttacker(player);
							}
						}
					}
				}

				if (timeOfDay < 13000) {
					alreadyCheckedTonight = false;
					isFullMoon = false;
				}
			}
		}
	}
}