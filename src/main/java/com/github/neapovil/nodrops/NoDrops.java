package com.github.neapovil.nodrops;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;
import net.kyori.adventure.text.Component;

public final class NoDrops extends JavaPlugin implements Listener
{
    private static NoDrops instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Config config;

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("config.json", false);

        try
        {
            this.load();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("enabled"))
                .withArguments(new BooleanArgument("bool"))
                .executes((sender, args) -> {
                    final boolean bool = (boolean) args.get("bool");

                    try
                    {
                        this.config.enabled = bool;
                        this.save();

                        sender.sendMessage("NoDrops status changed to: " + bool);
                    }
                    catch (IOException e)
                    {
                        this.getLogger().severe(e.getMessage());
                        throw CommandAPI.failWithString("Unable to toggle.");
                    }
                })
                .register();

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("whitelist"))
                .withArguments(new LiteralArgument("add"))
                .withArguments(new ItemStackArgument("itemStack"))
                .executes((sender, args) -> {
                    final ItemStack itemstack = (ItemStack) args.get("itemStack");

                    if (this.hasItem(itemstack))
                    {
                        throw CommandAPI.failWithString("Item is already whitelisted");
                    }

                    try
                    {
                        this.config.whitelist.add(itemstack.getType());
                        this.save();

                        final Component hover = itemstack.displayName().hoverEvent(itemstack.asHoverEvent());
                        sender.sendMessage(Component.text("Item added to the whitelist: ").append(hover));
                    }
                    catch (IOException e)
                    {
                        this.getLogger().severe(e.getMessage());
                        throw CommandAPI.failWithString("Unable to add.");
                    }
                })
                .register();

        new CommandAPICommand("nodrops")
                .withPermission("nodrops.command")
                .withArguments(new LiteralArgument("whitelist"))
                .withArguments(new LiteralArgument("remove"))
                .withArguments(new ItemStackArgument("itemStack").replaceSafeSuggestions(SafeSuggestions.suggest(info -> {
                    return this.config.whitelist.stream().map(i -> new ItemStack(i)).toArray(ItemStack[]::new);
                })))
                .executes((sender, args) -> {
                    final ItemStack itemstack = (ItemStack) args.get("itemStack");

                    if (!this.hasItem(itemstack))
                    {
                        throw CommandAPI.failWithString("Item is not whitelisted");
                    }

                    try
                    {
                        this.config.whitelist.removeIf(i -> i.equals(itemstack.getType()));
                        this.save();

                        final Component hover = itemstack.displayName().hoverEvent(itemstack.asHoverEvent());
                        sender.sendMessage(Component.text("Item removed from whitelist: ").append(hover));
                    }
                    catch (IOException e)
                    {
                        this.getLogger().severe(e.getMessage());
                        throw CommandAPI.failWithString("Unable to remove.");
                    }
                })
                .register();
    }

    @Override
    public void onDisable()
    {
    }

    public static NoDrops instance()
    {
        return instance;
    }

    @EventHandler
    private void playerDeath(PlayerDeathEvent event)
    {
        if (!this.config.enabled)
        {
            return;
        }

        event.getDrops().clear();
        event.setShouldDropExperience(false);
    }

    @EventHandler
    private void playerDropItem(PlayerDropItemEvent event)
    {
        if (!this.config.enabled)
        {
            return;
        }

        if (!this.hasItem(event.getItemDrop().getItemStack()))
        {
            event.getItemDrop().remove();
        }
    }

    private void load() throws IOException
    {
        final String string = Files.readString(this.getDataFolder().toPath().resolve("config.json"));
        this.config = this.gson.fromJson(string, Config.class);
    }

    private void save() throws IOException
    {
        final String string = this.gson.toJson(this.config);
        Files.write(this.getDataFolder().toPath().resolve("config.json"), string.getBytes());
    }

    private boolean hasItem(ItemStack itemStack)
    {
        return this.config.whitelist.contains(itemStack.getType());
    }

    class Config
    {
        public boolean enabled;
        public List<Material> whitelist = new ArrayList<>();
    }
}
