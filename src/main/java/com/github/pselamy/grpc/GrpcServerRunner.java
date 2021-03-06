package com.github.pselamy.grpc;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Module;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.inject.util.Modules.combine;
import static java.lang.Integer.parseInt;

/**
 * Manages startup/shutdown of a gRPC server.
 */
public class GrpcServerRunner {
    private static final Logger logger =
            Logger.getLogger(GrpcServerRunner.class.getName());

    private final Supplier<Server> server;

    @Inject
    GrpcServerRunner(PortSupplier portSupplier, Set<BindableService> services) {
        this.server = Suppliers.memoize(() ->
                createServer(bindServices(services), portSupplier.get()));
    }

    private static Server createServer(
            ImmutableList<ServerServiceDefinition> serviceDefinitions, int port) {
        try {
            return ServerBuilder
                    .forPort(port)
                    .addServices(serviceDefinitions)
                    .build()
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ImmutableList<ServerServiceDefinition> bindServices(
            Set<BindableService> services) {
        return services.stream()
                .map(BindableService::bindService)
                .collect(toImmutableList());
    }

    public static void run(Module... modules) throws InterruptedException {
        Module module = combine(combine(modules), new GrpcServerRunnerModule());
        run(Guice.createInjector(module).getInstance(GrpcServerRunner.class));
    }

    private static void run(GrpcServerRunner runner) throws InterruptedException {
        runner.start();
        runner.server().awaitTermination();
    }

    private Server server() {
        return server.get();
    }

    private void start() {
        logger.info("Server started, listening on " + server().getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                GrpcServerRunner.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        server().shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }

    static class PortSupplier implements Supplier<Integer> {
        private static final String DEFAULT_PORT = "50051";

        @Override
        public Integer get() {
            String port = System.getenv()
                    .getOrDefault("PORT", DEFAULT_PORT);
            return parseInt(port);
        }
    }
}
