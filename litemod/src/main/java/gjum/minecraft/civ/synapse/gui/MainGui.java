package gjum.minecraft.civ.synapse.gui;

import gjum.minecraft.civ.synapse.*;
import gjum.minecraft.civ.synapse.common.Pos;
import gjum.minecraft.gui.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gjum.minecraft.civ.synapse.LiteModSynapse.getStandingColor;
import static gjum.minecraft.civ.synapse.McUtil.*;
import static gjum.minecraft.civ.synapse.common.Util.*;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_CENTER;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_LEFT;
import static gjum.minecraft.gui.Vec2.Direction.COLUMN;
import static gjum.minecraft.gui.Vec2.Direction.ROW;

public class MainGui extends GuiRoot {
	static final Vec2 spacer = new Vec2(7, 7);
	static final Vec2 stretchX = new Vec2(1, 0);

	private static final String someAccountRegex = ".*" + accountNameRegex + ".*";
	private static final String multiAccountsRegex = ".*" + accountNameRegex + accountsSepRegex + accountNameRegex + ".*";

	private final LiteModSynapse mod = LiteModSynapse.instance;

	private final ScheduledExecutorService delayedSearchThreadPool = Executors.newScheduledThreadPool(1);

	@Nullable
	private ScrollBox resultsScroller = null;

	@Nonnull
	private String query = "";
	@Nullable
	private List<PersonOrAccount> results;

	public MainGui(GuiScreen parentScreen) {
		super(parentScreen);
	}

	@Override
	public void rebuild() {
		super.rebuild();
		results = null;
	}

	@Override
	public GuiElement build() {
		final FlexListLayout titleRow = makeTitleRow(this, LiteModSynapse.MOD_NAME);

		final FlexListLayout settingsRow = new FlexListLayout(ROW);
		settingsRow.add(new Button(() -> {
			mod.showGuiAndRemember(new SettingsGui(this));
		}, "Settings").setWeight(stretchX));
		settingsRow.add(new Spacer().setWeight(stretchX));
		if (mod.isServerEnabled()) {
			settingsRow.add(new Button(
					() -> {
						mod.setServerEnabled(false);
						rebuild();
					},
					"Disable on " + mod.gameAddress).setWeight(stretchX));
		} else if (mod.gameAddress != null) {
			settingsRow.add(new Button(() -> {
				mod.config.setModEnabled(true);
				mod.setServerEnabled(true);
				rebuild();
			}, "Enable on " + mod.gameAddress).setWeight(stretchX));
		}
		settingsRow.add(new Spacer().setWeight(stretchX));
		settingsRow.add(new CycleButton<>(val -> {
			mod.config.setModEnabled("Disable mod".equals(val));
			rebuild();
		}, mod.config.isModEnabled() ? "Disable mod" : "Enable mod",
				"Disable mod", "Enable mod").setWeight(stretchX));

		final FlexListLayout content = new FlexListLayout(COLUMN);
		content.add(new Spacer(spacer));
		content.add(titleRow);
		content.add(new Spacer(spacer));
		content.add(settingsRow);
		content.add(new Spacer(spacer));

		if (mod.personsConfig == null) {
			resultsScroller = null;
			content.add(new Spacer());
		} else {
			final Button createPersonBtn = new Button(() -> {
				final String[] words = query.split(accountsSepRegex);
				final Person person = mod.personsConfig.getPersonsRegistry()
						.personByAccountNameOrCreate(words[0]);
				person.setAccounts(Arrays.asList(words));
				mod.showGuiAndRemember(new PersonGui(this, person));
			}, query.matches(multiAccountsRegex) ? "Associate accounts" : "Create person");
			createPersonBtn.setEnabled(query.matches(someAccountRegex));

			final TextField queryField = new TextField(query -> {
				final String prevQuery = this.query;
				this.query = query;
				if (!Objects.equals(prevQuery, query)) {
					debouncedUpdate();
					createPersonBtn.setEnabled(query.matches(someAccountRegex));
					createPersonBtn.setText(query.matches(multiAccountsRegex)
							? "Associate accounts" : "Create person");
				}
				return true;
			}, query, "Search persons, accounts, factions");
			queryField.setFocused(true);
			queryField.setWeight(new Vec2(1, 0));

			final FlexListLayout queryRow = new FlexListLayout(ROW)
					.add(new Tooltip("Make empty to list nearby players",
							queryField))
					.add(createPersonBtn);

			resultsScroller = new ScrollBox(makeResultsTable());
			resultsScroller.setWeight(new Vec2(1, 1));

			content.add(queryRow);
			content.add(new Spacer(spacer));
			content.add(resultsScroller);
			content.add(new Spacer(spacer));
		}

		return new FlexListLayout(ROW)
				.add(new Spacer(spacer))
				.add(content)
				.add(new Spacer(spacer));
	}

