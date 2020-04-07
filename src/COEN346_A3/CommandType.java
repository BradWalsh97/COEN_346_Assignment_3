package COEN346_A3;

public enum CommandType {
    STORE("STORE"),
    LOOKUP("LOOKUP"),
    RELEASE("RELEASE");

    private String value;

    CommandType(final String value){
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
