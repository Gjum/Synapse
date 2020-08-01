package gjum.minecraft.civ.synapse.gui;

import com.mumfrey.liteloader.modconfig.AbstractConfigPanel;
import com.mumfrey.liteloader.modconfig.ConfigPanelHost;
import gjum.minecraft.civ.synapse.LiteModSynapse;
import net.minecraft.client.gui.GuiButton;

public class PanelGui extends AbstractConfigPanel {
	@Override
	public String getPanelTitle() {
		return LiteModSynapse.MOD_NAME + " Settings";
	}

	public PanelGui() {
	}

	@Override
	protected void addOptions(ConfigPanelHost host) {
		addControl(new GuiButton(0, 0, 0, 200, 20, "Open GUI"), button -> {
			try {
				LiteModSynapse.instance.openLastGui();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void onPanelHidden() {
	}
}
