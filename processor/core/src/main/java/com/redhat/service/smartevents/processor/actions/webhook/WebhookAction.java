package com.redhat.service.smartevents.processor.actions.webhook;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface WebhookAction extends GatewayBean {

    String TYPE = "Webhook";
    String ENDPOINT_PARAM = "endpoint";
    String USE_TECHNICAL_BEARER_TOKEN_PARAM = "useTechnicalBearerToken";

    @Override
    default String getType() {
        return TYPE;
    }
}
