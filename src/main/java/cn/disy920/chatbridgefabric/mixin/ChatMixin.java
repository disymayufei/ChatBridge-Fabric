package cn.disy920.chatbridgefabric.mixin;

import cn.disy920.chatbridgefabric.utils.Logger;
import cn.disy920.chatbridgefabric.fabric.FabricMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

import static cn.disy920.chatbridgefabric.Main.MOD_INSTANCE;

@Environment(EnvType.SERVER)
@Mixin(ServerPlayNetworkHandler.class)
public class ChatMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), method = "onChatMessage")
    private void onChatMessage(ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (MOD_INSTANCE.getWebsocketConn() != null) {
            if (MOD_INSTANCE.getWebsocketConn().isOpen()){
                try {
                    String chatMessage = packet.chatMessage();
                    if (!chatMessage.startsWith("/")) {
                        Map<String, Object> chatPacket = new HashMap<>();
                        chatPacket.put("type", "chatBridge");

                        Map<String, Object> chatTextPacket = new HashMap<>();
                        chatTextPacket.put("identity", MOD_INSTANCE.getWebsocketConn().getIdentity());
                        chatTextPacket.put("sender", this.player.getName().getString());
                        chatTextPacket.put("text", chatMessage);

                        chatPacket.put("args", chatTextPacket);

                        chatPacket.put("reqGroup", new long[1]);

                        MOD_INSTANCE.getWebsocketConn().send(MOD_INSTANCE.getGson().toJson(chatPacket));
                    }
                } catch (Exception e){
                    Logger.error("消息格式化失败！以下是错误的堆栈信息：");
                    e.printStackTrace();
                }
            }
        }
    }
}
