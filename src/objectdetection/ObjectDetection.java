package objectdetection;

import processing.core.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Andrew King
 */
public class ObjectDetection extends PApplet {

    //our display window
    PImage dispWindow;
    //counter for the number of items
    short itemCount = 0;
    //array for our binary image
    short[] imageArray;
    //list of our components
    List<ConnectedComponent> listOfItems = new ArrayList<>();

    //display width and height
    int displayWidth = 512;
    int displayHeight = 512;

    /**
     * Driver method called by the main method
     */
    public void setup() {
        //set our current display to 512x512 gray scale image
        dispWindow = createImage(displayWidth, displayHeight, ALPHA);
        //Arg 1: filename -- Arg 2: # of bytes to trim from head
        loadImageData("comb.img", 512);

        simpleThreshold(128, imageArray);
        //modeThreshold();
        //iterativeThreshold(imageArray);
        //adaptiveThreshold((short) 128);
        //doubleThreshold();

        //takes filter size as argument
        connectedComponent(1000);
        //skeletonize our components removing all pixel data except the medial axis
        //skeletonize();
        //reconstruct our components based on their medial axes
        //deSkeletonize();
        //color our image (binary?, show perimeter?, show medial axis?)
        colorImage(false, true, true);
        //Display image in display window beginning at top left corner
        image(dispWindow, 0, 0);
        //Show bounding box, centroid and axis but not centroid coordinates
        showItemDetails(true, true, false, false);
        //output component details
        //outputImageInfo();
        print("--END--");
    }


    /**
     * Required by the processing library to set up our display window
     */
    public void settings() {
        size(displayWidth, displayWidth);
    }

    /**
     * Loads our gray scale image to a byte array and trims the header info
     * pulls from byte file to short array since that will accommodate more label numbers
     */
    public void loadImageData(String filename, int trimHeader) {
        //utilize processing loadBytes call
        byte[] grayPixels = loadBytes(filename);
        //create binary image array
        imageArray = new short[displayWidth * displayHeight];
        //trim the header data from the image and make it a short so we can have larger label numbers
        for (int i = trimHeader; i < grayPixels.length; i++) {
            //since these are unsigned bytes we have to read them as such and covert them back to short
            imageArray[i - trimHeader] = (short) Byte.toUnsignedInt(grayPixels[i]);
        }
    }

    /**
     * Loads our gray scale Image and converts it to a binary image based on our threshold
     */
    public void simpleThreshold(int threshold, short[] imageRegion) {
        //CONVERT TO BINARY IMAGE
        //for each pixel in our image
        for (int i = 0; i < imageRegion.length; i++) {
            //if the brightness is less than our threshold color the pixel black otherwise white
            if (imageRegion[i] > threshold) {
                imageRegion[i] = 0;
            } else {
                imageRegion[i] = 1;
            }
        }
    }

    /**
     * Mode Thresholds the Image
     */
    public void modeThreshold() {
        int[] histogram = createHistogram();
        int[] FOM = findFigureOfMerit(histogram);

        //what you have is your best option
        simpleThreshold(FOM[1], imageArray);
    }

    /**
     * Iterative Thresholds the Image
     */
    public void iterativeThreshold(short[] imageRegion) {
        //calculate average intensity for initial threshold
        int averageIntensity = 0;
        for (int pixel : imageRegion) {
            averageIntensity += pixel;
        }
        averageIntensity = averageIntensity / imageRegion.length;

        //Find our threshold value
        double movingThreshold = averageIntensity;
        //use doubles to prevent divide by zero
        double regionOneMean = 0;
        double regionTwoMean = 0;

        while (true) {
            int regionOne = 0;
            int regionTwo = 0;
            int regionOneSize = 0;
            int regionTwoSize = 0;
            for (int pixel : imageRegion) {
                if (pixel <= movingThreshold) {
                    regionOne += pixel;
                    regionOneSize++;
                } else {
                    regionTwo += pixel;
                    regionTwoSize++;
                }
            }
            //prevent divide by zero error AND if we haven't changed then move on
            if (regionOneSize< 1 || regionTwoSize < 1 || (regionOne / regionOneSize == regionOneMean && regionTwoMean == regionTwo / regionTwoSize)) {
                break;
                //else calculate our new means and our new threshold
            } else {
                regionOneMean = regionOne / regionOneSize;
                regionTwoMean = regionTwo / regionTwoSize;

                movingThreshold = (regionOneMean + regionTwoMean) / 2;

            }
        }

        simpleThreshold((int) movingThreshold, imageRegion);
    }

