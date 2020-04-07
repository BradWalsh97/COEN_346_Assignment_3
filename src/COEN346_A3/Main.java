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

    static Queue<Process> waitingQueue = new LinkedList<>();
    static Queue<Process> readyQueue = new LinkedList<>();
    static Queue<Command> commandQueue = new LinkedList<>();
    static long start;

}

public class Main {

    static ArrayList<Process> waitingList = new ArrayList<>();
    static ArrayList<Command> commandList = new ArrayList<>();

    public static void main(String[] args) throws IOException {

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

        File commandsFile = new File(Paths.get("commands.txt").toAbsolutePath().toString());
        BufferedReader commandsInput = new BufferedReader(new FileReader(commandsFile));

        String[] commandsArray;
        String commandsString;

        while ((commandsString = commandsInput.readLine()) != null) {
            commandsArray = commandsString.split("\\s", 3); //split arrival time and burst time
            if (commandsArray.length < 3) {
                commandList.add(
                        new Command(
                                Integer.parseInt(commandsArray[1].trim()), //The variable the command is dealing with
                                commandsArray[0].trim())); //The type of command
            }
            else {
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

        for (int m = 0; m < waitingList.size(); m++){

            sharedQueues.waitingQueue.add(waitingList.get(m));

        }

        for (int m = 0; m < commandList.size(); m++){

            sharedQueues.commandQueue.add(commandList.get(m));

        }



        Scheduler cpu0 = new Scheduler(new Process(0, 0, 0), cpuLock, "cpu0");
        Scheduler cpu1 = new Scheduler(new Process(1, 0, 0), cpuLock, "cpu1");
        sharedQueues.start = System.nanoTime();
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
        private void oneRoundRobinRound(Process p){


            if (p.getRunTime() <= 3) {

                if(p.getRunTime() == p.getTotalRunTime()){
                    p.setStatus(ProcessStatus.STARTED); //resume the process
                    printProcessStatus(p);
                }
                p.setStatus(ProcessStatus.RESUMED); //resume the process
                printProcessStatus(p);
                executeProcessComplete(p);
                p.setStatus(ProcessStatus.FINISHED);
                printProcessStatus(p);

            } else {

                if(p.getRunTime() == p.getTotalRunTime()){
                    p.setStatus(ProcessStatus.STARTED); //resume the process
                    printProcessStatus(p);
                }
                p.setStatus(ProcessStatus.RESUMED); //resume the process
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

            while (executionStart - sharedQueues.start < 3000000){

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
            long executionStart = System.nanoTime();
            MemoryManager memManager = new MemoryManager();

            while (executionStart - sharedQueues.start < (p.getRunTime() * 1000000000)){

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

            p.setRunTime(0);

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
