package com.enderboy9217.werewolves.sounds;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import static com.enderboy9217.werewolves.EndersWerewolves.LOGGER;
import static com.enderboy9217.werewolves.EndersWerewolves.MOD_ID;

public class ModSounds {

    public static final SoundEvent WOLF_HOWL = registerSoundEvent("wolf_howl");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        LOGGER.info("Registered Mod Sounds for " + MOD_ID);
    }

}
