package COEN346_A3;

import java.util.Comparator;

class SortByArrival implements Comparator<Process> {

    public int compare(Process a, Process b){
        double aDur = a.getArrivalTime();
        double bDur = b.getArrivalTime();
        return Double.compare(aDur, bDur);
    }
}

