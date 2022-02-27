package com.github.neapovil.nodrops;

import org.bukkit.plugin.java.JavaPlugin;

public final class NoDrops extends JavaPlugin
{
    private static NoDrops instance;

    @Override
    public void onEnable()
    {
        instance = this;
    }

    @Override
    public void onDisable()
    {
    }

    public static NoDrops getInstance()
    {
        return instance;
    }
}
