package com.github.pselamy.grpc;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.grpc.BindableService;

import java.util.Set;

class GrpcServerRunnerModule extends AbstractModule {
    private static final Key<Set<BindableService>> BINDABLE_SERVICE_KEY =
            Key.get(new TypeLiteral<Set<BindableService>>() {
    });

    @Override
    protected void configure() {
        requireBinding(BINDABLE_SERVICE_KEY);
    }
}
