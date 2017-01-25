package componentdetect;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 *
 * @author Andrew King
 */
public class EquivalenceTable {

    List<List<Short>> listOfLists = new ArrayList<List<Short>>();

    //what label number we are currently assigning
    short labelCount = 2;
    
    //creats a new list in the list and adds the variable to it
    public void createNewLabel(short newInt) {
        List<Short> myList = new ArrayList();
        myList.add(newInt);
        listOfLists.add(myList);
        listOfLists.size();
    }

    //find the row in the table to add to, then add it
    public void assignNewValue(short west, short north){
        int indexOfNorth = find(north);
        int indexOfWest = find(west);
        //cast to object so we can remove as object instead of it being treated as a removeat
        listOfLists.get(indexOfWest).remove((Object) west);
        listOfLists.get(indexOfNorth).add(west);
        //if list is now empty remove it
        if(listOfLists.get(indexOfWest).size() == 0){
            listOfLists.remove(indexOfWest);
        }
    }
    
    //find what row x is
    public int find(short x) {
        for (int i = 0; i < listOfLists.size(); i++) {
            if (listOfLists.get(i).contains(x)){
                return i;
            }
        }
        //could not find
        System.out.println("Error! We could not find " +x);
        return -2;
    }
    public short getLabelNumber(){
        return labelCount++;
    }


    short findLowest(short label) {
        int i = find(label);
        short min = Collections.min(listOfLists.get(i));
        return min;
    }

    int getNumOfObjects(){
        return listOfLists.size();
    }
    Short getLabel(int i){
        return listOfLists.get(i).get(0);
    }
}