    /**
     * Adaptive Thresholds the Image
     */
    public void adaptiveThreshold(short m) {
        //mask loop (x and y of mask size m)
        for (int y = 0; y < displayWidth / m; y++) {
            for (int x = 0; x < displayHeight / m; x++) {
                short[] region = new short[m * m];
                //add pixels i to our region array from mask region
                for (int y2 = 0; y2 < m; y2++) {
                    for (int x2 = 0; x2 < m; x2++) {
                        region[x2 + (y2 * m)] = imageArray[((y * m) * displayWidth) + (x * m) + (y2 * displayWidth) + x2];
                    }
                }
                //threshold region
                iterativeThreshold(region);
                //modify our image array (stitch portions together)
                for (int y2 = 0; y2 < m; y2++) {
                    for (int x2 = 0; x2 < m; x2++) {
                        imageArray[((y * m) * displayWidth) + (x * m) + (y2 * displayWidth) + x2] = region[x2 + (y2 * m)];
                    }
                }
            }
        }
    }

    /**
     * Double Thresholds the Image
     */
    public void doubleThreshold() {
        int[] histogram = createHistogram();
        int[] FOM = findFigureOfMerit(histogram);
        println(FOM);
        //threshold out the regions that are likely 0 or 1
        for (int i = 0; i < imageArray.length; i++) {
            //if you are close to black you are foreground
            if (imageArray[i] < FOM[0]) {
                imageArray[i] = 1;
                //if you are close to white then you are background
            } else if (imageArray[i] > FOM[2]) {
                imageArray[i] = 0;
                //otherwise you are in r2 between them
            } else {
                imageArray[i] = 2;
            }
        }

        //get the first 0 pixel
        int firstBackground = -1;
        //get index of first background pixel
        for (int x = 0; x < imageArray.length; x++) {
            if (imageArray[x] == 0) {
                firstBackground = x;
                break;
            }
        }
        //run until we make no changes on run
        while (true) {
            int changes = 0;
            for (int i = 0; i < imageArray.length; i++) {
                if (imageArray[i] == 2) {
                    int N = i - displayWidth;
                    int E = i + 1;
                    int S = i + displayWidth;
                    int W = i - 1;
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
                    //whoa exception for -1
                    if (W % displayWidth == displayWidth - 1 || W == -1) {
                        W = firstBackground;
                    }
                    if (imageArray[N] == 1 || imageArray[E] == 1 || imageArray[S] == 1 || imageArray[W] == 1) {
                        imageArray[i] = 1;
                        changes++;
                    }
                }
            }
            if (changes == 0) {
                break;
            }
        }
        //reassign any pixels that are still in r2 to the background
        for (int i = 0; i < imageArray.length; i++) {
            if (imageArray[i] == 2) {
                imageArray[i] = 0;
            }
        }

    }

    /**
     * Creates and smooths a Histogram for use by Thresholding Algorithms
     */
    public int[] createHistogram() {
        int[] histogram = new int[256];
        for (short pixel : imageArray) {
            histogram[pixel]++;
        }
        //smooth histogram
        for (int i = 1; i < histogram.length - 1; i++) {
            histogram[i] = (histogram[i - 1] + histogram[i] + histogram[i + 1]) / 3;
        }
        return histogram;
    }

