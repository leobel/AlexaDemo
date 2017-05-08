package org.freelectron.leobel.testlwa.models;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;

import org.freelectron.leobel.testlwa.config.ObjectMapperFactory;
import org.freelectron.leobel.testlwa.models.exception.AVSJsonProcessingException;
import org.freelectron.leobel.testlwa.models.message.Directive;

import java.io.IOException;

/**
 * Created by leobel on 2/22/17.
 */

public class MessageParser {

    private static String TAG = "MESSAGE_PARSER_ERROR";


    /**
     * Parses a single valid Directive in the given byte array
     *
     * @return Directive if the bytes composed a valid Directive
     * @throws IOException
     *             Directive parsing failed
     */

    public Directive parseServerMessage(byte[] bytes) throws IOException {
        return parse(bytes, Directive.class);
    }

    protected <T> T parse(byte[] bytes, Class<T> clazz) throws IOException {
        try {
            ObjectReader reader = ObjectMapperFactory.getObjectReader();
            Object logBody = reader.forType(Object.class).readValue(bytes);
            Log.d("Response metadata: \n{}", ObjectMapperFactory.getObjectWriter()
                    .withDefaultPrettyPrinter()
                    .writeValueAsString(logBody));
            return reader.forType(clazz).readValue(bytes);
        } catch (JsonProcessingException e) {
            String unparseable = new String(bytes, "UTF-8");
            throw new AVSJsonProcessingException(
                    String.format("Failed to parse a %1$s", clazz.getSimpleName()), e, unparseable);
        }
    }
}
