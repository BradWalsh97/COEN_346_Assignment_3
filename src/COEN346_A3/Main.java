package COEN346_A3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import static java.lang.String.format;

//TODO: Error checking on inputs for commands, memoryconfig, processes

class sharedQueues {

    //These queues are accessible by the two CPU cores and contain the processes to be run
    static Queue<Process> waitingQueue = new LinkedList<>();
    static Queue<Process> readyQueue = new LinkedList<>();

    //This Queue contains the list of commands from commands.txt
    static Queue<Command> commandQueue = new LinkedList<>();

    //This value will hold the system time at start of execution
    static long start;

}

public class Main {

    //These are temporary arraylists for the purpose of sorting before transferring to a queue
    static ArrayList<Process> waitingList = new ArrayList<>();
    static ArrayList<Command> commandList = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        //A semaphore that only allows one core at a time to access certain areas
        Semaphore cpuLock = new Semaphore(1);

        //Start by getting the input file for processes
        File processesFile = new File(Paths.get("processes.txt").toAbsolutePath().toString());
        BufferedReader processesInput = new BufferedReader(new FileReader(processesFile));
        int processCount = 2; //starts at 2 since schedulers are Considered p0 and p1

        //Read data from file and populate the waitingQueue
        String[] splitInputArray;
        String processesString;

        //start by creating a process object for each item in the text file and
        //provide the info for that process
        processesInput.readLine(); //Skip the first line
        while ((processesString = processesInput.readLine()) != null) {
            splitInputArray = processesString.split("\\s", 2); //split arrival time and burst time
            waitingList.add(
                    new Process(processCount,
                            Integer.parseInt(splitInputArray[0].trim()),
                            Integer.parseInt(splitInputArray[1].trim())));

            processCount++;
        }

        //Take the commands.txt info and read it
        File commandsFile = new File(Paths.get("commands.txt").toAbsolutePath().toString());
        BufferedReader commandsInput = new BufferedReader(new FileReader(commandsFile));

        //To hold the commands during reading
        String[] commandsArray;
        String commandsString;

        while ((commandsString = commandsInput.readLine()) != null) {
            commandsArray = commandsString.split("\\s", 3); //Split the command name, value and variableID
            if (commandsArray.length < 3) { //If there are less than three strings, LOOKUP or RELEASE commands
                commandList.add(
                        new Command(
                                Integer.parseInt(commandsArray[1].trim()), //The variable the command is dealing with
                                commandsArray[0].trim())); //The type of command
            }
            else { //if there are 3 strings in the array (A STORE command)
                commandList.add(
                        new Command(
                                Integer.parseInt(commandsArray[1].trim()), //The variable the command is dealing with
                                Integer.parseInt(commandsArray[2].trim()), //The value the command is dealing with, if it exists
                                commandsArray[0].trim())); //The type of command
            }
        }

        Collections.sort(waitingList, new SortByArrival()); //Sort the temporary waiting list by arrival time (earliest first)

        //print all processes in waiting queue
        System.out.println("=========Processes=========");
        for (Process p :waitingList) {
            System.out.println("Process: " + p.getPID() + " Arrival Time: "+ p.getArrivalTime() + " Run Time: "+ p.getRunTime());
        }

        //begin process execution/simulation
        System.out.println("================Process Execution=================");


        //Add the sorted by arrival processes in Waiting List to the Waiting Queue
        for (int m = 0; m < waitingList.size(); m++){

            sharedQueues.waitingQueue.add(waitingList.get(m));

        }

        //Add the commands read from commands.txt to a queue for easier retrieval by processes
        for (int m = 0; m < commandList.size(); m++){

            sharedQueues.commandQueue.add(commandList.get(m));

        }

        //Create two instances of scheduler, these will act as our CPU cores cpu0 and cpu1
        Scheduler cpu0 = new Scheduler(new Process(0, 0, 0), cpuLock, "cpu0");
        Scheduler cpu1 = new Scheduler(new Process(1, 0, 0), cpuLock, "cpu1");

        //Set the start time of the simulation
        sharedQueues.start = System.nanoTime();

