package gjum.minecraft.civ.synapse.gui;

import gjum.minecraft.civ.synapse.*;
import gjum.minecraft.civ.synapse.config.ServerConfig;
import gjum.minecraft.gui.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gjum.minecraft.civ.synapse.LiteModSynapse.getStandingColor;
import static gjum.minecraft.civ.synapse.common.Util.*;
import static gjum.minecraft.civ.synapse.gui.MainGui.*;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_LEFT;
import static gjum.minecraft.gui.Label.Alignment.ALIGN_RIGHT;
import static gjum.minecraft.gui.Vec2.Direction.COLUMN;
import static gjum.minecraft.gui.Vec2.Direction.ROW;

public class FactionGui extends GuiRoot {
	private static final int maxAccountSuggestions = 5;

	private String faction;

	private String newName;
	private String addAccountsText = "";
	@Nullable
	private FlexListLayout suggestMembersRow = null;

	final LiteModSynapse mod;
	final PersonsRegistry personsRegistry;

	public FactionGui(GuiScreen parent, String faction) {
		super(parent);
		this.faction = faction;
		newName = this.faction;

		mod = LiteModSynapse.instance;
		personsRegistry = mod.getPersonsRegistry();
	}

	@Override
	public GuiElement build() {
		if (mod.serverConfig == null) {
			mod.showGuiAndRemember(parentScreen);
			return null;
		}
		@Nonnull final ServerConfig serverConfig = mod.serverConfig;

		final FlexListLayout titleRow = makeTitleRow(this, "Faction: " + faction);

		final Button renameFactionBtn = new Button(() -> {
			final Standing standing = serverConfig.getFactionStanding(faction);
			serverConfig.setFactionStanding(faction, null);
			serverConfig.setFactionStanding(newName, standing);
			for (Person member : personsRegistry.personsInFaction(faction)) {
				member.removeFaction(faction);
				member.addFaction(newName);
			}

			faction = newName;
			rebuild();
		}, "Rename faction");

		final FlexListLayout factionRow = new FlexListLayout(ROW);
		factionRow.add(new TextField(text -> {
			final boolean valid = !text.isEmpty()
					&& serverConfig.getFactionStanding(text) != Standing.UNSET;
			renameFactionBtn.setEnabled(valid);
			if (valid) this.newName = text;
			return valid;
		}, newName, "Faction name")
				.setEnterHandler(renameFactionBtn.clickHandler)
				.setWeight(stretchX));
		factionRow.add(renameFactionBtn);

		factionRow.add(new Spacer(spacer));
		factionRow.add(new Spacer().setWeight(stretchX));

		final Button deleteFactionBtn = new Button(() -> {
			for (Person member : personsRegistry.personsInFaction(faction)) {
				member.removeFaction(faction);
			}
			serverConfig.setFactionStanding(faction, Standing.UNSET);
			mod.showGuiAndRemember(parentScreen);
		}, "Delete faction");
		factionRow.add(new Tooltip("Leaves alt associations intact.",
				deleteFactionBtn));

		final Standing currentStanding = serverConfig.getFactionStanding(faction);
		final FlexListLayout standingRow = new FlexListLayout(ROW);
		standingRow.add(new Label("Set standing: ", ALIGN_RIGHT));
		for (Standing standing : Standing.values()) {
			if (standing == Standing.FOCUS) continue;
			final Button btn = new Button(() -> {
				serverConfig.setFactionStanding(faction, standing);
				rebuild();
			}, standing.name().toLowerCase());
			btn.setEnabled(standing != currentStanding);
			standingRow.add(btn);
		}

		suggestMembersRow = new FlexListLayout(ROW);
		addMembersSuggestions();

		final Button addMembersButton = new Button(() -> {
			final String[] split = addAccountsText.split(accountsSepRegex);
			for (String word : split) {
				final Person person = personsRegistry.personByAccountNameOrCreate(word);
				person.addFaction(faction);
			}
			addAccountsText = "";
			addMembersSuggestions();
			rebuild();
		}, "Add members");
		addMembersButton.setEnabled(false);

		final TextField addMembersField = new TextField(text -> {
			final String[] split = text.split(accountsSepRegex);
			final boolean valid = Arrays.stream(split).allMatch(word ->
					word.matches("[A-Za-z0-9_]{3,}"));
			addMembersButton.setEnabled(valid);
			addAccountsText = text; // addAccountsText may be invalid account but valid query
			addMembersSuggestions();
			return valid;
		}, addAccountsText, "Add accounts to faction");
		addMembersField.setWeight(stretchX);
		addMembersField.setFocused(true);
		addMembersField.setEnterHandler(addMembersButton.clickHandler);

		final FlexListLayout addMembersRow = new FlexListLayout(ROW);
		addMembersRow.add(addMembersField);
		addMembersRow.add(addMembersButton);

		// XXX use TableLayout to list members
		final FlexListLayout membersList = new FlexListLayout(COLUMN);
		personsRegistry.personsInFaction(faction).stream()
				.sorted(Comparator.comparing(p -> p.getName().toLowerCase()))
				.forEachOrdered(person -> {
					final FlexListLayout row = new FlexListLayout(ROW);

					final Button removeBtn = new Button(() -> {
						person.removeFaction(faction);
						rebuild();
					}, "X");
					final String tooltip = "Remove " + person.getName() + " from " + faction;
					row.add(new Tooltip(tooltip, removeBtn));
					row.add(new Spacer(spacer));

					final Label personLabel = new Label(person.getName(), ALIGN_LEFT);
					personLabel.setColor(getStandingColor(serverConfig.getStanding(person)));
					personLabel.setClickHandler(() -> mod.showGuiAndRemember(new PersonGui(this, person)));
					row.add(personLabel);

					addOtherFactions(person, row);

					// TODO show some alts of each member

					membersList.add(row);
					membersList.add(new Spacer(spacer));
				});
		membersList.add(new Spacer());

		final FlexListLayout copyMembersRow = new FlexListLayout(ROW);
		final Runnable copyMembersAlts = () -> {
			final String str1 = personsRegistry.personsInFaction(faction).stream()
					.sorted(Comparator.comparing(Person::getName))
					.map(p1 -> p1.getAccounts().stream()
							.sorted(Comparator.comparing(String::toLowerCase))
							.collect(Collectors.joining(" ")))
					.collect(Collectors.joining(" \n"));
			setClipboardString(str1);
		};
		copyMembersRow.add(new Tooltip("One line per person, with all their alts",
				new Button(copyMembersAlts, "Copy members alts")));

		copyMembersRow.add(new Spacer(spacer));

		final Runnable copySortedAccounts = () -> {
			final String str = personsRegistry.personsInFaction(faction).stream()
					.flatMap(p -> p.getAccounts().stream())
					.sorted(Comparator.comparing(String::toLowerCase))
					.collect(Collectors.joining(" "));
			setClipboardString(str);
		};
		copyMembersRow.add(new Tooltip("Accounts sorted by name",
				new Button(copySortedAccounts, "Copy sorted accounts")));

		final FlexListLayout content = new FlexListLayout(COLUMN);
		content.add(new Spacer(spacer));
		content.add(titleRow);
		content.add(new Spacer(spacer));
		content.add(factionRow);
		content.add(new Spacer(spacer));
		content.add(standingRow);
		content.add(new Spacer(spacer));
		content.add(addMembersRow);
		content.add(new Spacer(spacer));
		content.add(suggestMembersRow);
		content.add(new Spacer(spacer));
		content.add(new ScrollBox(membersList)
				.setWeight(new Vec2(1, 1)));
		content.add(new Spacer(spacer));
		content.add(copyMembersRow);
		content.add(new Spacer(spacer));

		return new FlexListLayout(ROW)
				.add(new Spacer(spacer))
				.add(content)
				.add(new Spacer(spacer));
	}

