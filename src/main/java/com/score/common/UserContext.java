package com.score.common;

/**
 * @author imyiwen
 * @data 2026/4/22 15:38
 */

public class UserContext {
    private static final ThreadLocal<String> CLASS_NAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_NAME_HOLDER = new ThreadLocal<>();

    public static void setClassName(String className){
        CLASS_NAME_HOLDER.set(className);
    }
    public static String getClassName(){
        return CLASS_NAME_HOLDER.get();
    }

    public static void setUserName(String userName){
        USER_NAME_HOLDER.set(userName);
    }
    public static String getUserName(){
        return USER_NAME_HOLDER.get();
    }

    public static void remove(){
        CLASS_NAME_HOLDER.remove();
        USER_NAME_HOLDER.remove();
    }
}