	@Nonnull
	private GuiElement makeResultsTable() {
		if (LiteModSynapse.instance.serverConfig == null) {
			return new Label("Connect to a server to manage player associations.", ALIGN_CENTER);
		}
		if (results == null) {
			results = search(query);
		}
		final TableLayout resultsTable = new TableLayout();
		if (!results.isEmpty()) {
			for (PersonOrAccount poa : results) {
				final ArrayList<GuiElement> row = new ArrayList<>();
				resultsTable.addRow(row);
				populatePersonOrAccountRow(poa, row, resultsTable);
				resultsTable.addRow(Collections.singletonList(new Spacer(spacer)));
			}
		} else {
			resultsTable.addRow(Collections.singletonList(new Label(
					"Found no matching persons, accounts, or factions.", ALIGN_CENTER)));
		}
		resultsTable.addRow(Arrays.asList(
				null, null, null, null, null, null, null, null, null, null, null,
				new Spacer()));

		return resultsTable;
	}

	private void populatePersonOrAccountRow(
			@Nonnull PersonOrAccount poa,
			@Nonnull ArrayList<GuiElement> row,
			@Nonnull TableLayout resultsTable
	) {
		final LiteModSynapse mod = LiteModSynapse.instance;
		final Standing standing = poa.getStanding();
		final TextFormatting standingFmt = mod.config.getStandingColor(standing);
		final Color standingColor = FloatColor.fromTextFormatting(standingFmt).toColor();

		row.clear();

		final Button editBtn = new Button(() -> {
			mod.showGuiAndRemember(new PersonGui(this, poa.personOrCreate()));
		}, "edit");
		row.add(editBtn);

		final CycleButton<String> focusBtn = new CycleButton<>(newValue -> {
			if ("unfocus".equals(newValue)) {
				// "focus" was pressed
				if (poa.account != null) mod.announceFocusedAccount(poa.account);
				else mod.announceFocusedAccounts(poa.person.getAccounts());
			} else {
				// "unfocus" was pressed
				mod.setFocusedAccountNames(Collections.emptyList());
			}
			populatePersonOrAccountRow(poa, row, resultsTable);
		}, standing != Standing.FOCUS ? "focus" : "unfocus",
				"focus", "unfocus");
		row.add(focusBtn);

		final Button friendlyBtn = new Button(() -> {
			mod.serverConfig.setPersonStanding(poa.personOrCreate(), Standing.FRIENDLY);
			populatePersonOrAccountRow(poa, row, resultsTable);
		}, "friend");
		friendlyBtn.setEnabled(standing != Standing.FRIENDLY);
		row.add(friendlyBtn);

		final Button hostileBtn = new Button(() -> {
			mod.serverConfig.setPersonStanding(poa.personOrCreate(), Standing.HOSTILE);
			populatePersonOrAccountRow(poa, row, resultsTable);
		}, "hostile");
		hostileBtn.setEnabled(standing != Standing.HOSTILE);
		row.add(hostileBtn);

		final Label nameLabel = new Label(poa.getName(), ALIGN_LEFT);
		if (standingColor != null) nameLabel.setColor(standingColor);
		nameLabel.setClickHandler(() -> mod.showGuiAndRemember(new PersonGui(this, poa.personOrCreate())));

		final String queryLower = query.toLowerCase();

		String accountsStr = poa.getAccounts().stream()
				.map(account -> {
					final float score = scoreSimilarity(queryLower, account.toLowerCase());
					return new Tuple<>(account, score);
				})
				.sorted(Comparator.comparing(Tuple<String, Float>::getSecond)
						.reversed())
				.limit(3)
				.map(Tuple<String, Float>::getFirst)
				.collect(Collectors.joining(" "));
		final int notListedAccounts = poa.getAccounts().size() - 5;
		if (notListedAccounts > 0) {
			accountsStr += " +" + notListedAccounts;
		}
		final Label accountsLabel = new Label(accountsStr, ALIGN_LEFT);
		if (standingColor != null) accountsLabel.setColor(standingColor);
		accountsLabel.setClickHandler(() -> mod.showGuiAndRemember(new PersonGui(this, poa.personOrCreate())));

		final FlexListLayout factionsRow = new FlexListLayout(ROW);
		poa.getFactions().stream().map(faction -> {
			final float score = scoreSimilarity(queryLower, faction.toLowerCase());
			return new Tuple<>(faction, score);
		})
				.sorted(Comparator.comparing(Tuple<String, Float>::getSecond)
						.reversed())
				.limit(3)
				.forEachOrdered(t -> {
					final String faction = t.getFirst();
					final Label label = new Label(faction, ALIGN_LEFT);
					if (mod.serverConfig != null) {
						label.setColor(getStandingColor(mod.serverConfig.getFactionStanding(faction)));
					}
					label.setClickHandler(() -> mod.showGuiAndRemember(new FactionGui(this, faction)));
					factionsRow.add(new Spacer(spacer));
					factionsRow.add(label);
				});
		final int notListedFactions = poa.getFactions().size() - 5;
		if (notListedFactions > 0) {
			factionsRow.add(new Label(" +" + notListedFactions, ALIGN_LEFT));
		}

		row.add(new Spacer(spacer));
		row.add(nameLabel);
		row.add(new Spacer(spacer));
		row.add(new Label("aka: ", ALIGN_LEFT).setColor(Color.GRAY));
		row.add(accountsLabel);
		row.add(new Spacer(spacer));
		row.add(new Label("with:", ALIGN_LEFT).setColor(Color.GRAY));
		row.add(factionsRow);

		resultsTable.updateRow(row);
	}

