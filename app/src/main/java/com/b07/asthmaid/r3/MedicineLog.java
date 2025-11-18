package com.b07.asthmaid.r3;

import java.util.ArrayList;

public class MedicineLog {
    ArrayList<ControllerLogEntry> controllerLogs;
    ArrayList<RescueLogEntry> rescueLogs;

    public MedicineLog(){
        this.controllerLogs = new ArrayList<ControllerLogEntry>();
        this.rescueLogs = new ArrayList<RescueLogEntry>();
    }

    public MedicineLog(ArrayList<ControllerLogEntry> controllerLogs,
                           ArrayList<RescueLogEntry> rescueLogs){
        this.controllerLogs = controllerLogs;
        this.rescueLogs = rescueLogs;
    }

    public ArrayList<ControllerLogEntry> getControllerLogs() {
        return controllerLogs;
    }

    public ArrayList<RescueLogEntry> getRescueLogs() {
        return rescueLogs;
    }

    protected void addEntry(ControllerLogEntry entry) {
        controllerLogs.add(entry);
    }

    protected void addEntry(RescueLogEntry entry) {
        rescueLogs.add(entry);
    }

    protected boolean entryExists(ControllerLogEntry entry) {
        return controllerLogs.contains(entry);
    }

    protected boolean entryExists(RescueLogEntry entry) {
        return rescueLogs.contains(entry);
    }

    protected void removeEntry(ControllerLogEntry entry){

        if (controllerLogs.contains(entry)){
            controllerLogs.remove(entry);
        }
        else{
            System.out.println("Entry does not exist.");
        }
    }

    protected void removeEntry(RescueLogEntry entry){

        if (rescueLogs.contains(entry)){
            rescueLogs.remove(entry);
        }
        else{
            System.out.println("Entry does not exist.");
        }
    }
}
