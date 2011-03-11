package com.nhksos.jjfs85.BetterShop;

import java.util.HashMap;
//import java.util.Iterator;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.bukkit.Material;
//import org.bukkit.Material;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import org.bukkit.World;

import com.jascotty2.CheckInput;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.MaterialData;

public class BSCommand {

    private final static HashMap<Integer, ItemStack> leftover = new HashMap<Integer, ItemStack>();
    private final static String commandPrefix = "";

    public BSCommand() {
    }

    public boolean add(CommandSender player, String[] s) {
        if (s.length != 3) {
            return false;
        }
        if (!BSutils.hasPermission(player, "BetterShop.admin.add", true)) {
            return true;
        }
        MaterialData mat = new MaterialData(0);
        String itemName;
        try {
            mat = itemDb.get(s[0]);
            itemName = itemDb.getName(mat.getItemTypeId(), mat.getData());
        } catch (Exception e1) {
            BSutils.sendMessage(player, String.format(BetterShop.config.getString("unkitem").replace("<item>", "%s"), s[0]));
            return false;
        }

        String prior = "";
        if (BetterShop.pricelist.isForSale(itemName)) {
            prior = BetterShop.config.getString("chgmsg").replace("<item>", "%1$s").replace("<buyprice>", "%2$01.2f").replace("<sellprice>", "%3$01.2f").replace("<curr>", "%4$s");

        }
        if (CheckInput.IsDouble(s[1]) && CheckInput.IsDouble(s[2])) {
            if (BetterShop.pricelist.setPrice(s[0], s[1], s[2])) {
                if (prior.length() == 0) {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("addmsg").replace("<item>", "%1$s").replace("<buyprice>", "%2$01.2f").replace("<sellprice>", "%3$01.2f").replace("<curr>", "%4$s"),
                            itemName, BetterShop.pricelist.getBuyPrice(itemName),
                            BetterShop.pricelist.getSellPrice(itemName), BetterShop.config.currency)); // s[1], s[2], currency)); //
                } else {
                    BSutils.sendMessage(player, String.format(prior, itemName, BetterShop.pricelist.getBuyPrice(itemName),
                            BetterShop.pricelist.getSellPrice(itemName), BetterShop.config.currency));
                }
            } else {
                BSutils.sendMessage(player, "&4An Error Occurred While Adding.");
            }
        } else {
            BSutils.sendMessage(player, BetterShop.config.getString("paramerror"));
            return false;
        }

        return true;
    }

    public boolean buy(CommandSender player, String[] s) {
        if (!BSutils.hasPermission(player, "BetterShop.user.buy", true)) {
            return true;
        } else if ((s.length > 2) || (s.length == 0)) {
            BSutils.sendMessage(player, "What?");
            return false;
        } else if (BSutils.anonymousCheck(player)) {
            return true;
        } else {
            MaterialData item = new MaterialData(0);
            double price = 0;
            int amtleft = 0;
            int amtbought = 1;
            double cost = 0;

            try {
                item = itemDb.get(s[0]);
            } catch (Exception e2) {
                BSutils.sendMessage(player, String.format(BetterShop.config.getString("unkitem").replace("<item>", "%s"), s[0]));
                return true;
            }
            price = BetterShop.pricelist.getBuyPrice(item.getItemTypeId() + (double) item.getData() / 100);
            if (price < 0) {
                try {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("notforsale").replace("<item>", "%s"), itemDb.getName(item)));
                } catch (Exception ex) {
                    //Logger.getLogger("Minecraft").log(Level.SEVERE, null, ex);
                }
                return true;
            }
            if (s.length == 2) {
                amtbought = CheckInput.GetInt(s[1], -1);//Integer.parseInt(s[1]);
                if (amtbought <= 0) {
                    BSutils.sendMessage(player, BetterShop.config.getString("nicetry"));
                    return true;
                }
            }

            // todo: first scan player's inventory & see how much of this item they can recieve


            cost = amtbought * price;
            try {
                if (BSutils.debit(player, cost)) {
                    BSutils.sendMessage(player, String.format(
                            BetterShop.config.getString("buymsg").replace(
                            "<item>", "%1$s").replace("<amt>", "%2$d").replace("<priceper>", "%3$01.2f").replace(
                            "<total>", "%4$01.2f").replace(
                            "<curr>", "%5$s"), itemDb.getName(
                            item.getItemTypeId(), item.getData()),
                            amtbought, price, cost, BetterShop.config.currency));
                    leftover.clear();
                    ItemStack itemS = new ItemStack(item.getItemTypeId(),
                            amtbought, (short) 0, item.getData());
                    leftover.putAll(((Player) player).getInventory().addItem(
                            itemS));
                    // drop in front of player
                    //World w = player.getServer().getWorld(""); w.dropItem(player.getServer().getPlayer("").getLocation(), leftover.values());//.dropItem(

                    amtleft = (leftover.isEmpty()) ? 0 : (leftover.get(0)).getAmount();
                    if (amtleft > 0) {
                        cost = amtleft * price;
                        BSutils.sendMessage(player, String.format(
                                BetterShop.config.getString("outofroom").replace("<item>", "%1$s").replace(
                                "<leftover>", "%2$d").replace(
                                "<refund>", "%3$01.2f").replace("<curr>", "%5$s").replace(
                                "<priceper>", "%4$d"), itemDb.getName(item.getItemTypeId(), item.getData()), amtleft, cost,
                                price, BetterShop.config.currency));
                        BSutils.credit(player, cost);
                    }
                    return true;
                } else {
                    BSutils.sendMessage(player, String.format(
                            BetterShop.config.getString("insuffunds").replace("<item>", "%1$s").replace("<amt>",
                            "%2$d").replace("<total>",
                            "%3$01.2f").replace("<curr>",
                            "%5$s").replace("<priceper>",
                            "%4$01.2f"), itemDb.getName(item.getItemTypeId(), item.getData()),
                            amtbought, cost, price, BetterShop.config.currency));
                    return true;
                }
            } catch (Exception e) {
                BetterShop.Log("Error while debiting player", e);
                return false;
            }
        }
    }

    public boolean check(CommandSender player, String[] s) {
        MaterialData mat;
        double i = 0;
        if (!BSutils.hasPermission(player, "BetterShop.user.check", true)) {
            return true;
        }
        if (s.length != 1) {
            return false;
        } // todo: instead of grabbing an exception, have return -1 if not exist
        try {
            mat = itemDb.get(s[0]);
        } catch (Exception e) {
            BSutils.sendMessage(player, String.format(BetterShop.config.getString("unkitem").replace("<item>", "%s"), s[0]));
            return true;
        }
        try {
            i = mat.getItemTypeId() + ((double) mat.getData() / 100);
            String sellStr = (BetterShop.pricelist.getSellPrice(i) <= 0) ? "No" : String.format("%01.2f", BetterShop.pricelist.getSellPrice(i));
            String buyStr = (BetterShop.pricelist.getBuyPrice(i) < 0) ? "No" : String.format("%01.2f", BetterShop.pricelist.getBuyPrice(i));
            BSutils.sendMessage(player, String.format(
                    BetterShop.config.getString("pricecheck").
                    replace("<buyprice>", "%1$s").
                    replace("<sellprice>", "%2$s").
                    replace("<item>", "%3$s").
                    replace("<curr>", "%4$s"),
                    buyStr, sellStr, itemDb.getName(i), BetterShop.config.currency));
        } catch (Exception e) {
            //logger.log(Level.INFO, "Error getting Price: ", e);
            try {
                //BSutils.sendMessage(player, "Error getting Price: "+ e.getLocalizedMessage());e.printStackTrace();
                BSutils.sendMessage(player, String.format(BetterShop.config.getString("nolisting").replace("<item>", "%s"), itemDb.getName(mat.getItemTypeId(), mat.getData())));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            return true;
        }
        return true;
    }

    public boolean help(CommandSender player) {
        if (!BSutils.hasPermission(player, "BetterShop.user.help", true)) {
            return true;
        }
        BSutils.sendMessage(player, "--------- Better Shop Usage --------");
        BSutils.sendMessage(player, "/" + commandPrefix
                + "shoplist <page> - List shop prices");
        BSutils.sendMessage(player, "/" + commandPrefix
                + "shopbuy [item] <amount> - Buy items");
        BSutils.sendMessage(player, "/" + commandPrefix
                + "shopsell [item] <amount> - Sell items");
        BSutils.sendMessage(player, "/" + commandPrefix
                + "shopcheck [item] - Check prices of item");

        if (BSutils.hasPermission(player, "BetterShop.admin", false)) {
            BSutils.sendMessage(player, "**-------- Admin commands --------**");
            BSutils.sendMessage(player, "/" + commandPrefix
                    + "shopadd [item] [$buy] [$sell] - Add an item to the shop");
            BSutils.sendMessage(player, "/" + commandPrefix
                    + "shopremove [item] - Remove an item from the shop");
            BSutils.sendMessage(player, "/" + commandPrefix
                    + "shopload - Reload the PriceList.yml file");
            BSutils.sendMessage(player, "----------------------------------");
        }
        return true;
    }

    public boolean list(CommandSender player, String[] s) {
        //int pagesize = 9;
        int page = 0;

        if (!BSutils.hasPermission(player, "BetterShop.user.list", true)) {
            return true;
        }
        if ((s.length != 0) && (s.length != 1)) {
            return false;
        }
        try {
            page = (s.length == 0) ? 1 : Integer.parseInt(s[0]);
        } catch (Exception e) {
            BSutils.sendMessage(player, "That's not a page number.");
            return false;
        }

        for (String line : BetterShop.pricelist.GetShopListPage(page, player instanceof Player)) {
            BSutils.sendMessage(player, line);
        }

        return true;
    }

    public boolean load(CommandSender player) {
        if (!BSutils.hasPermission(player, "BetterShop.admin.load", true)) {
            return true;
        }
        BetterShop.config.load();
        BSutils.sendMessage(player, "Config.yml loaded.");
        if (BetterShop.pricelist.reload()) {
            BSutils.sendMessage(player, "Price Database loaded.");
        } else {
            BSutils.sendMessage(player, "&4Price Database Load Error.");
        }
        return true;
    }

    public boolean remove(CommandSender player, String[] s) {
        if ((!BSutils.hasPermission(player, "BetterShop.admin.remove", true))) {
            return true;
        }
        if (s.length != 1) {
            return false;
        } else {
            try {
                String itemName = itemDb.getName(itemDb.get(s[0]));
                BetterShop.pricelist.remove(itemName);
                BSutils.sendMessage(player, String.format(BetterShop.config.getString("removemsg").replace("<item>", "%1$s"), itemName));// s[0]));
            } catch (Exception e) {
                BSutils.sendMessage(player, String.format(BetterShop.config.getString("unkitem").replace("<item>", "%s"), s[0]));
            }
            return true;
        }
    }

    public boolean sell(CommandSender player, String[] s) {
        if (!BSutils.hasPermission(player, "BetterShop.user.sell", true)) {
            return true;
        }
        if (/*(s.length == 1 && !s[0].equalsIgnoreCase("all")) || */(s.length == 0 || s.length > 2)) {
            return false;
        } else if (BSutils.anonymousCheck(player)) {
            return true;
        } else {
            String itemname = "[All Sellable]";
            double price = 0;
            double total = 0;
            int amtSold = 1;
            MaterialData item = new MaterialData(0);
            if (s.length == 1 && s[0].equalsIgnoreCase("all")) {
                item = null;
            } else {
                try {
                    if(s[0].equalsIgnoreCase("all")){
                        item = itemDb.get(s[1]);
                    }else
                    item = itemDb.get(s[0]);
                } catch (Exception e1) {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("unkitem").replace("<item>", "%s"), s[0]));
                    return false;
                }
                try {
                    itemname = itemDb.getName(item);//item.getItemTypeId(), item.getData());
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
                price = BetterShop.pricelist.getSellPrice(itemname);
                if (price <= 0) {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("donotwant").replace("<item>", "%1$s"), itemname));
                    return true;
                }
            }

            // go through inventory & find how much user has

            PlayerInventory inv = ((Player) player).getInventory();
            int amtHas = 0;

            for (ItemStack i : inv.getContents()) {
                if ((item == null && BetterShop.pricelist.isForSale((double) i.getTypeId() + ((double) i.getDurability() / 100.)))
                        || (item != null && item.getItemTypeId() == i.getTypeId() && item.getData() == i.getDurability())) {
                    amtHas += i.getAmount();
                }
            }

            if (s.length == 2) {
                try {
                    if (s[0].equalsIgnoreCase("all") || s[1].equalsIgnoreCase("all")) {
                        amtSold = amtHas;
                    } else {
                        amtSold = Integer.parseInt(s[1]);
                        if (amtSold > amtHas) {
                            BSutils.sendMessage(player, String.format(BetterShop.config.getString("donthave").replace("<hasamt>", "%1$d").replace("<amt>", "%2$d"), amtHas, amtSold));
                            amtSold = amtHas;
                        }
                    }
                } catch (Exception e) {
                    BSutils.sendMessage(player, s[1] + " is definitely not a number.");
                }
            } else {
                amtSold = amtHas;
            }
            if (amtHas <= 0) {
                BSutils.sendMessage(player, "You Don't have any " + item == null ? "Sellable Items" : itemname);//BetterShop.configfile.getString("nicetry"));
                return true;
            }
            if (amtSold <= 0) {
                BSutils.sendMessage(player, BetterShop.config.getString("nicetry"));
                return true;
            }
            if (item != null) {
                price = BetterShop.pricelist.getSellPrice(itemname);
                // min. buy reduced from 1 to .01
                if ((price * amtSold) < .01) {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("donotwant").replace("<item>", "%1$s"), itemname));
                    return true;
                }
                total = amtSold * price;
            }
            try {

                int itemsLeft = amtSold;
                // todo: make a cache of full pricelist (if MySQL).. makes query faster if only needs one
                for (int i = 0; i
                        <= 35;
                        ++i) {
                    ItemStack thisSlot = inv.getItem(i);
                    if ((item == null && BetterShop.pricelist.isForSale((double) thisSlot.getTypeId() + ((double) thisSlot.getDurability() / 100.)))
                            || (item != null && thisSlot.getTypeId() == item.getItemTypeId() && item.getData() == thisSlot.getDurability())) {
                        int amt = thisSlot.getAmount();
                        if (itemsLeft >= amt) {
                            inv.setItem(i, null);
                            if (item == null) {
                                total += amt * BetterShop.pricelist.getSellPrice((double) thisSlot.getTypeId() + ((double) thisSlot.getDurability() / 100.));
                            }
                        } else {
                            // remove only what left to remove
                            ItemStack itemsToSell = item.toItemStack();
                            itemsToSell.setAmount(amt - itemsLeft);
                            inv.setItem(i, itemsToSell);
                            if (item == null) {
                                total += itemsLeft * BetterShop.pricelist.getSellPrice((double) thisSlot.getTypeId() + ((double) thisSlot.getDurability() / 100.));
                            }
                        }
                        itemsLeft -= amt;
                        if (itemsLeft <= 0) {
                            break;
                        }

                    }
                }

                BSutils.credit(player, total);
                if (item == null) {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("sellmsg").
                            replace("<item>", "%1$s").
                            replace("<amt>", "%2$d").
                            replace("<priceper>", "%3$01.2f").
                            replace("<total>", "%4$01.2f").
                            replace("<curr>", "%5$s"),
                            itemname, amtSold, total / amtSold, total, BetterShop.config.currency));

                } else {
                    BSutils.sendMessage(player, String.format(BetterShop.config.getString("sellmsg").
                            replace("<item>", "%1$s").
                            replace("<amt>", "%2$d").
                            replace("<priceper>", "%3$01.2f").
                            replace("<total>", "%4$01.2f").
                            replace("<curr>", "%5$s"),
                            itemDb.getName(item.getItemTypeId(), item.getData()), amtSold, price, total, BetterShop.config.currency));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }
}
