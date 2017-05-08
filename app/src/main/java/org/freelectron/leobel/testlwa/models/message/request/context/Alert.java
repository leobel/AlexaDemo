/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package org.freelectron.leobel.testlwa.models.message.request.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;

import org.freelectron.leobel.testlwa.models.DateUtils;
import org.freelectron.leobel.testlwa.models.message.response.alerts.SetAlert;
import org.joda.time.DateTime;

import java.io.IOException;


/**
 * Represents an alert (timer/alarm)
 */
public class Alert {
    private final String token;
    private final SetAlert.AlertType type;
    private final DateTime scheduledTime;

    public Alert(String token, SetAlert.AlertType type, DateTime scheduledTime) {
        this.token = token;
        this.type = type;
        this.scheduledTime = scheduledTime;
    }

    @JsonCreator
    public Alert(@JsonProperty("token") String token, @JsonProperty("type") SetAlert.AlertType type,
            @JsonProperty("scheduledTime") String scheduledTime) {
        this.token = token;
        this.type = type;
        this.scheduledTime = DateTime.parse(scheduledTime, DateUtils.AVS_ISO_OFFSET_DATE_TIME);
    }

    public String getToken() {
        return this.token;
    }

    public SetAlert.AlertType getType() {
        return this.type;
    }

    @JsonSerialize(using = DateTimeSerializer.class)
    public DateTime getScheduledTime() {
        return scheduledTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Alert other = (Alert) obj;
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        return true;
    }

    public static class ZonedDateTimeSerializer extends JsonSerializer<DateTime> {
        @Override
        public void serialize(DateTime dateTime, JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeString(dateTime.toString(DateUtils.AVS_ISO_OFFSET_DATE_TIME));
        }
    }
}
