package de.blau.android.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import de.blau.android.osm.MergeResult.Issue;

/**
 * Small container for results of merge operation
 * 
 * @author simon
 *
 */
public class MergeResult {

    enum Issue {
        ROLECONFLICT, MERGEDTAGS, NOTREVERSABLE
    }

    private OsmElement element;
    Set<Issue>         issues = null;

    /**
     * Empty default constructor
     */
    public MergeResult() {
    }

    /**
     * Construct a new MergeResult
     * 
     * @param element the OsmElement we are returning
     */
    public MergeResult(@NonNull OsmElement element) {
        this.element = element;
    }

    /**
     * Add an issue to the list of issues
     * 
     * @param issue the MergeIssue to add
     */
    public void addIssue(@NonNull Issue issue) {
        if (issues == null) {
            issues = new HashSet<Issue>();
        }
        issues.add(issue);
    }

    /**
     * Add a Collection of issues
     * 
     * @param issues a Collection containing Issues
     */
    public void addAllIssues(@NonNull Collection<Issue> issues) {
        if (this.issues == null) {
            this.issues = new HashSet<Issue>();
        }
        this.issues.addAll(issues);
    }

    /**
     * Check if the merge had issues
     * 
     * @return true if there are issues
     */
    public boolean hasIssue() {
        return issues != null && !issues.isEmpty();
    }

    /**
     * Get the current Issues
     * 
     * @return a Collection of Issues
     */
    @Nullable
    public Collection<Issue> getIssues() {
        return issues;
    }

    /**
     * Get the stored OsmElement
     * 
     * @return the element
     */
    public OsmElement getElement() {
        return element;
    }

    /**
     * Set the stored OsmElement
     * 
     * @param element the element to set
     */
    public void setElement(OsmElement element) {
        this.element = element;
    }
}
