package com.nomercymc.oldpvp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import com.nomercymc.oldpvp.nms.DamageValues_1_10_R1;
import com.nomercymc.oldpvp.nms.DamageValues_1_9_R1;
import com.nomercymc.oldpvp.nms.DamageValues_1_9_R2;

public class OldPvP
  extends JavaPlugin
  implements Listener
{
  String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
  List<Player> isBlocking = new ArrayList();
  HashMap<Player, BukkitRunnable> unBlock = new HashMap();
  OldPvP plugin;
  Scoreboard sb;
  Team team;
  private DamageValues damageValues;
  
  public void onEnable()
  {
    getConfig().options().copyDefaults(true);
    saveConfig();
    if (!setupVersions()) {
      getServer().getConsoleSender().sendMessage(ChatColor.RED + "[1.8 PvP] Error occured, please report on the forums!");
    }
    if (!getConfig().getBoolean("Settings.enableCollision"))
    {
      if (this.sb == null) {
        this.sb = Bukkit.getScoreboardManager().getNewScoreboard();
      }
      if (this.team == null)
      {
        this.team = this.sb.registerNewTeam("collision");
        this.team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
      }
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (p.getScoreboard() != null)
        {
          Scoreboard b = p.getScoreboard();
          Team team;
          if (b.getTeam("collision") == null) {
            team = b.registerNewTeam("collision");
          } else {
            team = b.getTeam("collision");
          }
          team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
          team.addPlayer(p);
          p.setScoreboard(b);
        }
        else
        {
          this.team.addPlayer(p);
          p.setScoreboard(this.sb);
        }
      }
    }
    if (getConfig().getBoolean("Settings.enableGodApples"))
    {
      ShapedRecipe godAppleRecipe = new ShapedRecipe(new ItemStack(Material.GOLDEN_APPLE, 1, (short)1));
      godAppleRecipe.shape(new String[] { "ggg", "gag", "ggg" });
      godAppleRecipe.setIngredient('g', Material.GOLD_BLOCK);
      godAppleRecipe.setIngredient('a', Material.APPLE);
      getServer().addRecipe(godAppleRecipe);
    }
    this.plugin = this;
    getServer().getPluginManager().registerEvents(this, this);
    if (getConfig().getBoolean("Settings.enableBlocking"))
    {
      BukkitRunnable checkBlocking = new BukkitRunnable()
      {
        public void run()
        {
          final List<Player> pToRemove = new ArrayList();
          for (final Player p : isBlocking) {
            if (p.isBlocking())
            {
              if (unBlock.get(p) != null) {
                ((BukkitRunnable)unBlock.get(p)).cancel();
              }
              unBlock.put(p, new BukkitRunnable()
              {
                public void run()
                {
                  pToRemove.add(p);
                  p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                  p.updateInventory();
                }
              });
              ((BukkitRunnable)unBlock.get(p)).runTaskLater(plugin, 20L);
            }
            else
            {
              if (unBlock.get(p) != null) {
                ((BukkitRunnable)unBlock.get(p)).cancel();
              }
              pToRemove.add(p);
              p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
              p.updateInventory();
            }
          }
          for (Player p : pToRemove)
          {
            isBlocking.remove(p);
            unBlock.remove(p);
          }
        }
      };
      checkBlocking.runTaskTimer(this.plugin, 0L, 20L);
    }
  }
  
  private boolean setupVersions()
  {
    try
    {
      version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }
    catch (ArrayIndexOutOfBoundsException whatVersionAreYouUsingException)
    {
      return false;
    }
    getLogger().info("Your server is running version " + version);
    if (version.equals("v1_9_R1")) {
      this.damageValues = new DamageValues_1_9_R1();
    } else if (version.equals("v1_9_R2")) {
      this.damageValues = new DamageValues_1_9_R2();
    } else if (version.equals("v1_10_R1")) {
      this.damageValues = new DamageValues_1_10_R1();
    }
    return this.damageValues != null;
  }
  
  public void onDisable()
  {
    this.team.unregister();
  }
  
  @EventHandler
  public void move(PlayerMoveEvent e)
  {
    if (!getConfig().getBoolean("Settings.enableCollision"))
    {
      if (this.team == null)
      {
        this.team = this.sb.registerNewTeam("collision");
        this.team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
      }
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (p.getScoreboard() != null)
        {
          Scoreboard b = p.getScoreboard();
          Team team;
          if (b.getTeam("collision") == null) {
            team = b.registerNewTeam("collision");
          } else {
            team = b.getTeam("collision");
          }
          team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
          team.addPlayer(p);
          p.setScoreboard(b);
        }
        else
        {
          this.team.addPlayer(p);
          p.setScoreboard(this.sb);
        }
      }
    }
  }
  
  @EventHandler
  public void onClose(InventoryCloseEvent e)
  {
    if ((e.getInventory().getType() == InventoryType.BREWING) && (getConfig().getBoolean("Settings.enableAutoBlazeRod"))) {
      e.getInventory().setItem(4, new ItemStack(Material.AIR, 64));
    }
    if (getConfig().getBoolean("Settings.enableOldAttackDamageValues"))
    {
      Player p = (Player)e.getPlayer();
      updateItemAttributes(p);
    }
    if ((getConfig().getBoolean("Settings.enableAutoLapisLazuli")) && 
      (e.getInventory().getType() == InventoryType.ENCHANTING)) {
      e.getInventory().setItem(1, new ItemStack(Material.AIR, 64));
    }
  }
  
  @EventHandler
  public void InventoryMove(InventoryClickEvent e)
  {
    if ((getConfig().getBoolean("Settings.enableAutoBlazeRod")) && 
      (e.getClickedInventory() != null) && 
      (e.getClickedInventory().getType() == InventoryType.BREWING) && 
      (e.getCurrentItem().getType() == Material.BLAZE_POWDER)) {
      e.setCancelled(true);
    }
    if ((getConfig().getBoolean("Settings.enableAutoLapisLazuli")) && 
      (e.getClickedInventory() != null) && (e.getClickedInventory().getType() == InventoryType.ENCHANTING) && 
      (e.getCurrentItem().getType() == Material.INK_SACK) && 
      (e.getCurrentItem().getDurability() == 4)) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler
  public void openInv(InventoryOpenEvent e)
  {
    if ((e.getInventory().getType() == InventoryType.BREWING) && (getConfig().getBoolean("Settings.enableAutoBlazeRod"))) {
      e.getInventory().setItem(4, new ItemStack(Material.BLAZE_POWDER, 64));
    }
    if ((getConfig().getBoolean("Settings.enableAutoLapisLazuli")) && 
      (e.getInventory().getType() == InventoryType.ENCHANTING)) {
      e.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short)4));
    }
    if (getConfig().getBoolean("Settings.enableOldAttackDamageValues"))
    {
      Player p = (Player)e.getPlayer();
      updateItemAttributes(p);
    }
  }
  
  @EventHandler
  public void craftItem(CraftItemEvent e)
  {
    if (getConfig().getBoolean("Settings.enableOldAttackDamageValues"))
    {
      Player p = (Player)e.getWhoClicked();
      e.setCurrentItem(resetAttackDamage(e.getCurrentItem()));
      updateItemAttributes(p);
    }
  }
  
  void updateItemAttributes(Player p)
  {
    ItemStack[] arrayOfItemStack;
    int j = (arrayOfItemStack = p.getInventory().getContents()).length;
    for (int i = 0; i < j; i++)
    {
      ItemStack is = arrayOfItemStack[i];
      if (is != null)
      {
        Material type = is.getType();
        if (type != null) {
          if ((type == Material.WOOD_AXE) || (type == Material.STONE_AXE) || (type == Material.IRON_AXE) || (type == Material.GOLD_AXE) || (type == Material.DIAMOND_AXE) || (type == Material.WOOD_PICKAXE) || (type == Material.STONE_PICKAXE) || (type == Material.IRON_PICKAXE) || (type == Material.GOLD_PICKAXE) || (type == Material.DIAMOND_PICKAXE) || (type == Material.WOOD_SPADE) || (type == Material.STONE_SPADE) || (type == Material.IRON_SPADE) || (type == Material.GOLD_SPADE) || (type == Material.DIAMOND_SPADE))
          {
            ItemStack newItemStack = resetAttackDamage(is);
            is = newItemStack;
            p.updateInventory();
          }
          else if ((type == Material.LEATHER_HELMET) || (type == Material.LEATHER_CHESTPLATE) || (type == Material.LEATHER_LEGGINGS) || (type == Material.LEATHER_BOOTS) || (type == Material.CHAINMAIL_HELMET) || (type == Material.CHAINMAIL_CHESTPLATE) || (type == Material.CHAINMAIL_LEGGINGS) || (type == Material.CHAINMAIL_BOOTS) || (type == Material.IRON_HELMET) || (type == Material.IRON_CHESTPLATE) || (type == Material.IRON_LEGGINGS) || (type == Material.IRON_BOOTS) || (type == Material.GOLD_HELMET) || (type == Material.GOLD_CHESTPLATE) || (type == Material.GOLD_LEGGINGS) || (type == Material.GOLD_BOOTS) || (type == Material.DIAMOND_HELMET) || (type == Material.DIAMOND_CHESTPLATE) || (type == Material.DIAMOND_LEGGINGS) || (type == Material.DIAMOND_BOOTS))
          {
            ItemStack newItemStack = resetAttackDamage(is);
            is = newItemStack;
            p.updateInventory();
          }
        }
      }
    }
  }
  
  public ItemStack resetArmourPoints(ItemStack i)
  {
    return i;
  }
  
  public ItemStack resetAttackDamage(ItemStack i)
  {
    if (i != null)
    {
      Material type = i.getType();
      if (type == Material.WOOD_AXE)
      {
        i = this.damageValues.changeDamageValue(i, 3.0D);
        return i;
      }
      if (type == Material.STONE_AXE)
      {
        i = this.damageValues.changeDamageValue(i, 4.0D);
        return i;
      }
      if (type == Material.IRON_AXE)
      {
        i = this.damageValues.changeDamageValue(i, 5.0D);
        return i;
      }
      if (type == Material.GOLD_AXE)
      {
        i = this.damageValues.changeDamageValue(i, 3.0D);
        return i;
      }
      if (type == Material.DIAMOND_AXE)
      {
        i = this.damageValues.changeDamageValue(i, 6.0D);
        return i;
      }
      if (type == Material.WOOD_PICKAXE)
      {
        i = this.damageValues.changeDamageValue(i, 2.0D);
        return i;
      }
      if (type == Material.STONE_PICKAXE)
      {
        i = this.damageValues.changeDamageValue(i, 3.0D);
        return i;
      }
      if (type == Material.IRON_PICKAXE)
      {
        i = this.damageValues.changeDamageValue(i, 4.0D);
        return i;
      }
      if (type == Material.GOLD_PICKAXE)
      {
        i = this.damageValues.changeDamageValue(i, 2.0D);
        return i;
      }
      if (type == Material.DIAMOND_PICKAXE)
      {
        i = this.damageValues.changeDamageValue(i, 5.0D);
        return i;
      }
      if (type == Material.WOOD_SPADE)
      {
        i = this.damageValues.changeDamageValue(i, 1.0D);
        return i;
      }
      if (type == Material.STONE_SPADE)
      {
        i = this.damageValues.changeDamageValue(i, 2.0D);
        return i;
      }
      if (type == Material.IRON_SPADE)
      {
        i = this.damageValues.changeDamageValue(i, 3.0D);
        return i;
      }
      if (type == Material.GOLD_SPADE)
      {
        i = this.damageValues.changeDamageValue(i, 1.0D);
        return i;
      }
      if (type == Material.DIAMOND_SPADE)
      {
        i = this.damageValues.changeDamageValue(i, 4.0D);
        return i;
      }
      if (type == Material.WOOD_HOE)
      {
        i = this.damageValues.changeDamageValue(i, 0.0D);
        return i;
      }
      if (type == Material.STONE_HOE)
      {
        i = this.damageValues.changeDamageValue(i, 0.0D);
        return i;
      }
      if (type == Material.IRON_HOE)
      {
        i = this.damageValues.changeDamageValue(i, 0.0D);
        return i;
      }
      if (type == Material.GOLD_HOE)
      {
        i = this.damageValues.changeDamageValue(i, 0.0D);
        return i;
      }
      if (type == Material.DIAMOND_HOE)
      {
        i = this.damageValues.changeDamageValue(i, 0.0D);
        return i;
      }
      return i;
    }
    return i;
  }
  
  private Class<?> getNMSClass(String nmsClassString)
    throws ClassNotFoundException
  {
    String version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3] + ".";
    String name = "net.minecraft.server." + version + nmsClassString;
    Class<?> nmsClass = Class.forName(name);
    return nmsClass;
  }
  
  @EventHandler
  public void onEat(PlayerItemConsumeEvent e)
  {
    Player p = e.getPlayer();
    if (e.getItem().getType() == Material.GOLDEN_APPLE)
    {
      e.setCancelled(true);
      if ((getConfig().getBoolean("Settings.enableOldGoldenAppleEffects")) && 
        (e.getItem().getDurability() == 0)) {
        gappleEffect(p, e.getItem());
      }
      if ((getConfig().getBoolean("Settings.enableOldGodAppleEffects")) && 
        (e.getItem().getDurability() == 1))
      {
        godEffect(p, e.getItem());
        e.setCancelled(true);
      }
    }
  }
  
  @EventHandler
  public void onPlayerClickEvent(PlayerSwapHandItemsEvent event)
  {
    if (this.isBlocking.contains(event.getPlayer())) {
      event.setCancelled(true);
    }
  }
  
  void gappleEffect(Player p, ItemStack eaten)
  {
    ItemStack itemToRemove = eaten;
    itemToRemove.setAmount(1);
    p.getInventory().removeItem(new ItemStack[] { itemToRemove });
    p.updateInventory();
    if (p.getFoodLevel() + 4 <= 20) {
      p.setFoodLevel(p.getFoodLevel() + 20);
    } else {
      p.setFoodLevel(20);
    }
    p.removePotionEffect(PotionEffectType.ABSORPTION);
    p.removePotionEffect(PotionEffectType.REGENERATION);
    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
    PotionEffect pEffect = new PotionEffect(PotionEffectType.REGENERATION, 100, 0);
    pEffect.apply(p);
  }
  
  void godEffect(Player p, ItemStack eaten)
  {
    ItemStack itemToRemove = eaten;
    itemToRemove.setAmount(1);
    p.getInventory().removeItem(new ItemStack[] { itemToRemove });
    p.updateInventory();
    if (p.getFoodLevel() + 4 <= 20) {
      p.setFoodLevel(p.getFoodLevel() + 20);
    } else {
      p.setFoodLevel(20);
    }
    p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    p.removePotionEffect(PotionEffectType.ABSORPTION);
    p.removePotionEffect(PotionEffectType.REGENERATION);
    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0));
    PotionEffect pEffect = new PotionEffect(PotionEffectType.REGENERATION, 600, 4);
    pEffect.apply(p);
    p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 6000, 0));
    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0));
  }
  
  @EventHandler
  public void onBlock(PlayerInteractEvent e)
  {
    if ((getConfig().getBoolean("Settings.enableBlocking")) && 
      ((e.getAction() == Action.RIGHT_CLICK_AIR) || (e.getAction() == Action.RIGHT_CLICK_BLOCK)) && 
      (e.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) && 
      ((e.getPlayer().getInventory().getItemInMainHand().getType() == Material.WOOD_SWORD) || (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.STONE_SWORD) || (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.IRON_SWORD) || (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.GOLD_SWORD) || (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.DIAMOND_SWORD)) && 
      (!this.isBlocking.contains(e.getPlayer())))
    {
      this.isBlocking.add(e.getPlayer());
      if ((e.getPlayer().getInventory().getItemInOffHand() != null) && (e.getPlayer().getInventory().getItemInOffHand().getType() != Material.SHIELD)) {
        if (e.getPlayer().getInventory().firstEmpty() != -1)
        {
          e.getPlayer().getInventory().addItem(new ItemStack[] { e.getPlayer().getInventory().getItemInOffHand() });
          e.getPlayer().getInventory().setItemInOffHand(null);
          e.getPlayer().updateInventory();
        }
        else
        {
          if (e.getPlayer().getInventory().getItemInOffHand().getType() != Material.AIR) {
            e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), e.getPlayer().getInventory().getItemInOffHand());
          }
          String msg = getConfig().getString("Settings.dropItemMsg");
          msg = msg.replaceAll("%item%", e.getPlayer().getInventory().getItemInOffHand().getType().toString().toLowerCase());
          msg = ChatColor.translateAlternateColorCodes('&', msg);
          e.getPlayer().sendMessage(msg);
          e.getPlayer().getInventory().setItemInOffHand(null);
          e.getPlayer().updateInventory();
        }
      }
      e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
      e.getPlayer().updateInventory();
    }
  }
  
  @EventHandler
  public void onJoin(PlayerJoinEvent event)
  {
    if (!getConfig().getBoolean("Settings.enableCollision"))
    {
      if (this.sb == null) {
        this.sb = Bukkit.getScoreboardManager().getNewScoreboard();
      }
      if (this.team == null)
      {
        this.team = this.sb.registerNewTeam("collision");
        this.team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
      }
      for (Player p : Bukkit.getOnlinePlayers()) {
        if (p.getScoreboard() != null)
        {
          Scoreboard b = p.getScoreboard();
          Team team;
          if (b.getTeam("collision") == null) {
            team = b.registerNewTeam("collision");
          } else {
            team = b.getTeam("collision");
          }
          team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
          team.addPlayer(p);
          p.setScoreboard(b);
        }
        else
        {
          this.team.addPlayer(p);
          p.setScoreboard(this.sb);
        }
      }
    }
    if (getConfig().getBoolean("Settings.changeAttackSpeed")) {
      event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(100.0D);
    } else {
      event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0D);
    }
  }
  
  @EventHandler
  public void onQuit(PlayerQuitEvent event)
  {
    getConfig().getBoolean("Settings.enableCollision");
    
    event.getPlayer().getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0D);
  }
}
