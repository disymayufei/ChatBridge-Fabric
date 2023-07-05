package cn.disy920.chatbridgefabric;

import cn.disy920.chatbridgefabric.fabric.FabricMod;
import cn.disy920.chatbridgefabric.utils.Logger;
import cn.disy920.chatbridgefabric.websocket.WSClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class Main extends FabricMod {

    public static Main MOD_INSTANCE = null;

    private WSClient websocketClient = null;

    private Thread openConnThread = null;

    @Override
    public void onEnable() {
        MOD_INSTANCE = this;

        saveDefaultConfig();

        try {
            initWebsocket();
        } catch (URISyntaxException e) {
            Logger.error("检测到不合法的Websocket地址，请检查配置文件是否正确！");
            return;
        }

        websocketClient.connect();

        Logger.info("跨服聊天插件已启动，作者Disy");

        openConnThread = new Thread(() -> {
            while (!websocketClient.isOpen()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            Map<String, Object> chatPacket = new HashMap<>();
            chatPacket.put("type", "chatBridge");

            Map<String, Object> chatTextPacket = new HashMap<>();
            chatTextPacket.put("identity", MOD_INSTANCE.getWebsocketConn().getIdentity());
            chatTextPacket.put("sender", "server");
            chatTextPacket.put("text", "服务器正在启动...");
            chatPacket.put("args", chatTextPacket);
            chatPacket.put("reqGroup", new long[1]);

            MOD_INSTANCE.getWebsocketConn().send(MOD_INSTANCE.getGson().toJson(chatPacket));
        });

        openConnThread.start();
    }

    @Override
    public void onDisable() {
        if (openConnThread != null) {
            openConnThread.interrupt();
        }

        if (websocketClient != null) {
            Map<String, Object> chatPacket = new HashMap<>();
            chatPacket.put("type", "chatBridge");

            Map<String, Object> chatTextPacket = new HashMap<>();
            chatTextPacket.put("identity", MOD_INSTANCE.getWebsocketConn().getIdentity());
            chatTextPacket.put("sender", "server");
            chatTextPacket.put("text", "服务器正在关闭...");
            chatPacket.put("args", chatTextPacket);
            chatPacket.put("reqGroup", new long[1]);

            MOD_INSTANCE.getWebsocketConn().send(MOD_INSTANCE.getGson().toJson(chatPacket));

            websocketClient.activeClose();
        }

        Logger.info("跨服聊天插件已关闭，有缘再会！");
    }

    public void initWebsocket() throws URISyntaxException {
        String host = getConfig().getString("host", "127.0.0.1");
        int port = getConfig().getInt("port", 16123);
        String identity = getConfig().getString("serverName");

        this.websocketClient = new WSClient(new URI("ws://" + host + ":" + port), identity);
    }

    public WSClient getWebsocketConn() {
        return this.websocketClient;
    }
}
