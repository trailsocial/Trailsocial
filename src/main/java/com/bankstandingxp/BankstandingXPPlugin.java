package com.bankstandingxp;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Bankstanding XP",
	description = "Tracks how long you stand idle near a bank and levels up a joke 'Bankstanding' skill for it",
	tags = {"bank", "xp", "level", "skill", "afk", "idle", "meme", "bankstanding"}
)
public class BankstandingXPPlugin extends Plugin
{
	private static final double SECONDS_PER_TICK = 0.6;
	private static final int LEVEL_UP_SPOTANIM_KEY = 1337;
	private static final int XP_PER_HOUR = 300;
	private static final int BANK_RADIUS = 12;
	private static final int IDLE_THRESHOLD_SECONDS = 30;

	@Inject
	private Client client;

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
	private BankstandingXPConfig config;

	private final Map<Integer, Boolean> bankObjectCache = new HashMap<>();
	private final Map<Integer, Boolean> bankNpcCache = new HashMap<>();

	private BankstandingXPPanel panel;
	private NavigationButton navButton;

	private double xp = 0;
	private long lastWholeXp = 0;
	private int idleTicks = 0;
	private int saveCounter = 0;
	private long currentAccountHash = -1;
	private WorldPoint lastLocation;
	private BankstandingStatus status = BankstandingStatus.NOT_LOGGED_IN;

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

		overlayManager.add(overlay);
		overlayManager.add(xpDropOverlay);

		panel = new BankstandingXPPanel(config, BANK_RADIUS, IDLE_THRESHOLD_SECONDS, this::onOverlayToggle, this::resetXp);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Bankstanding XP")
			.icon(icon)
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loadXp();
		}
	}

	@Override
	protected void shutDown()
	{
		saveXp();
		overlayManager.remove(overlay);
		overlayManager.remove(xpDropOverlay);
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

		boolean nearBank = isNearBank(player);

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
		SwingUtilities.invokeLater(() -> panel.update(displayXp, displayStatus));
	}

	long getXp()
	{
		return (long) Math.floor(xp);
	}

	BankstandingStatus getStatus()
	{
		return status;
	}

	private String xpConfigKey(long accountHash)
	{
		return "xp_" + accountHash;
	}

	private void loadXp()
	{
		currentAccountHash = client.getAccountHash();
		String value = configManager.getConfiguration(BankstandingXPConfig.GROUP, xpConfigKey(currentAccountHash));
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
		refreshPanel();
	}

	private void saveXp()
	{
		if (currentAccountHash == -1)
		{
			return;
		}
		configManager.setConfiguration(BankstandingXPConfig.GROUP, xpConfigKey(currentAccountHash), Double.toString(xp));
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
