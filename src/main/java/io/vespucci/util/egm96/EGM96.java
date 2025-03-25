/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the National Aeronautics and Space
 * Administration. All Rights Reserved.
 */

package io.vespucci.util.egm96;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import androidx.annotation.NonNull;

/**
 * 
 * Computes EGM96 geoid offsets.
 * <p>
 * A file with the offset grid must be passed to the constructor. This file must have 721 rows of 1440 2-byte integer
 * values. Each row corresponding to a latitude, with the first row corresponding to +90 degrees (90 North). The integer
 * values must be in centimeters.
 * <p>
 * Once constructed, the instance can be passed to
 * {@link gov.nasa.worldwind.globes.EllipsoidalGlobe#applyEGMA96Offsets(String)} to apply the offsets to elevations
 * produced by the globe.
 *
 * @author tag
 * @version $Id: EGM96.java 770 2012-09-13 02:48:23Z tgaskins $
 */
public class EGM96 {
    private ShortBuffer deltas;

    /**
     * Construct a new instance using a file with the EGM96 data specified by a file system path
     * 
     * @param path the path to the file containing the EGM96 data
     * @throws IOException if the file can't be found or read
     */
    public EGM96(@NonNull String path) throws IOException {
        loadOffsetFile(path);
    }

    /**
     * Load the offset file give its path
     * 
     * @param path the path
     * @throws IOException if the file can't be read
     */
    private void loadOffsetFile(@NonNull String path) throws IOException {
        File egm = new File(path);
        int length = (int) egm.length(); // the file is 2MB large
        byte[] temp = new byte[length];
        try (InputStream is = new FileInputStream(path)) {
            int read = 0;
            int available = is.available();
            while (available > 0) {
                read += is.read(temp, read, available);
                available = is.available();
            }
            if (read != length) {
                throw new IOException("File " + path + " not completely read");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(temp);
            deltas = ((ByteBuffer) (byteBuffer.rewind())).asShortBuffer(); // NOSONAR not compilable without cast
        }
    }

    // Description of the EGMA96 offsets file:
    // See http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/binary/binarygeoid.html
    // The total size of the file is 2,076,480 bytes. This file was created
    // using an INTEGER*2 data type format and is an unformatted direct access
    // file. The data on the file is arranged in records from north to south.
    // There are 721 records on the file starting with record 1 at 90 N. The
    // last record on the file is at latitude 90 S. For each record, there
    // are 1,440 15 arc-minute geoid heights arranged by longitude from west to
    // east starting at the Prime Meridian (0 E) and ending 15 arc-minutes west
    // of the Prime Meridian (359.75 E). On file, the geoid heights are in units
    // of centimeters. While retrieving the Integer*2 values on file, divide by
    // 100 and this will produce a geoid height in meters.

    private static final double INTERVAL = 15d / 60d; // 15' angle delta
    private static final int    NUM_ROWS = 721;
    private static final int    NUM_COLS = 1440;

    /**
     * Get the offset
     * 
     * @param lat WGS84 latitude
     * @param lon WGS84 longitude
     * @return the offset in meters
     */
    public double getOffset(double lat, double lon) {

        // Return 0 for all offsets if the file failed to load. A log message of the failure will have been generated
        // by the load method.
        if (this.deltas == null) {
            return 0;
        }

        lon = lon >= 0 ? lon : lon + 360;

        int topRow = (int) ((90 - lat) / INTERVAL);
        if (lat <= -90) {
            topRow = NUM_ROWS - 2;
        }
        int bottomRow = topRow + 1;

        // Note that the number of columns does not repeat the column at 0 longitude, so we must force the right
        // column to 0 for any longitude that's less than one interval from 360, and force the left column to the
        // last column of the grid.
        int leftCol = (int) (lon / INTERVAL);
        int rightCol = leftCol + 1;
        if (lon >= 360 - INTERVAL) {
            leftCol = NUM_COLS - 1;
            rightCol = 0;
        }

        double latBottom = 90 - bottomRow * INTERVAL;
        double lonLeft = leftCol * INTERVAL;

        try {
            double ul = this.gePostOffset(topRow, leftCol);
            double ll = this.gePostOffset(bottomRow, leftCol);
            double lr = this.gePostOffset(bottomRow, rightCol);
            double ur = this.gePostOffset(topRow, rightCol);

            double u = (lon - lonLeft) / INTERVAL;
            double v = (lat - latBottom) / INTERVAL;

            double pll = (1.0 - u) * (1.0 - v);
            double plr = u * (1.0 - v);
            double pur = u * v;
            double pul = (1.0 - u) * v;

            double offset = pll * ll + plr * lr + pur * ur + pul * ul;

            return offset / 100d; // convert centimeters to meters
        } catch (IllegalArgumentException iaex) {
            return 0;
        }
    }

    /**
     * Retrieve offset from deltas assuming that they are arranged in 15' grid
     * 
     * @param row the row
     * @param col the column
     * @return the offset in cm
     */
    private double gePostOffset(int row, int col) {
        int k = row * NUM_COLS + col;
        if (k >= this.deltas.limit()) {
            throw new IllegalArgumentException("row " + row + " col " + col + " out of range");
        }
        return this.deltas.get(k);
    }
}
