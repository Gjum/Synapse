package gjum.minecraft.gui;

import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class CycleButton<T> extends Clickable {
	private GuiButton button;
	@Nonnull
	private final Consumer<T> onChange;
	private T[] options;
	private int selectionIndex;

	@SafeVarargs
	public CycleButton(@Nonnull Consumer<T> onChange, @Nullable T selection, T... options) {
		this.onChange = onChange;
		button = new GuiButton(getId(), 0, 0, ""); // text is set below in setOptions()
		setClickHandler(this::onClicked);

		setOptions(selection, options);

		int maxTextWidth = 0;
		for (T option : options) {
			int optionWidth = mc.fontRenderer.getStringWidth(option.toString());
			maxTextWidth = Math.max(maxTextWidth, optionWidth);
		}
	}

	@SafeVarargs
	public final CycleButton<T> setOptions(@Nullable T selection, T... options) {
		this.options = options;

		int longestOption = 0;
		for (int i = options.length - 1; i >= 0; i--) {
			T value = options[i];

			final int width = mc.fontRenderer.getStringWidth(value.toString());
			if (longestOption < width) longestOption = width;

			if (value.equals(selection)) {
				selectionIndex = i;
			}
		}

		button.displayString = options[selectionIndex].toString();

		setMinSize(new Vec2(Math.max(20, longestOption + 6), 20));
		setMaxSize(new Vec2(380, 20));

		return this;
	}

	private void onClicked() {
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
				|| Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
		) selectionIndex--;
		else selectionIndex++;
		setIndex(selectionIndex);
	}

	public void setIndex(int index) {
		selectionIndex = index;
		selectionIndex %= options.length;
		// don't need to invalidate size because the size fits all options
		button.displayString = options[selectionIndex].toString();
		onChange.accept(options[selectionIndex]);
	}

	@Override
	public boolean isEnabled() {
		return button.enabled;
	}

	public CycleButton setEnabled(boolean enabled) {
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
