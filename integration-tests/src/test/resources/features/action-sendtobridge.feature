Feature: SendToBridge Action tests

  Background:
    Given authenticate against Manager

  Scenario: Send Cloud Event to SendToBridge with bridgeId defined
    Given create a new Bridge "bridge1"
    And create a new Bridge "bridge2"

    And the Bridge "bridge1" is existing with status "ready" within 4 minutes
    And the Bridge "bridge2" is existing with status "ready" within 4 minutes

    And the Ingress of Bridge "bridge1" is available within 2 minutes
    And the Ingress of Bridge "bridge2" is available within 2 minutes

    And add a Processor to the Bridge "bridge1" with body:
    """
    {
      "name": "sendToBridgeWithBridgeIdProcessor",
      "action": {
        "parameters": {
            "bridgeId":  "${bridge.bridge2.id}"
        },
        "type": "SendToBridge"
      }
    }
    """
    And the Processor "sendToBridgeWithBridgeIdProcessor" of the Bridge "bridge1" is existing with status "ready" within 3 minutes
    And the Processor "sendToBridgeWithBridgeIdProcessor" of the Bridge "bridge1" has action of type "SendToBridge" and parameters:
      | bridgeId | ${bridge.bridge2.id} |

    And add a Processor to the Bridge "bridge2" with body:
    """
    {
      "name": "webhookProcessor",
      "action": {
        "parameters": {
          "endpoint": "${env.slack.webhook.url}"
        },
        "type": "Webhook"
      },
      "transformationTemplate" : "{ \"text\": \"hello {data.name} by {id}\" }"
    }
    """
    And the Processor "webhookProcessor" of the Bridge "bridge2" is existing with status "ready" within 3 minutes
    And the Processor "webhookProcessor" of the Bridge "bridge2" has action of type "Webhook" and parameters:
      | endpoint | ${env.slack.webhook.url} |

    When send a cloud event to the Ingress of the Bridge "bridge1":
    """
    {
      "specversion": "1.0",
      "type": "hello.invoked",
      "source": "HelloService",
      "id": "my-id",
      "data": {
        "name": "world"
      }
    }
    """
    
    Then Slack channel contains message with text "hello world by ${cloud-event.my-id.id}" within 1 minute
