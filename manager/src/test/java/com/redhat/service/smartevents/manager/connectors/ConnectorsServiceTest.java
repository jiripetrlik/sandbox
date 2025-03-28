package com.redhat.service.smartevents.manager.connectors;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.dao.ConnectorsDAO;
import com.redhat.service.smartevents.manager.models.ConnectorEntity;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConnectorsServiceTest {

    private static final String TEST_CONNECTOR_ID = "test-connector-id";
    private static final String TEST_CONNECTOR_EXTERNAL_ID = "test-connector-ext-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_NAME = "TestProcessor";
    private static final String TEST_ACTION_CHANNEL = "testchannel";
    private static final String TEST_ACTION_WEBHOOK = "https://test.example.com/webhook";

    @Inject
    ConnectorsService connectorsService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @InjectMock
    ConnectorsDAO connectorsDAOMock;

    @Test
    @Transactional
    void doNotCreateConnector() {
        connectorsService.createConnectorEntity(testProcessor(), testWebhookAction());

        verify(connectorsDAOMock, never()).persist(any(ConnectorEntity.class));
    }

    @Test
    @Transactional
    void doCreateConnector() {
        connectorsService.createConnectorEntity(testProcessor(), testSlackAction());

        verify(connectorsDAOMock).persist(any(ConnectorEntity.class));
    }

    @Test
    @Transactional
    void doCreateConnectorFromConnectorEntity() {
        connectorsService.createConnectorEntity(testProcessor(), testSlackAction());

        verify(connectorsDAOMock).persist(any(ConnectorEntity.class));
    }

    @Test
    @Transactional
    void doNotDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(null);

        connectorsService.deleteConnectorEntity(testProcessor());

        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    @Test
    @Transactional
    void doDeleteConnector() {
        when(connectorsDAOMock.findByProcessorId(TEST_PROCESSOR_ID)).thenReturn(testConnectorEntity());

        connectorsService.deleteConnectorEntity(testProcessor());

        verify(connectorsDAOMock, never()).delete(any(ConnectorEntity.class));
    }

    private ConnectorEntity testConnectorEntity() {
        ConnectorEntity connectorEntity = new ConnectorEntity();
        connectorEntity.setId(TEST_CONNECTOR_ID);
        connectorEntity.setConnectorExternalId(TEST_CONNECTOR_EXTERNAL_ID);
        connectorEntity.setProcessor(testProcessor());
        return connectorEntity;
    }

    private Processor testProcessor() {
        Processor processor = new Processor();
        processor.setId(TEST_PROCESSOR_ID);
        processor.setName(TEST_PROCESSOR_NAME);
        return processor;
    }

    private Action testSlackAction() {
        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setParameters(Map.of(
                SlackAction.CHANNEL_PARAM, TEST_ACTION_CHANNEL,
                SlackAction.WEBHOOK_URL_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

    private Action testWebhookAction() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setParameters(Map.of(
                WebhookAction.ENDPOINT_PARAM, TEST_ACTION_WEBHOOK));
        return action;
    }

    private String testActionTopic() {
        return resourceNamesProvider.getProcessorTopicName(TEST_PROCESSOR_ID);
    }
}
