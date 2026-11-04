package com.opensandbox.dhizuku.core;

/**
 * 一次操作的结果：要么带值成功，要么带原因失败。
 * 用它替掉散落在各处的 try/catch + null + boolean 混用。
 */
public final class Outcome<T> {

    private final T value;
    private final String error;

    private Outcome(T value, String error) {
        this.value = value;
        this.error = error;
    }

    public static <T> Outcome<T> ok(T value) {
        return new Outcome<>(value, null);
    }

    public static <T> Outcome<T> ok() {
        return new Outcome<>(null, null);
    }

    public static <T> Outcome<T> fail(String error) {
        return new Outcome<>(null, error == null ? "unknown" : error);
    }

    public static <T> Outcome<T> fail(Throwable throwable) {
        String message = throwable.getMessage();
        String type = throwable.getClass().getSimpleName();
        return fail(message == null ? type : type + ": " + message);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public T value() {
        return value;
    }

    public String error() {
        return error;
    }
}
