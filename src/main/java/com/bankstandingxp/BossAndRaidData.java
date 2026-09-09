package com.bankstandingxp;

import java.util.Set;

/**
 * Reference data for "boss or raid" detection.
 * <p>
 * BOSS_NAMES is the OSRS Wiki's "Category:Bosses" listing (lowercased), used to
 * match against whatever NPC the player is currently interacting with - covers
 * world bosses, slayer bosses, quest bosses, wilderness bosses, minigame bosses,
 * etc. Some entries are group/category names (e.g. "Dagannoth Kings", "Barrows")
 * that will never literally match an NPC's name; they're harmless no-ops.
 * <p>
 * TOB_REGION_IDS and TOA_REGION_IDS are sourced from real, actively-maintained
 * RuneLite raid-tracking plugins (Theatre of Blood Stats and Tombs of Amascut),
 * covering the lobby plus every room of each raid. Chambers of Xeric doesn't need
 * a region list - it's detected via VarbitID.RAIDS_CLIENT_INDUNGEON instead.
 */
final class BossAndRaidData
{
	private BossAndRaidData()
	{
	}

	static final Set<Integer> TOB_REGION_IDS = Set.of(
		14642, // lobby
		12613, 12869, // Maiden
		13125, // Bloat
		13122, // Nylocas
		13123, 13379, // Sotetseg (+ maze)
		12612, // Xarpus
		12611, // Verzik
		12867  // throne room
	);

	static final Set<Integer> TOA_REGION_IDS = Set.of(
		13454, // lobby
		14160, // Nexus
		15698, // Crondis
		15700, // Zebak
		14162, // Scabaras
		14164, // Kephri
		15186, // Apmeken
		15188, // Ba-Ba
		14674, // Het
		14676, // Akkha
		15184, 15696, // Wardens
		14672  // Tomb
	);

	static final Set<String> BOSS_NAMES = Set.of(
		"abyssal sire", "agrith naar", "agrith-na-na", "ahrim the blighted", "akkha",
		"alchemical hydra", "amoxliatl", "araxxor", "arrg", "artio",
		"arzinian avatar of magic", "arzinian avatar of ranging", "arzinian avatar of strength",
		"arzinian being of bordanzan", "ba-ba", "barrelchest", "barrows", "barrows brothers",
		"black demon", "black golem", "black knight titan", "blood moon", "blue moon",
		"bouncer", "branda the fire queen", "brutus", "bryophyta", "callisto", "calvar'ion",
		"cerberus", "chaos elemental", "chaos fanatic", "chronozon", "commander zilyana",
		"corporeal beast", "corrupted hunllef", "count draynor", "crazy archaeologist",
		"crystalline hunllef", "culinaromancer", "dad", "dagannoth kings", "dagannoth mother",
		"dagannoth prime", "dagannoth rex", "dagannoth supreme", "damis", "dawn", "delrith",
		"demonic brutus", "deranged archaeologist", "dessourt", "dessous", "dharok the wretched",
		"doom of mokhaiotl", "duke sucellus", "dusk", "eclipse moon", "eldric the ice king",
		"elidinis' warden", "elvarg", "evil spirit", "fareed", "flambeed", "gadderanks",
		"galvek", "gelatinnoth mother", "gemstone crab", "general graardor", "general khazard",
		"giant mole", "giant roc", "giant scarab", "giant sea snake", "glod", "glough",
		"great olm", "grey golem", "grotesque guardians", "guthan the infested", "hespori",
		"ice demon", "ice troll king", "jungle demon", "k'ril tsutsaroth", "kalphite queen",
		"kamil", "karamel", "karil the tainted", "kephri", "king black dragon",
		"koschei the deathless", "kraken", "kree'arra", "the leviathan", "lowerniel drakan",
		"mad angel", "maggot king", "melzar the mad", "the mimic", "moss guardian",
		"muttadile", "nex", "nezikchened", "the nightmare", "nylocas vasilias", "obor",
		"penance queen", "pestilent bloat", "phantom muspah", "phosani's nightmare",
		"revenant maledictus", "salarin the twisted", "sarachnis", "scorpia", "scurrius",
		"scurrius (private)", "sea troll queen", "shellbane gryphon", "sigmund", "sir leye",
		"sir mordred", "skotizo", "slagilith", "slash bash", "slug prince", "sol heredit",
		"sotetseg", "spindel", "tarn", "tekton", "tempoross", "the draugen", "the everlasting",
		"the hueycoatl", "the illusive", "the inadequacy", "the maiden of sugadinti",
		"the untouchable", "thermonuclear smoke devil", "tolna", "torag the corrupted",
		"tree spirit", "treus dayth", "tumeken's warden", "tzkal-zuk", "tztok-jad", "ulfric",
		"vanguard", "vardorvis", "vasa nistirio", "venenatis", "verac the defiled",
		"verzik vitur", "vespula", "vet'ion", "vorkath", "the whisperer", "white golem",
		"wintertodt", "wrathmaw", "xamphur", "xarpus", "yama", "zalcano", "zebak", "zulrah"
	);
}
