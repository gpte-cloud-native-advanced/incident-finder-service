package com.redhat.emergency.response.incident.finder;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IncidentAggregationService {

    private static final Logger log = LoggerFactory.getLogger(IncidentAggregationService.class);

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "incident-aggregation-service.url")
    String serviceUrl;

    WebClient webClient;

    void onStart(@Observes StartupEvent e) {
        int servicePort = serviceUrl.contains(":") ? Integer.parseInt(serviceUrl.substring(serviceUrl.indexOf(":") + 1)) : 8080;
        String serviceHost = serviceUrl.contains(":") ? serviceUrl.substring(0, serviceUrl.indexOf(":")) : serviceUrl;
        webClient = WebClient.create(vertx, new WebClientOptions().setDefaultHost(serviceHost).setDefaultPort(servicePort));
    }

    public Uni<JsonObject> incidentById(String id) {
        return webClient.get("/incident/" + id).send().onItem().apply(resp -> {
            if (resp.statusCode() == 404) {
                log.warn("Incident with id + " + id + " not found");
                return new JsonObject();
            } else if (resp.statusCode() != 200) {
                log.error("Error when calling incident aggregation service. Return code " + resp.statusCode());
                throw new IllegalStateException(Integer.toString(resp.statusCode()));
            } else {
                return resp.bodyAsJsonObject();
            }
        });
    }

}
