package com.redhat.service.smartevents.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.shard.operator.TestSupport;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReason;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.ConditionType;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class BridgeExecutorControllerTest {

    @Inject
    BridgeExecutorController bridgeExecutorController;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    void setup() {
        kubernetesResourcePatcher.cleanUp();
    }

    @Test
    void testCreateNewBridgeExecutorWithoutSecrets() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();

        // When
        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.reconcile(bridgeExecutor, null);

        // Then
        assertThat(updateControl.isNoUpdate()).isTrue();
    }

    @Test
    void testCreateNewBridgeExecutor() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();
        deployBridgeExecutorSecret(bridgeExecutor);

        // When
        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.reconcile(bridgeExecutor, null);

        // Then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(bridgeExecutor.getStatus()).isNotNull();
        assertThat(bridgeExecutor.getStatus().isReady()).isFalse();
        assertThat(bridgeExecutor.getStatus().getConditionByType(ConditionType.Augmentation)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeExecutor.getStatus().getConditionByType(ConditionType.Ready)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
            assertThat(c.getReason()).isEqualTo(ConditionReason.DeploymentNotAvailable);
        });
    }

    @Test
    void testBridgeExecutorDeployment() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();
        deployBridgeExecutorSecret(bridgeExecutor);

        // When
        bridgeExecutorController.reconcile(bridgeExecutor, null);

        // Then
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).get();
        assertThat(deployment).isNotNull();
        assertThat(deployment.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(deployment.getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getSelector().getMatchLabels().size()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName()).isNotNull();
    }

    @Test
    void testBridgeExecutorDeployment_deploymentReplicaFailure() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();
        deployBridgeExecutorSecret(bridgeExecutor);

        // When
        bridgeExecutorController.reconcile(bridgeExecutor, null);
        Deployment deployment = getDeploymentFor(bridgeExecutor);

        // Then
        kubernetesResourcePatcher.patchDeploymentAsReplicaFailed(deployment.getMetadata().getName(), deployment.getMetadata().getNamespace());

        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.reconcile(bridgeExecutor, null);
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(updateControl.getResource().getStatus().getConditionByType(ConditionType.Ready).get().getReason()).isEqualTo(ConditionReason.DeploymentFailed);
        assertThat(updateControl.getResource().getStatus().getConditionByType(ConditionType.Augmentation).get().getStatus()).isEqualTo(ConditionStatus.False);
    }

    @Test
    void testBridgeExecutorDeployment_deploymentTimeoutFailure() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();
        deployBridgeExecutorSecret(bridgeExecutor);

        // When
        bridgeExecutorController.reconcile(bridgeExecutor, null);
        Deployment deployment = getDeploymentFor(bridgeExecutor);

        // Then
        kubernetesResourcePatcher.patchDeploymentAsTimeoutFailed(deployment.getMetadata().getName(), deployment.getMetadata().getNamespace());

        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.reconcile(bridgeExecutor, null);
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(updateControl.getResource().getStatus().getConditionByType(ConditionType.Ready).get().getReason()).isEqualTo(ConditionReason.DeploymentFailed);
        assertThat(updateControl.getResource().getStatus().getConditionByType(ConditionType.Augmentation).get().getStatus()).isEqualTo(ConditionStatus.False);
    }

    @Test
    void testBridgeExecutorNewImage() {
        // Given
        BridgeExecutor bridgeExecutor = buildBridgeExecutor();
        String oldImage = "oldImage";
        bridgeExecutor.getSpec().setImage(oldImage);
        deployBridgeExecutorSecret(bridgeExecutor);

        // When
        UpdateControl<BridgeExecutor> updateControl = bridgeExecutorController.reconcile(bridgeExecutor, null);

        //Then
        assertThat(updateControl.isUpdateResource()).isTrue();
        assertThat(updateControl.getResource().getSpec().getImage()).isEqualTo(TestSupport.EXECUTOR_IMAGE); // Should be restored
    }

    private void deployBridgeExecutorSecret(BridgeExecutor bridgeExecutor) {
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(bridgeExecutor.getMetadata().getNamespace())
                                .withName(bridgeExecutor.getMetadata().getName())
                                .build())
                .build();
        kubernetesClient
                .secrets()
                .inNamespace(bridgeExecutor.getMetadata().getNamespace())
                .withName(bridgeExecutor.getMetadata().getName())
                .createOrReplace(secret);
    }

    private Deployment getDeploymentFor(BridgeExecutor bridgeExecutor) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).get();
        assertThat(deployment).isNotNull();
        return deployment;
    }

    private BridgeExecutor buildBridgeExecutor() {
        return BridgeExecutor.fromBuilder()
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestSupport.CUSTOMER_ID))
                .withImageName(TestSupport.EXECUTOR_IMAGE)
                .withProcessorId(TestSupport.PROCESSOR_ID)
                .withProcessorName(TestSupport.PROCESSOR_NAME)
                .withBridgeId(TestSupport.BRIDGE_ID)
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withDefinition(new ProcessorDefinition())
                .build();
    }
}
