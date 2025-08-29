package bige.bbttfun;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public final class FUNChickenDupe extends JavaPlugin implements Listener {
    private File dataFile;
    private int taskId;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();

        int intervalSeconds = getConfig().getInt("SpawnInterval");
        long intervalTicks = intervalSeconds * 20L;

        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "无法创建数据文件", e);
            }
        }

        taskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, this::spawnItemsForChickens, 0L, intervalTicks);

        getLogger().info(ChatColor.GREEN + "--------------------");
        getLogger().info(ChatColor.GREEN + "FUNChickenDupe");
        getLogger().info(ChatColor.GREEN + "插件加载成功");
        getLogger().info(ChatColor.GREEN + "作者:bige666");
        getLogger().info(ChatColor.GREEN + "--------------------");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(taskId);

        getLogger().info(ChatColor.GREEN + "--------------------");
        getLogger().info(ChatColor.GREEN + "FUNChickenDupe");
        getLogger().info(ChatColor.GREEN + "插件卸载成功");
        getLogger().info(ChatColor.GREEN + "作者:bige666");
        getLogger().info(ChatColor.GREEN + "--------------------");
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Chicken) {
            Chicken chicken = (Chicken) entity;
            if (chicken.isAdult()) {
                Player player = event.getPlayer();
                if (player.hasPermission("chicken.dupe")) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    Material material = item.getType();
                    if (material != Material.AIR) {
                        storeItemInDataFile(chicken.getUniqueId(), item);
                        playEffectAndSetChickenName(chicken, player, item);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Chicken) {
            removeFromDataFile(entity.getUniqueId());
        }
    }

    private void storeItemInDataFile(UUID uniqueId, ItemStack item) {
        try {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            data.set(uniqueId.toString(), item);
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "无法存储物品到数据文件", e);
        }
    }

    private void removeFromDataFile(UUID uniqueId) {
        try {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            data.set(uniqueId.toString(), null);
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "无法从数据文件中删除记录", e);
        }
    }

    private void playEffectAndSetChickenName(Chicken chicken, Player player, ItemStack item) {
        player.playEffect(chicken.getLocation(), Effect.CLICK2, null);
        chicken.setCustomName(item.getI18NDisplayName());
        chicken.setCustomNameVisible(true);
    }

    private void spawnItemsForChickens() {
        int spawnNumber = getConfig().getInt("SpawnNumber");
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            for (World world : getServer().getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Chicken) {
                        Chicken chicken = (Chicken) entity;
                        UUID uuid = chicken.getUniqueId();
                        String key = uuid.toString();
                        if (config.contains(key)) {
                            ItemStack itemStack = config.getItemStack(key);
                            if (itemStack != null && itemStack.getType() != Material.AIR) {
                                itemStack.setAmount(spawnNumber);
                                entity.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "读取文件失败", ex);
        }
    }
}
