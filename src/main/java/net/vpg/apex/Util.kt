/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vpg.apex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            throw new IllegalStateException(e);
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
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static List<File> collectFilesOf(File base) {
        assert base.isDirectory();
        List<File> tor = new ArrayList<>();
        for (File f : base.listFiles()) {
            if (f.isDirectory())
                tor.addAll(collectFilesOf(f));
            else
                tor.add(f);
        }
        return tor;
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
