import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.powerbot.concurrent.Task;
import org.powerbot.concurrent.strategy.Condition;
import org.powerbot.concurrent.strategy.Strategy;
import org.powerbot.game.api.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.Walking;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.node.SceneEntities;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.widget.Bank;
import org.powerbot.game.api.methods.widget.Camera;
import org.powerbot.game.api.util.Filter;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.util.Timer;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.map.TilePath;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.node.SceneObject;
import org.powerbot.game.api.wrappers.widget.WidgetChild;

@Manifest(name = "SlimeWorshipper", description = "Prays at Ectofuntus", authors = { "Agassi" }, version = 1.0)
public class SlimeWorshipper extends ActiveScript {

	private State state = State.CONFIGURE;
	private Prayer prayer = Prayer.BONES; // TODO Allow user to set later
	private boolean checked = false;

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
	private static final int STAIRS = 37454;
	private static final int HOPPER = 11162;

	// Areas and Paths
	private static final Area DAEMONHEIM = new Area(new Tile(3440, 3721, 0),
			new Tile(3454, 3688, 0));
	private static final Area BANK = new Area(new Tile(3445, 3722, 0),
			new Tile(3451, 3716, 0));
	private static final Area ECTOFUNTUS_GROUND_FLOOR = new Area(new Tile[] {
			new Tile(3651, 3527, 0), new Tile(3651, 3511, 0),
			new Tile(3667, 3511, 0), new Tile(3667, 3527, 0) });
	private static final Area ECTOFUNTUS_FIRST_FLOOR = new Area(new Tile[] {
			new Tile(3651, 3527, 1), new Tile(3651, 3511, 1),
			new Tile(3667, 3511, 1), new Tile(3667, 3527, 1) });
	private static final Area GRINDER_AREA = new Area(new Tile(3655, 3527, 1),
			new Tile(3667, 3522, 1));
	private static final Tile BANKER = new Tile(3448, 3719, 0);
	private static final TilePath PATH_TO_BANKER = new TilePath(new Tile[] {
			new Tile(3443, 3692, 0), new Tile(3447, 3695, 0),
			new Tile(3448, 3700, 0), new Tile(3448, 3704, 0),
			new Tile(3449, 3708, 0), new Tile(3449, 3711, 0),
			new Tile(3449, 3715, 0), new Tile(3448, 3718, 0) });
	private static final TilePath PATH_TO_STAIRS = new TilePath(new Tile[] {
			new Tile(3661, 3522, 0), new Tile(3661, 3520, 0),
			new Tile(3661, 3519, 0), new Tile(3662, 3517, 0),
			new Tile(3663, 3516, 0), new Tile(3664, 3516, 0),
			new Tile(3665, 3516, 0) });

	@Override
	protected void setup() {
		Configure configure = new Configure();

		// Grinding Classes
		BankForGrinding bankForGrinding = new BankForGrinding();
		WalkToBoneGrinder walkToBoneGrinder = new WalkToBoneGrinder();
		GrindBones grindBones = new GrindBones();

		provide(new Strategy(configure, configure));
		provide(new Strategy(bankForGrinding, bankForGrinding));
		provide(new Strategy(walkToBoneGrinder, walkToBoneGrinder));
		provide(new Strategy(grindBones, grindBones));
	}

	private class GrindBones implements Task, Condition {

		@Override
		public void run() {
			SceneObject grinder = SceneEntities.getNearest(HOPPER);
			if (grinder != null && !grinder.isOnScreen()) {
				Camera.turnTo(grinder);
				if (!grinder.isOnScreen())
					Walking.walk(grinder);

				Time.sleep(300, 600);
				return;
			}

			grinder.interact("Fill");

			int interval = 0;
			int pots = Inventory.getCount(POT);
			while (Inventory.getItem(POT) != null) {
				Time.sleep(1000);

				if (Inventory.getCount(POT) == pots) {
					interval++;
				} else {
					interval = 0;
					pots = Inventory.getCount(POT);
				}

				if (interval >= 30)
					break;
			}

			state = State.CONFIGURE;
		}

		@Override
		public boolean validate() {
			return state == State.GRIND_BONES && isIn(GRINDER_AREA)
					&& Inventory.getItem(prayer.getId()) != null;
		}

	}

	private class WalkToBoneGrinder implements Task, Condition {

		@Override
		public void run() {
			if (!isIn(ECTOFUNTUS_GROUND_FLOOR) && !isIn(ECTOFUNTUS_FIRST_FLOOR)) {
				teleportToEctofuntus();

				// Wait for ectophial refill and teleport
				Time.sleep(3000);
				while (Inventory.getItem(ECTOPHIAL) == null) {
					Time.sleep(300);
				}
				
				return;
			}

			if (isIn(ECTOFUNTUS_GROUND_FLOOR)) {
				SceneObject stairs = SceneEntities.getNearest(STAIRS);

				if (!stairs.isOnScreen()) {
					Camera.turnTo(stairs);
					
					if (!stairs.isOnScreen()) {
						PATH_TO_STAIRS.traverse();
						Time.sleep(1000, 1600);
					}
					
					return;
				}
				
				stairs.interact("Climb-up");

				Time.sleep(1000, 1600);
			}

		}

