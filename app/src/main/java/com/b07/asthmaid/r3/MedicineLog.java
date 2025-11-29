package com.b07.asthmaid.r3;

import java.util.ArrayList;

public class MedicineLog {
    ArrayList<ControllerLogEntry> controllerLogs;
    ArrayList<RescueLogEntry> rescueLogs;

    public MedicineLog(){
        this.controllerLogs = new ArrayList<ControllerLogEntry>();
        this.rescueLogs = new ArrayList<RescueLogEntry>();
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

    public void removeEntry(MedicineLogEntry entry) {
        if (entry instanceof ControllerLogEntry) {
            removeControllerEntry((ControllerLogEntry) entry);
        }
        else if (entry instanceof RescueLogEntry) {
            removeRescueEntry((RescueLogEntry) entry);
        }
    }
    protected void removeControllerEntry(ControllerLogEntry entry){

        if (controllerLogs.contains(entry)){
            controllerLogs.remove(entry);
        }
        else{
            System.out.println("Entry does not exist.");
        }
    }

    protected void removeRescueEntry(RescueLogEntry entry){

        if (rescueLogs.contains(entry)){
            rescueLogs.remove(entry);
        }
        else{
            System.out.println("Entry does not exist.");
        }
    }

    public void clear() {
        controllerLogs.clear();
        rescueLogs.clear();
    }
}
