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
    //binary image threshhold
    int threshold = 128;
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
        convertToBinaryImage();
        //takes filter size as argument
        connectedComponent(1500);
        //skeletonize our components removing all pixel data except the medial axis
        skeletonize();
        //reconstruct our components based on their medial axes
        deSkeletonize();
        //color our image (binary?, show perimeter?, show medial axis?)
        colorImage(false, true, true);
        //Display image in display window beginning at top left corner
        image(dispWindow, 0, 0);
        //Show bounding box and centroid but not centroid coordinates
        showItemDetails(true, true, false);
        //output component details
        outputImageInfo();
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
    public void convertToBinaryImage() {
        //CONVERT TO BINARY IMAGE
        //for each pixel in our image
        for (int i = 0; i < imageArray.length; i++) {
            //if the brightness is less than our threshold color the pixel black otherwise white
            if (imageArray[i] > threshold) {
                imageArray[i] = 0;
            } else {
                imageArray[i] = 1;
            }
        }
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
    private void showItemDetails(boolean boundingBox, boolean centroid, boolean cCoordinates) {
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
