package org.qi4j.entitystore.gigaspaces.processor;

import org.qi4j.entitystore.gigaspaces.common.Data;

import org.openspaces.events.adapter.SpaceDataEvent;


/**
 * The processor simulates work done no un-processed Data object. The processData
 * accepts a Data object, simulate work by sleeping, and then sets the processed
 * flag to true and returns the processed Data.
 */
public class Processor {

    private long workDuration = 100;

    /**
     * Sets the simulated work duration (in milliseconds). Default to 100.
     */
    public void setWorkDuration(long workDuration) {
        this.workDuration = workDuration;
    }

    /**
     * Process the given Data object and returning the processed Data.
     *
     * Can be invoked using OpenSpaces Events when a matching event
     * occurs.
     */
    @SpaceDataEvent
    public Data processData(Data data) {
        // sleep to simulate some work
        try {
            Thread.sleep(workDuration);
        } catch (InterruptedException e) {
            // do nothing
        }
        data.setProcessed(true);
        data.setData("PROCESSED : " + data.getRawData());
        System.out.println(" ------ PROCESSED : " + data);
        return data;
    }

}
