package com.github.idelstak.mp3tagging;

import javafx.scene.image.*;
import org.jaudiotagger.audio.*;
import org.jaudiotagger.tag.*;
import org.jaudiotagger.tag.datatype.*;
import org.slf4j.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

class Mp3 {

    private static final Logger LOG = LoggerFactory.getLogger(Mp3.class);
    private final String file;
    private final String header;
    private String title;
    private String artist;
    private String album;
    private String year;
    private String track;
    private String genre;
    private Image coverArt;

    Mp3(AudioFile audioFile) {
        file = audioFile.getFile().getAbsolutePath();
        header = createHeaderSummary(audioFile.getAudioHeader());
        Tag tag = audioFile.getTag();
        if (tag != null) {
            title = tag.getFirst(FieldKey.TITLE);
            artist = tag.getFirst(FieldKey.ARTIST);
            album = tag.getFirst(FieldKey.ALBUM);
            year = tag.getFirst(FieldKey.YEAR);
            track = tag.getFirst(FieldKey.TRACK);
            genre = tag.getFirst(FieldKey.GENRE);
            try {
                coverArt = readFirstArtWork(tag);
            } catch (IOException e) {
                LOG.error("", e);
            }
        }
    }

    private static String createHeaderSummary(AudioHeader audioHeader) {
        String bitRate = "%s kbps".formatted(audioHeader.getBitRate());
        String channels = audioHeader.getChannels();
        String sampleRate = "%d kHz".formatted(audioHeader.getSampleRateAsNumber() / 1000);
        String encoding = audioHeader.getEncodingType().toUpperCase();
        return "%s | %s | %s | %s".formatted(encoding, channels, sampleRate, bitRate);
    }

    private static Image readFirstArtWork(Tag tag) throws IOException {
        Image art = null;
        Artwork artwork = tag.getFirstArtwork();
        if (artwork != null) {
            art = Mp3.createImage(artwork.getImage());
        }
        return art;
    }

    private static Image createImage(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            byte[] buffer = out.toByteArray();
            try (ByteArrayInputStream in = new ByteArrayInputStream(buffer)) {
                return new Image(in);
            }
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Mp3.class.getSimpleName() + "[", "]").add("file='" + file + "'").toString();
    }

    String title() {
        return title;
    }

    String file() {
        return file;
    }

    String header() {
        return header;
    }

    String artist() {
        return artist;
    }

    String album() {
        return album;
    }

    String year() {
        return year;
    }

    String track() {
        return track;
    }

    String genre() {
        return genre;
    }

    Image coverArt() {
        return coverArt;
    }

    static boolean isMp3(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".mp3");
    }
}