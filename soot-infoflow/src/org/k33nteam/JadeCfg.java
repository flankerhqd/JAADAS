package org.k33nteam;

/**
 * Created by hqd on 12/12/14.
 */
public class JadeCfg {

    public static String getCallback_file() {
        return callback_file;
    }

    public static void setCallback_file(String callback_file) {
        JadeCfg.callback_file = callback_file;
    }

    private static String callback_file = "AndroidCallbacks.txt";
    //FLANKER ADD
    private static boolean enable_custom_clinit = false;

    public static boolean custom_clinit_enabled() {
        return enable_custom_clinit;
    }
    public static void setJcustom_clinit_enabled(boolean jade_optimize_enabled) {
        enable_custom_clinit = jade_optimize_enabled;
    }

    //FLANKER ADD
    private static boolean enable_public_component_only = false;

    public static boolean public_component_only_enabled() {
        return enable_public_component_only;
    }
    public static void setpublic_component_only_enabled(boolean jade_optimize_enabled) {
        enable_public_component_only = jade_optimize_enabled;
    }

    //FLANKER ADD
    private static boolean enable_callbacks = true;

    public static boolean callbacks_enabled() {
        return enable_callbacks;
    }
    public static void setcallbacks_enabled(boolean jade_optimize_enabled) {
        enable_callbacks = jade_optimize_enabled;
    }

    //FLANKER ADD
    private static boolean enable_field_taint_propagate = true;

    public static boolean field_taint_propagate_enabled() {
        return enable_field_taint_propagate;
    }
    public static void setfield_taint_propagate_enabled(boolean jade_optimize_enabled) {
        enable_field_taint_propagate = jade_optimize_enabled;
    }

    //FLANKER ADD
    private static boolean enable_intent_special_wrapper_tacking = true;

    public static boolean intent_special_wrapper_tacking_enabled() {
        return enable_intent_special_wrapper_tacking;
    }
    public static void setintent_special_wrapper_tacking_enabled(boolean jade_optimize_enabled) {
        enable_intent_special_wrapper_tacking = jade_optimize_enabled;
    }

    public static boolean isEnhance_callback_body() {
        return enhance_callback_body;
    }

    public static void setEnhance_callback_body(boolean enhance_callback_body) {
        JadeCfg.enhance_callback_body = enhance_callback_body;
    }

    private static boolean enhance_callback_body = false;

    public static boolean isError_in_dexpler() {
        return error_in_dexpler;
    }

    public static void setError_in_dexpler(boolean error_in_dexpler) {
        JadeCfg.error_in_dexpler = error_in_dexpler;
    }

    private static boolean error_in_dexpler = false;

    public static boolean isEnable_apklibs() {
        return enable_apklibs;
    }

    private static boolean enable_apklibs = true;

    public final static String APK_LIBPATH = "tmplibs/";
}
