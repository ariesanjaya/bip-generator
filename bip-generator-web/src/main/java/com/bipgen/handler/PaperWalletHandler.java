package com.bipgen.handler;

import com.bipgen.service.PaperWalletService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PaperWalletHandler {

    private final PaperWalletService paperWalletService;

    public PaperWalletHandler() {
        this.paperWalletService = new PaperWalletService();
    }

    public Handler<RoutingContext> generate() {
        return ctx -> {
            try {
                JsonObject body = ctx.body().asJsonObject();

                String mnemonic = body.getString("mnemonic");
                String address = body.getString("address");
                String privateKey = body.getString("privateKey");
                String path = body.getString("path");
                String publicKey = body.getString("publicKey", "");
                String passphrase = body.getString("passphrase", "");
                String masterPrivateKey = body.getString("masterPrivateKey", "");
                String masterPublicKey = body.getString("masterPublicKey", "");

                if (mnemonic == null || address == null || privateKey == null || path == null) {
                    ctx.response()
                        .setStatusCode(400)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                            .put("error", "Missing required fields: mnemonic, address, privateKey, path")
                            .encode());
                    return;
                }

                byte[] pdfBytes = paperWalletService.generatePdf(mnemonic, address, privateKey, path, publicKey,
                        passphrase, masterPrivateKey, masterPublicKey);

                ctx.response()
                    .setStatusCode(200)
                    .putHeader("content-type", "application/pdf")
                    .putHeader("Content-Disposition", "attachment; filename=\"paper-wallet.pdf\"")
                    .end(io.vertx.core.buffer.Buffer.buffer(pdfBytes));

            } catch (Exception e) {
                ctx.response()
                    .setStatusCode(500)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject()
                        .put("error", "Failed to generate paper wallet: " + e.getMessage())
                        .encode());
            }
        };
    }
}
