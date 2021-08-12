package net.vplaygames.apex;

import net.vplaygames.apex.components.WrappedTextArea;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.vplaygames.apex.Apex.apex;

public class Util {
    @SafeVarargs
    public static <E> E apply(E input, ConsumerWithAChanceOfException<E>... functions) {
        Arrays.stream(functions).forEach(a -> {
            try {
                a.accept(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return input;
    }

    public static <E> E get(SupplierWithAChanceOfException<E> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            button -> button.addActionListener(e -> apex.takeAction(action)));
    }

    public static WrappedTextArea makeTextArea(String toolTip) {
        return makeTextArea("Loading...", toolTip);
    }

    public static WrappedTextArea makeTextArea(String name, String toolTip) {
        return Util.apply(new WrappedTextArea(name), text -> text.setToolTipText(toolTip));
    }

    public static void lookAndFeel() throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
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

    public interface ConsumerWithAChanceOfException<E> {
        void accept(E operand) throws Exception;
    }

    public interface SupplierWithAChanceOfException<E> {
        E get() throws Exception;
    }
}
