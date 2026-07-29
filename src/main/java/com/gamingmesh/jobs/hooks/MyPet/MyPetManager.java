package com.gamingmesh.jobs.hooks.MyPet;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.Pet;

public class MyPetManager {

    private final boolean legacyApi;
    private final LegacyApi legacy;

    public MyPetManager() {
        boolean legacyApi = false;
        LegacyApi legacy = null;

        try {
            MyPetApi.class.getMethod("getPetManager");
        } catch (NoSuchMethodException ignored) {
            legacyApi = true;
            legacy = LegacyApi.create();
        }

        this.legacyApi = legacyApi;
        this.legacy = legacy;
    }

    public boolean isMyPet(Entity entity, Player owner) {
        UUID petOwner = getOwner(entity);
        return petOwner != null && (owner == null || petOwner.equals(owner.getUniqueId()));
    }

    public UUID getOwnerOfPet(Entity entity) {
        return getOwner(entity);
    }

    private UUID getOwner(Entity entity) {
        if (legacyApi) {
            return legacy == null ? null : legacy.getOwner(entity);
        }
        return ModernApi.getOwner(entity);
    }

    private static final class ModernApi {

        private static UUID getOwner(Entity entity) {
            Pet pet = MyPetApi.getPetManager().getPetFromEntity(entity);
            return pet == null || pet.getOwner() == null ? null : pet.getOwner().getUniqueId();
        }
    }

    private static final class LegacyApi {

        private final Class<?> petEntityClass;
        private final Method getMyPet;
        private final Method getOwner;
        private final Method getPlayer;

        private LegacyApi(Class<?> petEntityClass, Method getMyPet, Method getOwner, Method getPlayer) {
            this.petEntityClass = petEntityClass;
            this.getMyPet = getMyPet;
            this.getOwner = getOwner;
            this.getPlayer = getPlayer;
        }

        private static LegacyApi create() {
            try {
                ClassLoader classLoader = MyPetApi.class.getClassLoader();
                Class<?> petEntityClass = Class.forName("de.Keyle.MyPet.api.entity.MyPetBukkitEntity", false,
                    classLoader);
                Class<?> myPetClass = Class.forName("de.Keyle.MyPet.api.entity.MyPet", false, classLoader);
                Class<?> myPetPlayerClass = Class.forName("de.Keyle.MyPet.api.player.MyPetPlayer", false,
                    classLoader);

                return new LegacyApi(
                    petEntityClass,
                    petEntityClass.getMethod("getMyPet"),
                    myPetClass.getMethod("getOwner"),
                    myPetPlayerClass.getMethod("getPlayer"));
            } catch (ReflectiveOperationException e) {
                MyPetApi.getLogger().warning("Jobs could not initialize its MyPet 3 integration: " + e.getMessage());
                return null;
            }
        }

        private UUID getOwner(Entity entity) {
            if (!petEntityClass.isInstance(entity)) {
                return null;
            }

            try {
                Object pet = getMyPet.invoke(entity);
                if (pet == null) {
                    return null;
                }

                Object owner = getOwner.invoke(pet);
                if (owner == null) {
                    return null;
                }

                Object player = getPlayer.invoke(owner);
                return player instanceof Player ? ((Player) player).getUniqueId() : null;
            } catch (ReflectiveOperationException | ClassCastException e) {
                return null;
            }
        }
    }
}
