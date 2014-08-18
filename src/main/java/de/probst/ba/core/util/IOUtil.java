package de.probst.ba.core.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class IOUtil {

    private IOUtil() {

    }

    public static void serialize(Path path, Object object) throws IOException {
        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
            objectOutputStream.writeObject(object);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(Path path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(new FileInputStream(path.toFile()))) {
            return (T) objectInputStream.readObject();
        }
    }
}
