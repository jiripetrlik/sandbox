package com.redhat.service.smartevents.shard.operator.resources;

import java.util.Objects;

public class BridgeExecutorSpec {
    private String image;

    private String id;

    private String bridgeId;

    private String customerId;

    private String processorName;

    private String processorDefinition;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProcessorName() {
        return processorName;
    }

    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public String getProcessorDefinition() {
        return processorDefinition;
    }

    public void setProcessorDefinition(String processorDefinition) {
        this.processorDefinition = processorDefinition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BridgeExecutorSpec that = (BridgeExecutorSpec) o;
        return Objects.equals(image, that.image) && Objects.equals(id, that.id) && Objects.equals(bridgeId, that.bridgeId) && Objects.equals(customerId, that.customerId)
                && Objects.equals(processorName, that.processorName) && Objects.equals(processorDefinition, that.processorDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, id, bridgeId, customerId, processorName, processorDefinition);
    }
}
