package gjum.minecraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GuiRoot extends GuiScreen implements GuiParent {
	public final GuiScreen parentScreen;
	@Nullable
	private GuiElement root = null;
	private boolean dirtyLayout = false;
	private Vec2 prevSize = null;
	private Vec2 prevMouse = new Vec2(0, 0);
	private Vec2 dragStart = null;

	public GuiRoot(GuiScreen parentScreen) {
		this.parentScreen = parentScreen;
		this.mc = Minecraft.getMinecraft();
	}

	public GuiElement build() {
		return new Spacer();
	}

	/**
	 * If elements need to be added/removed, call this to rebuild the whole layout tree and elements list.
	 */
	public void rebuild() {
		if (root != null) root.onDestroy();
		root = null;
		dirtyLayout = true;
	}

	protected void handleError(Throwable e) {
		e.printStackTrace();
		mc.displayGuiScreen(parentScreen);
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		try {
			rebuild();
		} catch (Throwable e) {
			handleError(e);
		}
	}

	/**
	 * Mark the layout to need to be recomputed on the next render.
	 * Useful when the size of an element changed.
	 */
	@Override
	public void invalidateLayout() {
		dirtyLayout = true;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		try {
			drawDefaultBackground();

			if (root == null) {
				root = build();
				root.setParent(this);
				prevSize = null;
				dirtyLayout = true;
			}

			final Vec2 newSize = new Vec2(width, height);
			if (!newSize.equals(prevSize)) {
				prevSize = newSize;
				dirtyLayout = true;
			}

			if (dirtyLayout) {
				root.updateSize(newSize);
				root.setPos(new Vec2(0, 0));
				dirtyLayout = false;
			}

			final Vec2 mousePos = new Vec2(mouseX, mouseY);
			root.draw(mousePos, newSize, partialTicks);
			root.drawOverlays(mousePos, newSize, partialTicks);
		} catch (Throwable e) {
			handleError(e);
		}
	}

	@Override
	public void keyTyped(char keyChar, int keyCode) {
		try {
			if (root != null) {
				root.onKeyTyped(keyChar, keyCode);
			}
		} catch (Throwable e) {
			handleError(e);
		}
		try {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				mc.displayGuiScreen(null);
			}
		} catch (Throwable e) {
			handleError(e);
		}
	}

	@Override
	public void mouseClicked(int x, int y, int mouseButton) {
		try {
			if (root != null) root.onMouseClicked(new Vec2(x, y), mouseButton);
			prevMouse = new Vec2(x, y);
		} catch (Throwable e) {
			handleError(e);
		}
	}

	@Override
	protected void mouseClickMove(int x, int y, int clickedMouseButton, long timeSinceLastClick) {
		try {
			if (root != null) root.onMouseDragged(
					new Vec2(x, y),
					prevMouse,
					dragStart,
					clickedMouseButton, timeSinceLastClick);
			prevMouse = new Vec2(x, y);
		} catch (Throwable e) {
			handleError(e);
		}
	}

	@Override
	protected void mouseReleased(int x, int y, int state) {
		try {
			if (root != null) {
				root.onMouseReleased(new Vec2(x, y),
						dragStart != null ? dragStart : prevMouse,
						state);
			}
		} catch (Throwable e) {
			handleError(e);
		}
		dragStart = null;
	}

	@Override
	public void handleMouseInput() {
		try {
			super.handleMouseInput();
			int scrollAmount = Mouse.getEventDWheel();
			if (scrollAmount != 0) {
				if (root != null) {
					final Vec2 mouse = getMousePos();
					root.onMouseScrolled(mouse, scrollAmount);
				}
			}
		} catch (Throwable e) {
			handleError(e);
		}
	}

	@Override
	public void onGuiClosed() {
		try {
			if (root != null) root.onDestroy();
		} catch (Throwable e) {
			handleError(e);
		}
		Keyboard.enableRepeatEvents(false);
	}

	@Nonnull
	public Vec2 getMousePos() {
		int mouseX = Mouse.getEventX() * width / mc.displayWidth;
		int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
		return new Vec2(mouseX, mouseY);
	}
}
