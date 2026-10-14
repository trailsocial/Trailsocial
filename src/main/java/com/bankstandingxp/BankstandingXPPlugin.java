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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
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
import net.runelite.api.MessageNode;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
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
import net.runelite.client.util.Text;
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
	// Derived from the OSRS wiki's Grand Exchange coordinates (3164, 3487): regionId = (x >> 6 << 8) | (y >> 6)
	private static final int GRAND_EXCHANGE_REGION_ID = 12598;
	// Gilded chainbody, per the official Grand Exchange item id (obj=20149)
	private static final int GILDED_CHAINBODY_ITEM_ID = 20149;
	// Trailblazer cane (tier 3 Leagues relic hunter reward), per the official Grand Exchange item id (obj=25013)
	private static final int TRAILBLAZER_CANE_ITEM_ID = TrailSocialOutfit.TRAILBLAZER_CANE;

	private static final String EVENTS_URL = "https://trailsocial.net/.netlify/functions/get-event";
	private static final int EVENTS_POLL_INITIAL_DELAY_SECONDS = 15;
	private static final int EVENTS_POLL_PERIOD_SECONDS = 300;
	private static final int LEADERBOARD_INITIAL_DELAY_SECONDS = 45;
	private static final int LEADERBOARD_PERIOD_SECONDS = 300;
	private static final long BOARD_TTL_MS = 60_000;
	private static final Set<ChatMessageType> CHAT_COMMAND_TYPES = EnumSet.of(
		ChatMessageType.PUBLICCHAT, ChatMessageType.PRIVATECHAT, ChatMessageType.PRIVATECHATOUT,
		ChatMessageType.FRIENDSCHAT, ChatMessageType.CLAN_CHAT, ChatMessageType.CLAN_GUEST_CHAT);
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
		AnimationID.SITTING_EATING,
		AnimationID.HUMAN_SITTING_CHAIR,
		// Clan Hall's own furniture (dining chairs, barstools) uses these
		// instead of the generic POH sitting animations above.
		AnimationID.CLAN_MEDIEVAL_DININGCHAIR_SITTING,
		AnimationID.CLAN_MEDIEVAL_DININGCHAIR_EATING,
		AnimationID.CLAN_MEDIEVAL_BARSTOOL_SITTING,
		AnimationID.CLAN_MEDIEVAL_BARSTOOL_EATING
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

	private final XpDropTracker xpDrops = new XpDropTracker();
	// Bankstanding xp lost since the last game tick, shown as one "-N" popup instead of one per skill.
	private long pendingPenalty = 0;
	// Whether the player was in the Grand Exchange on the last game tick; xp drops there cost nothing.
	private boolean inGrandExchange = false;

	private LeaderboardSync leaderboard;
	private ScheduledFuture<?> leaderboardTask;
	// In-game name of the logged-in account, captured on the client thread; null while logged out.
	private volatile String rsn;
	private volatile long leaderboardPausedUntilMs = 0;

	// Chat commands: other players' stats come from the public leaderboard, cached for a minute.
	// Only touched on the client thread.
	private LeaderboardSync.Board boardCache;
	private long boardFetchedAtMs = 0;
	private boolean boardFetching = false;
	private final List<PendingLookup> pendingLookups = new ArrayList<>();

	private static final class PendingLookup
	{
		private final MessageNode node;
		private final BankstandingChatCommands.Command command;
		private final String sender;

		private PendingLookup(MessageNode node, BankstandingChatCommands.Command command, String sender)
		{
			this.node = node;
			this.command = command;
			this.sender = sender;
		}
	}
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
	private boolean bankInterfaceOpen = false;
	private long balloonsPopped = 0;
	private long gildedChainsPickedUp = 0;
	private double secondsSpentSitting = 0;
	private long clanHallWealthPickedUp = 0;
	private double bankstandingSeconds = 0;
	private double secondsInGe = 0;
	private double secondsOutsideGe = 0;
	private BankstandingStatus status = BankstandingStatus.NOT_LOGGED_IN;

	private boolean inRaid = false;
	private boolean fightingBoss = false;
	private final BossEngagement bossEngagement = new BossEngagement();
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
		bankInterfaceOpen = false;
		balloonsPopped = 0;
		gildedChainsPickedUp = 0;
		secondsSpentSitting = 0;
		clanHallWealthPickedUp = 0;
		bankstandingSeconds = 0;
		secondsInGe = 0;
		secondsOutsideGe = 0;
		xpDrops.reset();
		pendingPenalty = 0;
		lastInventoryCounts.clear();
		inRaid = false;
		fightingBoss = false;
		bossEngagement.reset();
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

		leaderboard = new LeaderboardSync(okHttpClient, gson, LeaderboardSync.ENDPOINT);
		leaderboardPausedUntilMs = 0;
		showInitialLeaderboardStatus();
		leaderboardTask = executor.scheduleWithFixedDelay(this::syncLeaderboard,
			LEADERBOARD_INITIAL_DELAY_SECONDS, LEADERBOARD_PERIOD_SECONDS, TimeUnit.SECONDS);
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
		if (leaderboardTask != null)
		{
			leaderboardTask.cancel(false);
			leaderboardTask = null;
		}
		leaderboard = null;
		rsn = null;
		boardCache = null;
		boardFetching = false;
		pendingLookups.clear();
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

	private String leaderboardCode()
	{
		String code = config.leaderboardCode();
		return code == null ? "" : code.trim();
	}

	private void showLeaderboardStatus(String text, Color color)
	{
		BankstandingXPPanel current = panel;
		if (current != null)
		{
			SwingUtilities.invokeLater(() -> current.updateSyncStatus(text, color));
		}
	}

	private void showInitialLeaderboardStatus()
	{
		if (leaderboardCode().isEmpty())
		{
			showLeaderboardStatus("Leaderboard: off. Paste a link code from your trailsocial.net profile into the plugin settings to join.", Color.GRAY);
		}
		else
		{
			showLeaderboardStatus("Leaderboard: waiting to sync...", Color.YELLOW);
		}
	}

	// Timer entry point (executor thread): hop to the client thread, where the stats live.
	private void syncLeaderboard()
	{
		if (leaderboardCode().isEmpty() || System.currentTimeMillis() < leaderboardPausedUntilMs)
		{
			return;
		}
		clientThread.invoke(this::sendLeaderboardSync);
	}

	// Client thread only.
	private void sendLeaderboardSync()
	{
		String code = leaderboardCode();
		LeaderboardSync sync = leaderboard;
		String name = rsn;
		if (code.isEmpty() || sync == null || name == null || currentAccountHash == -1)
		{
			return;
		}

		LeaderboardSync.Snapshot snapshot = createSnapshot(name);
		if (!snapshot.hasProgress())
		{
			return;
		}

		sync.send(code, snapshot, result -> onLeaderboardResult(result, name));
	}

	// Client thread only.
	private LeaderboardSync.Snapshot createSnapshot(String name)
	{
		return new LeaderboardSync.Snapshot(
			name,
			(long) Math.floor(xp),
			(long) Math.floor(bankstandingSeconds),
			clanHallWealthPickedUp,
			balloonsPopped,
			gildedChainsPickedUp,
			(long) Math.floor(secondsSpentSitting),
			bossDeaths,
			gnomesKilled,
			(long) Math.floor(caneHeldSeconds),
			(long) Math.floor(secondsInGe),
			(long) Math.floor(secondsOutsideGe));
	}

	private void onLeaderboardResult(LeaderboardSync.Result result, String name)
	{
		if (result.kind == LeaderboardSync.Kind.THROTTLED)
		{
			return;
		}

		long pause = result.pauseMillis();
		leaderboardPausedUntilMs = pause > 0 ? System.currentTimeMillis() + pause : 0;
		showLeaderboardStatus(result.describe(name),
			result.kind == LeaderboardSync.Kind.SYNCED ? Color.GREEN : result.needsAttention() ? Color.ORANGE : Color.YELLOW);

		if (result.restore != null)
		{
			clientThread.invoke(() -> applyRestore(result.restore, name));
		}
	}

	// The site says this install is behind the saved row (new computer, fresh install, wiped profile), so
	// pull the saved xp and stats down. Takes the larger of each value, so nothing local is ever lost, and
	// the site only ever hands back what it already holds, so this can't raise anyone's total.
	// Client thread only.
	private void applyRestore(LeaderboardSync.Snapshot saved, String name)
	{
		String current = rsn;
		if (currentAccountHash == -1 || current == null || !current.equalsIgnoreCase(name))
		{
			return;
		}

		LeaderboardSync.Snapshot local = createSnapshot(current);
		if (local.covers(saved))
		{
			showLeaderboardStatus("Leaderboard: synced", Color.GREEN);
			return;
		}

		LeaderboardSync.Snapshot merged = local.mergedWith(saved);
		if (merged.xp > local.xp)
		{
			xp = Math.min(merged.xp, BankstandingXP.MAX_XP);
			lastWholeXp = (long) Math.floor(xp);
		}
		if (merged.bankstandingSeconds > local.bankstandingSeconds)
		{
			bankstandingSeconds = merged.bankstandingSeconds;
		}
		clanHallWealthPickedUp = merged.clanHallWealth;
		balloonsPopped = merged.balloons;
		gildedChainsPickedUp = merged.gildedChains;
		if (merged.sittingSeconds > local.sittingSeconds)
		{
			secondsSpentSitting = merged.sittingSeconds;
		}
		bossDeaths = merged.bossDeaths;
		gnomesKilled = merged.gnomesKilled;
		if (merged.caneHeldSeconds > local.caneHeldSeconds)
		{
			caneHeldSeconds = merged.caneHeldSeconds;
		}
		if (merged.geSecondsIn > local.geSecondsIn)
		{
			secondsInGe = merged.geSecondsIn;
		}
		if (merged.geSecondsOut > local.geSecondsOut)
		{
			secondsOutsideGe = merged.geSecondsOut;
		}

		saveXp();
		refreshPanel();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "TrailSocial: restored your Bankstanding XP and stats from your leaderboard profile.", null);
		showLeaderboardStatus("Leaderboard: restored your XP and stats from trailsocial.net", Color.GREEN);
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

		if ("leaderboardCode".equals(event.getKey()))
		{
			leaderboardPausedUntilMs = 0;
			showInitialLeaderboardStatus();
			if (!leaderboardCode().isEmpty())
			{
				executor.schedule(this::syncLeaderboard, 2, TimeUnit.SECONDS);
			}
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
			if (state == GameState.LOGIN_SCREEN)
			{
				sendLeaderboardSync();
			}
			rsn = null;
			xpDrops.reset();
			bossEngagement.reset();
			pendingPenalty = 0;
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

		if (player.getName() != null)
		{
			rsn = Text.sanitize(player.getName());
		}

		// Baseline once per login, after the client has finished loading every skill's xp.
		if (!xpDrops.isReady())
		{
			xpDrops.baseline(client::getSkillExperience);
		}
		if (pendingPenalty > 0)
		{
			xpDropOverlay.addDrop(-pendingPenalty);
			pendingPenalty = 0;
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

		inGrandExchange = instanceRegionId == GRAND_EXCHANGE_REGION_ID;
		if (inGrandExchange)
		{
			secondsInGe += SECONDS_PER_TICK;
		}
		else
		{
			secondsOutsideGe += SECONDS_PER_TICK;
		}

		boolean inCox = client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
		boolean inTob = BossAndRaidData.TOB_REGION_IDS.contains(instanceRegionId);
		boolean inToa = BossAndRaidData.TOA_REGION_IDS.contains(instanceRegionId);
		inRaid = inCox || inTob || inToa;
		fightingBoss = isFightingBoss(player);
		bossEngagement.tick(fightingBoss || isBossTargeting(player));

		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		boolean fullOutfit = equipment != null && TrailSocialOutfit.isComplete(equipment::contains);
		if (equipment != null && equipment.contains(TRAILBLAZER_CANE_ITEM_ID))
		{
			caneHeldSeconds += SECONDS_PER_TICK;
		}

		nearBank = isNearBank(player);

		// The Grand Exchange is packed with clerks/booths, so players are
		// expected to wander between them - don't require standing still
		// there the way every other bank does.
		if ((moved && !inGrandExchange) || !nearBank)
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

		double xpGain = XP_PER_HOUR * (SECONDS_PER_TICK / 3600.0) * (fullOutfit ? TrailSocialOutfit.XP_MULTIPLIER : 1);
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

	// Skilling costs Bankstanding xp per xp drop; combat xp and anything earned in the Grand Exchange does not (see XpDropTracker.penaltyFor).
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (currentAccountHash == -1 || !xpDrops.isDrop(event.getSkill(), event.getXp()) || xp <= 0)
		{
			return;
		}

		int penalty = XpDropTracker.penaltyFor(event.getSkill(), inGrandExchange);
		if (penalty <= 0)
		{
			return; // combat xp, or xp earned in the Grand Exchange, is free
		}

		double lost = Math.min(penalty, xp);
		xp -= lost;
		lastWholeXp = (long) Math.floor(xp);
		// The popup only ever shows the standard cost per drop; agility's bigger cost is deliberately not shown in the UI.
		pendingPenalty += Math.min(Math.round(lost), XpDropTracker.PENALTY_PER_DROP);
	}

	// !lvl bankstanding, !hours and !gnomes: replace the command with the sender's stats, on this player's screen only.
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.chatCommands() || !CHAT_COMMAND_TYPES.contains(event.getType()))
		{
			return;
		}
		BankstandingChatCommands.Command command = BankstandingChatCommands.parse(Text.removeTags(event.getMessage()));
		if (command == null)
		{
			return;
		}

		String self = rsn;
		// On a private message you sent, the name on the message is the recipient's, not yours.
		String sender = event.getType() == ChatMessageType.PRIVATECHATOUT ? self : Text.sanitize(event.getName());
		if (sender == null || sender.isEmpty())
		{
			return;
		}

		if (self != null && sender.equalsIgnoreCase(self))
		{
			showCommand(event.getMessageNode(), command, ownStats(self), sender);
		}
		else
		{
			lookUp(event.getMessageNode(), command, sender);
		}
	}

	private BankstandingChatCommands.Stats ownStats(String self)
	{
		LeaderboardSync.Board board = boardCache;
		LeaderboardSync.BoardRow onBoard = board == null ? null : board.find(self);
		long wholeXp = (long) Math.floor(xp);
		return new BankstandingChatCommands.Stats(self, BankstandingXP.getLevelForXp(wholeXp), wholeXp,
			onBoard == null ? 0 : onBoard.rank, board == null ? 0 : board.total, createSnapshot(self));
	}

	private void lookUp(MessageNode node, BankstandingChatCommands.Command command, String sender)
	{
		LeaderboardSync sync = leaderboard;
		if (sync == null)
		{
			return;
		}
		if (boardCache != null && System.currentTimeMillis() - boardFetchedAtMs < BOARD_TTL_MS)
		{
			showCommand(node, command, statsFor(boardCache, sender), sender);
			return;
		}

		pendingLookups.add(new PendingLookup(node, command, sender));
		if (!boardFetching)
		{
			boardFetching = true;
			sync.fetchBoard(fetched -> clientThread.invoke(() -> onBoardFetched(fetched)));
		}
	}

	private void onBoardFetched(LeaderboardSync.Board fetched)
	{
		boardFetching = false;
		List<PendingLookup> waiting = new ArrayList<>(pendingLookups);
		pendingLookups.clear();
		if (fetched == null)
		{
			return; // couldn't reach the site: leave the typed commands as they were
		}

		boardCache = fetched;
		boardFetchedAtMs = System.currentTimeMillis();
		for (PendingLookup p : waiting)
		{
			showCommand(p.node, p.command, statsFor(fetched, p.sender), p.sender);
		}
	}

	private static BankstandingChatCommands.Stats statsFor(LeaderboardSync.Board board, String player)
	{
		LeaderboardSync.BoardRow row = board.find(player);
		return row == null ? null : new BankstandingChatCommands.Stats(row.name, row.level, row.xp, row.rank, board.total, row.stats);
	}

	// stats == null means the player isn't on the leaderboard.
	private void showCommand(MessageNode node, BankstandingChatCommands.Command command, BankstandingChatCommands.Stats stats, String player)
	{
		ChatMessageBuilder text = new ChatMessageBuilder();
		if (stats == null)
		{
			text.append(ChatColorType.HIGHLIGHT).append(player)
				.append(ChatColorType.NORMAL).append(" is not on the TrailSocial leaderboard");
		}
		else
		{
			switch (command)
			{
				case LEVEL:
					text.append(ChatColorType.NORMAL).append("Level Bankstanding: ")
						.append(ChatColorType.HIGHLIGHT).append(String.valueOf(stats.level))
						.append(ChatColorType.NORMAL).append(" Experience: ")
						.append(ChatColorType.HIGHLIGHT).append(BankstandingChatCommands.formatNumber(stats.xp));
					if (stats.rank > 0)
					{
						text.append(ChatColorType.NORMAL).append(" Rank: ")
							.append(ChatColorType.HIGHLIGHT).append("#" + stats.rank + " of " + stats.total);
					}
					break;
				default:
					text.append(ChatColorType.NORMAL).append(command.label + ": ")
						.append(ChatColorType.HIGHLIGHT).append(command.value(stats.stats));
					break;
			}
		}
		node.setRuneLiteFormatMessage(text.build());
		client.refreshChat();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankInterfaceOpen = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankInterfaceOpen = false;
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

		// Withdrawing from a bank (Clan Halls have one) increases inventory
		// counts the same way picking up loot does, so don't mistake that
		// for Clan Hall wealth.
		if (inClanHall && !bankInterfaceOpen)
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
			if (inRaid || fightingBoss || bossEngagement.recentlyEngaged())
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
		return target instanceof NPC && isBoss((NPC) target);
	}

	// A boss that has the player as its target counts as a fight even if the player is targeting a
	// minion, or nothing, at that moment.
	private boolean isBossTargeting(Player player)
	{
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc != null && npc.getInteracting() == player && isBoss(npc))
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isBoss(NPC npc)
	{
		NPCComposition comp = npc.getTransformedComposition();
		if (comp == null)
		{
			comp = npc.getComposition();
		}
		return comp != null && BossAndRaidData.isBossName(comp.getName());
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
		long displaySecondsInGe = (long) Math.floor(secondsInGe);
		long displaySecondsOutsideGe = (long) Math.floor(secondsOutsideGe);
		List<TrailSocialEvent> displayEvents = recentEvents;
		SwingUtilities.invokeLater(() -> {
			panel.update(displayXp, displayStatus);
			panel.updateStats(displayBankstandingSeconds, displayClanHallWealth, displayBalloonsPopped,
				displayGildedChains, displaySittingSeconds, displayBossDeaths, displayGnomesKilled, displayCaneHeldSeconds,
				displaySecondsInGe, displaySecondsOutsideGe);
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

	long getSecondsInGe()
	{
		return (long) Math.floor(secondsInGe);
	}

	long getSecondsOutsideGe()
	{
		return (long) Math.floor(secondsOutsideGe);
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
		secondsInGe = parseDoubleConfig(accountKey("geTimeIn", currentAccountHash));
		secondsOutsideGe = parseDoubleConfig(accountKey("geTimeOut", currentAccountHash));

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
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("geTimeIn", currentAccountHash), Double.toString(secondsInGe));
		configManager.setConfiguration(BankstandingXPConfig.GROUP, accountKey("geTimeOut", currentAccountHash), Double.toString(secondsOutsideGe));
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
