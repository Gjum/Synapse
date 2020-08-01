package gjum.minecraft.gui;

import javax.annotation.Nonnull;
import java.awt.Color;

import static com.mumfrey.liteloader.gl.GLClippingPlanes.glDisableClipping;
import static com.mumfrey.liteloader.gl.GLClippingPlanes.glEnableClipping;
import static net.minecraft.client.gui.Gui.drawRect;

/**
 * Scrolls the contained child {@link GuiElement}.
 */
public class ScrollBox extends GuiElement {
	private static final int scrollBarSize = 7;
	private static final int scrollBarAreaSize = scrollBarSize + 1;

	@Nonnull
	private GuiElement child;
	private Vec2 scrollOffset = new Vec2(0, 0);

	public ScrollBox(@Nonnull GuiElement child) {
		this.child = child;
		setMinSize(new Vec2(scrollBarAreaSize, 2 + scrollBarSize));
		setChild(child);
		setWeight(new Vec2(1, 1));
		updateSize(getMinSize());
	}

	public ScrollBox setChild(GuiElement child) {
		this.child = child;
		child.setParent(this);
		final Vec2 childMax = child.getMaxSize();
		setMaxSize(new Vec2(
				childMax.x + scrollBarAreaSize,
				childMax.y + 2));
		clipScroll();
		return this;
	}

	@Override
	public void setPos(@Nonnull Vec2 pos) {
		super.setPos(pos);
		child.setPos(new Vec2(
				pos.x + scrollOffset.x,
				pos.y + scrollOffset.y + 1));
	}

	@Override
	public void updateSize(Vec2 availableSize) {
		super.updateSize(availableSize);
		child.updateSize(new Vec2(getInnerWidth(), getInnerHeight()));
		// may not become larger than child
		super.updateSize(new Vec2(
				Math.min(getSize().x, child.getSize().x + scrollBarAreaSize),
				Math.min(getSize().y, child.getSize().y + 2)));
		clipScroll();
	}

	private int getInnerWidth() {
		return getSize().x - scrollBarAreaSize;
	}

	private int getInnerHeight() {
		return getSize().y - 2;
	}

	private boolean isMouseInsideChild(Vec2 mouse) {
		if (mouse == null) return false;
		if (getPos() == null) return false;
		if (mouse.x < getPos().x) return false;
		if (mouse.x >= getPos().x + getSize().x) return false;
		if (mouse.y < getPos().y) return false;
		if (mouse.y >= getPos().y + getSize().y) return false;
		return true;
	}

	private boolean isMouseOnScrollBar(Vec2 mouse) {
		if (mouse == null) return false;
		if (getPos() == null) return false;
		if (mouse.x < getPos().x + getSize().x - scrollBarAreaSize) return false;
		if (mouse.x >= getPos().x + getSize().x) return false;
		if (mouse.y < getPos().y) return false;
		if (mouse.y >= getPos().y + getSize().y) return false;
		return true;
	}

	private Vec2 mouseChildTranslated(Vec2 mouse) {
		return isMouseInsideChild(mouse) ? mouse : new Vec2(Vec2.LARGE, Vec2.LARGE);
	}

	@Override
	public boolean onMouseScrolled(Vec2 mouse, int rows) {
		final boolean mouseInsideChild = isMouseInsideChild(mouse);
		if (mouseInsideChild) {
			if (child.onMouseScrolled(mouseChildTranslated(mouse), rows)) {
				return true;
			}
		}
		if (mouseInsideChild || isMouseOnScrollBar(mouse)) {
			int pixels = 20 * rows / 120; // one scroll wheel movement is 120 units
			scrollOffset = new Vec2(scrollOffset.x, scrollOffset.y + pixels);
			clipScroll();
			return true;
		}
		return false;
	}

