package com.bipgen.handler;

import com.bipgen.service.BIP38Service;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class BIP38Handler {

    private final BIP38Service bip38Service;

    public BIP38Handler() {
        this.bip38Service = new BIP38Service();
    }

    /**
     * Handle POST /api/bip38/encrypt
     */
    public Handler<RoutingContext> encrypt() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String privateKey = body.getString("privateKey");
                String password = body.getString("password");

                if (privateKey == null || privateKey.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Private key is required")
                            .encode());
                    return;
                }

                if (password == null || password.length() < 4) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Password must be at least 4 characters")
                            .encode());
                    return;
                }

                JsonObject result = bip38Service.encrypt(privateKey, password);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(result.encode());

            } catch (Exception e) {
                handleError(ctx, e);
            }
        };
    }

    /**
     * Handle POST /api/bip38/decrypt
     */
    public Handler<RoutingContext> decrypt() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String encryptedKey = body.getString("encryptedKey");
                String password = body.getString("password");

                if (encryptedKey == null || encryptedKey.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Encrypted key is required")
                            .encode());
                    return;
                }

                if (password == null || password.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Password is required")
                            .encode());
                    return;
                }

                JsonObject result = bip38Service.decrypt(encryptedKey, password);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(result.encode());

            } catch (Exception e) {
                handleError(ctx, e);
            }
        };
    }

    /**
     * Handle errors
     */
    private void handleError(RoutingContext ctx, Exception e) {
        e.printStackTrace();

        int statusCode = 500;
        String errorMessage = e.getMessage() != null ? e.getMessage() : "Internal server error";

        // Check for specific error types
        if (e instanceof IllegalArgumentException) {
            statusCode = 400;
        }

        ctx.response()
            .setStatusCode(statusCode)
            .putHeader("content-type", "application/json")
            .end(new JsonObject()
                .put("error", errorMessage)
                .encode());
    }
}
