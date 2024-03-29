package objectdetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static processing.core.PApplet.println;

/**
 * @author Andrew King
 *         Class for our components, contains their pixel, perimeter, centroid, and medial medial axis data
 *         Also contains metrics such as bounding box points, compactness, and axis of elongation
 */
public class ConnectedComponent {

    //item and perimeter pixels in object
    List<Integer> pixels = new ArrayList<>();
    HashMap<Integer, Short> medialAxis = new HashMap<>();

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
        calcMedialAxis(displayWidth, displayHeight);
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
            if (uvInBounds(N, E, S, W, displayWidth, displayHeight) && (binaryImg[N] == 0 || binaryImg[E] == 0 || binaryImg[S] == 0 || binaryImg[W] == 0)) {
                perimeter.add(pixels.get(i));
            }
        }
    }

    /**
     * Helper Method for Distance Transform and Perimeter finding - returns true if the neighbors we are checking are within the image bounds
     */
    private boolean uvInBounds(int N, int E, int S, int W, int displayWidth, int displayHeight) {
        if (N < 0 || S >= displayWidth * displayHeight || E % displayWidth == 0 || W % displayWidth == displayWidth - 1) {
            return false;
        } else {
            return true;
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
     * Determines the medial axis/skeleton of the object and adds those pixels to the medialaxis array
     */
    public void calcMedialAxis(int displayWidth, int displayHeight) {
        //GET DISTANCE TRANSFORMS - use temporary binary image for calculations without affecting our actual binary image
        short[] distTrans = new short[displayWidth * displayHeight];
        calcDistanceTransforms(displayWidth, displayHeight, distTrans);

        //CALC MEDIAL AXIS
        int firstBackground = -1;
        //get index of first background pixel
        for (int x = 0; x < distTrans.length; x++) {
            if (distTrans[x] == 0) {
                firstBackground = x;
                break;
            }
        }
        for (int i = 0; i < pixels.size(); i++) {
            short current = distTrans[pixels.get(i)];
            int N = pixels.get(i) - displayWidth;
            int E = pixels.get(i) + 1;
            int S = pixels.get(i) + displayWidth;
            int W = pixels.get(i) - 1;
            //if were checking a neighbor that is outside of the image area just pretend it is a background pixel
            if (N < 0) {
                N = firstBackground;
            }
            if (S >= displayWidth * displayHeight) {
                S = firstBackground;
            }
            if (E % displayWidth == 0) {
                E = firstBackground;
            }
            if (W % displayWidth == displayWidth - 1) {
                W = firstBackground;
            }
            //check if current pixel is greater than or equal to all its neighbors
            if (current >= distTrans[N] && current >= distTrans[E] && current >= distTrans[S] && current >= distTrans[W]) {
                medialAxis.put(pixels.get(i), distTrans[pixels.get(i)]);
            }
        }
    }

    private void calcDistanceTransforms(int displayWidth, int displayHeight, short[] distTrans) {
        //set all pixels to 1's as first iteration
        for (int i = 0; i < pixels.size(); i++) {
            distTrans[pixels.get(i)] = 1;
        }
        //next we will set all inner pixels to 2
        for (short x = 2; x < Math.min(displayWidth, displayHeight); x++) {
            boolean changesMade = false;
            for (int i = 0; i < pixels.size(); i++) {
                //find neighbors
                int N = pixels.get(i) - displayWidth;
                int E = pixels.get(i) + 1;
                int S = pixels.get(i) + displayWidth;
                int W = pixels.get(i) - 1;
                //if we are not on an edge && (short circuit) if we have ALL 4-neighbors that are the current count (has already been changed) or is the count-1, change the current pixel to the current count
                if (uvInBounds(N, E, S, W, displayWidth, displayHeight) && (distTrans[N] == x || distTrans[N] == x - 1) && (distTrans[E] == x || distTrans[E] == x - 1)
                        && (distTrans[S] == x || distTrans[S] == x - 1) && (distTrans[W] == x || distTrans[W] == x - 1)) {
                    distTrans[pixels.get(i)] = x;
                    changesMade = true;
                }
            }
            //if we made no changes we are done
            if (changesMade == false) {
                break;
            }
        }
    }

    /**
     * Converts the object to its medial axis only (removes all other pixel data)
     */
    public void skeletonize() {
        pixels = new ArrayList<>();
        for (Integer pixel : medialAxis.keySet()) {
            pixels.add(pixel);
        }
    }

    /**
     * Reconstructs the object from its medial axis
     */
    public void deSkeletonize(int displayWidth, int displayHeight) {
        short[] deSkeleton = new short[displayWidth * displayHeight];
        for (Integer pixel : medialAxis.keySet()) {
            deSkeleton[pixel] = medialAxis.get(pixel);
        }

        //get the maximum value/distance in the image array to use as for loop start
        short maxValue = 0;
        for (short pixel : deSkeleton) {
            if (pixel > maxValue) {
                maxValue = pixel;
            }
        }
        //loop from our max value down to 2(inclusive, which builds the 1s ring) incrementally building out our binary image again
        for (int i = maxValue; i > 1; i--) {
            for (int x = 0; x < deSkeleton.length; x++) {
                if (deSkeleton[x] == i) {
                    //get our compass pixels
                    int N = x - displayWidth;
                    int E = x + 1;
                    int S = x + displayWidth;
                    int W = x - 1;
                    //if there is a north pixel and it hasn't been rebuilt already, build it with our current i-1
                    if (!(N < 0) && deSkeleton[N] == 0) {
                        deSkeleton[N] = (short) (i - 1);
                        pixels.add(N);
                    }
                    //if there is an east pixel
                    if (!(E % displayWidth == 0) && deSkeleton[E] == 0) {
                        deSkeleton[E] = (short) (i - 1);
                        pixels.add(E);
                    }
                    //if there is a south pixel
                    if (!(S >= displayWidth * displayHeight) && deSkeleton[S] == 0) {
                        deSkeleton[S] = (short) (i - 1);
                        pixels.add(S);
                    }
                    //if there is a west pixel
                    if (!(W % displayWidth == displayWidth - 1) && deSkeleton[W] == 0) {
                        deSkeleton[W] = (short) (i - 1);
                        pixels.add(W);
                    }
                }
            }
        }
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
