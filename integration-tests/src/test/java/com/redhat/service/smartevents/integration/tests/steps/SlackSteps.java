
package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.SlackResource;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class SlackSteps {

    private TestContext context;

    public SlackSteps(TestContext context) {
        this.context = context;
    }

    @Then("^Slack channel contains message with text \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void slackActionTest(String messageText, int timeoutMinutes) {
        String messageTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, messageText);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> assertThat(SlackResource.getListOfSlackMessages()).as("Searching for message containing text: '%s'", messageTextWithoutPlaceholders)
                                .anyMatch(msg -> msg.contains(messageTextWithoutPlaceholders)));
    }
}
