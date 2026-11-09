package com.moneytrack.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

public final class UiTasks {
    private UiTasks() { }

    public static <T> void run(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> failure.accept(task.getException()));
        Thread thread = new Thread(task, "moneytrack-data");
        thread.setDaemon(true);
        thread.start();
    }
}
