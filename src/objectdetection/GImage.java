package objectdetection;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Andrew
 */
public class GImage {

    public byte[] pixels;
    public int width;
    public int height;
    public int strip = 0;
    public String filename;

    //filename, width, height, optional strip for number of bytes to be removed from beginning of file
    public GImage(String file, int w, int h) {
        loadPixels(file, w, h);
    }

    public GImage(String file, int w, int h, int s) {
        loadPixels(file, w, h);
        strip = s;

    }

    void loadPixels(String file, int w, int h) {
        width = w;
        height = h;
        filename = file;
        Path p = FileSystems.getDefault().getPath("", filename);
        try {
            pixels = Files.readAllBytes(p);
        } catch (IOException ex) {
            Logger.getLogger(ObjectDetection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    byte[] getPixels() {
        return Arrays.copyOfRange(pixels, strip, pixels.length);
    }

}
