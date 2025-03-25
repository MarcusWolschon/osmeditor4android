package io.vespucci.layer;

import androidx.annotation.NonNull;
import io.vespucci.Map;

public abstract class NonSerializeableLayer extends MapViewLayer {

    protected Map map;

    @Override
    public void setMapInstance(@NonNull Map map) {
        this.map = map;
    }
}
