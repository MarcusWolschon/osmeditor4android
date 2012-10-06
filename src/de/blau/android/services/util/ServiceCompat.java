package de.blau.android.services.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.acra.ACRA;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;

/**
 * Largely copied from:
 * http://developer.android.com/reference/android/app/Service.html#startForeground(int, android.app.Notification)
 * @author Andrew Gregory
 *
 */
public class ServiceCompat {
	private static final Class<?>[] mSetForegroundSignature = new Class[] {boolean.class};
	private static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};
	
	private Service mService;
	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	
	private void invokeMethod(Method method, Object[] args) {
		try {
			method.invoke(mService, args);
		} catch (InvocationTargetException e) {
			ACRA.getErrorReporter().handleException(e);
		} catch (IllegalAccessException e) {
			ACRA.getErrorReporter().handleException(e);
		}
	}
	
	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForeground(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}
		
		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
		mNM.notify(id, notification);
	}
	
	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	public void stopForeground(int id) {
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			invokeMethod(mStopForeground, mStopForegroundArgs);
			return;
		}
		
		// Fall back on the old API.  Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
		mNM.cancel(id);
		mSetForegroundArgs[0] = Boolean.FALSE;
		invokeMethod(mSetForeground, mSetForegroundArgs);
	}
	
	public ServiceCompat(Service service) {
		mService = service;
		mNM = (NotificationManager)service.getSystemService(Context.NOTIFICATION_SERVICE);
		try {
			mStartForeground = service.getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = service.getClass().getMethod("stopForeground", mStopForegroundSignature);
			return;
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
		try {
			mSetForeground = service.getClass().getMethod("setForeground", mSetForegroundSignature);
		} catch (NoSuchMethodException e) {
			ACRA.getErrorReporter().handleException(new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!"));
		}
	}
	
	public void destroy() {
		mService = null;
	}
}