    /**
     * Determines our Figure of Merit (used in double thresholding and mode thresholding)
     */
    public int[] findFigureOfMerit(int[] histogram) {
        //GET MODES
        List<Integer> modes = new ArrayList<>();
        for (int i = 1; i < histogram.length - 1; i++) {
            //normal case (we use the >= in case there are plateaus, but use it once so we don't add the whole plateau)
            if (histogram[i] >= histogram[i + 1] && histogram[i] > histogram[i - 1]) {
                modes.add(i);
            }
        }
        int minDistance = 60;
        float highestPeakiness = 0;
        int threshold = 0;
        int peak1 = 0;
        int peak2 = 0;
        //for each mode combination get its valley if the one you are checking beats the last one save it as the best until no more combinations are available
        for (int i = 0; i < modes.size(); i++) {
            //set x = i so we are only checking all the modes after it (to avoid checking a pair twice)
            for (int x = i; x < modes.size(); x++) {
                //if these two modes are far enough apart then find the min between them
                if (modes.get(i) + minDistance <= modes.get(x)) {
                    //println(modes.get(i) +"," +modes.get(x));
                    int valley = modes.get(i);
                    for (int from = modes.get(i); from < modes.get(x); from++) {
                        if (histogram[from] < histogram[valley]) {
                            valley = from;
                        }
                    }
                    //calculate their peakiness
                    float peakiness = Integer.min(histogram[modes.get(i)], histogram[modes.get(x)]) / (float) histogram[valley];
                    if (peakiness > highestPeakiness) {
                        highestPeakiness = peakiness;
                        peak1 = modes.get(i);
                        peak2 = modes.get(x);
                        threshold = valley;
                        //println("peak1:" + modes.get(i) + "," + histogram[modes.get(i)] + ", peak2:" + modes.get(x) + "," + histogram[modes.get(x)] + ", valley:" + valley + "," + histogram[valley] + ", peakiness:" + peakiness);
                    }
                }
            }
        }
        return new int[]{peak1, threshold, peak2};
    }

    /**
     * Performs a raster scan of our image and calls the sequential find
     * component method if it hits a 1 pixel because our connected component
     * algorithm changes the cc pixel labels it will avoid running the same
     * object twice
     */
    public void connectedComponent(int filtersize) {
        //create equivalence table
        EquivalenceTable equivTab = new EquivalenceTable();

        //scan left to right until you find a 1 pixel
        for (int i = 0; i < imageArray.length; i++) {
            if (imageArray[i] == 1) {

                //SEQUENTIAL CONNECTED COMPONENT
                int N = i - displayWidth;
                int W = i - 1;
                //catch exceptions where we are looking at a pixel that does not exist (treat them as 0 pixels)
                if (N < 0) {
                    N = 0;
                }
                if (W < 0) {
                    W = 0;
                }
                //if the N pixel has a label and the W does not: assign the value of the N pixel to the current pixel
                if (imageArray[N] > 1 && imageArray[W] < 2) {
                    imageArray[i] = imageArray[N];
                    //if the W pixel has a label and the N does not: assign the value of the W pixel to the current pixel
                } else if (imageArray[W] > 1 && imageArray[N] < 2) {
                    imageArray[i] = imageArray[W];
                    //if they both have a label and it is the same
                } else if (imageArray[N] == imageArray[W] && imageArray[N] > 1 && imageArray[W] > 1) {
                    imageArray[i] = imageArray[N];
                    //if they both have labels AND they are both different ones
                } else if (imageArray[N] != imageArray[W] && imageArray[N] > 1 && imageArray[W] > 1) {
                    imageArray[i] = imageArray[N];
                    //(west, north)
                    equivTab.assignNewValue(imageArray[W], imageArray[N]);
                } else {
                    imageArray[i] = equivTab.getLabelNumber();
                    equivTab.createNewLabel(imageArray[i]);
                }
            }
        }
        //Calls to other methods to finish algorithm
        collapseLabels(equivTab);
        createComponents();
        componentSizeFilter(filtersize);
        //run the calculations for each component so that they generate their metrics
        for (ConnectedComponent cc : listOfItems) {
            cc.runCalculations(displayWidth, displayHeight, imageArray);
        }
    }

    /**
     * Collapse our labels so each component only has 1 and then shift them down
     * so they are sequential
     */
    public void collapseLabels(EquivalenceTable equivTab) {
        //first collapse the labels to 1 per component
        for (int i = 0; i < imageArray.length; i++) {
            //when we find a label
            if (imageArray[i] > 1) {
                //this could be optimized
                imageArray[i] = equivTab.findLowest(imageArray[i]);
            }
        }

        //then shift the label numbers to be sequential starting with 2
        int numOfComp = equivTab.getNumOfObjects();//get the number of components from the equiv table

        //for each item get its label
        for (int i = 0; i < numOfComp; i++) {
            short currentLabel = equivTab.getLabel(i);
            //loop through image and reset the pixels to our count
            itemCount++;
            for (int p = 0; p < imageArray.length; p++) {
                if (imageArray[p] == currentLabel) {
                    imageArray[p] = (short) (itemCount + 1);
                }
            }
        }
    }

