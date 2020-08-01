package gjum.minecraft.civ.synapse;

import gjum.minecraft.civ.synapse.common.Pos;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static gjum.minecraft.civ.synapse.integrations.JourneyMapPlugin.jmApi;
import static net.minecraft.potion.PotionUtils.getPotionFromItem;

public class McUtil {
	public static Minecraft getMc() {
		return Minecraft.getMinecraft();
	}

	public static boolean isJourneyMapLoaded() {
		try {
			Class.forName("journeymap.common.Journeymap");
			return jmApi != null;
		} catch (ClassNotFoundException | NoClassDefFoundError var1) {
		}
		return false;
	}

	/**
	 * does the rounding correctly for negative coordinates
	 */
	@Nonnull
	public static Pos getEntityPosition(@Nonnull Entity entity) {
		return new Pos(
				MathHelper.floor(entity.posX),
				MathHelper.floor(entity.posY),
				MathHelper.floor(entity.posZ));
	}

	public static Pos pos(BlockPos pos) {
		return new Pos(pos.getX(), pos.getY(), pos.getZ());
	}

	public static BlockPos blockPos(Pos pos) {
		return new BlockPos(pos.x, pos.y, pos.z);
	}

	@Nonnull
	public static String getDisplayNameFromTablist(NetworkPlayerInfo info) {
		if (info.getDisplayName() != null) {
			return info.getDisplayName().getUnformattedText().replaceAll("§.", "");
		}
		return info.getGameProfile().getName().replaceAll("§.", "");
	}

	@Nonnull
	public static String getSelfAccount() {
		if (getMc().getConnection() != null) {
			final NetworkPlayerInfo tabEntry = getMc().getConnection().getPlayerInfo(getMc().getSession().getPlayerID());
			if (tabEntry != null) {
				return tabEntry.getGameProfile().getName();
			}
		}
		if (getMc().player != null) {
			return getMc().player.getName();
		}
		return getMc().getSession().getUsername();
	}

	public static float getHealth() {
		if (getMc().player == null) return -1;
		return getMc().player.getHealth();
	}

	public static int getNumHealthPots() {
		// TODO cache health pot count instead of recomputing each frame; invalidate cache when inventory packet received
		final InventoryPlayer inv = getMc().player.inventory;
		return (int) (inv.mainInventory.stream().filter(McUtil::isHealthPot).count()
				+ inv.offHandInventory.stream().filter(McUtil::isHealthPot).count());
	}

	public static boolean isHealthPot(ItemStack stack) {
		return stack.getItem() == Items.SPLASH_POTION
				&& getPotionFromItem(stack) == PotionTypes.STRONG_HEALING;
	}

	public static int blockIdAtPos(@Nonnull BlockPos pos) {
		return Block.getIdFromBlock(getMc().world.getBlockState(pos).getBlock());
	}

	@Nullable
	public static List<String> getLore(@Nullable ItemStack item) {
		if (item == null) return null;
		NBTTagCompound itemTag = item.getTagCompound();
		if (itemTag == null) return null;
		if (!itemTag.hasKey("display", 10)) return null;
		NBTTagCompound displayTag = itemTag.getCompoundTag("display");
		if (displayTag.getTagId("Lore") != 9) return null;
		NBTTagList loreTag = displayTag.getTagList("Lore", 8);
		if (loreTag.hasNoTags()) return null;
		List<String> lore = new ArrayList<String>(loreTag.tagCount());
		for (int i = 0; i < loreTag.tagCount(); i++) {
			lore.add(loreTag.getStringTagAt(i));
		}
		return lore;
	}

	public static boolean isSameBlock(@Nonnull BlockPos posA, @Nonnull BlockPos posB) {
		return getMc().world.getBlockState(posA).getBlock() == getMc().world.getBlockState(posB).getBlock();
	}

	@Nonnull
	public static String getBiomeName(@Nonnull BlockPos pos) {
		return getMc().world.getBiome(pos).getBiomeName();
	}

	@Nullable
	public static BlockPos getLookedAtBlockPos(int reach) {
		final RayTraceResult trace = getMc().player.rayTrace(reach, getMc().getRenderPartialTicks());
		return (trace == null) ? null : trace.getBlockPos();
	}
}