	private void debouncedUpdate() {
		final String targetQuery = query;
		results = null;
		delayedSearchThreadPool.schedule(() -> {
			mc.addScheduledTask(() -> {
				if (!query.equals(targetQuery)) return; // query was changed during timeout
				if (results != null) return; // already searched for this query
				results = search(query);
				if (resultsScroller != null) {
					resultsScroller.setChild(makeResultsTable());
				}
			});
		}, 300, TimeUnit.MILLISECONDS);
	}

	@Nonnull
	private static List<PersonOrAccount> search(@Nonnull String query) {
		final LiteModSynapse mod = LiteModSynapse.instance;
		if (mod.personsConfig == null) return Collections.emptyList();

		if (query.trim().isEmpty()) {
			final Pos self = getEntityPosition(getMc().player);
			final Stream<PersonOrAccount> radarStream = getMc().world.playerEntities.stream()
					.map(player -> new Tuple<>(getEntityPosition(player),
							new PersonOrAccount(player.getName())))
					.sorted(Comparator.comparing((Tuple<Pos, PersonOrAccount> t) -> {
						final double distSq = self.distanceSq(t.getFirst());
						if (t.getSecond().getStanding() == Standing.UNSET) {
							return distSq - 999; // show accounts without standing first
						} else return distSq;
					}))
					.map(Tuple<Pos, PersonOrAccount>::getSecond);
			final NetHandlerPlayClient conn = getMc().getConnection();
			if (conn == null) {
				return radarStream.collect(Collectors.toList());
			} else {
				final Stream<PersonOrAccount> tablistStream = conn.getPlayerInfoMap().stream()
						.map(player -> new PersonOrAccount(getDisplayNameFromTablist(player)));
				return Stream.concat(radarStream, tablistStream)
						.distinct()
						.collect(Collectors.toList());
			}
		}

		Stream<Tuple<PersonOrAccount, Float>> resultsStream = mod
				.accountsConfig.findSimilarScoredStream(query)
				.map(t -> new Tuple<>(new PersonOrAccount(t.getFirst()), t.getSecond()));

		final PersonsRegistry personsRegistry = mod.getPersonsRegistry();
		if (personsRegistry != null) {
			resultsStream = Stream.concat(resultsStream, personsRegistry.findSimilarScoredStream(query)
					.map(t -> new Tuple<>(new PersonOrAccount(t.getFirst()), t.getSecond())));
		}

		return resultsStream
				.sorted(Comparator.comparing(Tuple<PersonOrAccount, Float>::getSecond).reversed())
				.map(Tuple::getFirst)
				.distinct()
				.limit(20)
				.collect(Collectors.toList());
	}

	@Nonnull
	public static FlexListLayout makeTitleRow(@Nonnull GuiRoot gui, @Nonnull String title) {
		final LiteModSynapse mod = LiteModSynapse.instance;
		final FlexListLayout titleRow = new FlexListLayout(ROW);
		if (!(gui instanceof MainGui)) {
			titleRow.add(new Button(
					() -> mod.showGuiAndRemember(new MainGui(null)),
					"Home"));
		}
		if (gui.parentScreen != null) {
			titleRow.add(new Button(
					() -> mod.showGuiAndRemember(gui.parentScreen),
					"Back").setWeight(stretchX));
		}
		if (gui instanceof MainGui && gui.parentScreen == null) {
			// no buttons on the left side; fill space with spacer to keep title text centered
			titleRow.add(new Spacer().setWeight(stretchX));
		}

		titleRow.add(new Spacer().setWeight(stretchX));
		titleRow.add(new Label(title, ALIGN_CENTER)
				.setClickHandler(() -> mod.showGuiAndRemember(new MainGui(null))));
		titleRow.add(new Spacer().setWeight(stretchX));
		titleRow.add(new Button(
				() -> mod.showGuiAndRemember(null),
				"Close").setWeight(stretchX));
		return titleRow;
	}
}
