package org.thankthemaker.quarkus.opencensus;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import org.jboss.jandex.DotName;
import org.thankthemaker.quarkus.logging.RestLoggingFilter;
import org.thankthemaker.quarkus.logging.TraceInterceptor;
import org.thankthemaker.quarkus.logging.Traced;

import java.util.*;

public class OpencensusProcessor {
    static final String FEATURE_NAME = "opencensus-extension";

    @BuildStep
    public void createReasteasyBuildItem(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        System.out.println("OpencensusProcessor.createReasteasyBuildItem");
        providers.produce(new ResteasyJaxrsProviderBuildItem(RestLoggingFilter.class.getName()));
    }

    @BuildStep
    InterceptorBindingRegistrarBuildItem addInterceptorBindings() {
        System.out.println("OpencensusProcessor.addInterceptorBindings");
        InterceptorBindingRegistrarBuildItem additionalBindingsRegistrar =
                new InterceptorBindingRegistrarBuildItem(new InterceptorBindingRegistrar() {
                    @Override
                    public Map<DotName, Set<String>> registerAdditionalBindings() {
                        Map<DotName, Set<String>> result = new HashMap<>();
                        result.put(DotName.createSimple(Traced.class.getName()), Collections.EMPTY_SET);
                        return result;
                    }
                });
        return additionalBindingsRegistrar;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> additionalBeans() {
        System.out.println("OpencensusProcessor.additionalBeans");
        return Arrays.asList(
                new AdditionalBeanBuildItem(TraceInterceptor.class));
    }

/*    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializationConfiguration() {
        return new RuntimeInitializedClassBuildItem(sun.security.provider.SecureRandom.class.getCanonicalName());
    }*/

    @BuildStep
    FeatureBuildItem createFeatureItem() {
        System.out.println("OpencensusProcessor.createFeatureItem");
        return new FeatureBuildItem(FEATURE_NAME);
    }
}
