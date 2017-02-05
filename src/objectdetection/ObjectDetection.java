package objectdetection;



import processing.core.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Andrew King
 */
public class ObjectDetection extends PApplet {

    //our display image
    PImage dispImage;
    //binary image threshhold
    int threshold = 128;
    //counter for the number of items
    short itemCount = 0;
    //array for our binary image
    short[] binaryImg;
    //list of our components
    List<ConnectedComponent> listOfItems = new ArrayList<>();
    //create equivalence table
    EquivalenceTable equivTab = new EquivalenceTable();

    int displayWidth = 512;
    int displayHeight = 512;
    String filename = "comb.img";

    public void setup() {
        //set our current display to 512x512 grayscale image
        dispImage = createImage(displayWidth, displayHeight, ALPHA);

        //takes argument of number of bytes to trim from head
        loadGrayScaleImage(512);

        //load the pixels from the image into the pixel array
        //dispImage.loadPixels();
        convertToBinaryImage();
        rasterScanLabel();
        collapseLabels();
        createComponents();
        //takes filter size as argument
        componentSizeFilter(660);
        getPerimeter();
        calculateCentroids();
        colorImage();
        //Display image changes
        image(dispImage, 0, 0);
        showRectangleBoundary();
        outputImageInfo();
    }


    /**
     * Required by the processing library to set up our display window
     */
    public void settings() {
        size(displayWidth, displayWidth);
    }


    /**
     * Performs an 8-Neighbor tracing algorithm for finding the perimeter
     */
    public void getPerimeter() {
        List<Integer> Compass = new ArrayList<>();

        for (ConnectedComponent cc : listOfItems) {

            int startPix = cc.pixels.get(0);
            cc.addBoundary(startPix);
            int currentPix = startPix;
            int backTrack = startPix - 1;

            do {
                //can we add an if x y z statment here to +1 if it is on a corner top or bottom then else if what is below
                if (binaryImg[currentPix] > 0) {
                    cc.addBoundary(currentPix);
                    //N
                    Compass.add(0,currentPix - dispImage.width);
                    //NE...
                    Compass.add(1,currentPix - dispImage.width + 1);
                    //E
                    Compass.add(2,currentPix + 1);
                    Compass.add(3,currentPix + dispImage.width + 1);
                    Compass.add(4,currentPix + dispImage.width);
                    Compass.add(5,currentPix + dispImage.width - 1);
                    Compass.add(6,currentPix - 1);
                    Compass.add(7,currentPix - dispImage.width - 1);

                    currentPix = backTrack;
                } else {
                    backTrack = currentPix;
                    //get the next 8 neighbor pixel
                    if (Compass.indexOf(backTrack) < 7) {

                        currentPix = Compass.get(Compass.indexOf(backTrack) + 1);
                    } else {
                        //if there was something on top we would need something here
                        currentPix = Compass.get(0);
                    }
                }
            }
            while (currentPix != startPix);
        }
    }

    public void calculateCentroids(){
        for (ConnectedComponent cc : listOfItems) {
            int xBar = 0;
            int yBar = 0;
            for(Integer pixel : cc.pixels){
                xBar += (pixel % displayWidth)-1;
                yBar += Math.floorDiv(pixel,displayHeight);
            }
            xBar = xBar/cc.pixels.size();
            yBar = yBar/cc.pixels.size();
            cc.setCentroidX(xBar);
            cc.setCentroidY(yBar);
        }

    }
    /**
     * Loads our grayscale image GImage to a byte array
     */
    public void loadGrayScaleImage(int trimHeader) {
        //image title,width,height, number of bytes to strip (for metadata)
        GImage gray = new GImage(filename, displayWidth, displayHeight, trimHeader);
        byte[] grayPixels = gray.getPixels();
        //display our grayscale image (by affecting obImage)
        for (int i = 0; i < dispImage.pixels.length; i++) {
            dispImage.pixels[i] = color(Byte.toUnsignedInt(grayPixels[i]));

        }

    }

    /**
     * Loads our GImage and converts it to a binary image based on our threshold
     */
    public void convertToBinaryImage() {
        //create binary image array
        binaryImg = new short[dispImage.width * dispImage.height];
        //CONVERT TO BINARY IMAGE
        //for each pixel
        for (int i = 0; i < dispImage.pixels.length; i++) {
            //get its color and get the brightness of that color
            int pixelColor = dispImage.pixels[i];
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
    public void rasterScanLabel() {
        //scan left to right until you find a 1 pixel
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 1) {
                sFindConnected(i);
            }
        }
    }

    /**
     * Sequential find connected component algorithm
     */
    public void sFindConnected(int i) {
        int N = i - dispImage.width;
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

    /**
     * Collapse our labels so each component only has 1 and then shift them down
     * so they are sequential
     */
    public void collapseLabels() {

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
                dispImage.pixels[i] = color(255, 255, 255);
            }
        }
        //color foreground
        for (int i = 0; i < listOfItems.size(); i++) {
            for (int pix : listOfItems.get(i).pixels) {
                dispImage.pixels[pix] = color(254 / (i + 1), 150 / (i + 1), (i + 1) * 50);

            }
            for (int pix : listOfItems.get(i).boundary) {
                dispImage.pixels[pix] = color(1,1,1);

            }
        }

    }

    /**
     * Display the bounding box
     */
    private void showRectangleBoundary() {
        stroke(0, 255, 0);
        for (ConnectedComponent item : listOfItems) {
            fill(0, 0);
            item.getBounds(dispImage.width);
            rect(item.getLeft(), item.getTop(), item.getWidth(), item.getHeight());
            //fill(0, 255,0);
            ellipse(item.centroidX, item.centroidY, 6, 6);
        }
    }

    public void outputImageInfo(){

        for (int i = 0; i < listOfItems.size(); i++) {
            System.out.println("Item " + i +" area: "+ listOfItems.get(i).pixels.size());
            System.out.println("centroid: " + listOfItems.get(i).centroidX + "," + listOfItems.get(i).centroidY );
            System.out.println("perimeter: " + listOfItems.get(i).boundary.size()+ "\n");

        }
        println("Number of items: " + itemCount);
    }

    public static void main(String[] args) {
        //calls the setup method
        PApplet.main("objectdetection.ObjectDetection");
    }

}
