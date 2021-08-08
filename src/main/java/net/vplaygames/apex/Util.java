package net.vplaygames.apex;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Util {
    @SafeVarargs
    public static <A> A apply(A input, ConsumerWithAChanceOfException<A>... functions) {
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

    public static List<File> collectFilesOf(String path) {
        return collectFilesOf(new File(path));
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

    public interface ConsumerWithAChanceOfException<E> {
        void accept(E operand) throws Exception;
    }

    public interface SupplierWithAChanceOfException<E> {
        E get() throws Exception;
    }
}
