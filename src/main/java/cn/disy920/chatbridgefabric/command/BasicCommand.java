package cn.disy920.chatbridgefabric.command;

import cn.disy920.chatbridgefabric.utils.Logger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URISyntaxException;

import static cn.disy920.chatbridgefabric.Main.MOD_INSTANCE;
import static net.minecraft.server.command.CommandManager.literal;

public class BasicCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("chatbridge")
                        .then(literal("reload")
                                .requires(source -> source.hasPermissionLevel(4))
                                .executes(context -> {
                                    var source = context.getSource();

                                    boolean reloadStatus = MOD_INSTANCE.reloadConfig();
                                    if (!reloadStatus) {
                                        source.sendMessage(Text.literal("重载ChatBridge时发生错误，请检查后台的报错信息！").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    MOD_INSTANCE.getWebsocketConn().activeClose();
                                    try {
                                        MOD_INSTANCE.initWebsocket();
                                        MOD_INSTANCE.getWebsocketConn().connect();
                                    } catch (URISyntaxException e) {
                                        Logger.error("检测到不合法的Websocket地址，请检查配置文件是否正确！");
                                    }

                                    source.sendMessage(Text.literal("重载ChatBridge成功！").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
        );
    }
}
