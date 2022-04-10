package com.github.pselamy.grpc;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.lang.Integer.parseInt;

/**
 * Manages startup/shutdown of a gRPC server.
 */
public class GrpcServerRunner {
    private static final Logger logger = Logger.getLogger(GrpcServerRunner.class.getName());

    private final Supplier<Server> server;

    private static Server createServer(int port, ImmutableList<ServerServiceDefinition> serviceDefinitions) {
        return ServerBuilder.forPort(port).addServices(serviceDefinitions).build().start();
    }
    
    private static ImmutableList<ServerServiceDefinition> getServiceDefinitions(Set<BindableService> services) {
        return services.stream()
                .map(BindableService::bindService)
                .collect(toImmutableList());
    }
    
    @Inject
    GrpcServerRunner(PortSupplier portSupplier, Set<BindableService> services) {
        this.server = Suppliers.memoize(() -> createServer(portSupplier.get(), getServiceDefinitions(services));
    }

    public void run() throws InterruptedException {
        start();
        server.get().awaitTermination();
    }

    private void start() {
        logger.info("Server started, listening on " + server.get().getPort());
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
        server.get().shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }

    static class PortSupplier implements Supplier<Integer> {
        @Override
        public Integer get() {
            String port = System.getenv()
                    .getOrDefault("PORT", "50051");
            return parseInt(port);
        }
    }
}
