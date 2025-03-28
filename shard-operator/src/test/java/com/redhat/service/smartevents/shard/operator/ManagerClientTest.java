package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestStatus;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestType;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class ManagerClientTest extends AbstractShardWireMockTest {

    @Inject
    ManagerClient managerClient;

    @Test
    public void testNotifyBridgeStatusChange() throws InterruptedException {
        BridgeDTO dto = new BridgeDTO("bridgeStatusChange-1", "myName-1", "myEndpoint", "myCustomerId", ManagedResourceStatus.PROVISIONING, TestSupport.KAFKA_CONNECTION_DTO);
        stubBridgeUpdate();
        String expectedJsonUpdate = "{\"id\": \"bridgeStatusChange-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"provisioning\"}";

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addBridgeUpdateRequestListener(latch);

        managerClient.notifyBridgeStatusChange(dto).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void notifyProcessorStatusChange() throws Exception {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addProcessorUpdateRequestListener(latch);

        managerClient.notifyProcessorStatusChange(processor).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateManagerRequestMetricsOnSuccess() {
        ManagerRequestType requestType = ManagerRequestType.FETCH;
        HttpResponse<Buffer> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnSuccess(requestType, response);

        verify(metricsService).updateManagerRequestMetrics(requestType, ManagerRequestStatus.SUCCESS, "200");
    }

    @Test
    public void updateManagerRequestMetricsOnFailure() {
        ManagerRequestType requestType = ManagerRequestType.FETCH;
        Throwable error = mock(Throwable.class);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnFailure(requestType, error);

        verify(metricsService).updateManagerRequestMetrics(requestType, ManagerRequestStatus.FAILURE, "null");
    }

    @Test
    public void updateManagerRequestMetricsOnFailureWithHTTPResponseException() {
        ManagerRequestType requestType = ManagerRequestType.FETCH;
        HTTPResponseException error = mock(HTTPResponseException.class);
        when(error.getStatusCode()).thenReturn(404);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnFailure(requestType, error);

        verify(metricsService).updateManagerRequestMetrics(requestType, ManagerRequestStatus.FAILURE, "404");
    }

    @Test
    public void fetchBridgesToDeployOrDelete() throws JsonProcessingException {
        BridgeDTO dto = new BridgeDTO("bridgeStatusChange-1", "myName-1", "myEndpoint", "myCustomerId", ManagedResourceStatus.PROVISIONING, TestSupport.KAFKA_CONNECTION_DTO);
        stubBridgesToDeployOrDelete(List.of(dto));

        assertThat(managerClient.fetchBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void fetchProcessorsToDeployOrDelete() throws JsonProcessingException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        stubProcessorsToDeployOrDelete(List.of(processor));

        assertThat(managerClient.fetchProcessorsToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }
}
