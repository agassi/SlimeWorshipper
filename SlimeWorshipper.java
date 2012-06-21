import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.util.LinkedList;
import java.util.List;

import org.powerbot.concurrent.Task;
import org.powerbot.concurrent.strategy.Condition;
import org.powerbot.concurrent.strategy.Strategy;
import org.powerbot.game.api.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Game;
import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.Walking;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.input.Mouse;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.node.SceneEntities;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.tab.Skills;
import org.powerbot.game.api.methods.widget.Bank;
import org.powerbot.game.api.methods.widget.Camera;
import org.powerbot.game.api.util.Filter;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.map.TilePath;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.node.SceneObject;
import org.powerbot.game.api.wrappers.widget.WidgetChild;
import org.powerbot.game.bot.event.listener.PaintListener;

@Manifest(name = "SlimeWorshipper", description = "Prays at Ectofuntus", authors = { "Agassi" }, version = 1.0)
public class SlimeWorshipper extends ActiveScript implements PaintListener {

	private State state = State.CONFIGURE;
	private Prayer prayer = Prayer.BONES; // TODO Allow user to set later
	private int agility = 0;
	private boolean checked = false;
	private boolean dryrun = true;

	// Mouse Paint
	private Point[] mouseLocations = new Point[6];
	private Color[] mouseColors = mouseColors();
	private static final BasicStroke mouseStroke = new BasicStroke(2F);
	private static final Color mouseColor = new Color(245, 184, 0);

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
	private static final int TRAPDOOR_CLOSED = 5267;
	private static final int TRAPDOOR_OPENED = 5268;
	private static final int STAIRS_TRAPDOOR = 5263;
	private static final int TRAPDOOR_TILE = 5482;
	private static final int POOL_OF_SLIME = 17119;

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
	private static final Tile POOL = new Tile(3683, 9888, 0);
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
	private static final TilePath TRAPDOOR_THIRD_FLOOR_STEPS = new TilePath(
			new Tile[] { new Tile(3669, 9888, 3), new Tile(3669, 9881, 3),
					new Tile(3675, 9875, 3), new Tile(3684, 9875, 3),
					new Tile(3690, 9880, 3), new Tile(3691, 9885, 3) });
	private static final TilePath TRAPDOOR_SECOND_FLOOR_STEPS = new TilePath(
			new Tile[] { new Tile(3688, 9888, 2), new Tile(3688, 9883, 2),
					new Tile(3683, 9877, 2), new Tile(3678, 9877, 2),
					new Tile(3673, 9881, 2), new Tile(3671, 9885, 2) });
	private static final TilePath TRAPDOOR_FIRST_FLOOR_STEPS = new TilePath(
			new Tile[] { new Tile(3675, 9888, 1), new Tile(3674, 9884, 1),
					new Tile(3679, 9879, 1), new Tile(3682, 9879, 1),
					new Tile(3685, 9882, 1), new Tile(3687, 9885, 1) });

	@Override
	protected void setup() {
		Configure configure = new Configure();

		// Grinding Classes
		BankForGrinding bankForGrinding = new BankForGrinding();
		WalkToBoneGrinder walkToBoneGrinder = new WalkToBoneGrinder();
		GrindBones grindBones = new GrindBones();

		// Filling Classes
		BankForFilling bankForFilling = new BankForFilling();
		WalkToPoolOfSlime walkToPoolOfSlime = new WalkToPoolOfSlime();
		FillBuckets fillBuckets = new FillBuckets();

		provide(new Strategy(configure, configure));

		provide(new Strategy(bankForGrinding, bankForGrinding));
		provide(new Strategy(walkToBoneGrinder, walkToBoneGrinder));
		provide(new Strategy(grindBones, grindBones));

		provide(new Strategy(bankForFilling, bankForFilling));
		provide(new Strategy(walkToPoolOfSlime, walkToPoolOfSlime));
		provide(new Strategy(fillBuckets, fillBuckets));
	}
	
	private class FillBuckets implements Task, Condition {

