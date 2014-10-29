/*
 * Copyright (C) 2013-2014, Infthink (Beijing) Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tv.matchstick.server.fling.socket.data;

import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;

public final class FlingMessage {
    private static final int PAYLOAD_TYPE_STARING = 0;
    private static final int PAYLOAD_TYPE_BINARY = 1;

    private int protocolVersion;
    private String sourceId;
    private String destinationId;
    private String namespace;

    // 0 is STARING, return payloadUtf8
    // 1 is BINARY, return payloadBinary
    private int payloadType;
    private String payloadUtf8;
    private BinaryPayload payloadBinary;

    public FlingMessage() {
        protocolVersion = 0;
        sourceId = "";
        destinationId = "";
        namespace = "";
        payloadType = 0;
        payloadUtf8 = "";
        payloadBinary = BinaryPayload.mInstance;
    }

    public FlingMessage(byte[] bytes) {
        try {
            String message = new String(bytes, "utf-8");
            parseJson(message);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // public String payload() {
    // return "{\n" + "  protocolVersion: " + protocolVersion + ",\n"
    // + "  source_id:" + sourceId + ",\n" + "  destination_id:"
    // + destinationId + ",\n" + "  namespace:" + namespace + ",\n"
    // + "  payload_type:" + payloadType + ",\n" + "  payload_utf8:"
    // + payloadUtf8 + "\n" + "}";
    // }

    public JSONObject buildJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("protocolVersion", protocolVersion);
            obj.put("sourceId", sourceId);
            obj.put("destinationId", destinationId);
            obj.put("namespace", namespace);
            if (payloadType == PAYLOAD_TYPE_STARING) {
                obj.put("payloadType", "STRING");
                obj.put("payloadUtf8", payloadUtf8);
            } else {
                obj.put("payloadType", "BINARY");
                obj.put("payloadBinary",
                        Base64.encode(payloadBinary.b(), Base64.DEFAULT));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public void parseJson(String json) {
        JSONObject obj;
        try {
            obj = new JSONObject(json);
            if (obj.has("protocolVersion"))
                setProtocolVersion(obj.optInt("protocolVersion"));
            if (obj.has("sourceId"))
                setSourceId(obj.optString("sourceId"));
            if (obj.has("destinationId"))
                setDestinationId(obj.optString("destinationId"));
            if (obj.has("namespace"))
                setNamespace(obj.optString("namespace"));
            String type = null;
            if (obj.has("payloadType"))
                type = obj.optString("payloadType");
            if (type.equals("STRING")) {
                setPayloadMessage(obj.optString("payloadUtf8"));
            } else {
                setPayloadBinary(BinaryPayload.a(obj.optString("payloadBinary")
                        .getBytes("UTF-8")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public final void setProtocolVersion(int version) {
        protocolVersion = version;
    }

    public final void setPayloadBinary(BinaryPayload binary) {
        payloadType = PAYLOAD_TYPE_BINARY;
        payloadBinary = binary;
    }

    public final void setSourceId(String source) {
        sourceId = source;
    }

    public final String getNamespace() {
        return namespace;
    }

    public final void setDestinationId(String transId) {
        destinationId = transId;
    }

    public final void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public final int getPayloadType() {
        return payloadType;
    }

    public final void setPayloadMessage(String message) {
        payloadType = PAYLOAD_TYPE_STARING;
        payloadUtf8 = message;
    }

    public final String getMessage() {
        return payloadUtf8;
    }

    public final BinaryPayload getBinaryMessage() {
        return payloadBinary;
    }
}
