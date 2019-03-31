package org.team3128.common.hardware.limelight;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import org.team3128.common.util.RobotMath;

/**
 * Software wrapper to obtain data from and send data to the physical Limelight.
 * 
 * LIMELIGHT CONVENTIONS:
 *  - When the target is right of the vertical centerline, tx is positive.
 *  - When the target is above the horizontal centerline, ty is positive.
 * 
 * 
 * @author Adham Elarabawy, Mason Holst, Jude Lifset
 *
 */
public class Limelight {   
    public String hostname;
    public double cameraAngle;
    public double cameraHeight;
    public double targetWidth;

    public NetworkTable limelightTable;
    
    /**
     * 
     * @param cameraAngle - The vertical angle of the limelight
     * @param cameraHeight - The height off of the ground of the limelight
     * @param targetWidth - The width of the target
     */
    public Limelight(String hostname, double cameraAngle, double cameraHeight, double targetWidth) {
        this.hostname = hostname;

        this.cameraAngle = cameraAngle;
        this.cameraHeight = cameraHeight;
        this.targetWidth = targetWidth;

        limelightTable = NetworkTableInstance.getDefault().getTable(hostname);
    }

    /**
     * Gets the average value of the data value in a certain key output by the Limelight.
     * 
     * @param key - the LimelightKey corresponding to the desired value.
     * @param numSamples - how many samples of the value to average out.
     * @return
     */
    public double getValue(LimelightKey key, int numSamples) {
        double runningTotal = 0;
        int count = 0;

        while (count <= numSamples) {
            runningTotal += limelightTable.getEntry(key.getKey()).getDouble(0.0);
            count += 1;
        }

        return runningTotal / numSamples;
    }

    /**
     * Checks to see if the Limelight has a valid target
     */
    public boolean hasValidTarget() {
        return getValue(LimelightKey.VALID_TARGET, 1) > 0.99;
    }

    public LimelightData getValues(int numSamples) {
        LimelightData data = new LimelightData();
        double runningTotal;
        double[] camtranArray;
        int index = 0;
        for (String valueKey : LimelightConstants.valueKeys) {
            runningTotal = 0;
            for(int a = 0; a < numSamples; a++){
                runningTotal += limelightTable.getEntry(valueKey).getDouble(0.0);
            }

            data.set(valueKey, runningTotal / numSamples);
        }

        //load the camtranArray
        camtranArray = limelightTable.getEntry("camtran").getDoubleArray(new double[6]);
        //add each element in the array to the updated values for however many times numSamples dictates(minus 1 because the array is initially loaded in with values)
        for(int a = 0; a < numSamples - 1; a++){
            for(int b = 0; b < camtranArray.length; b++){
                camtranArray[b] += limelightTable.getEntry("camtran").getDoubleArray(new double[6])[b];
            }
        }
        for(String valueKey : LimelightConstants.valueKeysPnP) {
            data.set(valueKey, camtranArray[index] / numSamples);
            index++;
        }
        
        return data;
    }

    public double getYPrime(double targetHeight, int n) {
        if (!hasValidTarget()) return -1;

        return calculateYPrimeFromTY(getValue(LimelightKey.VERTICAL_OFFSET, n), targetHeight);
    }

    public double calculateYPrimeFromTY(double ty, double targetHeight) {
        return (targetHeight - cameraHeight) / RobotMath.tan(ty + cameraAngle);
    }

    public void setLEDMode(LEDMode mode) {
        limelightTable.getEntry("ledMode").setNumber(mode.getLEDMode());
    }

    public void setStreamMode(StreamMode mode) {
        limelightTable.getEntry("stream").setNumber(mode.getStream());
    }
}
