package de.blau.android.services.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.os.SystemClock;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.exception.UnsupportedFormatException;
import de.blau.android.util.ACRAHelper;

public final class Nmea {

    private static final String DEBUG_TAG = Nmea.class.getSimpleName().substring(0, Math.min(23, Nmea.class.getSimpleName().length()));

    private static final String NORTH = "N";
    private static final String EAST  = "E";

    private static final int HDOP_CUTOFF = 5;

    private static final String VTG_SENTENCE = "VTG";

    private static final int VTG_COURSE = 1;
    private static final int VTG_SPEED  = 7;

    private static final String GGA_SENTENCE = "GGA";

    private static final int GGA_FIX_TYPE         = 6;
    private static final int GGA_SAT_COUNT        = 7;
    private static final int GGA_HDOP             = 8;
    private static final int GGA_HEIGHT           = 9;
    private static final int GGA_GEOID_CORRECTION = 11;

    private static final int GGA_FIX_AUTONOMOUS   = 1;
    private static final int GGA_FIX_DIFFERENTIAL = 2;
    private static final int GGA_FIX_RTK_FIXED    = 4;
    private static final int GGA_FIX_RTK_FLOAT    = 5;

    // Based on mode, assume accuracy in meters, for HDOP of 1.
    // Further assume that accuracy scales with HDOP.
    private static final double GGA_FIX_AUTONOMOUS_MUL   = 10;
    private static final double GGA_FIX_DIFFERENTIAL_MUL = 5;
    private static final double GGA_FIX_RTK_FIXED_MUL    = 0.1;
    private static final double GGA_FIX_RTK_FLOAT_MUL    = 1;

    private static final String GNS_SENTENCE = "GNS";

    private static final int GNS_FIX_TYPE         = 6;
    private static final int GNS_SAT_COUNT        = 7;
    private static final int GNS_HDOP             = 8;
    private static final int GNS_HEIGHT           = 9;
    private static final int GNS_GEOID_CORRECTION = 10;

    private static final String GN = "GN"; // Multiple systems
    private static final String GL = "GL"; // GLONASS
    private static final String GP = "GP"; // GPS
    private static final String BD = "BD"; // unofficial BEIDOU
    private static final String GB = "GB"; // official BEIDOU
    private static final String QZ = "QZ"; // unofficial QZSS
    private static final String GQ = "GQ"; // official QZSS
    private static final String GI = "GI"; // NavIC
    private static final String GA = "GA"; // GALILEO

    private static final Map<String, Integer> TALKER_PRIORITY = new HashMap<>();
    static {
        TALKER_PRIORITY.put(GI, 0);
        TALKER_PRIORITY.put(QZ, 0);
        TALKER_PRIORITY.put(GQ, 0);
        TALKER_PRIORITY.put(BD, 1);
        TALKER_PRIORITY.put(GB, 1);
        TALKER_PRIORITY.put(GA, 2);
        TALKER_PRIORITY.put(GL, 3);
        TALKER_PRIORITY.put(GP, 4);
        TALKER_PRIORITY.put(GN, 5);
    }

    private static final Map<String, Integer> SENTENCE_PRIORITY = new HashMap<>();
    static {
        SENTENCE_PRIORITY.put(VTG_SENTENCE, 0);
        SENTENCE_PRIORITY.put(GGA_SENTENCE, 1);
        SENTENCE_PRIORITY.put(GNS_SENTENCE, 2);
    }

    private static int systemPriority   = -1;
    private static int sentencePriority = -1;

    /**
     * Private constructor to stop instantiation
     */
    private Nmea() {
        // Ignore
    }

    /**
     * Reset the NMEA processing
     */
    public static void reset() {
        systemPriority = -1;
        sentencePriority = -1;
    }

