package gjum.minecraft.gui;

import net.minecraft.util.text.TextFormatting;

import java.awt.Color;

public class Label extends Clickable {
	public enum Alignment {ALIGN_LEFT, ALIGN_CENTER, ALIGN_RIGHT}

	private String text;
	public Alignment alignment;
	private int color = Color.WHITE.getRGB();
	private int height = 20;

	public Label(String text, Alignment alignment) {
		super();
		this.text = text;
		this.alignment = alignment;
		int width = mc.fontRenderer.getStringWidth(text) - 1; // subtract space after last char
		setMinSize(new Vec2(width, height));
		setMaxSize(new Vec2(9999999, height));
		if (alignment == Alignment.ALIGN_CENTER) setWeight(new Vec2(1, 0));
		else setWeight(new Vec2(0, 0));
	}

	public void setText(String text) {
		this.text = text;
		int width = mc.fontRenderer.getStringWidth(text) - 1; // subtract space after last char
		if (getMinSize().x < width) {
			setMinSize(new Vec2(width, height));
		}
	}

	public Label setColor(Color color) {
		this.color = color.getRGB();
		return this;
	}

	public Label setHeight(int height) {
		this.height = height;
		setMinSize(new Vec2(getMinSize().x, height));
		setMaxSize(new Vec2(getMaxSize().x, height));
		return this;
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		final int x;
		switch (alignment) {
			case ALIGN_CENTER: {
				int w = mc.fontRenderer.getStringWidth(text) - 1; // subtract space after last char
				x = getPos().x + (getSize().x - w) / 2;
				break;
			}
			case ALIGN_LEFT: {
				x = getPos().x;
				break;
			}
			case ALIGN_RIGHT: {
				int w = mc.fontRenderer.getStringWidth(text) - 1; // subtract space after last char
				x = getPos().x + (getSize().x - w);
				break;
			}
			default:
				throw new IllegalStateException("Unexpected alignment " + alignment);
		}
		// TODO configure vertical alignment
		final int dy = (getSize().y - mc.fontRenderer.FONT_HEIGHT) / 2;

		String displayText = text;
		if (clickHandler != null) {
			// make text feel clickable
			displayText = TextFormatting.UNDERLINE + displayText;
			if (isMouseInside(mouse)) {
				displayText = TextFormatting.ITALIC + displayText;
			}
		}

		mc.fontRenderer.drawStringWithShadow(displayText, x, getPos().y + 1 + dy, color);
	}
}
