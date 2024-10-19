package main.tisfbungeecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkService {

    private Main plugin;
    private ServerSocket serverSocket;
    private boolean running = false;
    private int port; // 从配置中读取端口
    private ExecutorService executorService;

    public NetworkService(Main plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newCachedThreadPool();
        this.port = plugin.getConfig().getInt("network.port", 25570); // 默认值为 25570
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                plugin.getLogger().info("网络服务器已启动，端口：" + port);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.submit(() -> handleClient(clientSocket));
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            plugin.getLogger().info("网络服务器已关闭。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = in.readLine();
            plugin.getLogger().info("收到请求：" + request);

            String[] parts = request.split(";");
            if (parts.length >= 2) {
                String action = parts[0];
                String serverName = parts[1];

                if ("add".equalsIgnoreCase(action) && parts.length == 4) {
                    String address = parts[2];
                    int port = Integer.parseInt(parts[3]);

                    // 在主线程中添加服务器
                    plugin.getProxy().getScheduler().schedule(plugin, () -> {
                        plugin.addServer(serverName, address, port);
                        plugin.getLogger().info("成功添加服务器：" + serverName);
                    }, 0, TimeUnit.MILLISECONDS);

                    out.println("成功添加服务器：" + serverName);
                } else if ("remove".equalsIgnoreCase(action)) {
                    // 在主线程中移除服务器
                    plugin.getProxy().getScheduler().schedule(plugin, () -> {
                        plugin.removeServer(serverName);
                        plugin.getLogger().info("成功移除服务器：" + serverName);
                    }, 0, TimeUnit.MILLISECONDS);

                    out.println("成功移除服务器：" + serverName);
                } else {
                    out.println("无效的请求格式。");
                }
            } else {
                out.println("无效的请求。");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
