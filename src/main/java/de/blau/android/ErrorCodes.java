package de.blau.android;

public final class ErrorCodes {

    /**
     * Private constructor
     */
    private ErrorCodes() {
        // don't instantiate
    }

    public static final int OK = 0;

    public static final int NO_LOGIN_DATA           = 1;
    public static final int NO_CONNECTION           = 2;
    public static final int UPLOAD_PROBLEM          = 3;
    public static final int DATA_CONFLICT           = 4;
    public static final int BAD_REQUEST             = 5;
    public static final int API_OFFLINE             = 6;
    public static final int OUT_OF_MEMORY           = 7;
    public static final int OUT_OF_MEMORY_DIRTY     = 8;
    public static final int INVALID_DATA_RECEIVED   = 9;
    public static final int FILE_WRITE_FAILED       = 10;
    public static final int NAN                     = 11;
    public static final int INVALID_BOUNDING_BOX    = 12;
    public static final int SSL_HANDSHAKE           = 13;
    public static final int INVALID_DATA_READ       = 14;
    public static final int BOUNDING_BOX_TOO_LARGE  = 15;
    public static final int CORRUPTED_DATA          = 16;
    public static final int DOWNLOAD_LIMIT_EXCEEDED = 17;
    public static final int UPLOAD_LIMIT_EXCEEDED   = 18;
    public static final int UPLOAD_INCOMPLETE       = 19;

    public static final int UPLOAD_CONFLICT               = 50;
    public static final int INVALID_LOGIN                 = 51;
    public static final int FORBIDDEN                     = 52;
    public static final int NOT_FOUND                     = 53;
    public static final int UNKNOWN_ERROR                 = 54;
    public static final int NO_DATA                       = 55;
    public static final int REQUIRED_FEATURE_MISSING      = 56;
    public static final int APPLYING_OSC_FAILED           = 57;
    public static final int MISSING_API_KEY               = 58;
    public static final int DUPLICATE_TAG_KEY             = 59;
    public static final int ALREADY_DELETED               = 60;
    public static final int UPLOAD_BOUNDING_BOX_TOO_LARGE = 61;
    public static final int TOO_MANY_WAY_NODES            = 62;
    public static final int UPLOAD_WAY_NEEDS_ONE_NODE     = 63;
}
