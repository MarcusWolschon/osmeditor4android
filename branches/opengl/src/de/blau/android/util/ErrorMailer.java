package de.blau.android.util;

import java.util.List;

import android.content.Intent;
import android.content.res.Resources;
import de.blau.android.R;

/**
 * Convenience class for mailing error reports.
 * 
 * @author mb
 */
public class ErrorMailer {

	private static final String MIME_TYPE = "message/rfc822";

	private static final String ERROR_EMAIL_ADDRESS = "mattelacchiato@googlemail.com";

	/**
	 * Sends an email to me with the given exceptions
	 * 
	 * @param exceptions
	 * @param resources {@link Resources} for getting some Strings.
	 * @return Intent ready to start.
	 */
	public static Intent send(final List<Exception> exceptions, final Resources resources) {
		String[] addressee = { ERROR_EMAIL_ADDRESS };
		String subject = resources.getString(R.string.app_name_version) + " Errorreport";
		String message = resources.getString(R.string.error_email_message);
		for (Exception e : exceptions) {
			message += e.toString() + "\n";
		}

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, addressee);
		intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		intent.putExtra(Intent.EXTRA_TEXT, message);
		intent.setType(MIME_TYPE);
		return Intent.createChooser(intent, resources.getString(R.string.error_email_chooser_title));
	}

}
