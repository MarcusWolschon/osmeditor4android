package de.blau.android.util.utils;

/**
 * We avoid including a full library like org.apache.commons:commons-lang3 to avoid an unnecessary
 * large number of methods, which is inconvenient to Android devs.
 *
 * @see <a href="https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/text/TextUtils.java">Some code came from this source.</a>
 * @since 1.0.0
 */
public class TextUtils {

  /**
   * Returns true if the string is null or 0-length.
   *
   * @param str the string to be examined
   * @return true if str is null or zero length
   * @since 1.0.0
   */
  public static boolean isEmpty(CharSequence str) {
    if (str == null || str.length() == 0) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a string containing the tokens joined by delimiters.
   *
   * @param delimiter the delimeter on which to split.
   * @param tokens    An array objects to be joined. Strings will be formed from the objects by
   *                  calling object.toString().
   * @return {@link String}
   * @since 1.0.0
   */
  public static String join(CharSequence delimiter, Object[] tokens) {
    StringBuilder sb = new StringBuilder();
    boolean firstTime = true;
    for (Object token : tokens) {
      if (firstTime) {
        firstTime = false;
      } else {
        sb.append(delimiter);
      }
      sb.append(token);
    }
    return sb.toString();
  }

}
