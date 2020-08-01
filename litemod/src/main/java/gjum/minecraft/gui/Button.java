package gjum.minecraft.gui;

import net.minecraft.client.gui.GuiButton;

import javax.annotation.Nullable;

public class Button extends Clickable {
	private GuiButton button;

	public Button(@Nullable Runnable clickHandler, @Nullable String text) {
		if (text == null) text = "";
		button = new GuiButton(getId(), 0, 0, text);
		setClickHandler(clickHandler);

		final int textWidth = mc.fontRenderer.getStringWidth(text);
		setMinSize(new Vec2(Math.max(20, textWidth) + 6, 20));
		setMaxSize(new Vec2(380, 20));
	}

	public Button setText(String text) {
		button.displayString = text;
		final int textWidth = mc.fontRenderer.getStringWidth(text);
		// TODO instead of replacing, add this as an additional constraint
		setMinSize(new Vec2(textWidth + 6, getMinSize().y));
		return this;
	}

	@Override
	public boolean isEnabled() {
		return button.enabled;
	}

	public Button setEnabled(boolean enabled) {
		button.enabled = enabled;
		return this;
	}

	public GuiButton getButton() {
		return button;
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		button.drawButton(mc, mouse.x, mouse.y, partialTicks);
	}

	@Override
	public void setPos(Vec2 pos) {
		super.setPos(pos);
		button.x = pos.x;
		button.y = pos.y;
	}

	@Override
	public void updateSize(Vec2 size) {
		super.updateSize(size);

		final boolean wasEnabled = button.enabled;
		button = new GuiButton(button.id,
				button.x, button.y,
				getSize().x, getSize().y,
				button.displayString);
		button.enabled = wasEnabled;
	}
}
