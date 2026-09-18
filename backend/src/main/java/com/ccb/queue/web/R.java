package com.ccb.queue.web;

/** 统一响应包装 */
public class R {
    public int code;
    public String message;
    public Object data;

    public R(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    public static R ok(Object data) { return new R(0, "ok", data); }
    public static R ok() { return new R(0, "ok", null); }
    public static R fail(String msg) { return new R(500, msg, null); }
}
