package COEN346_A3;

public enum ProcessStatus{
    STARTED("Started"),
    RESUMED("Resumed"),
    PAUSED("Paused"),
    FINISHED("Finished"),
    WAITING("Waiting"),
    READY("Ready");

    private String value;

    ProcessStatus(final String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }

    @Override
    public String toString(){
        return this.value;
    }
}