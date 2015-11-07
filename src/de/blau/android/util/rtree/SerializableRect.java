package de.blau.android.util.rtree;

import java.io.IOException;
import java.io.Serializable;

import android.graphics.Rect;

public class SerializableRect implements Serializable {

	private static final long serialVersionUID = 1L;

	private Rect mRect;

	public SerializableRect(Rect rect) {
		mRect = rect;
	}

	public Rect getRect() {
		return mRect;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		int left = mRect.left;
		int top = mRect.top;
		int right = mRect.right;
		int bottom = mRect.bottom;

		out.writeInt(left);
		out.writeInt(top);
		out.writeInt(right);
		out.writeInt(bottom);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
	ClassNotFoundException {
		int left = in.readInt();
		int top = in.readInt();
		int right = in.readInt();
		int bottom = in.readInt();

		mRect = new Rect(left, top, right, bottom);
	}
}
