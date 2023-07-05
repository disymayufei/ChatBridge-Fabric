package cn.disy920.chatbridgefabric.utils;

import org.slf4j.LoggerFactory;

public final class Logger {

    private static org.slf4j.Logger logger = LoggerFactory.getLogger("ChatBridge");
    public static void info(Object msg){
        logger.info("[INFO] " + msg);
    }

    public static void warn(Object msg){
        logger.warn("[WARN] " + msg);
    }

    public static void error(Object msg){
        logger.error("[ERR] " + msg);
    }
}