        //Start both cores
        cpu0.start();
        cpu1.start();

    }

    public static class Scheduler extends Thread{

        Semaphore sem;
        String name;

        public Scheduler(Process p, Semaphore sem, String name) {
            this.sem = sem;
            this.name = name;
        }

        private void oneRoundRobinRound(Process p){ //One round of execution

            if (p.getRunTime() <= 3) {

                if(p.getRunTime() == p.getTotalRunTime()){ //If this is the first time the process has run
                    p.setStatus(ProcessStatus.STARTED); //Start the process
                    printProcessStatus(p);
                }
                p.setStatus(ProcessStatus.RESUMED); //Resume the process
                printProcessStatus(p);
                executeProcessComplete(p);
                p.setStatus(ProcessStatus.FINISHED);
                printProcessStatus(p);

            } else {

                if(p.getRunTime() == p.getTotalRunTime()){ //If this is the first time the process has run
                    p.setStatus(ProcessStatus.STARTED); //Start the process
                    printProcessStatus(p);
                }
                p.setStatus(ProcessStatus.RESUMED); //Resume the process
                printProcessStatus(p);
                executeProcess(p);
                p.setStatus(ProcessStatus.PAUSED);
                printProcessStatus(p);
                sharedQueues.readyQueue.add(p);

            }
        }

        private void printProcessStatus(Process p){

            System.out.println(name + ": Clock: " + getCurrentTime() + ", Process "
                    + p.getPID() + ", " + p.getStatus()
                    + ", remaining time " + format("%.3f",p.getRunTime()));

        }

        private long getCurrentTime(){
            long seconds = (System.nanoTime() - sharedQueues.start) / 1000000;
            return seconds;

        }

        private long setCurrentTime(){

            sharedQueues.start = System.nanoTime() - (sharedQueues.waitingQueue.peek().getArrivalTime()*1000000000); //should give you arrival time
            //TODO: Add a multiplier value to speed up execution
            long seconds = (System.nanoTime() - sharedQueues.start) / 1000000;
            return seconds;

        }

        private void executeProcess(Process p){
            long executionStart = System.nanoTime();
            MemoryManager memManager = new MemoryManager();

            while (System.nanoTime() - executionStart < 3000000){

                Command command = sharedQueues.commandQueue.remove();
                if (command.getCommandType() == CommandType.STORE){
                    memManager.memStore(command.getCommandVariable(), command.getCommandValue());
                }
                else if (command.getCommandType() == CommandType.LOOKUP){
                    memManager.memLookup(command.getCommandVariable());
                }
                else if (command.getCommandType() == CommandType.RELEASE){
                    memManager.memFree(command.getCommandVariable());
                }
                sharedQueues.commandQueue.add(command);

            }

            p.setRunTime(p.getRunTime()-3);

        }

        private void executeProcessComplete(Process p){
//            long executionStart = System.nanoTime();
//            MemoryManager memManager = new MemoryManager();
//
//            while (executionStart - sharedQueues.start < (p.getRunTime() * 1000000000)){
//
//                Command command = sharedQueues.commandQueue.remove();
//                if (command.getCommandType() == CommandType.STORE){
//                    memManager.memStore(command.getCommandVariable(), command.getCommandValue());
//                }
//                else if (command.getCommandType() == CommandType.LOOKUP){
//                    memManager.memLookup(command.getCommandVariable());
//                }
//                else if (command.getCommandType() == CommandType.RELEASE){
//                    memManager.memFree(command.getCommandVariable());
//                }
//                sharedQueues.commandQueue.add(command);
//
//            }
//
//            p.setRunTime(0);



        }

        @Override
        public void run() {


            System.out.println(setCurrentTime());

            while (true) {

                if (!sharedQueues.waitingQueue.isEmpty() && sharedQueues.waitingQueue.peek().getArrivalTime() <= (getCurrentTime()/1000)) {//If there are ready processes in the arrival/waiting queue, add them to the ready queue

                    Process p = new Process(0,0,0);

                    try {
                        sem.acquire();
                        if (sharedQueues.waitingQueue.peek().getArrivalTime() > getCurrentTime()/1000){}
                        else {
                            p = sharedQueues.waitingQueue.remove();
                            sharedQueues.readyQueue.add(p);
                        }

                    } catch (InterruptedException ex) {
                        System.out.println(ex);
                    }
                    sem.release();

                } else if (!sharedQueues.readyQueue.isEmpty()) {

                    Process p = new Process(0,0,0);

                    try {
                        sem.acquire();
                        p = sharedQueues.readyQueue.remove();
                    } catch (InterruptedException ex) {
                        System.out.println(ex);
                    }
                    sem.release();
                    oneRoundRobinRound(p);

                } else if(sharedQueues.waitingQueue.isEmpty() && sharedQueues.readyQueue.isEmpty()) {
                    System.out.println(name + " No more processes.");
                    break;
                } else {
                    //Keep looping
                }
            }//end round robin
        }
    }
}
