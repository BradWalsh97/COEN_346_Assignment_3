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

class sharedQueues {

    static Queue<Process> waitingQueue = new LinkedList<>();
    static Queue<Process> readyQueue = new LinkedList<>();
    static long start;

}

public class Main {

    static ArrayList<Process> waitingList = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        Semaphore cpuLock = new Semaphore(1);

        //Start by getting the input file and creating an array consisting of all the light bulbs
        File file = new File(Paths.get("input.txt").toAbsolutePath().toString());
        BufferedReader in = new BufferedReader(new FileReader(file));
        int processCount = 1; //starts at 1 since scheduler is Considered p0

        //Read data from file and populate the waitingQueue
        String[] splitInputArray;
        String str;

        //start by creating a process object for each item in the text file and
        //provide the info for that process
        while ((str = in.readLine()) != null) {
            splitInputArray = str.split("\\s", 2); //split arrival time and burst time
            waitingList.add(
                    new Process(processCount,
                            Integer.parseInt(splitInputArray[0].trim()),
                            Integer.parseInt(splitInputArray[1].trim())));

            processCount++;
        }

        Collections.sort(waitingList, new SortByArrival());

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

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            p.setRunTime(p.getRunTime()-3);

        }

        private void executeProcessComplete(Process p){

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            p.setRunTime(0);

        }





        @Override
        public void run() {


            System.out.println(setCurrentTime());

            while (true) {

//                System.out.println(name + " Arrival time of first process = " + Shared.waitingQueue.peek().getArrivalTime());
//                System.out.println(name + " Current time / 1000 = " + getCurrentTime()/1000);

                if (!sharedQueues.waitingQueue.isEmpty() && sharedQueues.waitingQueue.peek().getArrivalTime() <= (getCurrentTime()/1000)) {//If there are ready processes in the arrival/waiting queue, add them to the ready queue

//                    System.out.println(name + " " + Shared.waitingQueue.peek().getArrivalTime() + "<=" + (getCurrentTime()/1000));

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
