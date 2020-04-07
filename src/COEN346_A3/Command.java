package COEN346_A3;

public class Command {

    private int commandValue;
    private int commandVariable;
    private CommandType commandType;

    public Command( int variable, int value, String type) {
        this.commandValue = value;
        this.commandVariable = variable;
        if (type.equals("Store")) {
            this.commandType = CommandType.STORE;
        } else if (type.equals("Lookup")) {
            this.commandType = CommandType.LOOKUP;
        } else if (type.equals("Release")) {
            this.commandType = CommandType.RELEASE;
        }
    }

    public Command( int variable, String type) {
        this.commandVariable = variable;
        if (type.equals("Store")) {
            this.commandType = CommandType.STORE;
        } else if (type.equals("Lookup")) {
            this.commandType = CommandType.LOOKUP;
        } else if (type.equals("Release")) {
            this.commandType = CommandType.RELEASE;
        }
    }

    public int getCommandValue() {
        return commandValue;
    }

    public void setCommandValue(int commandValue) {
        this.commandValue = commandValue;
    }

    public int getCommandVariable() {
        return commandVariable;
    }

    public void setCommandVariable(int commandVariable) {
        this.commandVariable = commandVariable;
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }
}
