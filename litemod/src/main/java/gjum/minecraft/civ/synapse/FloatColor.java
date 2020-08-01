package gjum.minecraft.civ.synapse;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import java.awt.Color;

import static gjum.minecraft.civ.synapse.McUtil.getMc;

public class FloatColor {
	public float r;
	public float g;
	public float b;

	public FloatColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public int getHex() {
		return 0xff000000
				+ ((int) (r * 255) << 16)
				+ ((int) (g * 255) << 8)
				+ (int) (b * 255);
	}

	public Color toColor() {
		return new Color(r, g, b);
	}

	public static FloatColor fromHex(int hex) {
		return new FloatColor(
				((hex >> 16) & 0xff) / 255.f,
				((hex >> 8) & 0xff) / 255.f,
				(hex & 0xff) / 255.f);
	}

	public static FloatColor fromTextFormatting(@Nonnull TextFormatting color) {
		final FontRenderer fontRenderer = getMc().fontRenderer;
		return fromHex(fontRenderer.getColorCode(color.toString().charAt(1)));
	}
}
