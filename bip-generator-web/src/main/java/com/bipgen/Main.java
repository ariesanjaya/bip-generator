package com.bipgen;

import io.vertx.core.Vertx;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle())
            .onFailure(err -> {
                System.err.println("Failed to deploy MainVerticle: " + err.getMessage());
                vertx.close();
            });
    }
}
