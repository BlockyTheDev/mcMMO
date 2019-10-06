package com.gmail.nossr50.util;

import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.mcMMO;
import org.bukkit.entity.Player;

import java.util.HashMap;

public final class HardcoreManager {

    private final mcMMO pluginRef;

    public HardcoreManager(mcMMO pluginRef) {
        this.pluginRef = pluginRef;
    }

    public void invokeStatPenalty(Player player) {

        if(pluginRef.getWorldGuardUtils().isWorldGuardLoaded()) {
            if(!pluginRef.getWorldGuardManager().hasHardcoreFlag(player)) {
                return;
            }
        }

        double statLossPercentage = pluginRef.getConfigManager().getConfigHardcore().getDeathPenalty().getPenaltyPercentage();
        int levelThreshold = pluginRef.getConfigManager().getConfigHardcore().getDeathPenalty().getLevelThreshold();

        if (pluginRef.getUserManager().getPlayer(player) == null)
            return;

        PlayerProfile playerProfile = pluginRef.getUserManager().getPlayer(player).getProfile();
        int totalLevelsLost = 0;

        HashMap<String, Integer> levelChanged = new HashMap<>();
        HashMap<String, Double> experienceChanged = new HashMap<>();

        for (PrimarySkillType primarySkillType : pluginRef.getSkillTools().NON_CHILD_SKILLS) {
            if (!pluginRef.getSkillTools().getHardcoreStatLossEnabled(primarySkillType)) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0.0);
                continue;
            }

            int playerSkillLevel = playerProfile.getSkillLevel(primarySkillType);
            int playerSkillXpLevel = playerProfile.getSkillXpLevel(primarySkillType);

            if (playerSkillLevel <= 0 || playerSkillLevel <= levelThreshold) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0.0);
                continue;
            }

            double statsLost = playerSkillLevel * (statLossPercentage * 0.01D);
            int levelsLost = (int) statsLost;
            int xpLost = (int) Math.floor(playerSkillXpLevel * (statsLost - levelsLost));
            levelChanged.put(primarySkillType.toString(), levelsLost);
            experienceChanged.put(primarySkillType.toString(), (double) xpLost);

            totalLevelsLost += levelsLost;
        }

        if (!pluginRef.getEventManager().handleStatsLossEvent(player, levelChanged, experienceChanged)) {
            return;
        }

        pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.HARDCORE_MODE, "Hardcore.DeathStatLoss.PlayerDeath", String.valueOf(totalLevelsLost));
    }

    public void invokeVampirism(Player killer, Player victim) {

        if(pluginRef.getWorldGuardUtils().isWorldGuardLoaded()) {
            if(!pluginRef.getWorldGuardManager().hasHardcoreFlag(killer) || !pluginRef.getWorldGuardManager().hasHardcoreFlag(victim)) {
                return;
            }
        }

        double vampirismStatLeechPercentage = pluginRef.getConfigManager().getConfigHardcore().getVampirism().getPenaltyPercentage();
        int levelThreshold = pluginRef.getConfigManager().getConfigHardcore().getVampirism().getLevelThreshold();

        if (pluginRef.getUserManager().getPlayer(killer) == null || pluginRef.getUserManager().getPlayer(victim) == null)
            return;

        PlayerProfile killerProfile = pluginRef.getUserManager().getPlayer(killer).getProfile();
        PlayerProfile victimProfile = pluginRef.getUserManager().getPlayer(victim).getProfile();
        int totalLevelsStolen = 0;

        HashMap<String, Integer> levelChanged = new HashMap<>();
        HashMap<String, Double> experienceChanged = new HashMap<>();

        for (PrimarySkillType primarySkillType : pluginRef.getSkillTools().NON_CHILD_SKILLS) {
            if (!pluginRef.getSkillTools().getHardcoreVampirismEnabled(primarySkillType)) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0.0);
                continue;
            }

            int killerSkillLevel = killerProfile.getSkillLevel(primarySkillType);
            int victimSkillLevel = victimProfile.getSkillLevel(primarySkillType);

            if (victimSkillLevel <= 0 || victimSkillLevel < killerSkillLevel / 2 || victimSkillLevel <= levelThreshold) {
                levelChanged.put(primarySkillType.toString(), 0);
                experienceChanged.put(primarySkillType.toString(), 0.0);
                continue;
            }

            int victimSkillXpLevel = victimProfile.getSkillXpLevel(primarySkillType);

            double statsStolen = victimSkillLevel * (vampirismStatLeechPercentage * 0.01D);
            int levelsStolen = (int) statsStolen;
            int xpStolen = (int) Math.floor(victimSkillXpLevel * (statsStolen - levelsStolen));
            levelChanged.put(primarySkillType.toString(), levelsStolen);
            experienceChanged.put(primarySkillType.toString(), (double) xpStolen);

            totalLevelsStolen += levelsStolen;
        }

        if (!pluginRef.getEventManager().handleVampirismEvent(killer, victim, levelChanged, experienceChanged)) {
            return;
        }

        if (totalLevelsStolen > 0) {
            pluginRef.getNotificationManager().sendPlayerInformation(killer, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Killer.Success", String.valueOf(totalLevelsStolen), victim.getName());
            pluginRef.getNotificationManager().sendPlayerInformation(victim, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Victim.Success", killer.getName(), String.valueOf(totalLevelsStolen));
        } else {
            pluginRef.getNotificationManager().sendPlayerInformation(killer, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Killer.Failure", victim.getName());
            pluginRef.getNotificationManager().sendPlayerInformation(victim, NotificationType.HARDCORE_MODE, "Hardcore.Vampirism.Victim.Failure", killer.getName());
        }
    }

    /**
     * Check if Hardcore Stat Loss is enabled for one or more skill types
     *
     * @return true if Stat Loss is enabled for one or more skill types
     */
    public boolean isStatLossEnabled() {
        boolean enabled = false;

        for (PrimarySkillType primarySkillType : pluginRef.getSkillTools().NON_CHILD_SKILLS) {
            if (pluginRef.getSkillTools().getHardcoreStatLossEnabled(primarySkillType)) {
                enabled = true;
                break;
            }
        }

        return enabled;
    }

    /**
     * Check if Hardcore Vampirism is enabled for one or more skill types
     *
     * @return true if Vampirism is enabled for one or more skill types
     */
    public boolean isVampirismEnabled() {
        boolean enabled = false;

        for (PrimarySkillType primarySkillType : pluginRef.getSkillTools().NON_CHILD_SKILLS) {
            if (pluginRef.getSkillTools().getHardcoreVampirismEnabled(primarySkillType)) {
                enabled = true;
                break;
            }
        }

        return enabled;
    }
}