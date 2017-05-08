package org.freelectron.leobel.testlwa.models;

import android.util.Log;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.freelectron.leobel.testlwa.models.message.response.Directive;
import org.freelectron.leobel.testlwa.models.message.response.ResponseBody;


/**
 * Created by leobel on 2/22/17.
 */

public class MultipartParser extends MessageParser {
    private static final int MULTIPART_BUFFER_SIZE = 512;

    private static String TAG = "MULTIPART_PARSER_ERROR";

    private final MultipartParserConsumer consumer;
    private final AtomicBoolean shutdown;
    private MultipartStream multipartStream;
    private Map<String, String> headers;

    public MultipartParser(MultipartParserConsumer consumer) {
        this.consumer = consumer;
        this.shutdown = new AtomicBoolean(false);
    }

    public void parseStream(InputStream inputStream, String boundary) throws Exception {
        shutdown.set(false);
        multipartStream =  new MultipartStream(inputStream, boundary.getBytes(), MULTIPART_BUFFER_SIZE, null);
        headers = null;

        loopStream();
    }

    public void shutdownGracefully() {
        shutdown.set(true);
    }

    private ResponseBody parseResponseBody(byte[] bytes) throws IOException {
        return parse(bytes, ResponseBody.class);
    }

    private void loopStream() throws Exception {
        try {
            boolean hasNextPart = multipartStream.skipPreamble();
            while (hasNextPart) {
                handlePart();
                hasNextPart = multipartStream.readBoundary();
            }
        } catch (Exception e) {
            if (!shutdown.get()) {
                throw e;
            }
        }
    }

    private void handlePart() throws IOException {
        headers = getPartHeaders();
        byte[] partBytes = getPartBytes();
        boolean isMetadata = isPartJSON(headers);

        if (isMetadata) {
            handleMetadata(partBytes);
        } else {
            handleAudio(partBytes);
        }
    }

    private void handleMetadata(byte[] partBytes) throws IOException {
        Directive directive = parseResponseBody(partBytes).getDirective();
        if (directive != null) {
            consumer.onDirective(directive);
        } else {
            Log.e(TAG, "Failed to parse a directive.");
        }
    }

    private void handleAudio(byte[] partBytes) {
        String contentId = getMultipartContentId(headers);
        InputStream attachmentContent = new ByteArrayInputStream(partBytes);

        consumer.onDirectiveAttachment(contentId, attachmentContent);
    }

    private byte[] getPartBytes() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        multipartStream.readBodyData(data);
        return data.toByteArray();
    }

    private Map<String, String> getPartHeaders() throws IOException {
        String headers = multipartStream.readHeaders();
        BufferedReader reader = new BufferedReader(new StringReader(headers));
        Map<String, String> headerMap = new HashMap<>();
        try {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (!StringUtils.isBlank(line) && line.contains(":")) {
                    int colon = line.indexOf(":");
                    String headerName = line.substring(0, colon).trim();
                    String headerValue = line.substring(colon + 1).trim();
                    headerMap.put(headerName.toLowerCase(), headerValue);
                }
            }
        } catch (Exception e) {
        }

        return headerMap;
    }

    private String getMultipartHeaderValue(Map<String, String> headers, String searchHeader) {
        return headers.get(searchHeader.toLowerCase());
    }

    private String getMultipartContentId(Map<String, String> headers) {
        String contentId = getMultipartHeaderValue(headers, HttpHeaders.CONTENT_ID);
        contentId = contentId.substring(1, contentId.length() - 1);
        return contentId;
    }

    private boolean isPartJSON(Map<String, String> headers) {
        String contentType = getMultipartHeaderValue(headers, HttpHeaders.CONTENT_TYPE);
        return StringUtils.contains(contentType, ContentTypes.JSON);
    }

    public interface MultipartParserConsumer {
        void onDirective(Directive directive);

        void onDirectiveAttachment(String contentId, InputStream attachmentContent);
    }

//    @Override
//    public void onConnected() {
//        shutdown.set(false);
//    }
//
//    @Override
//    public void onDisconnected() {
//        shutdown.set(true);
//    }
}
