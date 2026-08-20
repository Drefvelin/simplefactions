package me.Plugins.SimpleFactions.War.battle.ui;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.War.battle.engine.Battle;
import me.Plugins.SimpleFactions.War.battle.engine.BattleCapturePoints;
import me.Plugins.SimpleFactions.War.battle.engine.BattleContestSetup;
import me.Plugins.SimpleFactions.War.battle.engine.BattleFactory;
import me.Plugins.SimpleFactions.War.battle.engine.BattleJoinService;
import me.Plugins.SimpleFactions.War.battle.engine.BattleManager;
import me.Plugins.SimpleFactions.War.battle.engine.BattleRaidSetup;
import me.Plugins.SimpleFactions.War.battle.engine.BattleSide;
import me.Plugins.SimpleFactions.War.battle.engine.CapturePoint;
import me.Plugins.SimpleFactions.War.battle.enums.BattleType;
import me.Plugins.SimpleFactions.War.battle.warband.Warband;
import me.Plugins.SimpleFactions.War.battle.warband.WarbandManager;
import me.Plugins.SimpleFactions.War.battle.util.BattlePermissions;

public class BattleCommandManager implements CommandExecutor{
	public String cmd1 = "warband";
	public String cmd2 = "battle";
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player) sender;
			if(cmd.getName().equalsIgnoreCase(cmd1) && args.length < 1) {
				p.sendMessage("§a[Battle]§c Error with command format, use the gameplay guide for a list of commands");
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("list") && args.length == 1) {
				Warband w = WarbandManager.getByPlayer(p);
				if(w == null) {
					p.sendMessage("§cYou need to lead a warband to view battles");
					return true;
				}
				if (!p.getUniqueId().equals(w.getLeaderId())) {
					p.sendMessage("§cOnly the leader can view battles!");
					return true;
				}
				BattleInventoryManager inv = new BattleInventoryManager();
				inv.battleList(p);
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("join") && args.length == 3) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				String joinError = BattleJoinService.join(p, b, args[2]);
				if (joinError != null) {
					p.sendMessage("§c" + joinError);
					return true;
				}
				p.sendMessage("§aJoined "+args[2]+" in the battle "+b.getId());
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd2) && !BattlePermissions.isAdmin(sender)) {
				p.sendMessage("§a[Battle]§4You do not have access to this command");
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("create") && args.length == 2) {
				if(WarbandManager.getByPlayer(p) != null) {
					p.sendMessage("§cYou already have a warband!");
					return true;
				}
				Warband w = new Warband(args[1], p);
				WarbandManager.addWarband(w);
				p.sendMessage("§aWarband "+w.getId()+" §acreated!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("delete") && args.length == 2) {
				Warband w = WarbandManager.getByString(args[1]);
				if(w == null) {
					p.sendMessage("§a[Battle]§c Error! Warband does not exist!");
					return true;
				}
				if(!BattlePermissions.isAdmin(sender)) {
					if (!w.hasMember(p)) {
						p.sendMessage("§cCannot delete a warband you are not part of!");
						return true;
					}
					if (!p.getUniqueId().equals(w.getLeaderId())) {
						p.sendMessage("§cOnly the warband leader can delete the warband!");
						return true;
					}
				}
				WarbandManager.deleteWarband(w);
				p.sendMessage("§aWarband "+w.getId()+" §adeleted!");
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("toggleopen") && args.length == 1) {
				Warband w = WarbandManager.getByLeader(p);
				if(w == null) {
					p.sendMessage("§cYou need to lead a warband to open/close it");
					return true;
				}
				w.setLocked(!w.isLocked());
				if(w.isLocked()) {
					p.sendMessage("§cWarband is now invite only");
				} else {
					p.sendMessage("§aWarband is now open");
				}
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("list") && args.length == 1) {
				BattleInventoryManager inv = new BattleInventoryManager();
				inv.warbandList(p);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("kick") && args.length == 2) {
				Warband w = WarbandManager.getByPlayer(p);
				if(w == null) {
					p.sendMessage("§cYou need to have a warband to kick someone");
					return true;
				}
				if (!p.getUniqueId().equals(w.getLeaderId())) {
					p.sendMessage("§cOnly the leader can kick players!");
					return true;
				}
				if(args[1].equalsIgnoreCase(w.getLeader().getName())) {
					p.sendMessage("§cCant kick the leader!");
					return true;
				}
				if (!w.hasMember(Bukkit.getPlayerExact(args[1]))) {
					p.sendMessage("§cPlayer is not a member");
					return true;
				}
				w.removePlayer(Bukkit.getPlayerExact(args[1]));;
				p.sendMessage("§aKicked "+args[1]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(pl.getName().equalsIgnoreCase(args[1])) {
						pl.sendMessage("§a"+p.getName()+ " kicked you from "+w.getId());
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("setleader") && args.length == 2) {
				Warband w = WarbandManager.getByPlayer(p);
				if(w == null) {
					p.sendMessage("§cYou need to have a warband to change leader");
					return true;
				}
				if (!p.getUniqueId().equals(w.getLeaderId())) {
					p.sendMessage("§cOnly the leader can set a new leader!");
					return true;
				}
				if (!w.hasMember(Bukkit.getPlayerExact(args[1]))) {
					p.sendMessage("§cPlayer is not in the warband");
					return true;
				}
				if(args[1].equalsIgnoreCase(w.getLeader().getName())) {
					p.sendMessage("§cPlayer is already the leader");
					return true;
				}
				w.setLeader(Bukkit.getPlayerExact(args[1]));
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if (w.hasMember(pl)) {
						pl.sendMessage("§a"+args[1]+ " is the new warband leader!");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("invite") && args.length == 2) {
				Warband w = WarbandManager.getByPlayer(p);
				if(w == null) {
					p.sendMessage("§cYou need to have a warband to invite someone");
					return true;
				}
				if (!p.getUniqueId().equals(w.getLeaderId())) {
					p.sendMessage("§cOnly the leader can invite players!");
					return true;
				}
				if (w.hasMember(Bukkit.getPlayerExact(args[1]))) {
					p.sendMessage("§cPlayer is already a member");
					return true;
				}
				w.invite(Bukkit.getPlayerExact(args[1]));
				p.sendMessage("§aInvited "+args[1]);
				for(Player pl : Bukkit.getOnlinePlayers()) {
					if(pl.getName().equalsIgnoreCase(args[1])) {
						pl.sendMessage("§a"+p.getName()+ " invited you to "+w.getId());
						pl.sendMessage("§aType /warband join "+w.getId() +"§a to join");
					}
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("join") && args.length == 2) {
				if(WarbandManager.getByPlayer(p) != null) {
					p.sendMessage("§cAlready in a warband, leave your current warband first!");
					return true;
				}
				Warband w = WarbandManager.getByString(args[1]);
				if(w.isLocked()) {
					if (!w.isInvited(p)) {
						p.sendMessage("§cYou need to be invited to this warband by the leader first!");
						return true;
					}
					w.uninvite(p);
				}
				w.addPlayer(p);
				p.sendMessage("§aJoined "+w.getId());
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd1) && args[0].equalsIgnoreCase("leave") && args.length == 1) {
				if(WarbandManager.getByPlayer(p) == null) {
					p.sendMessage("§cYou are not in a warband");
					return true;
				}
				if(WarbandManager.getByLeader(p) != null) {
					p.sendMessage("§cCant leave if you are the leader");
					p.sendMessage("§cUse /warband delete first");
					return true;
				}
				Warband w = WarbandManager.getByPlayer(p);
				w.removePlayer(p);;
				p.sendMessage("§aLeft "+w.getId());
				return true;
			}
			if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("create") && args.length == 3) {
				if(BattleManager.getByString(args[2]) != null) {
					p.sendMessage("§cThere already exists a battle with this id");
					return true;
				}
				BattleType type = BattleType.fromJson(args[1]);
				if (type == null) {
					p.sendMessage("§cInvalid battle type. Use field, siege, or raid.");
					return true;
				}
				try {
					Battle b = BattleFactory.createBlank(type, args[2]);
					BattleManager.addBattle(b);
					p.sendMessage("§aBattle "+b.getId()+" §acreated!");
					BattleManager.currentBattle.put(p, b);
					BattleInventoryManager inv = new BattleInventoryManager();
					inv.battleView(p, b);
				} catch (IllegalArgumentException e) {
					p.sendMessage("§c" + e.getMessage());
				}
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("edit") && args.length == 2) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				BattleManager.currentBattle.put(p, b);
				BattleInventoryManager inv = new BattleInventoryManager();
				inv.battleView(p, b);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("addside") && args.length == 3) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				if(b.getSideById(args[2]) != null) {
					p.sendMessage("§cAlready a side with this id");
					return true;
				}
				BattleSide s = new BattleSide(args[2], b.getLifeType(), b.getLives());
				b.addSide(s);
				p.sendMessage("§aSide §e"+s.getId()+" §acreated!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("addpoint") && args.length == 3) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				if(b.getSideById(args[2]) == null) {
					p.sendMessage("§cNo side with this id");
					return true;
				}
				CapturePoint point = BattleCapturePoints.createAtPlayer(
						b, args[2], p.getLocation(), b.getSideById(args[2]));
				b.addPoint(point);
				p.sendMessage("§aPoint §e"+point.getId()+" §acreated!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("addpoint") && args.length == 4) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				if(b.getPointById(args[2]) != null) {
					p.sendMessage("§cAlready a point with this id");
					return true;
				}
				if(b.getSideById(args[3]) == null) {
					p.sendMessage("§cNo side with this id");
					return true;
				}
				CapturePoint point = new CapturePoint(args[2], p.getLocation(), b.getSideById(args[3]), 100);
				point.setAdvanceSideId(args[3]);
				point.setSequenceIndex(BattleCapturePoints.nextSequenceIndex(b, args[3]));
				b.addPoint(point);
				p.sendMessage("§aPoint §e"+point.getId()+" §acreated!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setlives") && args.length == 3) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				b.setLives(Integer.parseInt(args[2]));
				p.sendMessage("§aLives set to "+args[2]);
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setspawn") && args.length == 3) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				if(b.getSideById(args[2]) == null) {
					p.sendMessage("§cNo side with this id");
					return true;
				}
				BattleSide s = b.getSideById(args[2]);
				s.setSpawn(p.getLocation());
				p.sendMessage("§aSide §e"+s.getId()+" §aspawn set!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setjail") && args.length == 3) {
				if(BattleManager.getByString(args[1]) == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				Battle b = BattleManager.getByString(args[1]);
				if(b.getSideById(args[2]) == null) {
					p.sendMessage("§cNo side with this id");
					return true;
				}
				BattleSide s = b.getSideById(args[2]);
				s.setJail(p.getLocation());
				p.sendMessage("§aSide §e"+s.getId()+" §ajail set!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setcontestmin") && args.length == 2) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				warnIfNotSiege(p, b);
				BattleContestSetup.setContestMin(b, p.getLocation());
				p.sendMessage("§aContest area min set!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setcontestmax") && args.length == 2) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				warnIfNotSiege(p, b);
				BattleContestSetup.setContestMax(b, p.getLocation());
				p.sendMessage("§aContest area max set!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setcontestduration") && args.length == 3) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				warnIfNotSiege(p, b);
				int seconds = Integer.parseInt(args[2]);
				if (seconds < 1) {
					p.sendMessage("§cDuration must be at least 1 second");
					return true;
				}
				b.setContestDurationSeconds(seconds);
				p.sendMessage("§aContest duration set to "+seconds+"s");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setraidtarget") && args.length == 2) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				warnIfNotRaid(p, b);
				BattleRaidSetup.setRaidTarget(b, p.getLocation());
				p.sendMessage("§aRaid target set!");
				return true;
			} else if(cmd.getName().equalsIgnoreCase(cmd2) && args[0].equalsIgnoreCase("setdefenderlives") && args.length == 3) {
				Battle b = BattleManager.getByString(args[1]);
				if(b == null) {
					p.sendMessage("§cThere is no battle with this id");
					return true;
				}
				warnIfNotRaid(p, b);
				int lives = Integer.parseInt(args[2]);
				if (lives < 1) {
					p.sendMessage("§cDefender lives must be at least 1");
					return true;
				}
				b.setDefenderLives(lives);
				p.sendMessage("§aDefender lives set to "+lives);
				return true;
			}
			p.sendMessage("§a[Battle]§c Error with command format, use the gameplay guide for a list of commands");
		}
		return false;
	}

	private void warnIfNotSiege(Player p, Battle b) {
		if (b.getBattleType() != BattleType.SIEGE) {
			p.sendMessage("§eWarning: battle type is not siege");
		}
	}

	private void warnIfNotRaid(Player p, Battle b) {
		if (b.getBattleType() != BattleType.RAID) {
			p.sendMessage("§eWarning: battle type is not raid");
		}
	}
}