package com.redhat.service.smartevents.processor;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicActionValidator;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionResolver;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeActionValidator;
import com.redhat.service.smartevents.processor.actions.slack.SlackAction;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionConnector;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionResolver;
import com.redhat.service.smartevents.processor.actions.slack.SlackActionValidator;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GatewayConfiguratorImplTest {

    private static final Map<String, ExpectedBeanClasses<Action>> EXPECTED_ACTION_BEANS = Map.of(
            KafkaTopicAction.TYPE, expect(KafkaTopicActionValidator.class, null, null),
            SendToBridgeAction.TYPE, expect(SendToBridgeActionValidator.class, SendToBridgeActionResolver.class, null),
            SlackAction.TYPE, expect(SlackActionValidator.class, SlackActionResolver.class, SlackActionConnector.class),
            WebhookAction.TYPE, expect(WebhookActionValidator.class, null, null));

    private static final Map<String, ExpectedBeanClasses<Source>> EXPECTED_SOURCE_BEANS = Collections.emptyMap();

    @Inject
    GatewayConfiguratorImpl configurator;

    @Test
    void testExpectedActionBeans() {
        for (Map.Entry<String, ExpectedBeanClasses<Action>> entry : EXPECTED_ACTION_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses<Action> expected = entry.getValue();

            assertThat(configurator.getActionValidator(type))
                    .as("GatewayConfigurator.getActionValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getActionValidator(type))
                    .as("GatewayConfigurator.getActionValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            assertThat(configurator.getActionResolver(type))
                    .as("GatewayConfigurator.getActionResolver(\"%s\") should not return null", type)
                    .isNotNull();

            if (expected.resolverClass == null) {
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(configurator.getActionResolver(type))
                        .as("GatewayConfigurator.getActionResolver(\"%s\") should contain instance of %s", type, expected.resolverClass.getSimpleName())
                        .containsInstanceOf(expected.resolverClass);
            }

            assertThat(configurator.getActionConnector(type))
                    .as("GatewayConfigurator.getActionConnector(\"%s\") should not return null", type)
                    .isNotNull();

            if (expected.connectorClass == null) {
                assertThat(configurator.getActionConnector(type))
                        .as("GatewayConfigurator.getActionConnector(\"%s\") should be empty", type)
                        .isNotPresent();
            } else {
                assertThat(configurator.getActionConnector(type))
                        .as("GatewayConfigurator.getActionConnector(\"%s\") should not be empty", type)
                        .isPresent();
                assertThat(configurator.getActionConnector(type))
                        .as("GatewayConfigurator.getActionConnector(\"%s\") should contain instance of %s", type, expected.connectorClass.getSimpleName())
                        .containsInstanceOf(expected.connectorClass);
            }
        }
    }

    @Test
    void testUnexpectedActionBeans() {
        for (GatewayValidator<Action> validator : configurator.getActionValidators()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
        for (GatewayResolver<Action> resolver : configurator.getActionResolvers()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
        for (GatewayConnector<Action> connector : configurator.getActionConnectors()) {
            assertThat(EXPECTED_ACTION_BEANS)
                    .as("Found unexpected connector bean for type %s of class %s. Add it to this test.", connector.getType(), connector.getClass())
                    .containsKey(connector.getType());
        }
    }

    @Test
    void testExpectedSourceBeans() {
        for (Map.Entry<String, ExpectedBeanClasses<Source>> entry : EXPECTED_SOURCE_BEANS.entrySet()) {
            String type = entry.getKey();
            ExpectedBeanClasses<Source> expected = entry.getValue();

            assertThat(configurator.getSourceValidator(type))
                    .as("GatewayConfigurator.getSourceValidator(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getSourceValidator(type))
                    .as("GatewayConfigurator.getSourceValidator(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.validatorClass);

            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should not return null", type)
                    .isNotNull();
            assertThat(configurator.getSourceResolver(type))
                    .as("GatewayConfigurator.getSourceResolver(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.resolverClass);

            assertThat(configurator.getSourceConnector(type))
                    .as("GatewayConfigurator.getSourceConnector(\"%s\") should be instance of %s", type, expected.validatorClass.getSimpleName())
                    .isInstanceOf(expected.connectorClass);
            assertThat(configurator.getSourceConnector(type))
                    .as("GatewayConfigurator.getSourceConnector(\"%s\") should not return null", type)
                    .isNotNull();
        }
    }

    @Test
    void testUnexpectedSourceBeans() {
        for (GatewayValidator<Source> validator : configurator.getSourceValidators()) {
            assertThat(EXPECTED_SOURCE_BEANS)
                    .as("Found unexpected source validator bean for type %s of class %s. Add it to this test.", validator.getType(), validator.getClass())
                    .containsKey(validator.getType());
        }
        for (GatewayResolver<Source> resolver : configurator.getSourceResolvers()) {
            assertThat(EXPECTED_SOURCE_BEANS)
                    .as("Found unexpected source resolver bean for type %s of class %s. Add it to this test.", resolver.getType(), resolver.getClass())
                    .containsKey(resolver.getType());
        }
        for (GatewayConnector<Source> connector : configurator.getSourceConnectors()) {
            assertThat(EXPECTED_SOURCE_BEANS)
                    .as("Found unexpected source connector bean for type %s of class %s. Add it to this test.", connector.getType(), connector.getClass())
                    .containsKey(connector.getType());
        }
    }

    private static class ExpectedBeanClasses<T extends Gateway> {
        Class<? extends GatewayValidator<T>> validatorClass;
        Class<? extends GatewayResolver<T>> resolverClass;
        Class<? extends GatewayConnector<T>> connectorClass;
    }

    private static <T extends Gateway> ExpectedBeanClasses<T> expect(
            Class<? extends GatewayValidator<T>> validatorClass,
            Class<? extends GatewayResolver<T>> resolverClass,
            Class<? extends GatewayConnector<T>> connectorClass) {
        ExpectedBeanClasses<T> expected = new ExpectedBeanClasses<>();
        expected.validatorClass = validatorClass;
        expected.resolverClass = resolverClass;
        expected.connectorClass = connectorClass;
        return expected;
    }
}
