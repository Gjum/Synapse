package gjum.minecraft.civ.synapse.integrations;

import journeymap.client.api.display.Waypoint;

import javax.annotation.Nonnull;

import static gjum.minecraft.civ.synapse.LiteModSynapse.MOD_NAME;
import static gjum.minecraft.civ.synapse.McUtil.blockPos;
import static gjum.minecraft.civ.synapse.McUtil.isJourneyMapLoaded;
import static gjum.minecraft.civ.synapse.integrations.JourneyMapPlugin.jmApi;

public class JourneyMapHelper {
	public static void createWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (!isJourneyMapLoaded()) return;
		waypoint.jmWaypoint = new Waypoint(MOD_NAME, waypoint.getName(), waypoint.getDimension(), blockPos(waypoint.pos));
		waypoint.jmWaypoint.setColor(waypoint.color.getHex());
		waypoint.jmWaypoint.setDisplayed(waypoint.getDimension(), waypoint.isVisible());
		waypoint.jmWaypoint.setEditable(false);
		waypoint.jmWaypoint.setPersistent(false);
		waypoint.jmWaypoint.setDirty();
		try {
			jmApi.show(waypoint.jmWaypoint);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void updateWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (!isJourneyMapLoaded()) return;
		if (waypoint.jmWaypoint == null) createWaypoint(waypoint);
		if (waypoint.jmWaypoint == null) return; // could not create
		waypoint.jmWaypoint.setName(waypoint.getName());
		waypoint.jmWaypoint.setPosition(waypoint.getDimension(), blockPos(waypoint.pos));
		waypoint.jmWaypoint.setColor(waypoint.color.getHex());
		waypoint.jmWaypoint.setDisplayed(waypoint.getDimension(), waypoint.isVisible());
		waypoint.jmWaypoint.setDirty();
		try {
			jmApi.show(waypoint.jmWaypoint);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void deleteWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (!isJourneyMapLoaded()) return;
		if (waypoint.jmWaypoint == null) return;
		jmApi.remove(waypoint.jmWaypoint);
		waypoint.jmWaypoint = null;
	}
}
