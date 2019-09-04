package de.blau.android.dialogs;

import java.io.Serializable;
import java.util.Map;

import android.support.annotation.NonNull;

public interface OnRelationsSelectedListener extends Serializable {
    
    /**
     * Relations and roles have been selected
     * 
     * @param memberships a map of the ids and roles
     */
    void onRelationsSelected(@NonNull Map<Long,String> memberships);
}
