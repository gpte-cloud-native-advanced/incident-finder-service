package com.redhat.emergency.response.incident.finder;

import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class IncidentResource {

    private static final Logger log = LoggerFactory.getLogger(IncidentResource.class);

    @Inject
    IncidentService incidentService;

    @Inject
    IncidentAggregationService incidentAggregationService;

    @GET
    @Path("/incidents")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getIncidents(@QueryParam("name") String name) {
        return incidentService.incidentsByName(name)
                .onItem().produceMulti(list -> Multi.createFrom().iterable(list))
                .onItem().produceUni(id -> incidentAggregationService.incidentById(id))
                .concatenate().collectItems().asList()
                .onItem().apply(list -> list.stream().filter(j -> j.containsKey("id")).collect(Collectors.toList()))
                .onItem().apply(JsonArray::new)
                .onItem().apply(jsonArray -> Response.ok(jsonArray.encodePrettily()).build())
                .onFailure().recoverWithUni(() -> Uni.createFrom().item(Response.status(500).build()));
    }



}
