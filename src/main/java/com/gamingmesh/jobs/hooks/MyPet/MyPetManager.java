package com.gamingmesh.jobs.hooks.MyPet;

import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.player.MyPetPlayer;
import de.Keyle.MyPet.api.repository.PlayerManager;

public class MyPetManager {

    private final PlayerManager mp = MyPetApi.getPlayerManager();

    // MyPet 4 removed MyPetBukkitEntity. Resolve its replacement API at runtime
    // so the same Jobs build remains compatible with MyPet 3.x.
    private boolean modernApi;
    private Object petManager;
    private Method getPetFromEntity;
    private Method getPetOwner;
    private Method getOwnerUniqueId;

    public MyPetManager() {
        Method getPetManager;
        try {
            getPetManager = MyPetApi.class.getMethod("getPetManager");
        } catch (NoSuchMethodException ignored) {
            return;
        }

        modernApi = true;

        try {
            petManager = getPetManager.invoke(null);
            getPetFromEntity = getPetManager.getReturnType().getMethod("getPetFromEntity", Entity.class);

            ClassLoader classLoader = MyPetApi.class.getClassLoader();
            Class<?> petClass = Class.forName("de.Keyle.MyPet.api.entity.Pet", false, classLoader);
            Class<?> petPlayerClass = Class.forName("de.Keyle.MyPet.api.player.MyPetPlayer", false, classLoader);

            getPetOwner = petClass.getMethod("getOwner");
            getOwnerUniqueId = petPlayerClass.getMethod("getUniqueId");
        } catch (ReflectiveOperationException e) {
            MyPetApi.getLogger().warning("Jobs could not initialize its MyPet 4 integration: " + e.getMessage());
        }
    }

    public boolean isMyPet(Entity entity, Player owner) {
	if (modernApi) {
	    UUID petOwner = getModernOwner(entity);
	    return petOwner != null && (owner == null || petOwner.equals(owner.getUniqueId()));
	}

	if (owner == null) {
	    return entity instanceof MyPetBukkitEntity;
	}

	if (!mp.isMyPetPlayer(owner)) {
	    return false;
	}

	MyPetPlayer myPetPlayer = mp.getMyPetPlayer(owner);
	if (!myPetPlayer.hasMyPet()) {
	    return false;
	}

	java.util.Optional<MyPetBukkitEntity> opt = myPetPlayer.getMyPet().getEntity();
	return opt.isPresent() && opt.get().getType() == entity.getType();
    }

    public UUID getOwnerOfPet(Entity ent) {
	if (modernApi) {
	    return getModernOwner(ent);
	}

	if (!(ent instanceof MyPetBukkitEntity))
	    return null;

	MyPet myPet = ((MyPetBukkitEntity) ent).getMyPet();

	try {
	    return myPet.getOwner().getPlayer().getUniqueId();
	} catch (Exception e) {
	    return null;
	}
    }

    private UUID getModernOwner(Entity entity) {
        if (getPetFromEntity == null || getPetOwner == null || getOwnerUniqueId == null) {
            return null;
        }

        try {
            Object pet = getPetFromEntity.invoke(petManager, entity);
            if (pet == null) {
                return null;
            }

            Object owner = getPetOwner.invoke(pet);
            return (UUID) getOwnerUniqueId.invoke(owner);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }
}