	@Override
	public void onMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton, long timeSinceLastClick) {
		// forward even if mouseButton != 0
		child.onMouseDragged(mouseChildTranslated(mouse), mouseChildTranslated(prevMouse), mouseChildTranslated(dragStart), mouseButton, timeSinceLastClick);

		if (mouseButton != 0) return;
		if (mouse.y == prevMouse.y) return;

		final int delta;
		if (isMouseInsideChild(dragStart)) {
			delta = mouse.y - prevMouse.y;
		} else if (isMouseOnScrollBar(dragStart)) {
			// TODO fix scroll bar dragging
			final int mouseDy = -(mouse.y - prevMouse.y);
			final Vec2 childSize = child.getSize();
			if (childSize == null) return;
			if (childSize.y <= getInnerHeight()) return; // can't scroll, child too small
			final int scrollBarLength = Math.max(scrollBarSize, getInnerHeight() * getInnerHeight() / childSize.y);
			final int scrollBarTravel = getInnerHeight() - scrollBarLength;

			delta = mouseDy * Math.max(1, childSize.y - getInnerHeight()) / scrollBarTravel;
		} else return;

		scrollOffset = new Vec2(scrollOffset.x, scrollOffset.y + delta);
		clipScroll();
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		return child.onMouseClicked(mouseChildTranslated(mouse), mouseButton);
	}

	@Override
	public void onMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		child.onMouseReleased(mouseChildTranslated(mouse), dragStart, state);
	}

	@Override
	public void onKeyTyped(char keyChar, int keyCode) {
		child.onKeyTyped(keyChar, keyCode);
	}

	@Override
	public void onDestroy() {
		child.onDestroy();
	}

	/**
	 * Ensure scroll offsets are within bounds.
	 */
	private void clipScroll() {
		if (child.getSize() == null) return;
		final int tooFarDown = getInnerHeight() - child.getSize().y - scrollOffset.y;
		if (tooFarDown > 0) {
			scrollOffset = new Vec2(scrollOffset.x, scrollOffset.y + tooFarDown);
		}
		if (scrollOffset.y > 0) {
			scrollOffset = new Vec2(scrollOffset.x, 0);
		}
		try {
			child.setPos(new Vec2(getPos().x + scrollOffset.x, getPos().y + scrollOffset.y));
		} catch (Throwable ignored) {
			// child size not yet initialized
			// XXX don't catch every exception here
		}
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		if (child.getSize() == null) return;

		final int top = getPos().y + 1;
		glEnableClipping(
				getPos().x,
				getPos().x + getInnerWidth(),
				top,
				top + getInnerHeight());

		child.draw(mouse, winSize, partialTicks);

		// TODO nestable clipping: restore previous edges
		glDisableClipping();

		if (child.getSize().y <= getInnerHeight())
			return; // nothing to scroll, hide scroll bar; also prevents potential divide by zero

		final int lineColor = Color.GRAY.getRGB();
		final int scrollBarColor = Color.GRAY.getRGB();
		final int scrollBarTrackColor = Color.BLACK.getRGB();

		// lines at top and bottom
		if (scrollOffset.y < 0) {
			drawRect(
					getPos().x,
					getPos().y,
					getPos().x + getSize().x,
					getPos().y + 1,
					lineColor);
		}
		if (-scrollOffset.y < child.getSize().y - getInnerHeight()) {
			drawRect(
					getPos().x,
					getPos().y + getSize().y - 1,
					getPos().x + getSize().x,
					getPos().y + getSize().y,
					lineColor);
		}

		// scroll bar track
		drawRect(
				getPos().x + getSize().x - scrollBarSize,
				top,
				getPos().x + getSize().x,
				top + getInnerHeight(),
				scrollBarTrackColor);

		// scroll bar
		final int scrollBarLength = Math.max(scrollBarSize, getInnerHeight() * getInnerHeight() / child.getSize().y);
		final int scrollBarTravel = getInnerHeight() - scrollBarLength;
		final int scrollBarOffset = scrollBarTravel * -scrollOffset.y / Math.max(1, child.getSize().y - getInnerHeight());
		drawRect(
				getPos().x + getSize().x - scrollBarSize,
				top + scrollBarOffset,
				getPos().x + getSize().x,
				top + scrollBarOffset + scrollBarLength,
				scrollBarColor);
	}

	@Override
	public boolean drawOverlays(Vec2 mouse, Vec2 winSize, float partialTicks) {
		return child.drawOverlays(mouseChildTranslated(mouse), winSize, partialTicks);
	}
}
