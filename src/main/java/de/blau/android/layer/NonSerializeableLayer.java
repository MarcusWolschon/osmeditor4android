package de.blau.android.layer;

import androidx.annotation.NonNull;
import de.blau.android.Map;

public abstract class NonSerializeableLayer extends MapViewLayer {

    protected Map map;

    @Override
    public void setMapInstance(@NonNull Map map) {
        this.map = map;
    }
}
