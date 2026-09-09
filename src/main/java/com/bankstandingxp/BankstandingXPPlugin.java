package com.bankstandingxp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@PluginDescriptor(
	name = "TrailSocial",
	description = "A joke Bankstanding skill, Clan Hall/bossing stat tracking, and trailsocial.net event notifications",
	tags = {"trailsocial", "bank", "xp", "level", "skill", "afk", "idle", "meme", "clan hall", "events"}
)
public class BankstandingXPPlugin extends Plugin
{
	private static final double SECONDS_PER_TICK = 0.6;
	private static final int LEVEL_UP_SPOTANIM_KEY = 1337;
	private static final int XP_PER_HOUR = 300;
	private static final int BANK_RADIUS = 12;
	private static final int IDLE_THRESHOLD_SECONDS = 30;

	// Derived from the OSRS wiki's Clan Hall coordinates (1760, 5473): regionId = (x >> 6 << 8) | (y >> 6)
	private static final int CLAN_HALL_REGION_ID = 6997;
	// Gilded chainbody, per the official Grand Exchange item id (obj=20149)
	private static final int GILDED_CHAINBODY_ITEM_ID = 20149;
	// Trailblazer cane (tier 3 Leagues relic hunter reward), per the official Grand Exchange item id (obj=25013)
	private static final int TRAILBLAZER_CANE_ITEM_ID = 25013;

	private static final String EVENTS_URL = "https://trailsocial.net/.netlify/functions/get-event";
	private static final int EVENTS_POLL_INITIAL_DELAY_SECONDS = 15;
	private static final int EVENTS_POLL_PERIOD_SECONDS = 300;
	private static final Type EVENT_LIST_TYPE = new TypeToken<List<TrailSocialEvent>>()
	{
	}.getType();
	private static final Set<Integer> SITTING_ANIMATION_IDS = Set.of(
		AnimationID.SITTING_READY,
		AnimationID.SITTING_READY_NORTH,
		AnimationID.SITTING_READY_EAST,
		AnimationID.SITTING_READY_SOUTH,
		AnimationID.SITTING_READY_WEST,
		AnimationID.SITTING_READY_BACKWARDS,
		AnimationID.HUMAN_SITTINGDOWN_READY,
		AnimationID.HUMAN_SITTINGDOWN_SCRATCH,
		AnimationID.HUMAN_SITTINGDOWN_THINK,
		AnimationID.HUMAN_SITTINGDOWN_MOVE,
		AnimationID.SITTING_EATING
	);

	@Inject
	private Client client;

	@Inject
	private BossRaidStatsOverlay bossRaidOverlay;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Notifier notifier;

	@Inject
	private BankstandingXPOverlay overlay;

	@Inject
	private BankstandingXpDropOverlay xpDropOverlay;

	@Inject
	private ClanHallStatsOverlay clanHallOverlay;

	@Inject
	private BankstandingXPConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private EventSplashOverlay eventSplashOverlay;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private ClientThread clientThread;

	private final Map<Integer, Boolean> bankObjectCache = new HashMap<>();
	private final Map<Integer, Boolean> bankNpcCache = new HashMap<>();
	private final Map<Integer, Integer> lastInventoryCounts = new HashMap<>();
	private final Set<String> knownEventIds = new HashSet<>();

	private BankstandingXPPanel panel;
	private NavigationButton navButton;
	private BufferedImage normalNavIcon;
	private BufferedImage badgedNavIcon;
	private ScheduledFuture<?> eventsPollTask;
	private List<TrailSocialEvent> recentEvents = List.of();
	private boolean hasUnseenEvents = false;
	private boolean splashShownThisSession = false;

	private double xp = 0;
	private long lastWholeXp = 0;
	private int idleTicks = 0;
	private int saveCounter = 0;
	private long currentAccountHash = -1;
	private WorldPoint lastLocation;

	private boolean nearBank = false;
	private boolean inClanHall = false;
	private long balloonsPopped = 0;
	private long gildedChainsPickedUp = 0;
	private double secondsSpentSitting = 0;
	private long clanHallWealthPickedUp = 0;
	private double bankstandingSeconds = 0;
	private BankstandingStatus status = BankstandingStatus.NOT_LOGGED_IN;

	private boolean inRaid = false;
	private boolean fightingBoss = false;
	private long bossDeaths = 0;
	private long gnomesKilled = 0;
	private double caneHeldSeconds = 0;

