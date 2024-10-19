package main.tisfbungeecord;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.net.InetSocketAddress;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main extends Plugin {

    private NetworkService networkService;
    private Configuration config;

    @Override
    public void onEnable() {
        // 加载配置文件
        loadConfig();

        // 启动网络服务，接受来自 Spigot 插件的请求
        networkService = new NetworkService(this);
        networkService.start();
    }

    @Override
    public void onDisable() {
        // 停止网络服务
        if (networkService != null) {
            networkService.shutdown();
        }
    }

    public void addServer(String name, String address, int port) {
        InetSocketAddress socketAddress = new InetSocketAddress(address, port);
        ServerInfo serverInfo = getProxy().constructServerInfo(name, socketAddress, "Dynamic Server", false);
        getProxy().getServers().put(name, serverInfo);
        getLogger().info("已添加服务器：" + name);
    }

    public void removeServer(String name) {
        getProxy().getServers().remove(name);
        getLogger().info("已移除服务器：" + name);
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                // 从插件资源中保存默认的 config.yml
                saveDefaultConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveDefaultConfig() {
        try {
            Configuration defaultConfig = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(getResourceAsStream("config.yml"));
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(defaultConfig,
                    new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfig() {
        return config;
    }
}
