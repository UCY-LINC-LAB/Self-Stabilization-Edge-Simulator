package dsslib.utilities;

public class KeyValuePair {
    private final String key;
    private final Object value;

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "{'" + key + '\'' + ':'+value + '}';
    }
}