	@Provides
	BankstandingXPConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BankstandingXPConfig.class);
	}

	@Override
	protected void startUp()
	{
		bankObjectCache.clear();
		bankNpcCache.clear();
		idleTicks = 0;
		saveCounter = 0;
		lastLocation = null;
		currentAccountHash = -1;
		xp = 0;
		lastWholeXp = 0;
		status = BankstandingStatus.NOT_LOGGED_IN;
		nearBank = false;
		inClanHall = false;
		balloonsPopped = 0;
		gildedChainsPickedUp = 0;
		secondsSpentSitting = 0;
		clanHallWealthPickedUp = 0;
		bankstandingSeconds = 0;
		lastInventoryCounts.clear();
		inRaid = false;
		fightingBoss = false;
		splashShownThisSession = false;
		hasUnseenEvents = false;
		recentEvents = List.of();
		knownEventIds.clear();
		knownEventIds.addAll(loadKnownEventIds());

		overlayManager.add(overlay);
		overlayManager.add(xpDropOverlay);
		overlayManager.add(clanHallOverlay);
		overlayManager.add(bossRaidOverlay);
		overlayManager.add(eventSplashOverlay);

		BufferedImage logoImage = ImageUtil.loadImageResource(getClass(), "logo.png");
		panel = new BankstandingXPPanel(config, logoImage, BANK_RADIUS, IDLE_THRESHOLD_SECONDS, this::onOverlayToggle, this::resetXp);
		normalNavIcon = ImageUtil.loadImageResource(getClass(), "icon.png");
		badgedNavIcon = createBadgedIcon(normalNavIcon);
		navButton = NavigationButton.builder()
			.tooltip("TrailSocial")
			.icon(normalNavIcon)
			.priority(6)
			.panel(panel)
			.onClick(this::acknowledgeEvents)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loadXp();
		}

		eventsPollTask = executor.scheduleWithFixedDelay(this::pollEvents,
			EVENTS_POLL_INITIAL_DELAY_SECONDS, EVENTS_POLL_PERIOD_SECONDS, TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown()
	{
		saveXp();
		if (eventsPollTask != null)
		{
			eventsPollTask.cancel(false);
			eventsPollTask = null;
		}
		overlayManager.remove(overlay);
		overlayManager.remove(xpDropOverlay);
		overlayManager.remove(clanHallOverlay);
		overlayManager.remove(bossRaidOverlay);
		overlayManager.remove(eventSplashOverlay);
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
		lastLocation = null;
		bankObjectCache.clear();
		bankNpcCache.clear();
	}

	private void onOverlayToggle(boolean selected)
	{
		configManager.setConfiguration(BankstandingXPConfig.GROUP, "showOverlay", selected);
	}

	private void resetXp()
	{
		xp = 0;
		lastWholeXp = 0;
		idleTicks = 0;
		saveCounter = 0;
		saveXp();
		refreshPanel();
	}

	private void pollEvents()
	{
		Request request = new Request.Builder().url(EVENTS_URL).build();
		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to fetch trailsocial.net events", e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						return;
					}

					List<TrailSocialEvent> events = gson.fromJson(body.string(), EVENT_LIST_TYPE);
					if (events == null)
					{
						return;
					}

					clientThread.invoke(() -> onEventsFetched(events));
				}
				catch (Exception e)
				{
					log.debug("Failed to parse trailsocial.net events", e);
				}
			}
		});
	}

	private void onEventsFetched(List<TrailSocialEvent> events)
	{
		recentEvents = events;

		List<TrailSocialEvent> newEvents = events.stream()
			.filter(e -> e.id != null && !knownEventIds.contains(e.id))
			.collect(Collectors.toList());

		if (!newEvents.isEmpty())
		{
			for (TrailSocialEvent event : newEvents)
			{
				knownEventIds.add(event.id);
			}
			saveKnownEventIds();

			hasUnseenEvents = true;
			updateNavIcon(true);

			if (!splashShownThisSession)
			{
				eventSplashOverlay.show(newEvents);
				splashShownThisSession = true;
			}

			for (TrailSocialEvent event : newEvents)
			{
				String message = "New TrailSocial event: " + event.title
					+ (event.date != null && !event.date.isEmpty() ? " (" + event.date + ")" : "");
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
			}
			notifier.notify(newEvents.size() == 1
				? "New TrailSocial event: " + newEvents.get(0).title
				: newEvents.size() + " new TrailSocial events posted");
		}

		refreshPanel();
	}

	private void acknowledgeEvents()
	{
		if (!hasUnseenEvents)
		{
			return;
		}
		hasUnseenEvents = false;
		updateNavIcon(false);
	}

	private void updateNavIcon(boolean badged)
	{
		BufferedImage targetIcon = badged ? badgedNavIcon : normalNavIcon;
		if (navButton == null || navButton.getIcon() == targetIcon)
		{
			return;
		}

		SwingUtilities.invokeLater(() -> {
			if (navButton == null)
			{
				return;
			}
			clientToolbar.removeNavigation(navButton);
			navButton = NavigationButton.builder()
				.tooltip("TrailSocial")
				.icon(targetIcon)
				.priority(6)
				.panel(panel)
				.onClick(this::acknowledgeEvents)
				.build();
			clientToolbar.addNavigation(navButton);
		});
	}

	private static BufferedImage createBadgedIcon(BufferedImage base)
	{
		BufferedImage badged = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = badged.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(base, 0, 0, null);

		int dotSize = Math.max(8, base.getWidth() / 3);
		int x = base.getWidth() - dotSize - 1;
		int y = 1;
		g.setColor(Color.RED);
		g.fillOval(x, y, dotSize, dotSize);
		g.setColor(Color.WHITE);
		g.drawOval(x, y, dotSize, dotSize);
		g.dispose();

		return badged;
	}

	private Set<String> loadKnownEventIds()
	{
		String value = configManager.getConfiguration(BankstandingXPConfig.GROUP, "knownEventIds");
		if (value == null || value.isEmpty())
		{
			return Set.of();
		}
		return Arrays.stream(value.split(","))
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toSet());
	}

	private void saveKnownEventIds()
	{
		configManager.setConfiguration(BankstandingXPConfig.GROUP, "knownEventIds", String.join(",", knownEventIds));
	}

	List<TrailSocialEvent> getRecentEvents()
	{
		return recentEvents;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!BankstandingXPConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("showOverlay".equals(event.getKey()) && panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.setOverlaySelected(config.showOverlay()));
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();

		if (state == GameState.LOGGED_IN)
		{
			if (currentAccountHash == -1)
			{
				loadXp();
			}
			idleTicks = 0;
			lastLocation = null;
		}
		else if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING || state == GameState.CONNECTION_LOST)
		{
			saveXp();
			currentAccountHash = -1;
			idleTicks = 0;
			lastLocation = null;
			setStatus(BankstandingStatus.NOT_LOGGED_IN);
			refreshPanel();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		WorldPoint location = player.getWorldLocation();
		boolean moved = lastLocation == null || !lastLocation.equals(location);
		lastLocation = location;

		LocalPoint localPoint = player.getLocalLocation();
		int instanceRegionId = localPoint != null ? WorldPoint.fromLocalInstance(client, localPoint).getRegionID() : -1;

		inClanHall = instanceRegionId == CLAN_HALL_REGION_ID;
		if (inClanHall && SITTING_ANIMATION_IDS.contains(player.getAnimation()))
		{
			secondsSpentSitting += SECONDS_PER_TICK;
		}

		boolean inCox = client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
		boolean inTob = BossAndRaidData.TOB_REGION_IDS.contains(instanceRegionId);
		boolean inToa = BossAndRaidData.TOA_REGION_IDS.contains(instanceRegionId);
		inRaid = inCox || inTob || inToa;
		fightingBoss = isFightingBoss(player);

		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		if (equipment != null && equipment.contains(TRAILBLAZER_CANE_ITEM_ID))
		{
			caneHeldSeconds += SECONDS_PER_TICK;
		}

		nearBank = isNearBank(player);

		if (moved || !nearBank)
		{
			idleTicks = 0;
			setStatus(nearBank ? BankstandingStatus.MOVING : BankstandingStatus.NOT_NEAR_BANK);
			refreshPanel();
			return;
		}

		idleTicks++;

		int thresholdTicks = (int) Math.round(IDLE_THRESHOLD_SECONDS / SECONDS_PER_TICK);
		if (idleTicks < thresholdTicks)
		{
			setStatus(BankstandingStatus.WAITING);
			refreshPanel();
			return;
		}

		bankstandingSeconds += SECONDS_PER_TICK;

		if (xp >= BankstandingXP.MAX_XP)
		{
			setStatus(BankstandingStatus.MAX_LEVEL);
			refreshPanel();
			return;
		}

		int levelBefore = BankstandingXP.getLevelForXp((long) Math.floor(xp));

		double xpGain = XP_PER_HOUR * (SECONDS_PER_TICK / 3600.0);
		xp = Math.min(BankstandingXP.MAX_XP, xp + xpGain);

		long wholeXp = (long) Math.floor(xp);
		if (wholeXp > lastWholeXp)
		{
			xpDropOverlay.addDrop(wholeXp - lastWholeXp);
			lastWholeXp = wholeXp;
		}

		int levelAfter = BankstandingXP.getLevelForXp(wholeXp);
		if (levelAfter > levelBefore && config.notifyLevelUp())
		{
			announceLevelUp(levelAfter);
		}

		setStatus(xp >= BankstandingXP.MAX_XP ? BankstandingStatus.MAX_LEVEL : BankstandingStatus.GAINING_XP);

		if (++saveCounter >= 10)
		{
			saveCounter = 0;
			saveXp();
		}

		refreshPanel();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (inClanHall && "Burst".equals(event.getMenuOption()))
		{
			balloonsPopped++;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		Map<Integer, Integer> currentCounts = new HashMap<>();
		for (Item item : event.getItemContainer().getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			currentCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
		}

		if (inClanHall)
		{
			for (Map.Entry<Integer, Integer> entry : currentCounts.entrySet())
			{
				int itemId = entry.getKey();
				int newCount = entry.getValue();
				int oldCount = lastInventoryCounts.getOrDefault(itemId, 0);
				if (newCount <= oldCount)
				{
					continue;
				}

				int delta = newCount - oldCount;
				if (itemId == GILDED_CHAINBODY_ITEM_ID)
				{
					gildedChainsPickedUp += delta;
				}
				clanHallWealthPickedUp += (long) itemManager.getItemPrice(itemId) * delta;
			}
		}

		lastInventoryCounts.clear();
		lastInventoryCounts.putAll(currentCounts);
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		Actor actor = event.getActor();
		Player player = client.getLocalPlayer();

		if (actor == player)
		{
			if (inRaid || fightingBoss)
			{
				bossDeaths++;
				saveXp();
			}
			return;
		}

		if (player != null && actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			String name = npc.getName();
			boolean killedByMe = npc.getInteracting() == player || player.getInteracting() == npc;
			if (killedByMe && name != null && name.equalsIgnoreCase("Gnome"))
			{
				gnomesKilled++;
				saveXp();
				refreshPanel();
			}
		}
	}

	private boolean isFightingBoss(Player player)
	{
		Actor target = player.getInteracting();
		if (!(target instanceof NPC))
		{
			return false;
		}

		NPCComposition comp = ((NPC) target).getComposition();
		String name = comp != null ? comp.getName() : null;
		return name != null && BossAndRaidData.BOSS_NAMES.contains(name.toLowerCase());
	}

	private void announceLevelUp(int level)
	{
		String message = "Congratulations, you've just advanced a Bankstanding level. You are now level " + level + ".";
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
		notifier.notify("Bankstanding level up: " + level);

		Player player = client.getLocalPlayer();
		if (player != null)
		{
			player.createSpotAnim(LEVEL_UP_SPOTANIM_KEY, SpotanimID.LEVELUP_ANIM, 0, 0);
		}
	}

	private void setStatus(BankstandingStatus newStatus)
	{
		this.status = newStatus;
	}

	private void refreshPanel()
	{
		if (panel == null)
		{
			return;
		}
		long displayXp = (long) Math.floor(xp);
		BankstandingStatus displayStatus = status;
		long displayBankstandingSeconds = (long) Math.floor(bankstandingSeconds);
		long displayClanHallWealth = clanHallWealthPickedUp;
		long displayBalloonsPopped = balloonsPopped;
		long displayGildedChains = gildedChainsPickedUp;
		long displaySittingSeconds = (long) Math.floor(secondsSpentSitting);
		long displayBossDeaths = bossDeaths;
		long displayGnomesKilled = gnomesKilled;
		long displayCaneHeldSeconds = (long) Math.floor(caneHeldSeconds);
		List<TrailSocialEvent> displayEvents = recentEvents;
		SwingUtilities.invokeLater(() -> {
			panel.update(displayXp, displayStatus);
			panel.updateStats(displayBankstandingSeconds, displayClanHallWealth, displayBalloonsPopped,
				displayGildedChains, displaySittingSeconds, displayBossDeaths, displayGnomesKilled, displayCaneHeldSeconds);
			panel.updateEvents(displayEvents);
		});
	}

	long getXp()
	{
		return (long) Math.floor(xp);
	}

	BankstandingStatus getStatus()
	{
		return status;
	}

	boolean isInClanHall()
	{
		return inClanHall;
	}

	long getBalloonsPopped()
	{
		return balloonsPopped;
	}

	long getGildedChainsPickedUp()
	{
		return gildedChainsPickedUp;
	}

	long getSecondsSpentSitting()
	{
		return (long) Math.floor(secondsSpentSitting);
	}

	long getClanHallWealthPickedUp()
	{
		return clanHallWealthPickedUp;
	}

	long getBankstandingSeconds()
	{
		return (long) Math.floor(bankstandingSeconds);
	}

	boolean isNearBank()
	{
		return nearBank;
	}

	boolean isInRaid()
	{
		return inRaid;
	}

	boolean isFightingBoss()
	{
		return fightingBoss;
	}

	long getBossDeaths()
	{
		return bossDeaths;
	}

	long getGnomesKilled()
	{
		return gnomesKilled;
	}

	long getCaneHeldSeconds()
	{
		return (long) Math.floor(caneHeldSeconds);
	}

	private String accountKey(String prefix, long accountHash)
	{
		return prefix + "_" + accountHash;
	}

	private void loadXp()
	{
		currentAccountHash = client.getAccountHash();

		String value = configManager.getConfiguration(BankstandingXPConfig.GROUP, accountKey("xp", currentAccountHash));
		double loaded = 0;
		if (value != null)
		{
			try
			{
				loaded = Double.parseDouble(value);
			}
			catch (NumberFormatException e)
			{
				loaded = 0;
			}
		}
		xp = Math.max(0, Math.min(loaded, BankstandingXP.MAX_XP));
		lastWholeXp = (long) Math.floor(xp);

		balloonsPopped = parseLongConfig(accountKey("chBalloons", currentAccountHash));
		gildedChainsPickedUp = parseLongConfig(accountKey("chChains", currentAccountHash));
		secondsSpentSitting = parseDoubleConfig(accountKey("chSitting", currentAccountHash));
		clanHallWealthPickedUp = parseLongConfig(accountKey("chWealth", currentAccountHash));
		bankstandingSeconds = parseDoubleConfig(accountKey("bsTime", currentAccountHash));
		bossDeaths = parseLongConfig(accountKey("bossDeaths", currentAccountHash));
		gnomesKilled = parseLongConfig(accountKey("gnomesKilled", currentAccountHash));
		caneHeldSeconds = parseDoubleConfig(accountKey("caneHeld", currentAccountHash));

		lastInventoryCounts.clear();
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory != null)
		{
			for (Item item : inventory.getItems())
			{
				if (item.getId() <= 0)
				{
					continue;
				}
				lastInventoryCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}

		refreshPanel();
	}

	private long parseLongConfig(String key)
	{
		String value = configManager.getConfiguration(BankstandingXPConfig.GROUP, key);
		if (value == null)
		{
			return 0;
		}
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private double parseDoubleConfig(String key)
	{
		String value = configManager.getConfiguration(BankstandingXPConfig.GROUP, key);
		if (value == null)
		{
			return 0;
		}
		try
		{
			return Double.parseDouble(value);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private void saveXp()
	{
		if (currentAccountHash == -1)
		{
			return;
		}
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("xp", currentAccountHash), Double.toString(xp));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("chBalloons", currentAccountHash), Long.toString(balloonsPopped));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("chChains", currentAccountHash), Long.toString(gildedChainsPickedUp));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("chSitting", currentAccountHash), Double.toString(secondsSpentSitting));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("chWealth", currentAccountHash), Long.toString(clanHallWealthPickedUp));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("bsTime", currentAccountHash), Double.toString(bankstandingSeconds));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("bossDeaths", currentAccountHash), Long.toString(bossDeaths));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("gnomesKilled", currentAccountHash), Long.toString(gnomesKilled));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("caneHeld", currentAccountHash), Double.toString(caneHeldSeconds));
	}

	private boolean isNearBank(Player player)
	{
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null)
		{
			return false;
		}

		WorldView worldView = client.getTopLevelWorldView();
		Scene scene = worldView.getScene();
		Tile[][][] tiles = scene.getTiles();
		int plane = worldView.getPlane();
		int radius = BANK_RADIUS;

		int playerSceneX = localPoint.getSceneX();
		int playerSceneY = localPoint.getSceneY();

		int minX = Math.max(0, playerSceneX - radius);
		int maxX = Math.min(Constants.SCENE_SIZE - 1, playerSceneX + radius);
		int minY = Math.max(0, playerSceneY - radius);
		int maxY = Math.min(Constants.SCENE_SIZE - 1, playerSceneY + radius);

		for (int x = minX; x <= maxX; x++)
		{
			int dx = x - playerSceneX;
			for (int y = minY; y <= maxY; y++)
			{
				int dy = y - playerSceneY;
				if (Math.max(Math.abs(dx), Math.abs(dy)) > radius)
				{
					continue;
				}

				Tile tile = tiles[plane][x][y];
				if (tile != null && tileHasBankObject(tile))
				{
					return true;
				}
			}
		}

		for (NPC npc : worldView.npcs())
		{
			if (npc == null)
			{
				continue;
			}

			LocalPoint npcLocation = npc.getLocalLocation();
			if (npcLocation == null)
			{
				continue;
			}

			int dx = npcLocation.getSceneX() - playerSceneX;
			int dy = npcLocation.getSceneY() - playerSceneY;
			if (Math.max(Math.abs(dx), Math.abs(dy)) > radius)
			{
				continue;
			}

			if (isBankNpc(npc.getId()))
			{
				return true;
			}
		}

		return false;
	}

	private boolean tileHasBankObject(Tile tile)
	{
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject go : gameObjects)
			{
				if (go != null && isBankObject(go.getId()))
				{
					return true;
				}
			}
		}

		WallObject wall = tile.getWallObject();
		if (wall != null && isBankObject(wall.getId()))
		{
			return true;
		}

		DecorativeObject deco = tile.getDecorativeObject();
		if (deco != null && isBankObject(deco.getId()))
		{
			return true;
		}

		GroundObject ground = tile.getGroundObject();
		if (ground != null && isBankObject(ground.getId()))
		{
			return true;
		}

		return false;
	}

	private boolean isBankObject(int id)
	{
		return bankObjectCache.computeIfAbsent(id, this::computeIsBankObject);
	}

	private boolean computeIsBankObject(int id)
	{
		ObjectComposition comp = client.getObjectDefinition(id);
		if (comp == null)
		{
			return false;
		}

		if (comp.getImpostorIds() != null)
		{
			ObjectComposition impostor = comp.getImpostor();
			if (impostor != null)
			{
				comp = impostor;
			}
		}

		String name = comp.getName();
		if (name == null || name.equalsIgnoreCase("null"))
		{
			return false;
		}

		String lower = name.toLowerCase();
		if (!lower.contains("bank"))
		{
			return false;
		}

		String[] actions = comp.getActions();
		if (actions != null)
		{
			for (String action : actions)
			{
				if (action != null && action.equalsIgnoreCase("Bank"))
				{
					return true;
				}
			}
		}

		return lower.contains("bank booth") || lower.contains("bank chest")
			|| lower.contains("bank counter") || lower.contains("bank table");
	}

	private boolean isBankNpc(int id)
	{
		return bankNpcCache.computeIfAbsent(id, this::computeIsBankNpc);
	}

	private boolean computeIsBankNpc(int id)
	{
		NPCComposition comp = client.getNpcDefinition(id);
		if (comp == null)
		{
			return false;
		}

		if (comp.getConfigs() != null)
		{
			NPCComposition transformed = comp.transform();
			if (transformed != null)
			{
				comp = transformed;
			}
		}

		String name = comp.getName();
		if (name == null || name.equalsIgnoreCase("null"))
		{
			return false;
		}

		String[] actions = comp.getActions();
		if (actions != null)
		{
			for (String action : actions)
			{
				if (action != null && action.equalsIgnoreCase("Bank"))
				{
					return true;
				}
			}
		}

		return name.toLowerCase().contains("banker");
	}
}
