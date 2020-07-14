package me.shiming.guitp;

import com.sun.istack.internal.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import javax.jnlp.ClipboardService;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class GuiTp extends JavaPlugin implements Listener{
    YamlConfiguration config, warps;
    public static int page;
    @Override
    public void onEnable() {
        // Plugin startup logic
        File file = new File(getDataFolder(),"config.yml");
        File file_warps = new File(getDataFolder(),"warps.yml");
        if(!getDataFolder().exists())getDataFolder().mkdir();
        if(!file.exists()) this.saveDefaultConfig();
        if(!file_warps.exists()) {try{file_warps.createNewFile();} catch (IOException e) { e.printStackTrace(); }}
        this.reloadConfig();//重载配置
        config = YamlConfiguration.loadConfiguration(file);
        warps = YamlConfiguration.loadConfiguration(file_warps);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("§2 guiTp已加载");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("§2 guiTp已卸载");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player == false) { return false;}
        Player p = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("tpa")){
            if(args.length==0){
                sender.sendMessage("/tpa gui  打开传送界面");
                sender.sendMessage("/tpa set <路径点名字> 添加路径点");
                sender.sendMessage("/tpa del <路径点名字> 删除路径点");
            }
            else if (args[0].equalsIgnoreCase("gui")){
                p.openInventory(gui.chooseSever());
                return true;
            }
            else if (args[0].equalsIgnoreCase("set")){
                if (args.length >= 2){
                    if (is_in(args[1])){
                        p.sendMessage("§4 路近点"+args[1]+"已存在！");
                        return true;
                    }
                    warps.set("warps."+args[1]+".x", p.getLocation().getX());
                    warps.set("warps."+args[1]+".y", p.getLocation().getY());
                    warps.set("warps."+args[1]+".z", p.getLocation().getZ());
                    warps.set("warps."+args[1]+".world", p.getLocation().getWorld().getName());
                    warps.set("warps."+args[1]+".seter", p.getDisplayName());
                    if (warps.getStringList("list").size()==0){
                        warps.set("list", new ArrayList<String>(){{this.add(args[1]);}});
                    }else {
                        List<String> list = warps.getStringList("list");
                        list.add(args[1]);
                        warps.set("list", list);
                    }
                    try{warps.save(new File(getDataFolder(),"warps.yml"));}catch(IOException e){e.printStackTrace();}
                    sender.sendMessage("§2 传送点" + args[1] + "已成功被设置");
                } else {
                    sender.sendMessage("正确格式:/tpa set <路径点名称>");
                }
            }else if (args[0].equalsIgnoreCase("del")){
                if (args.length == 1) {
                    sender.sendMessage("请使用: /tpa del <路径点名字>");
                    return true;
                }

                for(int i = 1; i < args.length; ++i) {
                    if (!is_in(args[i])) {
                        sender.sendMessage("§4 路径点"+(args[i])+"不存在！");
                    } else {
                        String pn = args[i].toLowerCase();
                        warps.set("warps." + pn, (Object)null);

                        List<String> list = warps.getStringList("list");
                        list.remove(pn);
                        warps.set("list", list);
                        try{warps.save(new File(getDataFolder(),"warps.yml"));}catch(IOException e){e.printStackTrace();}
                        sender.sendMessage("§2 传送点" + args[i] + "已成功被删除");
                    }
                }
            }
            return false;
        }
        return false;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event)
    {
        {
            if (event.getWhoClicked() instanceof Player == false) { return;}
            if (event.getCurrentItem().getType() == Material.AIR) return;
            Player p = (Player)event.getWhoClicked();
            //选择玩家传送
            //选择服务
            if (event.getView().getTitle().equalsIgnoreCase("§2 选择服务") ){
                event.setCancelled(true);
                p.updateInventory();
                p.closeInventory();
                String name = event.getView().getTitle().split(config.getString("split"))[0];
                if (event.getRawSlot() == 11) {
                    event.getWhoClicked().openInventory(gui.choose(getServer().getOnlinePlayers().toArray()));
                }
                else if (event.getRawSlot() == 15) {
                    page = 1;
                    event.getWhoClicked().openInventory(gui.chooseWarp(warps, page));
                }
            }
            else if (event.getView().getTitle().equalsIgnoreCase("§2 选择要传送的玩家") )
            {
                event.setCancelled(true);
                p.updateInventory();
                p.closeInventory();
                getServer().getPlayer(event.getCurrentItem().getItemMeta().getDisplayName()).openInventory(
                            gui.accept(p.getDisplayName()));
            }
            //选择是否同意传送
            else if (event.getView().getTitle().matches("(.+?)"+config.getString("split")+"申请传送至你") ) {
                event.setCancelled(true);
                p.updateInventory();
                p.closeInventory();
                String name = event.getView().getTitle().split(config.getString("split"))[0];
                getLogger().info(name + "->" + event.getWhoClicked().getName());
                if (event.getSlot() == 11) {
                    getServer().getPlayer(name).teleport(p.getLocation());
                    p.sendMessage("§5 已传送");
                    getServer().getPlayer(name).sendMessage("§5 已传送");
                } else{
                    p.sendMessage("§5 你拒绝了他的传送请求");
                    getServer().getPlayer(name).sendMessage("§5 他拒绝了你的传送请求");
                }
            }
            else if (event.getView().getTitle().equalsIgnoreCase("§2 路径点")){
                event.setCancelled(true);
                p.updateInventory();
                p.closeInventory();
                if (event.getCurrentItem().getType().equals(Material.OBSIDIAN)){
                    String k = event.getCurrentItem().getItemMeta().getDisplayName();
                    event.getWhoClicked().teleport(
                            new Location(getServer().getWorld(warps.getString("warps."+k+".world")),
                            warps.getDouble("warps."+k+".x"),
                            warps.getDouble("warps."+k+".y"),
                            warps.getDouble("warps."+k+".z")));
                    p.sendMessage("§2 你已传送至传送点" + k);
                    getLogger().info("§2 "+p.getDisplayName()+"->"+k);
                }else if (event.getSlot() == 18 && page > 1){
                    page -= 1;
                    p.openInventory(gui.chooseWarp(warps,page));
                }else if (event.getSlot() == 26) {
                    page += 1;
                    p.openInventory(gui.chooseWarp(warps, page));
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Arrays.asList(new String[]{"gui","set","del"});
        }else if(args.length == 1){
            if (args[0].equalsIgnoreCase("g") ||
                    args[0].equalsIgnoreCase("gu")||
                    args[0].equalsIgnoreCase("gui")){
                return Arrays.asList(new String[]{"gui"});
            }else if (args[0].equalsIgnoreCase("s")||
                    args[0].equalsIgnoreCase("se")||
                    args[0].equalsIgnoreCase("set")) {
                return Arrays.asList(new String[]{"set"});
            }else if (args[0].equalsIgnoreCase("d")||
                    args[0].equalsIgnoreCase("de")||
                    args[0].equalsIgnoreCase("del")) {
                return Arrays.asList(new String[]{"del"});
            }else{
                return Arrays.asList(new String[]{"gui","set","del"});
            }
        }else if(args.length >= 3){
            return warps.getStringList("list");
        }
        return new ArrayList<String>();
    }

    boolean is_in (String name){
        for (String i :warps.getStringList("list")){
            if (i.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}

class gui{
    static ItemStack selfItem(Material m, String name, ArrayList<String> Lore){
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = Lore;
        meta.setLore(lore);
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    static @Nullable Inventory accept(String name){
        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST , name+"#申请传送至你");
        ItemStack i1 = new ItemStack(Material.GREEN_WOOL);
        ItemStack i2 = new ItemStack(Material.RED_WOOL);
        ItemMeta m1 = i1.getItemMeta();
        ItemMeta m2 = i2.getItemMeta();
        m1.setDisplayName("同意");
        m2.setDisplayName("拒绝");
        m1.setLore(new ArrayList<String>(){{ this.add("da ga。。虚晃一枪，给爷过来"); }});
        m1.setLore(new ArrayList<String>(){{ this.add("搭嘎口头哇路"); }});
        i1.setItemMeta(m1);
        i2.setItemMeta(m2);
        inv.setItem(11, i1);
        inv.setItem(15, i2);
        return inv;
    }
    static public @Nullable Inventory choose(Object[] playerlist){
        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST , "§2 选择要传送的玩家");
        for (Object player : playerlist) {
            inv.addItem(selfItem(Material.GREEN_WOOL, ((Player)player).getDisplayName(), new ArrayList<String>(){{ this.add("点击申请传送"); }}));
        }
        return inv;
    }
    static public @Nullable Inventory chooseSever () {
        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST, "§2 选择服务");
        inv.setItem(
                11, selfItem(Material.PLAYER_HEAD, "玩家", new ArrayList<String>(){{ this.add("选择玩家并传送"); }})
        );
        inv.setItem(
                15, selfItem(Material.CREEPER_HEAD, "路径点", new ArrayList<String>(){{ this.add("创建，传送，删除 路径点"); }})
        );
        return inv;
    }
    static public @Nullable Inventory chooseWarp(YamlConfiguration warps, int page){
        if ((page-1)*18>warps.getStringList("list").size()) GuiTp.page=page=page-1;
        Inventory inv = Bukkit.createInventory(null, InventoryType.CHEST , "§2 路径点");
        inv.setItem(
                18, selfItem(Material.ARROW, "上一页", new ArrayList<String>(){{this.add("点击进入上一页-如果有");}})
        );
        inv.setItem(
                22, selfItem(Material.PIG_SPAWN_EGG, "第"+String.valueOf(page)+"页", new ArrayList<String>(){{
                    this.add("当前页数");
                    this.add("§2 /tpa set <路径点名称> 添加路径点");
                    this.add("§4 /tpa del <路径点名称> 删除路径点");
                }})
        );
        inv.setItem(
                26, selfItem(Material.ARROW, "下一页", new ArrayList<String>(){{this.add("点击进入下一页-如果有");}})
        );
        if (warps.getStringList("list").size() == 0) return inv;
        int i = (page-1)*18;
        int f;
        if (warps.getStringList("list").size()-(page-1)*18<18){
            f = warps.getStringList("list").size();
        }else{
            f = i+18;
        }
        for (String k : warps.getStringList("list").subList(i,f)) {
            inv.addItem(selfItem(Material.OBSIDIAN,k,new ArrayList<String>(){{
                this.add("x:"+warps.getString("warps."+k+".x"));
                this.add("y:"+warps.getString("warps."+k+".y"));
                this.add("z:"+warps.getString("warps."+k+".z"));
                this.add("世界:"+warps.getString("warps."+k+".world"));
                this.add("设置者:"+warps.getString("warps."+k+".seter"));
            }}));
        }

        return inv;
    }
}
