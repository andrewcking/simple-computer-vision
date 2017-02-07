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
    short[] binaryImg;
    //list of our components
    List<ConnectedComponent> listOfItems = new ArrayList<>();


    //display width and height
    int displayWidth = 512;
    int displayHeight = 512;

    public void setup() {
        //set our current display to 512x512 grayscale image
        dispWindow = createImage(displayWidth, displayHeight, ALPHA);

        //Arg 1: filename -- Arg 2: # of bytes to trim from head
        loadImageData("comb.img", 512);

        convertToBinaryImage();
        //takes filter size as argument
        connectedComponent(150);

        //color our image turn this off to see original image
        colorImage();

        //Display image in display window beginning at top left corner
        image(dispWindow, 0, 0);

        //Show bounding box, centroid, axis of elongation
        showInfoDetails();

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
     * Loads our grayscale image GImage to a byte array
     */
    public void loadImageData(String filename, int trimHeader) {
        //image title,width,height, number of bytes to strip (for metadata)
        GImage gray = new GImage(filename, displayWidth, displayHeight, trimHeader);
        byte[] grayPixels = gray.getPixels();
        //display our grayscale image (by affecting dispImage)
        for (int i = 0; i < dispWindow.pixels.length; i++) {
            dispWindow.pixels[i] = color(Byte.toUnsignedInt(grayPixels[i]));

        }

    }

    /**
     * Loads our GImage and converts it to a binary image based on our threshold
     */
    public void convertToBinaryImage() {
        //create binary image array
        binaryImg = new short[dispWindow.width * dispWindow.height];
        //CONVERT TO BINARY IMAGE
        //for each pixel
        for (int i = 0; i < dispWindow.pixels.length; i++) {
            //get its color and get the brightness of that color
            int pixelColor = dispWindow.pixels[i];
            float brightness = brightness(pixelColor);
            //if the brightness is less than our threshold color the pixel black otherwise white
            if (brightness > threshold) {
                binaryImg[i] = 0;
            } else {
                binaryImg[i] = 1;
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
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 1) {

                //SEQUENTIAL CONNECTED COMPONENT
                int N = i - dispWindow.width;
                int W = i - 1;
                //catch exceptions where we are looking at a pixel that does not exist (treat them as 0 pixels)
                if (N < 0) {
                    N = 0;
                }
                if (W < 0) {
                    W = 0;
                }
                //if the N pixel has a label and the W does not: assign the value of the N pixel to the current pixel
                if (binaryImg[N] > 1 && binaryImg[W] < 2) {
                    binaryImg[i] = binaryImg[N];
                    //if the W pixel has a label and the N does not: assign the value of the W pixel to the current pixel
                } else if (binaryImg[W] > 1 && binaryImg[N] < 2) {
                    binaryImg[i] = binaryImg[W];
                    //if they both have a label and it is the same
                } else if (binaryImg[N] == binaryImg[W] && binaryImg[N] > 1 && binaryImg[W] > 1) {
                    binaryImg[i] = binaryImg[N];
                    //if they both have labels AND they are both different ones
                } else if (binaryImg[N] != binaryImg[W] && binaryImg[N] > 1 && binaryImg[W] > 1) {
                    binaryImg[i] = binaryImg[N];
                    //(west, north)
                    equivTab.assignNewValue(binaryImg[W], binaryImg[N]);
                } else {
                    binaryImg[i] = equivTab.getLabelNumber();
                    equivTab.createNewLabel(binaryImg[i]);
                }
            }
        }
        collapseLabels(equivTab);
        createComponents();
        componentSizeFilter(filtersize);
        for (ConnectedComponent cc : listOfItems) {
            cc.runComputations(displayWidth, displayHeight, binaryImg);
        }
    }

    /**
     * Collapse our labels so each component only has 1 and then shift them down
     * so they are sequential
     */
    public void collapseLabels(EquivalenceTable equivTab) {

        //first collapse the labels to 1 per component
        for (int i = 0; i < binaryImg.length; i++) {
            //when we find a label
            if (binaryImg[i] > 1) {
                //this could be optimized
                binaryImg[i] = equivTab.findLowest(binaryImg[i]);
            }
        }

        //then shift the label numbers to be sequential starting with 2
        int numOfComp = equivTab.getNumOfObjects();//get the number of components from the equiv table

        //for each item get its label
        for (int i = 0; i < numOfComp; i++) {
            short currentLabel = equivTab.getLabel(i);
            //loop through image and reset the pixels to our count
            itemCount++;
            for (int p = 0; p < binaryImg.length; p++) {
                if (binaryImg[p] == currentLabel) {
                    binaryImg[p] = (short) (itemCount + 1);
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
            for (int p = 0; p < binaryImg.length; p++) {
                if (binaryImg[p] == i + 2) {
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
                    binaryImg[cc.pixels.get(i2)] = 0;
                }
                iter.remove();
                itemCount--;
            }
        }

    }

    /**
     * Display the background and a unique color for each image
     */
    private void colorImage() {

        //color background
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 0) {
                dispWindow.pixels[i] = color(255, 255, 255);
            }
        }
        //color foreground
        for (int i = 0; i < listOfItems.size(); i++) {
            for (int pix : listOfItems.get(i).pixels) {
                dispWindow.pixels[pix] = color(254 / (i + 1), 150 / (i + 1), (i + 1) * 50);

            }
            for (int pix : listOfItems.get(i).boundary) {
                dispWindow.pixels[pix] = color(1, 1, 1);

            }
        }

    }

    /**
     * Display the bounding box
     */
    private void showInfoDetails() {
        stroke(0, 255, 0);
        fill(0, 0);
        for (ConnectedComponent cc : listOfItems) {
            //display bounding boxes
            rect(cc.getLeft(), cc.getTop(), cc.getWidth(), cc.getHeight());

            //display centroid
            ellipse(cc.getCentroidX(), cc.getCentroidY(), 6, 6);

            //display axis of orientation
            //line(item.centroidX,cc.centroidY,0,(float)cc.yIntercept);
            if (cc.getWidth() < cc.getHeight()) {
                line(cc.getCentroidX(), cc.getCentroidY(), (float) cc.getCentroidX() - ((float) cc.getSlope() * 100), (float) cc.getCentroidY() - 100);
                line(cc.getCentroidX(), cc.getCentroidY(), (float) cc.getCentroidX() + ((float) cc.getSlope() * 100), (float) cc.getCentroidY() + 100);
            } else {
                line(cc.getCentroidX(), cc.getCentroidY(), (float) cc.getCentroidX() - 100, (float) cc.getCentroidY() - ((float) cc.getSlope() * 100));
                line(cc.getCentroidX(), cc.getCentroidY(), (float) cc.getCentroidX() + 100, (float) cc.getCentroidY() + ((float) cc.getSlope() * 100));
            }

        }
    }

    public void outputImageInfo() {

        println("Number of items: " + itemCount);
        println("------------------");
        for (int i = 0; i < listOfItems.size(); i++) {
            ConnectedComponent cc = listOfItems.get(i);
            println("---Item " + i + "---");
            println("area: " + cc.pixels.size());
            println("centroid: " + cc.getCentroidX() + "," + cc.getCentroidY());
            println("bounding box: leftTop: " + cc.getLeft() + "," + cc.getTop() + " rightBottom: " + (cc.getLeft() + cc.getWidth()) + "," + (cc.getTop() + cc.getHeight()));
            println("axis of elongation: " + cc.getCentroidY() + " = " + cc.getSlope() + "*" + cc.getCentroidX() + " + " + cc.getyIntercept());
            println("eccentricity: " + cc.getEccentricity());
            println("perimeter: " + cc.boundary.size());
            println("compactness: " + (Math.pow(cc.boundary.size(), 2) / cc.pixels.size()));
            println();
        }

    }

    public static void main(String[] args) {
        //calls the setup method
        PApplet.main("objectdetection.ObjectDetection");
    }

}
