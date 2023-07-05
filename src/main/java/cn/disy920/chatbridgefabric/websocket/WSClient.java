package cn.disy920.chatbridgefabric.websocket;

import cn.disy920.chatbridgefabric.fabric.FabricMod;
import cn.disy920.chatbridgefabric.utils.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static cn.disy920.chatbridgefabric.Main.MOD_INSTANCE;

public class WSClient extends WebSocketClient {
    private final String SELF_IDENTITY;

    private boolean isReconnecting = false;
    private boolean isActiveClose = false;

    private final Thread currentWebsocketThread = Thread.currentThread();
    private Thread reconnectionThread = null;
    private Thread heartbeatThread = null;

    public WSClient(URI serverUri, String identity){
        super(serverUri, new Draft_6455(), null, 2);
        this.SELF_IDENTITY = identity;
        this.setReuseAddr(true);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        isActiveClose = false;

        launchHeartbeatThread();

        InetSocketAddress socketAddress = this.getRemoteSocketAddress();
        Logger.info(String.format("成功连接至：ws://%s:%s", socketAddress.getHostString(), socketAddress.getPort()));
    }

    @Override
    public void onMessage(String msg) {
        JsonObject receiveObj;

        try {
            receiveObj = MOD_INSTANCE.getGson().fromJson(msg, JsonObject.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return;
        }


        if (receiveObj != null) {
            String header = receiveObj.get("type").getAsString();

            switch (header){
                case "chatBridge" -> {
                    if (FabricMod.getServer() != null) {
                        try {
                            JsonObject chat = receiveObj.getAsJsonObject("args");

                            String identity = chat.get("identity").getAsString();
                            String sender = chat.get("sender").getAsString();
                            String text = chat.get("text").getAsString();

                            String chatText = String.format("<%s | %s> %s", identity, sender, text);

                            System.out.println(chatText);

                            for (ServerPlayerEntity player : FabricMod.getServer().getPlayerManager().getPlayerList()) {
                                player.sendMessage(Text.literal(chatText));
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if(!isActiveClose){
            autoReconnect();
        }
    }


    @Override
    public void onError(Exception e) {
        autoReconnect();
    }

    public void activeClose(){
        this.isActiveClose = true;

        if (heartbeatThread != null){
            heartbeatThread.interrupt();
        }

        if (reconnectionThread != null){
            reconnectionThread.interrupt();
        }

        this.close();
    }

    public String getIdentity(){
        return this.SELF_IDENTITY;
    }

    private void launchHeartbeatThread(){
        heartbeatThread = new Thread(() -> {
            while (isOpen() && !Thread.currentThread().isInterrupted()){
                sendPing();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }

            if(!Thread.currentThread().isInterrupted()){
                autoReconnect();
            }
        }, "Heartbeat-Thread");

        heartbeatThread.start();
    }

    private void autoReconnect(){
        reconnectionThread = new Thread(() -> {
            if(!isReconnecting){
                isReconnecting = true;
                while (!currentWebsocketThread.isInterrupted()){
                    try {
                        reconnect();
                        isReconnecting = false;
                        break;
                    }
                    catch (Exception e){
                        try{
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException ie){
                            break;
                        }
                    }
                }
            }
        });

        reconnectionThread.start();
    }
}
