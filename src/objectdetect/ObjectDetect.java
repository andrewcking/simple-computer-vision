package objectdetect;

import java.util.ArrayList;
import java.util.Collections;
import processing.core.*;

/**
 *
 * @author Andrew King
 */
public class ObjectDetect extends PApplet {

    public static void main(String[] args) {
        PApplet.main("objectdetect.ObjectDetect");
    }
    //our original image
    PImage obImage;
    //binary image threshhold
    int threshold = 60;
    //list to keep track of checked items so we don't recheck
    ArrayList checked = new ArrayList();
    //list of lists - each list in the list contains the pixels from a connected component
    ArrayList<ArrayList<Integer>> listOfLists = new ArrayList<ArrayList<Integer>>();
    //this list is reused to store the object pixels
    ArrayList<Integer> objectList = new ArrayList<>();
    //array for our binary image
    byte[] binaryImg;
    //counter for the number of items
    int itemCount = 0;
    //arraylist for shape outlines
    ArrayList rects = new ArrayList();

    public void settings() {
        //set up window size
        size(250, 344);
    }

    public void setup() {
        //load our image (objects.jpg, bapple.jpg) and downsize it
        obImage = loadImage("bapple.jpg");
        obImage.resize(250, 0);
        //load the pixels from the image into the pixel array
        obImage.loadPixels();
        //create binary image array
        binaryImg = new byte[obImage.width * obImage.height];
        //CONVERT TO BINARY IMAGE
        //for each pixel
        for (int i = 0; i < obImage.pixels.length; i++) {
            //get its color and get the brightness of that color
            int pixelColor = obImage.pixels[i];
            float bness = brightness(pixelColor);
            //if the brightness is less than our threshold color the pixel black otherwise white
            if (bness < threshold) {
                binaryImg[i] = 0;
            } else {
                binaryImg[i] = 1;
            }
        }

        //scan left to right until you find a white, unchecked pixel
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 1 && !checked.contains(i)) {
                objectList.clear();
                detectObjects(i);
                listOfLists.add((ArrayList<Integer>) objectList.clone());
                itemCount++;
            }
        }
        //color background pixels black
        for (int i = 0; i < binaryImg.length; i++) {
            if (binaryImg[i] == 0) {
                obImage.pixels[i] = color(0, 0, 0);
            }
        }
        //color our objects different colors
        for (int i = 1; i <= listOfLists.size(); i++) {
            ArrayList<Integer> ob = listOfLists.get(i - 1);

            int left = Integer.MAX_VALUE;
            int right = 0;

            for (int x = 0; x < ob.size(); x++) {
                if (i % 2 == 0) {
                    obImage.pixels[(int) ob.get(x)] = color(255 / i, 0, 0);
                } else {
                    obImage.pixels[(int) ob.get(x)] = color(255 / i, 255 / i, 0);
                }
                //get far right and far left coord of object
                if (ob.get(x) % obImage.width < left) {
                    left = ob.get(x) % obImage.width;
                }
                if (ob.get(x) % obImage.width > right) {
                    right = ob.get(x) % obImage.width;
                }
            }
            int obWidth = right - left;
            int max = Collections.max(ob);
            int min = Collections.min(ob);
            int high2 = (min / obImage.width) - 1;
            int low2 = max / obImage.width + 1;
            height = low2 - high2;
            
            Rectangle myRect = new Rectangle(left, high2, obWidth, height);
            rects.add(myRect);

        }

        image(obImage, 0, 0);
        fill(0, 0);
        stroke(0, 255, 0);
        for (int i = 0; i < rects.size(); i++) {
            Rectangle myrect = (Rectangle) rects.get(i);
            rect(myrect.x,myrect.y,myrect.width,myrect.height);
        }
        println("Number of items: " + listOfLists.size());
    }

    //recursive 4-neighbor
    public void detectObjects(int i) {
        //catch out of bounds exceptions (if an object starts on an edge)
        if (i < 0) {
            return;
        } else if (binaryImg[i] == 0) {
            return;
        } else if (checked.contains(i)) {
            return;
        } else {
            checked.add(i);
            objectList.add(i);
            //detectObjects(i - obImage.width - 1);
            detectObjects(i - obImage.width);
            //detectObjects(i - obImage.width + 1);
            detectObjects(i - 1);
            detectObjects(i + 1);
            //detectObjects(i + obImage.width - 1);
            detectObjects(i + obImage.width);
            //detectObjects(i + obImage.width + 1);
        }
    }

    private static class Rectangle {

        public int x;
        public int y;
        public int width;
        public int height;

        public Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
