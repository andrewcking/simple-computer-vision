package objectdetection;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Andrew King
 */
public class ConnectedComponent {

    ArrayList<Integer> pixels = new ArrayList<>();
    ArrayList<Integer> boundary = new ArrayList<>();
    int width;
    int height;
    int top;
    int left;
    int centroidX;
    int centroidY;
    double slope;
    double yIntercept;
    double xMax;
    double eccentricity;

    public void addPixel(int i) {
        pixels.add(i);
    }

    public void addBoundary(int i) {
        boundary.add(i);
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

    public void setCentroidX(int inCentX) {
        centroidX = inCentX;
    }

    public void setCentroidY(int inCentY) {

        centroidY = inCentY;
    }

    public void getAxis(int displayWidth, int displayHeight){
        SimpleRegression regression = new SimpleRegression();
        SimpleRegression regressionalt = new SimpleRegression();
        for (Integer pixel : pixels) {
            int x = pixel % displayWidth;
            int y = Math.floorDiv(pixel, displayHeight);
            if (width < height){
                regression.addData(y, x);
                regressionalt.addData(x, y);
            } else {
                regression.addData(x, y);
                regressionalt.addData(y, x);
            }
        }

        xMax = regressionalt.getSumSquaredErrors();
        eccentricity = xMax/regression.getSumSquaredErrors();
        yIntercept = regression.getIntercept();
        slope = regression.getSlope();
    }

}
