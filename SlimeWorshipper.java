import org.powerbot.concurrent.Task;
import org.powerbot.concurrent.strategy.Condition;
import org.powerbot.concurrent.strategy.Strategy;
import org.powerbot.game.api.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.widget.Bank;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.map.TilePath;

@Manifest(name = "SlimeWorshipper", description = "Prays at Ectofuntus", authors = { "Agassi" }, version = 1.0)
public class SlimeWorshipper extends ActiveScript {

	private State state = State.CONFIGURE;
	private Prayer prayer = Prayer.BONES; // TODO Allow user to set later

	// Bones Per Trip
	private int bpt;

	// Item IDs
	private static final int RING_OF_KINSHIP = 15707;
	private static final int ECTOPHIAL = 4251;
	private static final int WIDGET_EQUIP = 387;
	private static final int WIDGET_EQUIP_RING = 33;
	private static final int FREMMENIK_BANKER = 9710;
	private static final int BUCKET = 1925;
	private static final int POT = 1931;
	private static final int BUCKET_OF_SLIME = 4286;

	// Areas and Paths
	private static final Area DAEMONHEIM = new Area(new Tile(3440, 3721, 0),
			new Tile(3454, 3688, 0));
	private static final Tile BANKER = new Tile(3448, 3719, 0);
	private static final TilePath PATH_TO_BANKER = new TilePath(new Tile[] {
			new Tile(3443, 3692, 0), new Tile(3447, 3695, 0),
			new Tile(3448, 3700, 0), new Tile(3448, 3704, 0),
			new Tile(3449, 3708, 0), new Tile(3449, 3711, 0),
			new Tile(3449, 3715, 0), new Tile(3448, 3718, 0) });

	@Override
	protected void setup() {
		Configure configure = new Configure();

		provide(new Strategy(configure, configure));
	}

	private class Configure implements Task, Condition {

		@Override
		public void run() {
			// Check for proper equipment
			boolean equip = checkEquipment();
			if (!equip) {
				log.severe("Ectophial or Ring of Kinship missing!");
				stop();
			}

			if (!isIn(DAEMONHEIM))
				teleportToBank();

			goToBank();

			NPC banker = NPCs.getNearest(FREMMENIK_BANKER);
			banker.interact("Bank");

			while (!Bank.isOpen()) {
				Time.sleep(600);
			}
			int buckets = Bank.getItemCount(true, BUCKET);
			int pots = (prayer.isAsh()) ? buckets : Bank
					.getItemCount(true, POT);
			int bonashes = Bank.getItemCount(true, prayer.getId());

			int r = (prayer.isAsh()) ? 0 : Math.abs(Bank.getItemCount(true,
					prayer.getMealId())
					- Bank.getItemCount(true, BUCKET_OF_SLIME));

			// Set the Bones Per Trip
			bpt = Math.min(bonashes, Math.min(buckets, pots) + r);

			if (bpt == 0) {
				log.severe("Missing bones/ashes, buckets, or empty pots");
				stop();
			}

			state = (prayer.isAsh()) ? State.FILL_BUCKETS : State.GRIND_BONES;
		}

		@Override
		public boolean validate() {
			return state == State.CONFIGURE;
		}

		private boolean checkEquipment() {
			if (Tabs.getCurrent() != Tabs.EQUIPMENT)
				Tabs.EQUIPMENT.open();
			boolean ring = Widgets.get(WIDGET_EQUIP, WIDGET_EQUIP_RING)
					.getChildId() == RING_OF_KINSHIP;

			Time.sleep(500); // Small pause

			Tabs.INVENTORY.open();
			boolean ectophial = Inventory.getItem(ECTOPHIAL) != null;

			return ectophial && ring;
		}

	}

	private enum State {
		CONFIGURE, GRIND_BONES, FILL_BUCKETS, WORSHIP
	}

	private enum Prayer {
		BONES(526, 4255, false), BIG_BONES(532, 4257, false), BABYDRAGON_BONES(
				534, 4260, false), DRAGON_BONES(536, 4261, false), FROST_DRAGON_BONES(
				18832, 18834, false), IMPIOUS_ASHES(20264, -1, true), ACCURSED_ASHES(
				20266, -1, true), INFERNAL_ASHES(20268, -1, true);

		private final int id;
		private final int boneMealID;
		private final boolean ash;

		Prayer(int id, int boneMealID, boolean ash) {
			this.id = id;
			this.boneMealID = boneMealID;
			this.ash = ash;
		}

		int getId() {
			return id;
		}

		int getMealId() {
			return boneMealID;
		}

		boolean isAsh() {
			return ash;
		}

		static Prayer fromID(int id) {
			for (Prayer prayer : values()) {
				if (prayer.getId() == id)
					return prayer;
			}

			return null;
		}

	}

	private boolean isIn(Area area) {
		return area.contains(Players.getLocal().getLocation());
	}

	private void goToBank() {
		while (!BANKER.isOnScreen()) {
			PATH_TO_BANKER.traverse();
			Time.sleep(1000);
		}
	}

	private void teleportToBank() {
		if (Tabs.getCurrent() != Tabs.EQUIPMENT)
			Tabs.EQUIPMENT.open();

		Time.sleep(500); // Small pause

		Widgets.get(WIDGET_EQUIP, WIDGET_EQUIP_RING).interact("Teleport");

		Time.sleep(1000);
		while (Players.getLocal().getAnimation() != -1) {
			Time.sleep(300);
		}

		Tabs.INVENTORY.open();
	}

}