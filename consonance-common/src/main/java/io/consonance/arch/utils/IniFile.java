package io.consonance.arch.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This seems to be some representation of an INI file. This is a good candidate for replacement by Apache Commons Configuration
 *
 * @author boconnor
 */
public class IniFile {

    private final Pattern section = Pattern.compile("\\s*\\[([^]]*)\\]\\s*");
    private final Pattern keyValue = Pattern.compile("\\s*([^=]*)=(.*)");
    private final SortedMap<String, SortedMap<String, String>> entries = new TreeMap<>();

    public IniFile(String path) throws IOException {
        load(path);
    }

    public final void load(String path) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            String section = "no-section";
            while ((line = br.readLine()) != null) {
                Matcher m = this.section.matcher(line);
                if (m.matches()) {
                    section = m.group(1).trim();
                } else if (section != null) {
                    m = keyValue.matcher(line);
                    if (m.matches()) {
                        String key = m.group(1).trim();
                        String value = m.group(2).trim();
                        SortedMap<String, String> kv = entries.get(section);
                        if (kv == null) {
                            kv = new TreeMap<>();
                            entries.put(section, kv);
                        }
                        kv.put(key, value);
                    }
                }
            }
        }
    }

    public String getString(String section, String key, String defaultvalue) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return kv.get(key);
    }

    public int getInt(String section, String key, int defaultvalue) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Integer.parseInt(kv.get(key));
    }

    public float getFloat(String section, String key, float defaultvalue) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Float.parseFloat(kv.get(key));
    }

    public double getDouble(String section, String key, double defaultvalue) {
        Map<String, String> kv = entries.get(section);
        if (kv == null) {
            return defaultvalue;
        }
        return Double.parseDouble(kv.get(key));
    }

    public SortedMap<String, SortedMap<String, String>> getEntries() {
        return entries;
    }
}
