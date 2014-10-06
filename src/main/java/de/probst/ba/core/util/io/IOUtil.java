package de.probst.ba.core.util.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by chrisprobst on 18.08.14.
 */
public final class IOUtil {

    public static final int DEFAULT_TRANSFER_BUFFER_SIZE = 65535;

    private IOUtil() {

    }

    public static void transfer(ReadableByteChannel readableByteChannel, WritableByteChannel writableByteChannel)
            throws IOException {
        try (ReadableByteChannel ref1 = readableByteChannel; WritableByteChannel ref2 = writableByteChannel) {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(DEFAULT_TRANSFER_BUFFER_SIZE);
            while (ref1.read(byteBuffer) >= 0) {
                byteBuffer.flip();
                while (byteBuffer.hasRemaining()) {
                    ref2.write(byteBuffer);
                }
                byteBuffer.clear();
            }
        }
    }

    public static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(byteArrayOutputStream))) {
            objectOutputStream.writeObject(object);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] array) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(
                array)))) {
            return (T) objectInputStream.readObject();
        }
    }

    public static void serialize(File file, Object object) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            objectOutputStream.writeObject(object);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (T) objectInputStream.readObject();
        }
    }
}
