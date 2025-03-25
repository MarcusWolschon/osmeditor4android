package io.vespucci;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Add equal spacing around an item in a RecyclerView with the GridLayoutManager
 * 
 * Inspired by https://gist.github.com/UweTrottmann/c6344c32aa61d1bec5a6
 * 
 * @author simon
 *
 */
public class EqualSpacingDecoration extends RecyclerView.ItemDecoration {

    private final int inset;
    private int       spans = -1;

    /**
     * Decoration that adds a margin of inset width around all items
     * 
     * @param inset width of the margin
     */
    public EqualSpacingDecoration(int inset) {
        this.inset = inset;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) view.getLayoutParams();

        int position = layoutParams.getViewLayoutPosition();
        if (position == RecyclerView.NO_POSITION) {
            outRect.set(0, 0, 0, 0);
            return;
        }

        if (spans == -1) {
            spans = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
        }

        final int spanIndex = layoutParams.getSpanIndex();
        outRect.left = inset;
        outRect.top = position >= 0 && position < spans ? inset : 0;
        outRect.right = spanIndex == spans - 1 ? inset : 0;
        outRect.bottom = inset;
    }
}
