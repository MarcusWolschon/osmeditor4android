package de.blau.android.prefs.search;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Objects;

class ViewMatchers {

    private ViewMatchers() {
    }

    // adapted from https://stackoverflow.com/a/53289078/12982352
    public static Matcher<View> recyclerViewHasItem(final Matcher<View> matcher) {
        return new BoundedMatcher<>(RecyclerView.class) {

            @Override
            public void describeTo(final Description description) {
                description.appendText("has item: ");
                matcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView recyclerView) {
                final Adapter adapter = recyclerView.getAdapter();
                for (int position = 0; position < adapter.getItemCount(); position++) {
                    if (matcher.matches(createAndBindViewHolder(recyclerView, adapter, position).itemView)) {
                        return true;
                    }
                }
                return false;
            }

            private static <VH extends RecyclerView.ViewHolder> @NonNull VH createAndBindViewHolder(
                    final RecyclerView recyclerView,
                    final Adapter<VH> adapter,
                    final int position) {
                final VH holder =
                        adapter.createViewHolder(
                                recyclerView,
                                adapter.getItemViewType(position));
                adapter.onBindViewHolder(holder, position);
                return holder;
            }
        };
    }

    public static Matcher<View> recyclerViewHasItemCount(final Matcher<Integer> itemCountMatcher) {
        return new BoundedMatcher<>(RecyclerView.class) {

            @Override
            public void describeTo(final Description description) {
                description.appendText("recyclerView's itemCount ");
                itemCountMatcher.describeTo(description);
            }

            @Override
            protected boolean matchesSafely(final RecyclerView view) {
                return itemCountMatcher.matches(Objects.requireNonNull(view.getAdapter()).getItemCount());
            }
        };
    }

    public static Matcher<View> childAtPosition(final Matcher<View> parentMatcher, final int position) {
        return new TypeSafeMatcher<>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(final View view) {
                final ViewParent parent = view.getParent();
                return parent instanceof final ViewGroup viewGroup
                        && parentMatcher.matches(parent)
                        && view.equals(viewGroup.getChildAt(position));
            }
        };
    }
}
