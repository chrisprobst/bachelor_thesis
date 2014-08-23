package de.probst.ba.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class IOUtil {

    private IOUtil() {

    }

    public static void serialize(File file, Object object) throws IOException {
        try (ObjectOutputStream objectOutputStream =
                     new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutputStream.writeObject(object);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream =
                     new ObjectInputStream(new FileInputStream(file))) {
            return (T) objectInputStream.readObject();
        }
    }
}