	private void addMembersSuggestions() {
		if (suggestMembersRow == null) return;
		suggestMembersRow.clear();
		final String lastWord = getLastWord(addAccountsText).trim();
		if (lastWord.isEmpty()) return;
		if (mod.getPersonsRegistry() == null) return;
		suggestMembersRow.add(new Label("Suggestions: ", ALIGN_LEFT));
		final Collection<String> memberAccounts = mod.getPersonsRegistry().personsInFaction(faction).stream()
				.flatMap(p -> p.getAccounts().stream())
				.collect(Collectors.toList());
		final Set<String> addAccountsList = lowerCaseSet(Arrays.asList(addAccountsText.split(accountsSepRegex)));
		final Stream<String> suggestions = LiteModSynapse.instance
				.accountsConfig.findSimilarScoredStream(lastWord)
				.map(Tuple::getFirst)
				.filter(a -> !addAccountsList.contains(a.toLowerCase()))
				.limit(maxAccountSuggestions);
		suggestions.forEachOrdered(account -> {
			String text = account;
			if (personsRegistry != null) {
				final Person person = personsRegistry.personByAccountName(account);
				if (person != null) {
					final TextFormatting color = mod.config.getStandingColor(
							mod.serverConfig.getStanding(person));
					text = account + color + " (" + person.getName() + ")";
				}
			}
			final Button btn = new Button(() -> {
				if (personsRegistry != null) {
					personsRegistry.personByAccountNameOrCreate(account)
							.addFaction(faction);
					addAccountsText = replaceLastWord(addAccountsText, account);
				}
				rebuild();
			}, text);
			btn.setWeight(stretchX);
			btn.setEnabled(account != null
					&& !account.isEmpty());
			suggestMembersRow.add(btn);
		});
	}

	private void addOtherFactions(@Nonnull Person person, @Nonnull FlexListLayout row) {
		final Collection<String> otherFactions = person.getFactions();
		otherFactions.remove(Optional.ofNullable(person.hasFaction(faction)).orElse(faction));
		otherFactions.stream()
				.sorted(Comparator.comparing(mod.serverConfig::getFactionStanding))
				.limit(5)
				.forEachOrdered(otherFaction -> {
					final Label ofLabel = new Label(otherFaction, ALIGN_LEFT);
					ofLabel.setColor(getStandingColor(mod.serverConfig.getFactionStanding(otherFaction)));
					ofLabel.setClickHandler(() -> mod.showGuiAndRemember(new FactionGui(this, otherFaction)));
					row.add(new Spacer(spacer));
					row.add(ofLabel);
				});
		final int notListedFactions = otherFactions.size() - 5;
		if (notListedFactions > 0) {
			row.add(new Spacer(spacer));
			row.add(new Label("+" + notListedFactions, ALIGN_LEFT));
		}
	}
}