		@Override
		public void run() {
			SceneObject pool = SceneEntities.getNearest(POOL_OF_SLIME);
			if (pool != null && !pool.isOnScreen()) {
				Camera.turnTo(pool);
				if (!pool.isOnScreen())
					Walking.walk(pool);

				Time.sleep(300, 600);
				return;
			}

			Inventory.selectItem(BUCKET);
			for (int i = 0; i < 100 && Inventory.getSelectedItemIndex() == -1; i++) {
				Time.sleep(800, 1000);
			}
			pool.interact("Use");
			for (int i = 0; i < 3 && Players.getLocal().getAnimation() == -1; i++) {
				Time.sleep(1000);
				pool.interact("Use");
			}

			int interval = 0;
			int buckets = Inventory.getCount(BUCKET);
			while (Inventory.getItem(BUCKET) != null) {
				Time.sleep(1000);

				if (Inventory.getCount(BUCKET) == buckets) {
					interval++;
				} else {
					interval = 0;
					buckets = Inventory.getCount(BUCKET);
				}

				if (interval >= 6)
					break;
			}
			
			state = State.CONFIGURE;
		}

		@Override
		public boolean validate() {
			return state == State.FILL_BUCKETS && isIn(POOL)
					&& Inventory.getItem(BUCKET) != null; 
		}
		
	}

	private class WalkToPoolOfSlime implements Task, Condition {

		@Override
		public void run() {
			SceneObject tile = SceneEntities.getNearest(TRAPDOOR_TILE);
			
			if (!isIn(ECTOFUNTUS_GROUND_FLOOR) && !isIn(ECTOFUNTUS_FIRST_FLOOR) && tile == null) {
				teleportToEctofuntus();

				// Wait for ectophial refill and teleport
				Time.sleep(3000);
				while (Inventory.getItem(ECTOPHIAL) == null) {
					Time.sleep(300);
				}

				return;
			}

			if (isIn(ECTOFUNTUS_GROUND_FLOOR)) {
				SceneObject trapdoor = SceneEntities.getNearest(
						TRAPDOOR_CLOSED, TRAPDOOR_OPENED);

				if (!trapdoor.isOnScreen()) {
					Camera.turnTo(trapdoor);

					if (!trapdoor.isOnScreen()) {
						Walking.walk(trapdoor);
						Time.sleep(1000, 1600);
					}

					return;
				}

				if (trapdoor.getId() == TRAPDOOR_CLOSED) {
					trapdoor.interact("Open");
					Time.sleep(300, 600);
				}
				SceneObject opened = SceneEntities.getNearest(TRAPDOOR_OPENED);

				if (opened != null)
					opened.interact("Climb-down");

				Time.sleep(1000, 1600);
				return;
			}
			
			if (tile != null) {
				SceneObject stairs = SceneEntities.getNearest(STAIRS_TRAPDOOR);
				
				if (stairs == null || (stairs != null && !stairs.isOnScreen())) {
					int floor = Game.getPlane();
					if (floor == 3) {
						TRAPDOOR_THIRD_FLOOR_STEPS.traverse();
						Time.sleep(1000, 2000);
						return;
					} else if (floor == 2) {
						TRAPDOOR_SECOND_FLOOR_STEPS.traverse();
						Time.sleep(1000, 2000);
						return;
					} else if (floor == 1) {
						TRAPDOOR_FIRST_FLOOR_STEPS.traverse();
						Time.sleep(1000, 2000);
						return;
					}
				}
				
				stairs.interact("Climb-down");
				Time.sleep(2000, 2600);
				return;
			}
		}

		@Override
		public boolean validate() {
			return state == State.FILL_BUCKETS && !isIn(POOL)
					&& Inventory.getItem(BUCKET) != null;
		}

	}

	private class BankForFilling implements Task, Condition {

