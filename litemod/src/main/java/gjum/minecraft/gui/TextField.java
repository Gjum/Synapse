package gjum.minecraft.gui;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.init.SoundEvents;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.function.Predicate;

public class TextField extends Clickable {
	private static final int mutedColor = 0x555566;
	private GuiTextField textField;
	private int textColor = Color.WHITE.getRGB();
	private Predicate<String> validator;
	/**
	 * Displayed in mutedColor when textField is empty.
	 */
	private String hint;
	private boolean enabled = true;
	@Nullable
	private Runnable enterHandler = null;

	private final GuiPageButtonList.GuiResponder guiResponder = new GuiPageButtonList.GuiResponder() {
		@Override
		public void setEntryValue(int i, boolean b) {
		}

		@Override
		public void setEntryValue(int i, float v) {
		}

		@Override
		public void setEntryValue(int i, @Nonnull String s) {
			onChanged();
		}
	};

	public TextField(Predicate<String> validator, String text) {
		this(validator, text, "");
	}

	public TextField(Predicate<String> validator, String text, String hint) {
		this.validator = validator;
		this.hint = hint;

		textField = new GuiTextField(getId(), mc.fontRenderer, 0, 0, 0, 0);
		textField.setMaxStringLength(9999999);
		textField.setGuiResponder(guiResponder);
		if (text != null) setText(text);
		textField.setCursorPositionZero();
		onChanged();

		setMinSize(new Vec2(50, 20));
		setMaxSize(new Vec2(Vec2.LARGE, 20));
	}

	public String getText() {
		return textField.getText();
	}

	public TextField setText(String text) {
		textField.setText(text);
		textField.setCursorPositionEnd();
		onChanged();
		return this;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public TextField setEnabled(boolean enabled) {
		this.enabled = enabled;
		textField.setEnabled(enabled);
		return this;
	}

	public TextField setFocused(boolean focused) {
		if (textField.isFocused()) return this;
		textField.setFocused(focused);
		textField.setCursorPositionEnd();
		return this;
	}

	public TextField setColor(Color color) {
		textColor = color.getRGB();
		textField.setTextColor(textColor);
		return this;
	}

	public TextField setEnterHandler(@Nullable Runnable enterHandler) {
		this.enterHandler = enterHandler;
		return this;
	}

	private void onChanged() {
		if (validator == null) return;
		boolean valid = false;
		try {
			valid = validator.test(getText());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		if (valid) setColor(Color.WHITE);
		else setColor(Color.RED);
	}

	public GuiTextField getTextField() {
		return textField;
	}

	@Override
	public void draw(Vec2 mouse, Vec2 winSize, float partialTicks) {
		textField.drawTextBox();
		if (textField.getText().isEmpty() && hint != null && !hint.isEmpty()) {
			int x = textField.x + 4;
			int y = textField.y + (getSize().y - 4 - 8) / 2;
			final String hintTrimmed = mc.fontRenderer.trimStringToWidth(
					hint, getSize().x - 8);
			mc.fontRenderer.drawString(hintTrimmed, x, y, mutedColor);
		}
	}

	@Override
	public void setPos(Vec2 pos) {
		super.setPos(pos);
		textField.x = pos.x + 2;
		textField.y = pos.y + 2;
	}

	@Override
	public void updateSize(Vec2 size) {
		super.updateSize(size);

		final GuiTextField old = this.textField;
		textField = new GuiTextField(
				old.getId(),
				mc.fontRenderer,
				old.x,
				old.y,
				getSize().x - 4,
				getSize().y - 4);
		textField.setEnabled(enabled);
		textField.setMaxStringLength(old.getMaxStringLength());
		textField.setGuiResponder(guiResponder);
		textField.setText(old.getText());
		textField.setTextColor(textColor);
		textField.setCursorPosition(old.getCursorPosition());
		textField.setFocused(old.isFocused());
	}

	@Override
	public void onKeyTyped(char keyChar, int keyCode) {
		if (textField.isFocused()) {
			textField.textboxKeyTyped(keyChar, keyCode);
			if (keyCode == Keyboard.KEY_RETURN && enterHandler != null) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				try {
					enterHandler.run();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean onMouseClicked(Vec2 mouse, int mouseButton) {
		textField.mouseClicked(mouse.x, mouse.y, mouseButton);
		return false; // allow other text fields to lose focus
	}
}
