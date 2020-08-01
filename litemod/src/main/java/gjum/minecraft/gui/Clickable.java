package gjum.minecraft.gui;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

import javax.annotation.Nullable;

public class Clickable extends GuiElement {
	private static int nextId = 1;

	protected static int getId() {
		return nextId++;
	}

	@Nullable
	public Runnable clickHandler = null;

	public Clickable setClickHandler(@Nullable Runnable clickHandler) {
		this.clickHandler = clickHandler;
		return this;
	}

	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		if (mouseButton != 0) return false;
		if (!isEnabled()) return false;
		if (!isMouseInside(mouse)) return false;
		if (clickHandler == null) return false;

		mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		try {
			clickHandler.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return true;
	}

	boolean isMouseInside(Vec2 mouse) {
		if (getPos() == null) return false;
		return mouse.x >= getPos().x
				&& mouse.y >= getPos().y
				&& mouse.x < getPos().x + getSize().x
				&& mouse.y < getPos().y + getSize().y;
	}
}