		@Override
		public void run() {
			if (!Bank.isOpen()) {
				if (Tabs.getCurrent() != Tabs.INVENTORY)
					Tabs.INVENTORY.open();

				NPC banker = NPCs.getNearest(FREMMENIK_BANKER);

				if (banker != null && banker.isOnScreen())
					banker.interact("Bank");

				for (int i = 0; i < 60 && !Bank.isOpen(); i++) {
					Time.sleep(600);
				}
			}

			int buckets = Bank.getItemCount(true, BUCKET);

			if (buckets < 27) {
				while (Inventory.getCount(BUCKET) < buckets) {
					Bank.withdraw(BUCKET, buckets);
					Time.sleep(1100, 1400);
				}

				Time.sleep(200, 400);

				Bank.close();
				return;
			}

			while (buckets >= 27 && Inventory.getCount(BUCKET) < 27) {
				Bank.withdraw(BUCKET, (Inventory.getCount(BUCKET) == 0) ? 0
						: 27 - Inventory.getCount(BUCKET));
				Time.sleep(1100, 1400);
			}

			Time.sleep(200, 400);

			Bank.close();
		}

		@Override
		public boolean validate() {
			return state == State.FILL_BUCKETS && isIn(BANK)
					&& Inventory.getCount() <= 1;
		}

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

			if (dryrun) {
				agility = Skills.getRealLevel(Skills.AGILITY);
				log.info("Agility: " + agility);
			}

			Item[] items = Inventory.getItems(new Filter<Item>() {

				@Override
				public boolean accept(Item t) {
					return t.getId() == prayer.getId() || t.getId() == POT
							|| t.getId() == BUCKET;
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

			for (Item item : items) {
				if (item.getId() == BUCKET) {
					state = State.FILL_BUCKETS;
					return;
				}
			}

			if (!isIn(DAEMONHEIM))
				teleportToBank();

			goToBank();

			if (Tabs.getCurrent() != Tabs.INVENTORY)
				Tabs.INVENTORY.open();
			
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

			int r = (prayer.isAsh()) ? 0 : Bank.getItemCount(true,
					prayer.getMealId());

			if (dryrun && Math.min(bonashes + r, Math.min(buckets, pots)) == 0) {
				log.severe("Missing bones/ashes, buckets, or empty pots");
				stop();
			}

			dryrun = false;

			if (!prayer.isAsh()) {
				if (pots <= 0 || bonashes <= 0
						|| Bank.getItemCount(prayer.getMealId()) >= buckets) {
					state = State.FILL_BUCKETS;
					return;
				}
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

	private boolean isIn(Tile tile) {
		return tile.isOnScreen() && tile.getPlane() == Game.getPlane();
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

	private Color[] mouseColors() {
		Color[] colors = new Color[mouseLocations.length];
		int alpha = (int) (255D / (colors.length + 4D));
		for (int i = 0; i < colors.length; i++) {
			colors[i] = new Color(mouseColor.getRed(), mouseColor.getGreen(),
					mouseColor.getBlue(), 255 - alpha * i);
		}
		return colors;
	}

	private void drawMouse(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;

		// Draw mouse path
		for (int i = mouseLocations.length - 2; i >= 0; i--) {
			mouseLocations[i + 1] = mouseLocations[i];
		}
		mouseLocations[0] = Mouse.getLocation();
		Color original = g.getColor();
		Stroke originalStroke = g.getStroke();
		g.setStroke(mouseStroke);
		for (int i = 0; i < mouseLocations.length - 1; i++) {
			g.setColor(mouseColors[i]);
			Point p = mouseLocations[i];
			Point p2 = mouseLocations[i + 1];
			if (p != null && p2 != null)
				g.drawLine(p.x, p.y, p2.x, p2.y);
		}

		// Draw Cross-hair
		g.setColor(Color.WHITE);
		Point c = mouseLocations[0];
		g.drawLine(c.x, c.y - 5, c.x, c.y - 11);
		g.drawLine(c.x, c.y + 5, c.x, c.y + 11);
		g.drawLine(c.x - 5, c.y, c.x - 11, c.y);
		g.drawLine(c.x + 5, c.y, c.x + 11, c.y);

		if (Mouse.isPressed()) {
			g.setColor(Color.RED);
			g.drawLine(c.x, c.y, c.x + 2, c.y + 2);
		}

		g.setColor(original);
		g.setStroke(originalStroke);
	}

	@Override
	public void onRepaint(Graphics gr) {
		drawMouse(gr);
	}

}