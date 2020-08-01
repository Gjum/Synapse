package gjum.minecraft.gui;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static gjum.minecraft.gui.Layoutable1D.computeLayout;

public class TableLayout extends GuiElement {
	private final List<List<GuiElement>> rows = new ArrayList<>();
	private int maxCols = 0;
	@Nullable
	private Layoutable1D[] rowLayouts;
	@Nullable
	private Layoutable1D[] colLayouts;

	// TODO spacing between cells (horizontal and vertical)
	// TODO cell alignment per row/col (left/center/right, top/center/bottom)

	/**
	 * @param row List of cells. May contain nulls for empty cells. May be shorter than other rows; the difference will be treated as nulls.
	 * @return this
	 */
	public TableLayout addRow(List<GuiElement> row) {
		rows.add(row);
		for (GuiElement child : row) {
			if (child == null) continue;
			child.setParent(this);
		}
		if (maxCols < row.size()) maxCols = row.size();
		invalidateLayout();
		return this;
	}

	/**
	 * @param row Must already have been added using addRow(row).
	 * @return this
	 */
	public TableLayout updateRow(List<GuiElement> row) {
		for (GuiElement child : row) {
			if (child == null) continue;
			child.setParent(this);
		}
		if (maxCols < row.size()) maxCols = row.size();
		invalidateLayout();
		return this;
	}

	public TableLayout clear() {
		rows.forEach(row -> row.forEach(GuiElement::onDestroy));
		rows.clear();
		maxCols = 0;
		invalidateLayout();
		return this;
	}

	@Override
	public void invalidateLayout() {
		super.invalidateLayout();
		rowLayouts = null;
		colLayouts = null;
	}

	@Override
	public void updateSize(Vec2 sizeAvail) {
		rowLayouts = new Layoutable1D[rows.size()];
		for (int i = 0; i < rowLayouts.length; i++) {
			rowLayouts[i] = new Layoutable1D(0, 0, 0);
		}
		colLayouts = new Layoutable1D[maxCols];
		for (int i = 0; i < colLayouts.length; i++) {
			colLayouts[i] = new Layoutable1D(0, 0, 0);
		}
		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement cell = row.get(colNr);
				if (cell == null) continue;
				Vec2 minSize = cell.getMinSize();
				Vec2 maxSize = cell.getMaxSize();
				Vec2 weight = cell.getWeight();
				rowLayouts[rowNr].update(minSize.y, maxSize.y, weight.y);
				colLayouts[colNr].update(minSize.x, maxSize.x, weight.x);
			}
		}

		super.updateSize(new Vec2(
				computeLayout(sizeAvail.x, colLayouts),
				computeLayout(sizeAvail.y, rowLayouts)));

		int rowPos = 0;
		for (Layoutable1D rowLayout : rowLayouts) {
			rowLayout.pos = rowPos;
			rowPos += rowLayout.size;
		}
		int colPos = 0;
		for (Layoutable1D colLayout : colLayouts) {
			colLayout.pos = colPos;
			colPos += colLayout.size;
		}

		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement cell = row.get(colNr);
				if (cell == null) continue;
				final Vec2 weight = cell.getWeight();
				final Vec2 minSize = cell.getMinSize();
				cell.updateSize(new Vec2(
						weight.x > 0 ? colLayouts[colNr].size : minSize.x,
						weight.y > 0 ? rowLayouts[rowNr].size : minSize.y));
			}
		}
	}

	@Override
	public void setPos(Vec2 pos) {
		if (rowLayouts == null || rowLayouts.length != rows.size()
				|| colLayouts == null || colLayouts.length != maxCols) {
			throw new IllegalStateException("setPos() was called before setSize()");
		}
		super.setPos(pos);
		for (int rowNr = 0; rowNr < rows.size(); rowNr++) {
			List<GuiElement> row = rows.get(rowNr);
			int y = rowLayouts[rowNr].pos + pos.y;
			for (int colNr = 0; colNr < row.size(); colNr++) {
				GuiElement child = row.get(colNr);
				if (child == null) continue;
				int x = colLayouts[colNr].pos + pos.x;
				child.setPos(new Vec2(x, y));
			}
		}
	}

	@Override
	public Vec2 getSize() {
		if (rowLayouts != null && rowLayouts.length == rows.size()
				&& colLayouts != null && colLayouts.length == maxCols) {
			final Layoutable1D lastCol = colLayouts[colLayouts.length - 1];
			final int colSum = lastCol.pos - colLayouts[0].pos + lastCol.size;
			final Layoutable1D lastRow = rowLayouts[rowLayouts.length - 1];
			final int rowSum = lastRow.pos - rowLayouts[0].pos + lastRow.size;
			return new Vec2(colSum, rowSum);
		}
		int maxSumW = 0;
		int sumMaxH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxSumW = Math.max(maxSumW, sumW);
			sumMaxH += maxH;
		}
		return new Vec2(maxSumW, sumMaxH);
	}

	@Override
	public Vec2 getWeight() {
		int maxX = 0;
		int sumY = 0;
		for (List<GuiElement> row : rows) {
			int sumX = 0;
			int maxY = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 weight = child.getWeight();
				sumX += weight.x;
				maxY = Math.max(maxY, weight.y);
			}
			maxX = Math.max(maxX, sumX);
			sumY += maxY;
		}
		return new Vec2(maxX, sumY);
	}

	@Override
	public Vec2 getMaxSize() {
		int maxW = 0;
		int sumH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getMaxSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxW = Math.max(maxW, sumW);
			sumH += maxH;
		}
		return new Vec2(maxW, sumH);
	}

	@Override
	public Vec2 getMinSize() {
		int maxW = 0;
		int sumH = 0;
		for (List<GuiElement> row : rows) {
			int sumW = 0;
			int maxH = 0;
			for (GuiElement child : row) {
				if (child == null) continue;
				final Vec2 size = child.getMinSize();
				sumW += size.x;
				maxH = Math.max(maxH, size.y);
			}
			maxW = Math.max(maxW, sumW);
			sumH += maxH;
		}
		return new Vec2(maxW, sumH);
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.draw(mouse, winSize, partialTicks);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean drawOverlays(Vec2 mouse, Vec2 winSize, float partialTicks) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.drawOverlays(mouse, winSize, partialTicks)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.onMouseClicked(mouse, mouseButton)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public void onMouseDragged(Vec2 mouse, Vec2 prevMouse, Vec2 dragStart, int mouseButton, long timeSinceLastClick) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.onMouseDragged(mouse, prevMouse, dragStart, mouseButton, timeSinceLastClick);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onMouseReleased(Vec2 mouse, Vec2 dragStart, int state) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.onMouseReleased(mouse, dragStart, state);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onMouseScrolled(Vec2 mouse, int scrollAmount) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					if (child.onMouseScrolled(mouse, scrollAmount)) {
						return true;
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@Override
	public void onKeyTyped(char keyChar, int keyCode) {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.onKeyTyped(keyChar, keyCode);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		for (List<GuiElement> row : rows) {
			for (GuiElement child : row) {
				if (child == null) continue;
				try {
					child.onDestroy();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
}
