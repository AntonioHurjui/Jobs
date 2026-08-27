package com.gamingmesh.jobs.container;

import java.util.ArrayList;
import java.util.List;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.config.ConfigManager.KeyValues;

import net.Zrips.CMILib.Messages.CMIMessages;

public class QuestObjective {

    private int id;
    private String meta;
    private String name;
    private int amount = Integer.MAX_VALUE;
    private ActionType action = null;

    private String serializedLine = "";

    public static List<QuestObjective> get(String objective, String jobName) {

        String[] split = objective.split(";", 3);

        List<QuestObjective> list = new ArrayList<QuestObjective>();

        if (split.length < 2) {
            CMIMessages.consoleMessage("Job " + jobName + " has incorrect quest objective (" + objective + ")!");
            return list;
        }

        ActionType actionType = ActionType.getByName(split[0]);

        if (actionType == null)
            return list;

        try {

            String mats = split[1].toUpperCase();
            // Comma separates INDEPENDENT objectives (each needs its own full amount).
            // Ampersand (&) inside one of those entries separates EITHER/OR targets that
            // share a single counter, e.g. Break;iron_ore&deepslate_iron_ore;32
            String[] co = mats.split(",");

            int amount = 1;
            if (split.length <= 3)
                amount = Integer.parseInt(split[2]);

            if (co.length > 0) {
                for (String materials : co) {
                    QuestObjective obj = buildObjective(materials, actionType, jobName, amount);
                    if (obj != null)
                        list.add(obj);
                }
            } else {
                QuestObjective obj = buildObjective(mats, actionType, jobName, amount);
                if (obj != null)
                    list.add(obj);
            }
        } catch (Exception e) {
            CMIMessages.consoleMessage("Job " + jobName + " has incorrect quest objective (" + objective + ")!");
        }

        return list;
    }

    // Builds a single QuestObjective out of one comma-segment. If that segment
    // contains one or more '&' separators, all listed materials are merged into
    // ONE objective (either/or) that shares a single progress counter, instead of
    // being split into several independent objectives.
    private static QuestObjective buildObjective(String materialToken, ActionType actionType, String jobName, int amount) {
        String[] orGroup = materialToken.split("&");

        if (orGroup.length <= 1) {
            KeyValues kv = Jobs.getConfigManager().getKeyValue(materialToken, actionType, jobName);
            if (kv == null)
                return null;

            return new QuestObjective(actionType, kv.getId(), kv.getMeta(), (kv.getType() + kv.getSubType()).toUpperCase(), amount);
        }

        List<String> names = new ArrayList<String>();
        Integer firstId = null;
        String firstMeta = null;

        for (String single : orGroup) {
            single = single.trim();
            if (single.isEmpty())
                continue;

            KeyValues kv = Jobs.getConfigManager().getKeyValue(single, actionType, jobName);
            if (kv == null) {
                CMIMessages.consoleMessage("Job " + jobName + " has unknown material '" + single + "' in either/or quest objective!");
                continue;
            }

            String targetName = (kv.getType() + kv.getSubType()).toUpperCase();
            if (!names.contains(targetName))
                names.add(targetName);

            if (firstId == null) {
                firstId = kv.getId();
                firstMeta = kv.getMeta();
            }
        }

        if (names.isEmpty())
            return null;

        // Joining the resolved names back with '&' keeps them as the single map key used
        // for matching (see QuestProgression#objectiveKeyMatches), so breaking ANY of the
        // listed blocks increments the SAME shared counter.
        String combinedName = String.join("&", names);
        return new QuestObjective(actionType, firstId, firstMeta, combinedName, amount);
    }

    public QuestObjective(ActionType action, int id, String meta, String name, int amount) {
        this.action = action;
        this.id = id;
        this.meta = meta;
        this.name = name;
        this.amount = amount;
    }

    public int getTargetId() {
        return id;
    }

    public void setTargetId(int id) {
        this.id = id;
    }

    public String getTargetMeta() {
        return meta;
    }

    public void setTargetMeta(String meta) {
        this.meta = meta;
    }

    public String getTargetName() {
        return name;
    }

    public void setTargetName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public boolean same(QuestObjective obj) {
        return obj.id == this.id && obj.meta.equals(this.meta) && obj.name.equals(this.name) && obj.amount == this.amount && obj.action == this.action;
    }

    public String getIdentifier() {
        if (serializedLine.isEmpty()) {
            serializedLine = getAction().toString() + ";" + getTargetName() + ";" + getAmount();
        }

        return serializedLine;
    }
}