		@Override
		public boolean validate() {
			return state == State.GRIND_BONES
					&& !isIn(GRINDER_AREA)
					&& (Inventory.getItem(prayer.getId()) != null && Inventory
							.getItem(POT) != null);
		}

	}

	private class BankForGrinding implements Task, Condition {

		@Override
		public void run() {
			if (Tabs.getCurrent() != Tabs.INVENTORY)
				Tabs.INVENTORY.open();

			NPC banker = NPCs.getNearest(FREMMENIK_BANKER);

			if (banker != null && banker.isOnScreen())
				banker.interact("Bank");

			for (int i = 0; i < 60 && !Bank.isOpen(); i++) {
				Time.sleep(600);
			}

			int pots = Bank.getItemCount(true, POT);
			int bones = Bank.getItemCount(true, prayer.getId());

			if (pots < 13) {
				while (Inventory.getCount(POT) < pots) {
					Bank.withdraw(POT, pots);
					Time.sleep(1100, 1400);
				}

				while (Inventory.getCount(prayer.getId()) < pots) {
					Bank.withdraw(prayer.getId(), pots);
					Time.sleep(1100, 1400);
				}

				Time.sleep(200, 400);

				Bank.close();
				return;
			} else if (bones < 13) {
				while (Inventory.getCount(POT) < bones) {
					Bank.withdraw(POT, bones);
					Time.sleep(1100, 1400);
				}

				while (Inventory.getCount(prayer.getId()) < bones) {
					Bank.withdraw(prayer.getId(), bones);
					Time.sleep(1100, 1400);
				}

				Time.sleep(200, 400);

				Bank.close();
				return;
			}

			while (pots >= 13 && Inventory.getCount(POT) < 13) {
				Bank.withdraw(POT, 13 - Inventory.getCount(POT));
				Time.sleep(1100, 1400);
			}

			while (bones >= 13 && Inventory.getCount(prayer.getId()) < 13) {
				Bank.withdraw(prayer.getId(),
						13 - Inventory.getCount(prayer.getId()));
				Time.sleep(1100, 1400);
			}

			Time.sleep(200, 400);

			Bank.close();
		}

		@Override
		public boolean validate() {
			return state == State.GRIND_BONES && isIn(BANK)
					&& Inventory.getCount() <= 1;
		}

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

			Item[] items = Inventory.getItems(new Filter<Item>() {

				@Override
				public boolean accept(Item t) {
					return t.getId() == prayer.getId() || t.getId() == POT;
				}

			});

			Item bone = null;
			Item pot = null;
			for (Item item : items) {
				if (item.getId() == prayer.getId())
					bone = item;
				else if (item.getId() == POT)
					pot = item;

				if (bone != null && pot != null) {
					state = State.GRIND_BONES;
					return;
				}
			}

			if (!isIn(DAEMONHEIM))
				teleportToBank();

			goToBank();

			NPC banker = NPCs.getNearest(FREMMENIK_BANKER);
			banker.interact("Bank");

			while (!Bank.isOpen()) {
				Time.sleep(1000);
			}

			if (Inventory.getCount() > 1) {
				List<Item> is = toRealList(Inventory
						.getItems(new Filter<Item>() {

							@Override
							public boolean accept(Item t) {
								return t.getId() != ECTOPHIAL;
							}

						}));

				for (int n = 0; n < is.size(); n++) {
					Item i = is.get(n);
					if (!i.getWidgetChild().interact("Deposit-All"))
						i.getWidgetChild().interact("Deposit");

					removeAll(is, i);

					n = 0;
					Time.sleep(800);
				}

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

			Bank.close();

			state = (prayer.isAsh()) ? State.FILL_BUCKETS : State.GRIND_BONES;
		}

		@Override
		public boolean validate() {
			return state == State.CONFIGURE;
		}

		private List<Item> toRealList(Item[] o) {
			List<Item> list = new LinkedList<Item>();
			for (Item k : o) {
				list.add(k);
			}
			return list;
		}

		private void removeAll(List<Item> items, Item item) {
			for (int i = 0; i < items.size(); i++) {
				Item it = items.get(i);
				if (it.getId() == item.getId()) {
					items.remove(i);
					i--;
				}
			}
		}

		private boolean checkEquipment() {
			if (checked)
				return true;

			if (Tabs.getCurrent() != Tabs.EQUIPMENT)
				Tabs.EQUIPMENT.open();
			boolean ring = Widgets.get(WIDGET_EQUIP, WIDGET_EQUIP_RING)
					.getChildId() == RING_OF_KINSHIP;

			Time.sleep(500); // Small pause

			Tabs.INVENTORY.open();
			boolean ectophial = Inventory.getItem(ECTOPHIAL) != null;

			checked = true;
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

	private void teleportToEctofuntus() {
		if (Tabs.getCurrent() != Tabs.INVENTORY)
			Tabs.INVENTORY.open();

		Time.sleep(500); // Small pause

		WidgetChild[] items = Inventory.getWidget(true).getChildren();
		WidgetChild ectophial = null;
		for (WidgetChild item : items) {
			if (item.getChildId() == ECTOPHIAL) {
				ectophial = item;
				break;
			}
		}

		ectophial.interact("Empty");

		Time.sleep(1000);
		while (Players.getLocal().getAnimation() != -1
				|| Players.getLocal().isMoving()) {
			Time.sleep(1000);
		}
	}

}