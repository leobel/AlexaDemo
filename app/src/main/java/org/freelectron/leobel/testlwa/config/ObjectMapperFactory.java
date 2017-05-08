package org.freelectron.leobel.testlwa.config;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Created by leobel on 2/23/17.
 */

public class ObjectMapperFactory {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer();
    private static final ObjectReader OBJECT_READER = OBJECT_MAPPER.reader();

    private ObjectMapperFactory() {
    }

    /**
     *
     * @return A generic object reader
     */
    public static ObjectReader getObjectReader() {
        return OBJECT_READER;
    }

    /**
     * Get an ObjectReader that can parse JSON to type clazz
     *
     * @param clazz
     *            Type of class to parse the JSON into
     * @return
     */
    public static ObjectReader getObjectReader(Class<?> clazz) {
        return OBJECT_READER.forType(clazz);
    }

    public static ObjectWriter getObjectWriter() {
        return OBJECT_WRITER;
    }

    public static ObjectMapper getObjectMapper(){return OBJECT_MAPPER;}
}