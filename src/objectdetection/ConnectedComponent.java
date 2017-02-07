package objectdetection;

import org.apache.commons.math3.stat.regression.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Andrew King
 */
public class ConnectedComponent {

    //pixels and boundary pixels in object
    ArrayList<Integer> pixels = new ArrayList<>();
    ArrayList<Integer> boundary = new ArrayList<>();
    //width and height of object
    int width;
    int height;
    //top left coordinates
    int top;
    int left;
    //centroid coordinates
    int centroidX;
    int centroidY;
    //variables for axis of elongation and eccentricity
    double slope;
    double yIntercept;
    double eccentricity;
    double xMax;


    public void addPixel(int i) {
        pixels.add(i);
    }

    //gets item information such as centroids, bounding box and axis of elongation
    public void runComputations(int displayWidth, int displayHeight, short[] binaryImg) {
        calcPerimeter(displayWidth, displayHeight, binaryImg);
        calcCentroids(displayWidth);
        calcBounds(displayWidth);
        calcAxis(displayWidth, displayHeight);
    }

    private void calcPerimeter(int displayWidth, int displayHeight, short[] binaryImg) {
        for (int i = 0; i < pixels.size(); i++) {
            int N = pixels.get(i) - displayWidth;
            int E = pixels.get(i) + 1;
            int S = pixels.get(i) + displayWidth;
            int W = pixels.get(i) - 1;
            //if n or s pixel is beyond the image were on the perimeter, if w or e pixel is on an edge were on the perimeter
            if (N < 0 || S >= displayWidth * displayHeight || E % displayWidth == 0 || W % displayWidth == displayWidth - 1) {
                boundary.add(pixels.get(i));

            } else if (binaryImg[N] == 0 || binaryImg[E] == 0 || binaryImg[S] == 0 || binaryImg[W] == 0) {

                boundary.add(pixels.get(i));
            }
        }
    }

    private void calcCentroids(int displayWidth) {
        int xBar = 0;
        int yBar = 0;
        for (Integer pixel : pixels) {
            xBar += pixel % displayWidth;
            yBar += Math.floorDiv(pixel, displayWidth);
        }
        xBar = xBar / pixels.size();
        yBar = yBar / pixels.size();
        centroidX = xBar;
        centroidY = yBar;
    }

    private void calcBounds(int fullWidth) {

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

    private void calcAxis(int displayWidth, int displayHeight) {
        SimpleRegression regression = new SimpleRegression();
        SimpleRegression regressionalt = new SimpleRegression();
        for (Integer pixel : pixels) {
            int x = pixel % displayWidth;
            int y = Math.floorDiv(pixel, displayHeight);
            if (width < height) {
                regression.addData(y, x);
                regressionalt.addData(x, y);
            } else {
                regression.addData(x, y);
                regressionalt.addData(y, x);
            }
        }

        xMax = regressionalt.getSumSquaredErrors();
        eccentricity = xMax / regression.getSumSquaredErrors();
        yIntercept = regression.getIntercept();
        slope = regression.getSlope();
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

    public int getCentroidX() {
        return centroidX;
    }

    public int getCentroidY(){
        return centroidY;
    }
    public double getSlope(){
        return slope;
    }
    public double getEccentricity(){
        return eccentricity;
    }
    public double getyIntercept(){
        return yIntercept;
    }

}
