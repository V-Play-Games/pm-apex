package net.vpg.apex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    public static void sleep(int millis) {
        run(() -> Thread.sleep(millis));
    }

    public static void run(RunnableWithAChanceOfException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOGGER.error("Encountered unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public static <E> E apply(E input, ConsumerWithAChanceOfException<E>... functions) {
        for (var consumer : functions) {
            run(() -> consumer.accept(input));
        }
        return input;
    }

    public static <E, T> T compute(E input, FunctionWithAChanceOfException<E, T> function) {
        return get(() -> function.accept(input));
    }

    public static <E> E get(SupplierWithAChanceOfException<E> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            LOGGER.error("Encountered unexpected exception", e);
            throw new RuntimeException(e);
        }
    }

    public static Box addBox(Container container, String constraints, Component... components) {
        Box box = Box.createVerticalBox();
        container.add(box, constraints);
        for (Component component : components) {
            box.add(component);
        }
        return box;
    }

    public static String removeExtension(String fileName) {
        return fileName.replaceFirst("[.][^.]+$", "");
    }

    @SuppressWarnings("ConstantConditions")
    public static List<File> collectFilesOf(File base) {
        assert base.isDirectory();
        List<File> tor = new ArrayList<>();
        for (File f : base.listFiles()) {
            if (f.isDirectory()) {
                tor.addAll(collectFilesOf(f));
            } else {
                tor.add(f);
            }
        }
        return tor;
    }

    public static String bytesToString(long bytes) {
        String[] arr = {"bytes", "KB", "MB"};
        int i = 0;
        for (; i < arr.length - 1; i++) {
            if (bytes < 1024) {
                break;
            }
            bytes /= 1024;
        }
        return bytes + " " + arr[i];
    }

    public interface RunnableWithAChanceOfException {
        void run() throws Exception;
    }

    public interface ConsumerWithAChanceOfException<E> {
        void accept(E operand) throws Exception;
    }

    public interface FunctionWithAChanceOfException<E, T> {
        T accept(E operand) throws Exception;
    }

    public interface SupplierWithAChanceOfException<E> {
        E get() throws Exception;
    }
}
