package gjum.minecraft.civ.synapse.integrations;

import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mumfrey.liteloader.core.LiteLoader;
import gjum.minecraft.civ.synapse.FloatColor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VoxelMapHelper {
	@Nullable
	public static IWaypointManager getWaypointManager() {
		if (!isVoxelMapActive()) return null;
		final AbstractVoxelMap instance = AbstractVoxelMap.getInstance();
		return instance == null ? null : instance.getWaypointManager();
	}

	public static void createWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (getWaypointManager() == null) return;
		waypoint.vmWaypoint = new Waypoint(waypoint.getName(),
				waypoint.pos.x, waypoint.pos.y, waypoint.pos.z,
				waypoint.isVisible(),
				waypoint.color.r, waypoint.color.g, waypoint.color.b,
				waypoint.vmImage.toString(), waypoint.world, new TreeSet<>());
		waypoint.vmWaypoint.dimensions.add(waypoint.getDimension());
		getWaypointManager().addWaypoint(waypoint.vmWaypoint);
	}

	public static void updateWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (getWaypointManager() == null) return;
		if (waypoint.vmWaypoint == null) createWaypoint(waypoint);
		if (waypoint.vmWaypoint == null) return;
		waypoint.vmWaypoint.name = waypoint.getName();
		waypoint.vmWaypoint.x = waypoint.pos.x;
		waypoint.vmWaypoint.y = waypoint.pos.y;
		waypoint.vmWaypoint.z = waypoint.pos.z;
		waypoint.vmWaypoint.red = waypoint.color.r;
		waypoint.vmWaypoint.green = waypoint.color.g;
		waypoint.vmWaypoint.blue = waypoint.color.b;
		waypoint.vmWaypoint.enabled = waypoint.isVisible();
		waypoint.vmWaypoint.imageSuffix = waypoint.vmImage.toString();

		// add if it got deleted or it was somehow not already added
		// iterate reverse to avoid checking old waypoints
		final ArrayList<Waypoint> renderedWaypoints = getWaypointManager().getWaypoints();
		for (int i = renderedWaypoints.size() - 1; i >= 0; i--) {
			if (renderedWaypoints.get(i) == waypoint.vmWaypoint) {
				return;
			}
		}
		getWaypointManager().addWaypoint(waypoint.vmWaypoint);
	}

	public static void deleteWaypoint(@Nonnull MultiWaypoint waypoint) {
		if (getWaypointManager() == null) return;
		if (waypoint.vmWaypoint == null) return;
		getWaypointManager().deleteWaypoint(waypoint.vmWaypoint);
		waypoint.vmWaypoint = null;
	}

	public static void deleteWaypoint(@Nonnull Waypoint waypoint) {
		if (getWaypointManager() == null) return;
		getWaypointManager().deleteWaypoint(waypoint);
	}

	/**
	 * Set any arg to null to match any corresponding value.
	 */
	public static Collection<Waypoint> getWaypointsByNameImgColor(@Nullable Pattern namePattern, @Nullable VmImage image, @Nullable FloatColor color) {
		if (getWaypointManager() == null) return Collections.emptyList();
		return getWaypointManager().getWaypoints().stream().filter(p ->
				(image == null || image.toString().equals(p.imageSuffix)
				) && (color == null || (color.r == p.red && color.g == p.green && color.b == p.blue)
				) && (namePattern == null || namePattern.matcher(p.name).matches())
		).collect(Collectors.toList());
	}

	public static boolean isVoxelMapActive() {
		try {
			Class.forName("com.mamiyaotaru.voxelmap.litemod.LiteModVoxelMap");
			return LiteLoader.getInstance().isModActive("VoxelMap");
		} catch (NoClassDefFoundError | ClassNotFoundException ignored) {
		}
		return false;
	}
}