    /**
     * For each item, create a new ConnectedComponent and add it to our list of
     * items
     */
    private void createComponents() {
        for (int i = 0; i < itemCount; i++) {
            ConnectedComponent item = new ConnectedComponent();
            listOfItems.add(item);
            for (int p = 0; p < imageArray.length; p++) {
                if (imageArray[p] == i + 2) {
                    listOfItems.get(i).addPixel(p);
                }
            }
        }
    }

    /**
     * Filter out all components with size less than passed in filter size arg
     * set those components to background pixels
     */
    private void componentSizeFilter(int filterSize) {
        //we have to use an iterator to prevent concurrent modification
        Iterator<ConnectedComponent> iter = listOfItems.iterator();
        while (iter.hasNext()) {
            ConnectedComponent cc = iter.next();
            if (cc.pixels.size() < filterSize) {
                for (int i2 = 0; i2 < cc.pixels.size(); i2++) {
                    imageArray[cc.pixels.get(i2)] = 0;
                }
                iter.remove();
                itemCount--;
            }
        }
    }

    /**
     * Skeletonizes all of our components
     */
    public void skeletonize() {
        for (ConnectedComponent cc : listOfItems) {
            cc.skeletonize();
        }
    }

    /**
     * deSkeletonizes each component
     */
    public void deSkeletonize() {
        for (ConnectedComponent cc : listOfItems) {
            cc.deSkeletonize(displayWidth, displayHeight);
        }
    }

    /**
     * Display the background and a unique color for each image
     */
    private void colorImage(boolean binary, boolean perimeter, boolean axis) {
        //color background
        background(255);
        for (int i = 0; i < imageArray.length; i++) {
            //dispWindow.pixels[i] = color(imageArray[i]);
            if (imageArray[i] == 1) {
                dispWindow.pixels[i] = color(0);
            }
        }
        //color foreground
        for (int i = 0; i < listOfItems.size(); i++) {
            Integer myColor;
            if (binary) {
                myColor = color(0);
            } else {
                randomSeed(i);
                myColor = color(random(0, 200), random(0, 200), random(50, 255));
            }
            //Color Object
            for (int pix : listOfItems.get(i).pixels) {
                dispWindow.pixels[pix] = myColor;
            }
            //Color Perimeter
            if (perimeter) {
                for (int pix : listOfItems.get(i).perimeter) {
                    dispWindow.pixels[pix] = color(0);
                }
            }
            //Color Medial Axis
            if (axis) {
                for (int pix : listOfItems.get(i).medialAxis.keySet()) {
                    dispWindow.pixels[pix] = color(0, 255, 0);
                }
            }
        }

    }

    /**
     * Display the bounding box and centroids
     */
    private void showItemDetails(boolean boundingBox, boolean centroid, boolean axis, boolean cCoordinates) {
        stroke(0, 255, 0);
        fill(0, 0);
        for (ConnectedComponent cc : listOfItems) {
            //display bounding boxes
            if (boundingBox) {
                rect(cc.getLeft(), cc.getTop(), cc.getWidth() + 1, cc.getHeight() + 1);
            }
            //display centroid
            if (centroid) {
                ellipse(cc.getCentroidX(), cc.getCentroidY(), 6, 6);
            }
            //display axis orientation
            if (axis) {
                stroke(255, 0, 0);
                float tangent = cc.b / (cc.a - cc.c);
                line(cc.getCentroidX(), cc.centroidY, cc.centroidX + ((float) cc.cosTwoTheta * 100), cc.centroidY + ((float) cc.sinTwoTheta * 100));
                line(cc.getCentroidX(), cc.centroidY, cc.centroidX - ((float) cc.cosTwoTheta * 100), cc.centroidY - ((float) cc.sinTwoTheta * 100));
                stroke(0, 255, 0);
            }
            //display centroid coordinates
            if (cCoordinates) {
                fill(0);
                textSize(12);
                text(cc.getCentroidX() + "," + cc.getCentroidY(), cc.getCentroidX() - 50, cc.getCentroidY() + 5);
                fill(0, 0);
                updatePixels();
            }
        }
    }

    public void outputImageInfo() {
        println("------------------------------------");
        println("Number of items: " + itemCount);
        println("------------------------------------");
        for (int i = 0; i < listOfItems.size(); i++) {
            ConnectedComponent cc = listOfItems.get(i);
            println("------Item " + i + "------");
            cc.printObject();
            println();
        }
    }

    public static void main(String[] args) {
        //calls the setup method
        PApplet.main("objectdetection.ObjectDetection");
    }

}
