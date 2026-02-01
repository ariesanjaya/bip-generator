package com.bipgen.handler;

import com.bipgen.model.DerivedKeyResult;
import com.bipgen.model.DerivedMultipleResult;
import com.bipgen.model.MasterKeyResult;
import com.bipgen.model.MnemonicValidationResult;
import com.bipgen.service.BIP32Service;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class BIP32Handler {

    private final BIP32Service bip32Service;

    public BIP32Handler() {
        this.bip32Service = new BIP32Service();
    }

    /**
     * Handle POST /api/bip32/generate
     */
    public Handler<RoutingContext> generateMasterKey() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String customMnemonic = body.getString("mnemonic");
                int wordCount = body.getInteger("wordCount", 12);
                String passphrase = body.getString("passphrase", "");

                if (wordCount != 12 && wordCount != 24) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Word count must be 12 or 24")
                            .encode());
                    return;
                }

                MasterKeyResult result = bip32Service.generateMasterKey(customMnemonic, wordCount, passphrase);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(JsonObject.mapFrom(result).encode());

            } catch (Exception e) {
                handleError(ctx, e);
            }
        };
    }

    /**
     * Handle POST /api/bip32/derive
     */
    public Handler<RoutingContext> deriveChildKey() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String masterKey = body.getString("masterKey");
                String path = body.getString("path");

                if (masterKey == null || masterKey.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Master key is required")
                            .encode());
                    return;
                }

                if (path == null || path.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Derivation path is required")
                            .encode());
                    return;
                }

                DerivedKeyResult result = bip32Service.deriveChildKey(masterKey, path);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(JsonObject.mapFrom(result).encode());

            } catch (Exception e) {
                handleError(ctx, e);
            }
        };
    }

    /**
     * Handle POST /api/bip32/derive-multiple
     */
    public Handler<RoutingContext> deriveMultipleAddresses() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String masterKey = body.getString("masterKey");
                String pathPattern = body.getString("pathPattern");
                int count = body.getInteger("count", 5);

                if (masterKey == null || masterKey.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Master key is required")
                            .encode());
                    return;
                }

                if (pathPattern == null || pathPattern.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Path pattern is required")
                            .encode());
                    return;
                }

                if (count < 1 || count > 100) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Count must be between 1 and 100")
                            .encode());
                    return;
                }

                DerivedMultipleResult result = bip32Service.deriveMultipleAddresses(masterKey, pathPattern, count);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(JsonObject.mapFrom(result).encode());

            } catch (Exception e) {
                handleError(ctx, e);
            }
        };
    }

    /**
     * Handle POST /api/bip32/validate-mnemonic
     */
    public Handler<RoutingContext> validateMnemonic() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();
                String mnemonic = body.getString("mnemonic");

                if (mnemonic == null || mnemonic.isEmpty()) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Mnemonic is required")
                            .encode());
                    return;
                }

                MnemonicValidationResult result = bip32Service.validateMnemonic(mnemonic);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/json")
                    .end(JsonObject.mapFrom(result).encode());

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

        ctx.response()
            .setStatusCode(500)
            .putHeader("content-type", "application/json")
            .end(new JsonObject()
                .put("error", e.getMessage() != null ? e.getMessage() : "Internal server error")
                .encode());
    }
}
