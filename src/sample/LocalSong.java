package sample;

import javafx.beans.property.SimpleStringProperty;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * A local song.
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

    @Override
    public String getPath(){return path;}

    @Override
    public List<String> getUris(){return null;}

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

    public void start() {

    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    /**
     * @return the AudioFormat parsed
     * @throws IOException
     */
    @Override
    public AudioFormat getFormat() throws IOException {
        file.seek(0);
        if (file.read() != 'R') {
            throw new RuntimeException("The target file is not a RIFF file.");
        }
        file.skipBytes(11);
        if (file.read() != 'f') {
            throw new RuntimeException("The implementation of this program assumes that fmt is placed before data.");
        }
        file.skipBytes(3);
        final int fmtChunkSize = file.readLittleEndian(4);
        file.skipBytes(2);
        final int channels = file.readLittleEndian(2);
        final int sampleRate = file.readLittleEndian(4);
        final int byteRate = file.readLittleEndian(4);
        final int blockAlign = file.readLittleEndian(2);
        final int bitsPerSample = file.readLittleEndian(2);
        final int frameSize = bitsPerSample / 8 * 2;
        if (file.read() != 'd') {
            throw new RuntimeException("apparently we are not reading the 'data' chunk.");
        }
        file.skipBytes(7);
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitsPerSample, channels, frameSize, byteRate/frameSize, false);
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

    /**
     * @return a promise holding the lyrics text stream
     */
    public CompletableFuture<InputStream> lyrics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new FileInputStream(path + ".lrc");
            } catch (FileNotFoundException ex) {
                Logger.getLogger("LocalSong").info("returning null as lyrics due to 404 (on disk).");
                return null;
            }
        });
    }

    /**
     * @return the AudioFormat parsed by Java
     * @throws IOException when the file cannot be opened
     * @throws UnsupportedAudioFileException
     */
    private AudioFormat javaParsedFormat() throws IOException, UnsupportedAudioFileException {
        final AudioInputStream audio = AudioSystem.getAudioInputStream(new File(path));
        return audio.getFormat();
    }
}
