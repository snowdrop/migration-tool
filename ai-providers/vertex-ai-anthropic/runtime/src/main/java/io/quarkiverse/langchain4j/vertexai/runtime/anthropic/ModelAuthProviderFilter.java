package io.quarkiverse.langchain4j.vertexai.runtime.anthropic;

import com.google.auth.oauth2.GoogleCredentials;
import io.quarkiverse.langchain4j.auth.ModelAuthProvider;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

public class ModelAuthProviderFilter implements ResteasyReactiveClientRequestFilter {
    private final ModelAuthProvider authorizer;

    public ModelAuthProviderFilter(String modelId) {
        this.authorizer = (ModelAuthProvider) ModelAuthProvider.resolve(modelId)
                .orElse(new ModelAuthProviderFilter.ApplicationDefaultAuthProvider());
    }

    public void filter(final ResteasyReactiveClientRequestContext requestContext) {
        Executor executorService = this.createExecutor();
        requestContext.suspend();
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    ModelAuthProviderFilter.this.setAuthorization(requestContext);
                    requestContext.resume();
                } catch (Exception e) {
                    requestContext.resume(e);
                }

            }
        });
    }

    private Executor createExecutor() {
        InstanceHandle<ManagedExecutor> executor = Arc.container().instance(ManagedExecutor.class, new Annotation[0]);
        return executor.isAvailable() ? (Executor) executor.get() : Infrastructure.getDefaultExecutor();
    }

    private void setAuthorization(ResteasyReactiveClientRequestContext requestContext) {
        String authValue = this.authorizer.getAuthorization(
                new ModelAuthProviderFilter.AuthInputImpl(requestContext.getMethod(),
                        requestContext.getUri(), requestContext.getHeaders()));
        if (authValue != null) {
            requestContext.getHeaders().putSingle("Authorization", authValue);
        }

    }

    private static record AuthInputImpl(String method, URI uri,
            MultivaluedMap<String, Object> headers) implements ModelAuthProvider.Input {
    }

    private static class ApplicationDefaultAuthProvider implements ModelAuthProvider {
        private static final String SCOPE_CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform";
        private static final String SCOPE_GENERATIVE_LANGUAGE = "https://www.googleapis.com/auth/generative-language.retriever";

        public String getAuthorization(ModelAuthProvider.Input input) {
            try {
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                        .createScoped(List.of("https://www.googleapis.com/auth/generative-language.retriever",
                                "https://www.googleapis.com/auth/cloud-platform"));
                credentials.refreshIfExpired();
                return "Bearer " + credentials.getAccessToken().getTokenValue();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
