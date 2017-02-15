package objectdetection;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * @author Andrew King
 *         Simple Data Structure for building an equivalence table for use in sequential connected component algorithm
 */
public class EquivalenceTable {

    List<List<Short>> listOfLists = new ArrayList<List<Short>>();

    //what label number we are currently assigning
    public short labelCount = 2;

    //creates a new list in the list and adds the variable to it
    public void createNewLabel(short newInt) {
        List<Short> myList = new ArrayList();
        myList.add(newInt);
        listOfLists.add(myList);
    }


    public void assignNewValue(short west, short north) {
        int indexOfNorth = find(north);
        int indexOfWest = find(west);
        //if they arent in the same list then add all items from one list and then remove it (since if one of them are equiv then they all are)
        if (indexOfNorth != indexOfWest) {
            listOfLists.get(indexOfNorth).addAll(listOfLists.get(indexOfWest));
            listOfLists.remove(indexOfWest);
        }
    }

    //find what row x is
    public int find(short x) {
        for (int i = 0; i < listOfLists.size(); i++) {
            if (listOfLists.get(i).contains(x)) {
                return i;
            }
        }
        //could not find
        System.out.println("Error! We could not find " + x);
        return -2;
    }

    public short findLowest(short label) {
        int i = find(label);
        short min = Collections.min(listOfLists.get(i));
        return min;
    }

    //because we assigned the min we have to return the min when we condense
    public Short getLabel(int i) { return Collections.min(listOfLists.get(i)); }

    public int getNumOfObjects() { return listOfLists.size(); }

    public short getLabelNumber() { return labelCount++; }
}
