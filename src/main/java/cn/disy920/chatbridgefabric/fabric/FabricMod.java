package cn.disy920.chatbridgefabric.fabric;

import cn.disy920.chatbridgefabric.command.BasicCommand;
import cn.disy920.chatbridgefabric.config.JsonConfig;
import cn.disy920.chatbridgefabric.config.RootSection;
import cn.disy920.chatbridgefabric.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import static net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING;
import static net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING;

public abstract class FabricMod implements ModInitializer {
    private static MinecraftServer server = null;
    private final Gson GSON = new GsonBuilder().create();
    private final File configDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "ChatBridge");
    private final File configFile = new File(configDir, "config.json");

    private RootSection config;

    @Override
    public void onInitialize() {
        SERVER_STARTING.register((server) -> {
            FabricMod.server = server;
            this.onEnable();
        });

        SERVER_STOPPING.register((server) -> {
            this.onDisable();
        });

        /* 注册重载指令 */
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            BasicCommand.register(dispatcher);
        });
    }

    public boolean saveDefaultConfig() {
        if (!configDir.exists() || configDir.isFile()) {
            configDir.mkdirs();
        }

        try {
            JsonConfig.saveDefaultConfig(configFile.toPath());
            return true;
        }
        catch (Exception e) {
            Logger.error("保存默认的配置文件时出错，以下是错误的堆栈信息:");
            e.printStackTrace();
            return false;
        }
    }

    @NotNull
    public RootSection getConfig() {
        if (this.config == null) {
            try {
                this.config = JsonConfig.loadConfig(configFile);
            } catch (IOException e) {
                this.config = new RootSection(new JsonObject());
            }
        }

        return this.config;
    }

    public boolean saveConfig() {
        try {
            this.config.save(configFile);
            return true;
        } catch (IOException e) {
            Logger.error("保存配置文件时出错，以下是错误的堆栈信息:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean reloadConfig() {
        try {
            this.config = JsonConfig.loadConfig(configFile);
            return true;
        } catch (IOException e) {
            this.config = new RootSection(new JsonObject());
            return false;
        }
    }

    public Gson getGson() {
        return this.GSON;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public abstract void onEnable();

    public abstract void onDisable();
}
