package sample;

import javafx.beans.property.SimpleStringProperty;

import javax.sound.sampled.AudioFormat;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by isaac on 3/29/17.
 */
public class LocalSong extends Song implements AutoCloseable {
    private String path;
    public RandomAccessFile2 file;
    private static byte[] buffer = new byte[60*1024];

    public LocalSong(String path, String filename, String name, String singer, String album) throws FileNotFoundException {
        super.name = new SimpleStringProperty(name);
        super.singer = new SimpleStringProperty(singer);
        super.album = new SimpleStringProperty(album);
        super.filename = filename;
        this.path = path;
        file = new RandomAccessFile2(path, "r");
    }

    public LocalSong(String filename) throws FileNotFoundException {
        this("database/"+filename, filename, filename, "N/A", "N/A");
    }

    @Override
    public byte[] read() throws IOException, InterruptedException {
        if (file.read(buffer) == -1) {
            throw new EOFException();
        }
        return buffer;
    }


    @Override
    public void close() throws IOException {
        file.close();
    }

    @Override
    public AudioFormat getFormat() throws Exception {
        file.seek(0);
        if (file.read() != 'R') {
            throw new Exception("The target file is not a RIFF file.");
        }
        file.skipBytes(11);
        if (file.read() != 'f') {
            throw new Exception("The implementation of this program assumes that fmt is placed before data.");
        }
        file.skipBytes(3);
        int fmtChunkSize = file.readLittleEndian(4);
        file.skipBytes(2);
        int channels = file.readLittleEndian(2);
        int sampleRate = file.readLittleEndian(4);
        int byteRate = file.readLittleEndian(4);
        int blockAlign = file.readLittleEndian(2);
        int bitsPerSample = file.readLittleEndian(2);
        if (file.read() != 'd') {
            throw new Exception("apparently we are not reading the 'data' chunk.");
        }
        file.seek(20+fmtChunkSize);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitsPerSample, channels, 4, byteRate/4, false);
    }

    public String serialize() throws IOException {
        final String separator = ",,";
        String str = filename + separator + getFilesize() + separator + name.get() + separator + singer.get() + separator + album.get();
        return str;
    }

    public long getFilesize() throws IOException {
        return file.length();
    }

    public String getLocation() {
        return "Local";
    }
}