/*
 * Copyright (c) 2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rabbitmq.pcf;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.perf.PerfTest;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class PcfPerfTest {

    public static void main(String[] args) {
        String uris = uris(System.getenv("VCAP_SERVICES"));
        if (uris == null) {
            throw new IllegalArgumentException("Unable to retrieve broker URI(s) from VCAP_SERVICES");
        }
        if (args == null) {
            args = new String[0];
        }
        args = Arrays.copyOf(args, args.length + 2);
        args[args.length - 2] = "--uris";
        args[args.length - 1] = uris;
        PerfTest.main(args);
    }

    static String uris(String vcapServices) {
        if (vcapServices == null || vcapServices.trim().isEmpty()) {
            return null;
        }
        Gson gson = new Gson();
        JsonObject servicesType = (JsonObject) gson.fromJson(vcapServices, JsonElement.class);
        String uris = null;
        for (String serviceType : servicesType.keySet()) {
            JsonArray services = servicesType.getAsJsonArray(serviceType);
            uris = extractUrisFromServices(services, service -> service.get("tags").getAsJsonArray().contains(new JsonPrimitive("amqp")));
            if (uris != null) {
                break;
            }
            uris = extractUrisFromServices(services, service -> true);
            if (uris != null) {
                break;
            }
        }
        return uris;
    }

    static String extractUrisFromServices(JsonArray services, Predicate<JsonObject> shouldInspectService) {
        for (JsonElement service : services) {
            if (shouldInspectService.test(service.getAsJsonObject())) {
                JsonObject credentials = services.get(0).getAsJsonObject().get("credentials").getAsJsonObject();
                if (credentials.get("uris") == null && credentials.get("urls") == null) {
                    break;
                }
                JsonArray uris = credentials.get("uris") == null ? credentials.get("urls").getAsJsonArray() : credentials.get("uris").getAsJsonArray();
                Stream.Builder<String> builder = Stream.builder();
                uris.forEach(uri -> builder.accept(uri.getAsString()));
                return String.join(",", builder.build().collect(Collectors.toList()));
            }
        }
        return null;
    }
}
