package net.simforge.fsdatafeeder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimState {
    private final Map<SimStateField, Object> state = new HashMap<>();

    public Map<SimStateField, Object> getState() {
        return Collections.unmodifiableMap(state);
    }

    public void set(final SimStateField field, final Object value) {
        state.put(field, value);
    }

    public Integer getInt(final SimStateField field) {
        return (Integer) state.get(field);
    }

    public Double getDouble(final SimStateField field) {
        return (Double) state.get(field);
    }

    public String getString(final SimStateField field) {
        return (String) state.get(field);
    }
}