    /**
     * Minimal parsing of VTG, GGA and GNS NMEA 0183 sentences
     * 
     * Some filtering is applied to only use the best sources and only one sentence type
     * 
     * This method is not thread safe
     * 
     * @param sentence the NMEA sentence including checksum
     * @param nmeaLocation the ExtendedLocation to update and return
     * @return an ExtendedLocation object if we have a valid new fix otherwise null
     */
    @Nullable
    public static ExtendedLocation processSentence(@NonNull String sentence, @NonNull ExtendedLocation nmeaLocation) {
        boolean posUpdate = false;
        try {
            int length = sentence.length();
            if (length > 9) { // everything shorter is invalid
                int star = sentence.indexOf('*');
                if (star > 5 && length >= star + 3) {
                    String withoutChecksum = sentence.substring(1, star);
                    int receivedChecksum = Integer.parseInt(sentence.substring(star + 1, star + 3), 16);
                    if (receivedChecksum == checksum(withoutChecksum)) {
                        String talker = withoutChecksum.substring(0, 2);
                        Integer talkerPrio = TALKER_PRIORITY.get(talker);
                        if (talkerPrio == null) { // unsupported talker
                            return null;
                        }

                        String sentenceType = withoutChecksum.substring(2, 5);
                        Integer currentSentencePrio = SENTENCE_PRIORITY.get(sentenceType);
                        if (currentSentencePrio == null) { // unsupported sentence
                            return null;
                        }

                        double lat = Double.NaN;
                        double lon = Double.NaN;
                        double hdop = Double.NaN;
                        double fauxAccuracy = Double.NaN;
                        double geoidHeight = Double.NaN;
                        double geoidCorrection = Double.NaN;

                        switch (sentenceType) {
                        case GNS_SENTENCE:
                            String[] values = withoutChecksum.split(",", -12); // java magic
                            if (values.length == 13) {
                                String fixType = values[GNS_FIX_TYPE].toUpperCase(Locale.US);
                                // at least 4 satellites
                                if ((GN.equals(talker) && (!fixType.startsWith("NN")) || !"N".equals(fixType))
                                        && Integer.parseInt(values[GNS_SAT_COUNT]) >= 4) {
                                    // at least one "good" system needs a
                                    // fix
                                    lat = latFromNmea(values);
                                    lon = lonFromNmea(values);
                                    hdop = Double.parseDouble(values[GNS_HDOP]);
                                    geoidHeight = Double.parseDouble(values[GNS_HEIGHT]);
                                    geoidCorrection = Double.parseDouble(values[GNS_GEOID_CORRECTION]);
                                    posUpdate = true;
                                }
                            } else {
                                throw new UnsupportedFormatException(Integer.toString(values.length));
                            }
                            break;
                        case GGA_SENTENCE:
                            values = withoutChecksum.split(",", -14);
                            if (values.length == 15) {
                                // we need a fix
                                if (!"0".equals(values[GGA_FIX_TYPE]) && Integer.parseInt(values[GGA_SAT_COUNT]) >= 4) {
                                    lat = latFromNmea(values);
                                    lon = lonFromNmea(values);
                                    hdop = Double.parseDouble(values[GGA_HDOP]);
                                    geoidHeight = Double.parseDouble(values[GGA_HEIGHT]);
                                    geoidCorrection = Double.parseDouble(values[GGA_GEOID_CORRECTION]);
                                    // Extract fix type and create faux accuracy. The key idea is to
                                    // 1) give some sort of not-crazy accuracy in non-RTK mode
                                    // 2) give an accuracy in RTK which is arguably reasonable, and also a signal
                                    // to the user about fixed vs float vs not-RTK.
                                    int fixType = Integer.parseInt(values[GGA_FIX_TYPE]);
                                    switch (fixType) {
                                    case GGA_FIX_AUTONOMOUS:
                                        fauxAccuracy = GGA_FIX_AUTONOMOUS_MUL * hdop;
                                        break;
                                    case GGA_FIX_DIFFERENTIAL:
                                        fauxAccuracy = GGA_FIX_DIFFERENTIAL_MUL * hdop;
                                        break;
                                    case GGA_FIX_RTK_FIXED:
                                        fauxAccuracy = GGA_FIX_RTK_FIXED_MUL * hdop;
                                        break;
                                    case GGA_FIX_RTK_FLOAT:
                                        fauxAccuracy = GGA_FIX_RTK_FLOAT_MUL * hdop;
                                        break;
                                    default:
                                        // Should not happen. Indicate poor accuracy, without log spam.
                                        fauxAccuracy = 50.0;
                                        break;
                                    }
                                    posUpdate = true;
                                }
                            } else {
                                throw new UnsupportedFormatException(Integer.toString(values.length));
                            }
                            break;
                        case VTG_SENTENCE:
                            values = withoutChecksum.split(",", -11);
                            if (values.length == 12) {
                                if (!values[9].toUpperCase(Locale.US).startsWith("N")) {
                                    double course = Double.parseDouble(values[VTG_COURSE]);
                                    nmeaLocation.setBearing((float) course);
                                    double speed = Double.parseDouble(values[VTG_SPEED]);
                                    nmeaLocation.setSpeed((float) (speed / 3.6D));
                                }
                            } else {
                                throw new UnsupportedFormatException(Integer.toString(values.length));
                            }
                            break;
                        default:
                            // unsupported sentence, we should never get here
                            return null;
                        }

                        if (posUpdate) {
                            // hdop filtering
                            if (hdop > HDOP_CUTOFF) {
                                return null;
                            }
                            // system filtering
                            if (talkerPrio > systemPriority) {
                                systemPriority = talkerPrio;
                            } else if (talkerPrio < systemPriority) { // low prio talkers ignored
                                return null;
                            }
                            // sentence filtering
                            if (currentSentencePrio > sentencePriority) {
                                sentencePriority = currentSentencePrio;
                            } else if (currentSentencePrio < sentencePriority) { // low prio sentences ignored
                                return null;
                            }

                            nmeaLocation.setGeoidHeight(geoidHeight);
                            nmeaLocation.setLatitude(lat);
                            nmeaLocation.setLongitude(lon);
                            nmeaLocation.setHdop(hdop);
                            if (fauxAccuracy != Double.NaN) {
                                nmeaLocation.setAccuracy((float) fauxAccuracy);
                            }
                            nmeaLocation.setGeoidCorrection(geoidCorrection);
                            // we only really need to know this for determining how old the fix is
                            nmeaLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                            nmeaLocation.setTime(System.currentTimeMillis());
                            return nmeaLocation;
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            Log.d(DEBUG_TAG, "Invalid number format in " + sentence);
            return null;
        } catch (UnsupportedFormatException e) {
            Log.d(DEBUG_TAG, "Invalid number " + e.getMessage() + " of values in " + sentence);
            return null;
        } catch (Exception e) { // anything else
            Log.e(DEBUG_TAG, "NMEA sentence " + sentence + " caused exception " + e);
            ACRAHelper.nocrashReport(e, e.getMessage());
        }
        return null;
    }

    /**
     * Calculate the NMEA checksum from the sentence
     * 
     * @param withoutChecksum the sentence without the checksum
     * @return the checksum
     */
    static int checksum(@NonNull String withoutChecksum) {
        int checksum = 0;
        for (byte b : withoutChecksum.getBytes()) {
            checksum = checksum ^ b;
        }
        return checksum;
    }

    /**
     * Get the longitude from the NMEA values
     * 
     * @param values and array holding the values
     * @return the WGS84 longitude
     */
    static double lonFromNmea(@NonNull String[] values) {
        return nmeaLonToDecimal(values[4]) * (values[5].equalsIgnoreCase(EAST) ? 1 : -1);
    }

    /**
     * Get the latitude from the NMEA values
     * 
     * @param values and array holding the values
     * @return the WGS84 latitude
     */
    static double latFromNmea(@NonNull String[] values) {
        return nmeaLatToDecimal(values[2]) * (values[3].equalsIgnoreCase(NORTH) ? 1 : -1);
    }

    /**
     * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
     * 
     * @param nmea longitude value
     * @return the longitude
     * @throws NumberFormatException if the value can't be parsed
     */
    private static Double nmeaLonToDecimal(@NonNull String nmea) {
        int deg = Integer.parseInt(nmea.substring(0, 3));
        Double min = Double.parseDouble(nmea.substring(3));
        return deg + min / 60d;
    }

    /**
     * Convert from NMEA format to decimal (there is already a method in Location so this is not really necessary)
     * 
     * @param nmea latitude value
     * @return the latitude
     * @throws NumberFormatException if the value can't be parsed
     */
    private static Double nmeaLatToDecimal(@NonNull String nmea) {
        int deg = Integer.parseInt(nmea.substring(0, 2));
        Double min = Double.parseDouble(nmea.substring(2));
        return deg + min / 60d;
    }
}
