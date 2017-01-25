package componentdetect;

import java.util.ArrayList;
import java.util.List;
import processing.core.*;

/**
 *
 * @author Andrew King
 */
public class ComponentDetect extends PApplet {

    //our original image
    PImage obImage;
    //binary image threshhold
    int threshold = 50;
    //counter for the number of items
    short itemCount = 0;
    //array for our binary image
    short[] binaryImg;
    //list of our components
    List<ConnectedComponent> listOfItems = new ArrayList<ConnectedComponent>();
    //create equivalence table
    EquivalenceTable equivTab = new EquivalenceTable();

    public void settings() {
        //set up window size
        size(400, 550);
    }

    public void setup() {
        //load our image (objects.jpg, bapple.jpg) and downsize it
        obImage = loadImage("objects.jpg");
        obImage.resize(400, 0);
        //load the pixels from the image into the pixel array
        obImage.loadPixels();

        convertToBinaryImage();
        rasterScanLabel();
        collapseLabels();
        createComponents();
        colorImage();

        image(obImage, 0, 0);
        showRectangleBoundary();

        println("Number of items: " + itemCount);
    }

    public void convertToBinaryImage() {
        //create binary image array
        binaryImg = new short[obImage.width * obImage.height];
        //CONVERT TO BINARY IMAGE
        //for each pixel
        for (int i = 0; i < obImage.pixels.length; i++) {
            //get its color and get the brightness of that color
            int pixelColor = obImage.pixels[i];
            float brightness = brightness(pixelColor);
            //if the brightness is less than our threshold color the pixel black otherwise white
            if (brightness < threshold) {
                binaryImg[i] = 0;
            } else {
                binaryImg[i] = 1;
            }
        }
    }

    public void rasterScanLabel() {
        //scan left to right until you find a 1 pixel (the connected component should be unchecked) 
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 1) {
                sFindConnected(i);
            }
        }
    }

    //sequential find connected components
    public void sFindConnected(int i) {
        int N = i - obImage.width;
        int W = i - 1;
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

    public void collapseLabels() {
        //first collapse the labels to 1 per component
        for (int i = 0; i < binaryImg.length; i++) {
            //when we find a label
            if (binaryImg[i] > 1) {
                //this could be optimized
                binaryImg[i] = equivTab.findLowest(binaryImg[i]);
            }
        }
        //then shift the label numbers to be sequential starting with 1
        int numOfComp = equivTab.getNumOfObjects();//get the number of components from the equiv table
        //for each item get its label
        for (int i = 0; i < numOfComp; i++) {
            int currentLabel = equivTab.getLabel(i);
            //loop through image and reset the pixels to our count
            itemCount++;
            for (int p = 0; p < binaryImg.length; p++) {
                if (binaryImg[p] == currentLabel) {
                    binaryImg[p] = itemCount;
                }
            }
        }
    }

    private void createComponents() {
        for (int i = 0; i < itemCount; i++) {
            ConnectedComponent item = new ConnectedComponent();
            listOfItems.add(item);
            for (int p = 0; p < binaryImg.length; p++) {
                if (binaryImg[p] == i + 1) {
                    listOfItems.get(i).addPixel(p);
                }
            }
        }
    }

    private void colorImage() {
        //color our image based on label
        for (int i = 0; i < binaryImg.length; i++) {
            short pLabel = binaryImg[i];
            if (pLabel == 0) {
                obImage.pixels[i] = color(0, 0, 0);
            } else {
                obImage.pixels[i] = color(256 / pLabel, 128 / pLabel, 228);
            }
        }
    }

    private void showRectangleBoundary() {
        fill(0, 0);
        stroke(0, 255, 0);
        for (ConnectedComponent item : listOfItems) {
            item.getBounds(obImage.width);
            rect(item.getLeft(), item.getTop(), item.getWidth(), item.getHeight());
        }
    }

    public static void main(String[] args) {
        PApplet.main("componentdetect.ComponentDetect");
    }
}
