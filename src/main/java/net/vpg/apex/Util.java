package net.vpg.apex;

import net.vpg.apex.components.WrappedTextArea;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.vpg.apex.Apex.APEX;
import static net.vpg.apex.Apex.LOGGER;

public class Util {
    public static void sleep(int millis) {
        run(() -> Thread.sleep(millis));
    }

    public static void run(RunnableWithAChanceOfException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOGGER.error("Encountered an unexpected uncaught exception:", e);
        }
    }

    @SafeVarargs
    public static <E> E apply(E input, ConsumerWithAChanceOfException<E>... functions) {
        for (ConsumerWithAChanceOfException<E> consumer : functions) {
            try {
                consumer.accept(input);
            } catch (Exception e) {
                LOGGER.error("Encountered an unexpected uncaught exception:", e);
            }
        }
        return input;
    }

    public static <E, T> T compute(E input, FunctionWithAChanceOfException<E, T> function) {
        try {
            return function.accept(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <E> E get(SupplierWithAChanceOfException<E> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
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


    public static String getId(String fileName) {
        return fileName.substring(0, fileName.contains(".") ? fileName.lastIndexOf('.') : fileName.length());
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

    public static JButton makeButton(String name, String toolTip, int action) {
        return Util.apply(new JButton(name),
            button -> button.setToolTipText(toolTip),
            button -> button.addActionListener(e -> APEX.takeAction(action)));
    }

    @SafeVarargs
    public static WrappedTextArea makeTextArea(String toolTip, ConsumerWithAChanceOfException<WrappedTextArea>... actions) {
        return makeTextArea("Loading...", toolTip, actions);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static WrappedTextArea makeTextArea(String name, String toolTip, ConsumerWithAChanceOfException<WrappedTextArea>... actions) {
        List<ConsumerWithAChanceOfException<WrappedTextArea>> actualActions = new ArrayList<>();
        Collections.addAll(actualActions, actions);
        actualActions.add(text -> text.setToolTipText(toolTip));
        return Util.apply(new WrappedTextArea(name), actualActions.toArray(new ConsumerWithAChanceOfException[0]));
    }

    public static void lookAndFeel() {
        apply(UIManager.getSystemLookAndFeelClassName(), UIManager::setLookAndFeel);
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

    public static <E> void shuffle(List<E> base) {
        for (int i = base.size(); i > 0; i--) {
            base.add(base.remove(random(0, i)));
        }
    }

    public static int random(int from, int to) {
        return (int) (Math.random() * (to - from) + from);
    }

    public interface RunnableWithAChanceOfException {
        void run() throws Exception;
    }

    public interface ConsumerWithAChanceOfException<E> {
        void accept(E operand) throws Exception;
    }

    public interface FunctionWithAChanceOfException<E, T> {
        T accept(E operand) throws IOException;
    }

    public interface SupplierWithAChanceOfException<E> {
        E get() throws IOException;
    }
}
