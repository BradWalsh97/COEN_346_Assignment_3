package COEN346_A3;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MemoryManager{
    private int size; //size of the "memory module"
    private int usedMemory; //amount of free memory in RAM. If full, then must write to swap(VM)
    private ArrayList<Frame> RAM = new ArrayList();
    Semaphore fileSemaphore = new Semaphore(1); //binary semaphore for file access
    Semaphore ramSemaphore = new Semaphore(1); //binary semaphore for ram access
    //todo: actually semaphore some shit
    MemoryManager(int memorySize){
        this.size = memorySize;
        for (int i = 0; i < this.size; i++) {
            RAM.add(new Frame(-1, -1));
        }
    }

    public void memStore(int variableID, int value) throws IOException, InterruptedException {
        //first we check if the RAM is full
        if(usedMemory == size) {//ram is full
            System.out.println("RAM is full!"); //todo: remove this, used for debugging
            //moveOldestFrameToVirtualMemory(variableID, value);

            //if the ram is full we can just open the vm.txt and append the new variable to the end
            fileSemaphore.acquire();
            File file = new File(Paths.get("vm.txt").toAbsolutePath().toString());
            Writer output = new BufferedWriter(new FileWriter(file, true));
            output.append(variableID + " " + value + " " + System.currentTimeMillis());
            output.close();
            fileSemaphore.release();
        }
        else { //there is space in the ram
            usedMemory++;
            //we have to find where we can put the new variable
            //if the id is -1 then we can add it there as a id of -1 means its empty/free
            ramSemaphore.acquire();
            for (int i = 0; i < RAM.size(); i ++) {
                if(RAM.get(i).getID() == -1) {//if that point is free
                    RAM.get(i).setID(variableID);
                    RAM.get(i).setValue(value);
                    RAM.get(i).setAccessTime(System.currentTimeMillis());
                    break; //todo: make sure this actually skips the rest of the loop (maybe use break)
                }
            }
            ramSemaphore.release();
        }


    }
    private void moveOldestFrameToVirtualMemory(int varID, int value) throws InterruptedException, IOException {
        //start by  finding the value with the lowest access time
        //the lowest access time will be the variable who's been in the ram the longest and thus can be placed
        //into the disk
        long tmpSmallestAccessTime;
        int indexToSwap = 0;
        ArrayList<Frame> virtualMemory = new ArrayList<>();
        Frame tmpFrame = new Frame(-1, 0); //assign -1 as a temporary/invalid ID
        ramSemaphore.acquire(); //stop others from reading and writing to the file
        tmpSmallestAccessTime = RAM.get(0).getAccessTime();
        for (int i = 1; i < RAM.size(); i++) {
            if(RAM.get(i).getAccessTime() < tmpSmallestAccessTime){
                tmpFrame = RAM.get(i);
                indexToSwap = i;
                tmpSmallestAccessTime = RAM.get(i).getAccessTime();
            }
        }
        ramSemaphore.release();

        //now that we've gone through the entire RAM, we know the ID of what we need to swap and we've
        //stored it as a tmp frame. Now we can add the new variable to ram. Then we write the old one to vm.txt

        //Start by getting the input file and creating an array consisting of all the vm elements
        fileSemaphore.acquire();
        File file = new File(Paths.get("vm.txt").toAbsolutePath().toString());
        /*BufferedReader in = new BufferedReader(new FileReader(file));
        //Read data from file and populate the virtual memory
        String[] splitInputArray;
        String str;

        //load all pages in vm.txt into the virtualMemory arraylist
        while ((str = in.readLine()) != null) {
            splitInputArray = str.split("\\s", 3); //split variable ID, value, and access time
            virtualMemory.add(new Frame(Integer.parseInt(splitInputArray[0].trim()),
                    Integer.parseInt(splitInputArray[1].trim()),
                    Integer.parseInt(splitInputArray[2].trim())));
        }

        //now we add the frame we're adding to VM to the array list
        virtualMemory.add(tmpFrame);

        //finally, we store the virtual memory into the file
        */
        Writer output = new BufferedWriter(new FileWriter(file, true));
        output.append(tmpFrame.ID + " " + tmpFrame.value + " " + tmpFrame.accessTime);
        output.close();
        fileSemaphore.release();

        //now that the old variable is in the virtual memory, we can write the new variable to ram
        System.out.println("SWAP Variable " + varID + " with Variable " + tmpFrame.getID()); //todo: make sure these are the right values
        RAM.get(indexToSwap).setID(varID);
        RAM.get(indexToSwap).setValue(value);
        RAM.get(indexToSwap).setAccessTime(System.currentTimeMillis());

//        for (Frame f: RAM) {
//            if(f.getAccessTime() < tmpSmallestAccessTime){
//                tmpFrame = f;
//                tmpSmallestAccessTime = f.getAccessTime();
//            }
//        }
    }
    public void memFree(int variableID) throws IOException, InterruptedException {
        boolean erased = false;
        //first we're going to look through the ram and see if the variable exists
        ramSemaphore.acquire();
        for (int i = 0; i < RAM.size(); i++) {
            if(RAM.get(i).getID() == variableID){ //if tis there, erase (assign values of -1) the variable
                RAM.get(i).setValue(-1);
                RAM.get(i).setID(-1);
                erased = true;
            }
        }
        ramSemaphore.release();
        //if not in ram
        if (!erased){
            //now we have to check the disk
            fileSemaphore.acquire();
            File file = new File(Paths.get("vm.txt").toAbsolutePath().toString());
            BufferedReader in = new BufferedReader(new FileReader(file));
            ArrayList<Frame> virtualMemory = new ArrayList<>();
            String[] splitInputArray;
            String str;
            //load all pages in vm.txt into the virtualMemory arraylist
            while ((str = in.readLine()) != null) {
                splitInputArray = str.split("\\s", 3); //split variable ID, value, and access time
                virtualMemory.add(new Frame(Integer.parseInt(splitInputArray[0].trim()),
                        Integer.parseInt(splitInputArray[1].trim()),
                        Integer.parseInt(splitInputArray[2].trim())));
            }
            in.close();
            //look through the virtual memory and remove the necessary one
            for (int i = 0; i < virtualMemory.size(); i++) {
                if(virtualMemory.get(i).getID() == variableID){
                    virtualMemory.remove(i);
                    break;
                }
            }

            //write the changes to the file
            Writer output = new BufferedWriter(new FileWriter("vm.txt"));
            for (Frame f : RAM) {
                output.append(f.ID + " " + f.value + " " + f.accessTime);
            }
            output.close();
            fileSemaphore.release();

        }
        //if not in disk either
        if(!erased){
            System.out.println("Value could not be found");//todo: Change this to the correct output
        }
    }
    public int memLookup(int variableID) throws IOException, InterruptedException {
        //first we look through the ram
        for (Frame f : RAM) {
            if(f.getID() == variableID)
                return f.getValue();
        }
        //if its not in the ram we must look for it in the vm.txt
        fileSemaphore.acquire();
        File file = new File(Paths.get("vm.txt").toAbsolutePath().toString());
        BufferedReader in = new BufferedReader(new FileReader(file));
        ArrayList<Frame> virtualMemory = new ArrayList<>();
        String[] splitInputArray;
        String str;
        while ((str = in.readLine()) != null) {
            splitInputArray = str.split("\\s", 3); //split variable ID, value, and access time
            virtualMemory.add(new Frame(Integer.parseInt(splitInputArray[0].trim()),
                    Integer.parseInt(splitInputArray[1].trim()),
                    Integer.parseInt(splitInputArray[2].trim())));
        }
        in.close();

        for (Frame f: virtualMemory) {
            if(f.getID() == variableID) {
                moveOldestFrameToVirtualMemory(variableID, f.getValue());
                return f.getValue();
            }
        }
        fileSemaphore.release(); //release it down here because we dont want the contents of vm.txt to change while without the array list changing

        //finally, if its not in ram or vm we return -1;
        return -1;


    }
}


class Frame{
    int ID;
    int value;
    long accessTime;
    Frame(int id, int value){
        this.ID = id;
        this.value = value;
        this.accessTime = System.currentTimeMillis(); //todo: does this make sense
    }
    Frame(int id, int value, int accessTime){
        this.ID = id;
        this.value = value;
        this.accessTime = accessTime;
    }
    public int getID() {
        return ID;
    }
    public void setID(int ID) {
        this.ID = ID;
    }
    public int getValue() {
        return value;
    }
    public void setValue(int value) {
        this.value = value;
    }
    public long getAccessTime() {
        return accessTime;
    }
    public void setAccessTime(long accessTime) {
        this.accessTime = accessTime;
    }
}
