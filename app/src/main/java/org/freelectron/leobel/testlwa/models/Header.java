package org.freelectron.leobel.testlwa.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by leobel on 2/20/17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Header implements Serializable {

    public static final String RECOGNIZE_DIRECTIVE = "Recognize";
    public static final String STOP_CAPTURE_DIRECTIVE = "StopCapture";
    public static final String EXPECT_SPEECH_DIRECTIVE = "ExpectSpeech";

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("name")
    private String name;

    @JsonProperty("messageId")
    private String messageId;

    @JsonProperty("dialogRequestId")
    private String dialogRequestId;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDialogRequestId() {
        return dialogRequestId;
    }

    public void setDialogRequestId(String dialogRequestId) {
        this.dialogRequestId = dialogRequestId;
    }
}
