package gjum.minecraft.gui;

import javax.annotation.Nonnull;
import java.awt.Color;

import static net.minecraft.client.gui.Gui.drawRect;

/**
 * Renders a tooltip if the mouse is over the child.
 * Passes all events through to the child.
 * Uses child's layout constraints.
 */
public class Tooltip extends GuiElement {
	@Nonnull
	private String text;
	@Nonnull
	private GuiElement child;

	public Tooltip(@Nonnull String text, @Nonnull GuiElement child) {
		this.text = text;
		this.child = child;
	}

	public Tooltip setText(@Nonnull String text) {
		this.text = text;
		return this;
	}

	public Tooltip setChild(GuiElement child) {
		this.child = child;
		return this;
	}

	@Override
	public boolean drawOverlays(Vec2 mouse, Vec2 winSize, float partialTicks) {
		if (child.drawOverlays(mouse, winSize, partialTicks)) {
			return true;
		}

		if (getPos() == null) return false;
		if (getPos().x > mouse.x || mouse.x > getPos().x + getSize().x) return false;
		if (getPos().y > mouse.y || mouse.y > getPos().y + getSize().y) return false;

		int mouseX = mouse.x;
		int mouseY = mouse.y;
		final int lineHeight = 9;
		final int padding = 2;

		final String[] lines = text.replaceAll("\\r?\\n+$", "")
				.split("\\r?\\n");
		final int textHeight = lineHeight * lines.length;
		int textWidth = 0;
		for (String line : lines) {
			textWidth = Math.max(textWidth, mc.fontRenderer.getStringWidth(line));
		}
		final int boxWidth = textWidth - 1 + 2 * padding;
		final int boxHeight = textHeight - 1 + 2 * padding;
		mouseX -= padding;
		mouseY -= boxHeight;
		int left = Math.max(0, Math.min(mouseX, winSize.x - boxWidth));
		int top = Math.max(0, Math.min(mouseY, winSize.y - boxHeight));
		drawRect(left, top, left + boxWidth, top + boxHeight, Color.BLACK.getRGB());
		for (String line : lines) {
			mc.fontRenderer.drawStringWithShadow(line, left + padding, top + padding, Color.WHITE.getRGB());
			top += lineHeight;
		}
		return true;
	}

	@Override
	public Vec2 getPos() {
		return child.getPos();
	}

	@Override
	public void setPos(Vec2 pos) {
		child.setPos(pos);
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		child.draw(mouse, winSize, partialTicks);
	}

	@Override
	public void onKeyTyped(char keyChar, int keyCode) {
		child.onKeyTyped(keyChar, keyCode);
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		return child.onMouseClicked(mouse, mouseButton);
	}

	@Override
	public void onMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton, long timeSinceLastClick) {
		child.onMouseDragged(mouse, prevMouse, dragStart, mouseButton, timeSinceLastClick);
	}

	@Override
	public void onMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		child.onMouseReleased(mouse, dragStart, state);
	}

	@Override
	public boolean onMouseScrolled(Vec2 mouse, int scrollAmount) {
		return child.onMouseScrolled(mouse, scrollAmount);
	}

	@Override
	public void onDestroy() {
		child.onDestroy();
	}

	@Override
	public Vec2 getSize() {
		return child.getSize();
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		child.updateSize(sizeAvail);
	}

	@Override
	public GuiElement setFixedSize(Vec2 size) {
		return child.setFixedSize(size);
	}

	@Override
	public Vec2 getWeight() {
		return child.getWeight();
	}

	@Override
	public GuiElement setWeight(Vec2 weight) {
		return child.setWeight(weight);
	}

	@Override
	public Vec2 getMaxSize() {
		return child.getMaxSize();
	}

	@Override
	public GuiElement setMaxSize(Vec2 size) {
		return child.setMaxSize(size);
	}

	@Override
	public Vec2 getMinSize() {
		return child.getMinSize();
	}

	@Override
	public GuiElement setMinSize(Vec2 size) {
		return child.setMinSize(size);
	}
}
