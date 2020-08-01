package gjum.minecraft.gui;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static gjum.minecraft.gui.Layoutable1D.computeLayout;

public class FlexListLayout extends GuiElement {
	private final Vec2.Direction direction;
	private final ArrayList<GuiElement> children = new ArrayList<>();
	@Nullable
	private Layoutable1D[] childLayouts;

	public FlexListLayout(Vec2.Direction direction) {
		this.direction = direction;
	}

	public FlexListLayout add(GuiElement child) {
		children.add(child);
		child.setParent(this);
		invalidateLayout();
		return this;
	}

	public FlexListLayout insert(int index, GuiElement child) {
		children.add(index, child);
		child.setParent(this);
		invalidateLayout();
		return this;
	}

	public FlexListLayout clear() {
		children.forEach(GuiElement::onDestroy);
		children.clear();
		invalidateLayout();
		return this;
	}

	@Override
	public void invalidateLayout() {
		super.invalidateLayout();
		childLayouts = null;
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		childLayouts = new Layoutable1D[children.size()];
		for (int i = 0; i < childLayouts.length; i++) {
			GuiElement child = children.get(i);
			Vec2 minSize = child.getMinSize();
			Vec2 maxSize = child.getMaxSize();
			Vec2 weight = child.getWeight();

			childLayouts[i] = new Layoutable1D(
					minSize.getDim(direction),
					maxSize.getDim(direction),
					weight.getDim(direction));
		}

		int mainSize = computeLayout(sizeAvail.getDim(direction), childLayouts);

		int otherAvail = sizeAvail.getDim(direction.other());
		int otherSize = 0;
		int childPos = 0;
		for (int i = 0; i < childLayouts.length; i++) {
			childLayouts[i].pos = childPos;
			childPos += childLayouts[i].size;

			GuiElement child = children.get(i);
			child.updateSize(Vec2.setDims(
					childLayouts[i].size, otherAvail, direction));

			int childOtherSize = child.getSize().getDim(direction.other());
			otherSize = Math.max(otherSize, childOtherSize);
		}

		super.updateSize(Vec2.setDims(mainSize, otherSize, direction));
	}

	@Override
	public void setPos(Vec2 pos) {
		if (childLayouts == null || childLayouts.length != children.size()) {
			throw new IllegalStateException("setPos() was called before setSize()");
		}
		super.setPos(pos);
		final int other = pos.getDim(direction.other());
		for (int i = 0; i < children.size(); i++) {
			GuiElement child = children.get(i);
			int main = childLayouts[i].pos + pos.getDim(direction);
			child.setPos(Vec2.setDims(main, other, direction));
		}
	}

	@Override
	public Vec2 getWeight() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 weight = child.getWeight();
			sumMain += weight.getDim(direction);
			maxOther = Math.max(maxOther, weight.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public Vec2 getMaxSize() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 maxSize = child.getMaxSize();
			sumMain += maxSize.getDim(direction);
			maxOther = Math.max(maxOther, maxSize.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public Vec2 getMinSize() {
		int sumMain = 0;
		int maxOther = 0;
		for (GuiElement child : children) {
			final Vec2 minSize = child.getMinSize();
			sumMain += minSize.getDim(direction);
			maxOther = Math.max(maxOther, minSize.getDim(direction.other()));
		}
		return Vec2.setDims(sumMain, maxOther, direction);
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (GuiElement child : children) {
			try {
				child.draw(mouse, winSize, partialTicks);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean drawOverlays(Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (GuiElement child : children) {
			try {
				if (child.drawOverlays(mouse, winSize, partialTicks)) {
					return true;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		for (GuiElement child : children) {
			try {
				if (child.onMouseClicked(mouse, mouseButton)) {
					return true;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public void onMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton, long timeSinceLastClick) {
		for (GuiElement child : children) {
			try {
				child.onMouseDragged(mouse, prevMouse, dragStart, mouseButton, timeSinceLastClick);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		for (GuiElement child : children) {
			try {
				child.onMouseReleased(mouse, dragStart, state);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onMouseScrolled(Vec2 mouse, int scrollAmount) {
		for (GuiElement child : children) {
			try {
				if (child.onMouseScrolled(mouse, scrollAmount)) {
					return true;
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public void onKeyTyped(char keyChar, int keyCode) {
		for (GuiElement child : children) {
			try {
				child.onKeyTyped(keyChar, keyCode);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		for (GuiElement child : children) {
			try {
				child.onDestroy();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
