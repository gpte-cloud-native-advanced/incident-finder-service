package com.redhat.emergency.response.incident.finder;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentService {

    private static Logger log = LoggerFactory.getLogger(IncidentService.class);

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "incident-service.url")
    String serviceUrl;

    WebClient webClient;

    void onStart(@Observes StartupEvent e) {
        int servicePort = serviceUrl.contains(":") ? Integer.parseInt(serviceUrl.substring(serviceUrl.indexOf(":") + 1)) : 8080;
        String serviceHost = serviceUrl.contains(":") ? serviceUrl.substring(0, serviceUrl.indexOf(":")) : serviceUrl;
        webClient = WebClient.create(vertx, new WebClientOptions().setDefaultHost(serviceHost).setDefaultPort(servicePort));
    }

    public Uni<List<String>> incidentsByName(String name) {
        if (!name.startsWith("%")) {
            name = "%25" + name;
        }
        if (!name.endsWith("%")) {
            name = name + "%25";
        }
        name = name.replaceAll("\\s+", "%20");
        return webClient.get("/incidents/byname/" + name).send().onItem().transform(resp -> {
            if (resp.statusCode() != 200) {
                log.error("Error when calling incident service. Return code " + resp.statusCode());
                throw new IllegalStateException(Integer.toString(resp.statusCode()));
            } else {
                JsonArray array = resp.bodyAsJsonArray();
                return array.stream().map(o -> (JsonObject) o).map(j -> j.getString("id"))
                        .collect(Collectors.toList());
            }
        });
    }
}