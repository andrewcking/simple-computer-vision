package componentdetect;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author Andrew King
 */
public class ConnectedComponent {

    ArrayList<Integer> pixels = new ArrayList<Integer>();
    int width;
    int height;
    int top;
    int left;
    public void addPixel(int i) {
        pixels.add(i);
    }

    public void getBounds(int fullWidth) {
        left = Integer.MAX_VALUE;
        int right = 0;
        int max = Collections.max(pixels);
        int min = Collections.min(pixels);
        
        for (int x = 0; x < pixels.size(); x++) {
            //get far right and far left coord of object
            if (pixels.get(x) % fullWidth < left) {
                left = pixels.get(x) % fullWidth;
            }
            if (pixels.get(x) % fullWidth > right) {
                right = pixels.get(x) % fullWidth;
            }
        }
        width = right - left;
        top = (min / fullWidth) - 1;
        int bottom = max / fullWidth + 1;
        height = bottom - top;
        //System.out.println("width " + width + " height " + height + " top " + top + " left " + left);
    }

    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTop() {
        return top;
    }

    public int getLeft() {
        return left;
    }


}
