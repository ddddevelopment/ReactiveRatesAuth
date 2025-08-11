package com.reactiverates.auth.infrastructure.config;

import com.reactiverates.users.grpc.UsersServiceGrpc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcClientConfiguration {

    @Bean
    public UsersServiceGrpc.UsersServiceBlockingStub usersServiceStub(GrpcChannelFactory channelFactory) {
        return UsersServiceGrpc.newBlockingStub(channelFactory.createChannel("users-service"));
    }
}