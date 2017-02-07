package objectdetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static processing.core.PApplet.println;

/**
 * @author Andrew King
 *         Class for our components, contains their pixel info and metrics
 */
public class ConnectedComponent {

    //item and perimeter pixels in object
    List<Integer> pixels = new ArrayList<>();
    List<Integer> perimeter = new ArrayList<>();
    //width and height of object (not of bounding box)
    int width, height;
    //top left coordinates of bounding box
    int top, left, centroidX, centroidY;
    //second-order moments
    float a, b, c;
    //various metrics about the object
    double sinTwoTheta, cosTwoTheta, chiSquaredMax, chiSquaredMin, eccentricity, compactness;

    public void addPixel(int i) {
        pixels.add(i);
    }

    /**
     * Runs calculations on the component to retrieve perimeter, centroid, bounding box and axis information
     * Because some calculations depend on others being calculated first, the order these are called is important
     */
    public void runCalculations(int displayWidth, int displayHeight, short[] binaryImg) {
        calcPerimeter(displayWidth, displayHeight, binaryImg);
        calcCentroid(displayWidth);
        calcBounds(displayWidth);
        calcAxis(displayWidth);
        compactness = Math.pow(perimeter.size(), 2) / pixels.size();
    }

    /**
     * Determines the perimeter of the object and adds those pixels to the perimeter array
     */
    private void calcPerimeter(int displayWidth, int displayHeight, short[] binaryImg) {
        for (int i = 0; i < pixels.size(); i++) {
            int N = pixels.get(i) - displayWidth;
            int E = pixels.get(i) + 1;
            int S = pixels.get(i) + displayWidth;
            int W = pixels.get(i) - 1;
            //if n or s pixel is beyond the image were on the perimeter, if w or e pixel is on an edge were on the perimeter
            if (N < 0 || S >= displayWidth * displayHeight || E % displayWidth == 0 || W % displayWidth == displayWidth - 1) {
                perimeter.add(pixels.get(i));

            } else if (binaryImg[N] == 0 || binaryImg[E] == 0 || binaryImg[S] == 0 || binaryImg[W] == 0) {

                perimeter.add(pixels.get(i));
            }
        }
    }

    /**
     * Determines the location of the centroid
     */
    private void calcCentroid(int displayWidth) {
        int xBar = 0;
        int yBar = 0;
        for (Integer pixel : pixels) {
            //add all the j values
            xBar += pixel % displayWidth;
            //add all the i values
            yBar += Math.floorDiv(pixel, displayWidth);
        }
        xBar = xBar / pixels.size();
        yBar = yBar / pixels.size();
        centroidX = xBar;
        centroidY = yBar;
    }

    /**
     * Determines the axis-aligned minimum bounding box coordinates
     */
    private void calcBounds(int fullWidth) {

        left = Integer.MAX_VALUE;
        int right = 0;
        int max = Collections.max(pixels);
        int min = Collections.min(pixels);

        for (int x = 0; x < pixels.size(); x++) {
            //get far right and far left coordinates of component
            if (pixels.get(x) % fullWidth < left) {
                left = pixels.get(x) % fullWidth;
            }
            if (pixels.get(x) % fullWidth > right) {
                right = pixels.get(x) % fullWidth;
            }
        }
        //adjust so bounding box is not overlapping with component
        left = left - 1;
        width = right - left;
        top = (min / fullWidth) - 1;
        int bottom = max / fullWidth;
        height = bottom - top;

    }

    /**
     * Determines the metrics for the axis of elongation
     */
    private void calcAxis(int displayWidth) {
        //second order moments
        a = 0;
        b = 0;
        c = 0;
        //for each pixel in the component, add corresponding value to its second order moments
        for (Integer pixel : pixels) {
            int x = pixel % displayWidth;
            int y = Math.floorDiv(pixel, displayWidth);
            int xPrime = x - centroidX;
            int yPrime = y - centroidY;
            a += Math.pow(xPrime, 2);
            b += xPrime * yPrime;
            c += Math.pow(yPrime, 2);
        }
        //don't forget to multiply b by 2
        b = b * 2;
        //calculate other metrics
        sinTwoTheta = b / (Math.sqrt(Math.pow(b, 2) + Math.pow(a - c, 2)));
        cosTwoTheta = (a - c) / (Math.sqrt(Math.pow(b, 2) + Math.pow(a - c, 2)));
        chiSquaredMax = .5 * (a + c) + .5 * (a - c) * cosTwoTheta + .5 * b * sinTwoTheta;
        chiSquaredMin = .5 * (a + c) + .5 * (a - c) * (-cosTwoTheta) + .5 * b * (-sinTwoTheta);
        eccentricity = chiSquaredMax / chiSquaredMin;
    }

    /**
     * A method to print all metrics about the item
     */
    public void printObject() {
        println("area: " + pixels.size());
        println("centroid(xbar, ybar): " + centroidX + "," + centroidY);
        println("bounding box [i,j]: TopLeft: " + top + "," + left + " BottomRight: " + (top + height + 2) + "," + (left + width + 2));

        println("axis of elongation: sinTwoTheta: " + String.format("%.4f", sinTwoTheta));
        println("\t" + "cosTwoTheta: " + String.format("%.4f", cosTwoTheta));
        println("\t" + "chiSquaredMin: " + String.format("%.4f", chiSquaredMin));
        println("\t" + "second-order moments: a:" + String.format("%.1f", a) + " b:" + String.format("%.1f", b) + " c:" + String.format("%.1f", c));

        println("eccentricity: " + String.format("%.4f", eccentricity));
        println("perimeter (by simple count): " + perimeter.size());
        println("compactness: " + String.format("%.4f", compactness));
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

    public int getTop() { return top; }

    public int getLeft() { return left; }

    public int getCentroidX() { return centroidX; }

    public int getCentroidY() { return centroidY; }


}
