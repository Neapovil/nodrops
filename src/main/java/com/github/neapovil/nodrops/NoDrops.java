package com.github.neapovil.nodrops;

import java.io.File;
import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.electronwill.nightconfig.core.file.FileConfig;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public final class NoDrops extends JavaPlugin implements Listener
{
    private static NoDrops instance;
    private FileConfig config;

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("config.toml", false);

        this.config = FileConfig.builder(new File(this.getDataFolder(), "config.toml"))
                .autoreload()
                .autosave()
                .build();
        this.config.load();

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("enabled"))
                .withArguments(new BooleanArgument("bool"))
                .executes((sender, args) -> {
                    final boolean bool = (boolean) args[0];

                    this.config.set("general.enabled", bool);

                    sender.sendMessage("NoDrops enabled: " + bool);
                })
                .register();

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("whitelist"))
                .withArguments(new LiteralArgument("add"))
                .withArguments(new ItemStackArgument("itemstack"))
                .executes((sender, args) -> {
                    final String itemstack = (String) ((ItemStack) args[0]).getType().toString();
                    final List<String> items = this.config.get("general.safe_drops");

                    if (items.contains(itemstack))
                    {
                        CommandAPI.fail(itemstack + " already in whitelist!");
                    }

                    items.add(itemstack);

                    this.config.set("general.safe_drops", items);

                    sender.sendMessage(itemstack + " added to the whitelist");
                })
                .register();

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("whitelist"))
                .withArguments(new LiteralArgument("remove"))
                .withArguments(new StringArgument("itemstack").replaceSuggestions(info -> {
                    final List<String> items = this.config.get("general.safe_drops");
                    return items.toArray(String[]::new);
                }))
                .executes((sender, args) -> {
                    final String itemstack = (String) args[0];
                    final List<String> items = this.config.get("general.safe_drops");

                    if (!items.contains(itemstack))
                    {
                        CommandAPI.fail(itemstack + " is not whitelisted!");
                    }

                    items.removeIf(i -> i.equals(itemstack));

                    this.config.set("general.safe_drops", items);

                    sender.sendMessage(itemstack + " removed from the whitelist");
                })
                .register();
    }

    @Override
    public void onDisable()
    {
    }

    public static NoDrops getInstance()
    {
        return instance;
    }

    public boolean isNoDropsEnabled()
    {
        return this.config.get("general.enabled");
    }

    @EventHandler
    private void playerDeath(PlayerDeathEvent event)
    {
        if (!this.isNoDropsEnabled())
        {
            return;
        }

        event.getDrops().clear();
        event.setShouldDropExperience(false);
    }

    @EventHandler
    private void playerDropItem(PlayerDropItemEvent event)
    {
        if (!this.isNoDropsEnabled())
        {
            return;
        }

        final List<String> safedrops = this.config.get("general.safe_drops");

        if (!safedrops.contains(event.getItemDrop().getItemStack().getType().toString()))
        {
            event.getItemDrop().remove();
        }
    }
}
