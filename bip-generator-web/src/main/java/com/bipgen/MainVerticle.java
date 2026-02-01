package com.bipgen;

import com.bipgen.handler.BIP32Handler;
import com.bipgen.handler.BIP38Handler;
import com.bipgen.handler.PaperWalletHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        // Initialize handlers
        BIP32Handler bip32Handler = new BIP32Handler();
        BIP38Handler bip38Handler = new BIP38Handler();
        PaperWalletHandler paperWalletHandler = new PaperWalletHandler();

        // Create router
        Router router = Router.router(vertx);

        // Enable CORS
        router.route().handler(CorsHandler.create()
            .addOrigin("*")
            .allowedMethod(io.vertx.core.http.HttpMethod.GET)
            .allowedMethod(io.vertx.core.http.HttpMethod.POST)
            .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
            .allowedHeader("Content-Type")
            .allowedHeader("Accept"));

        // Body handler for POST requests
        router.route().handler(BodyHandler.create());

        // API Routes - BIP32
        router.post("/api/bip32/generate").handler(bip32Handler.generateMasterKey());
        router.post("/api/bip32/derive").handler(bip32Handler.deriveChildKey());
        router.post("/api/bip32/derive-multiple").handler(bip32Handler.deriveMultipleAddresses());
        router.post("/api/bip32/validate-mnemonic").handler(bip32Handler.validateMnemonic());

        // API Routes - BIP38
        router.post("/api/bip38/encrypt").handler(bip38Handler.encrypt());
        router.post("/api/bip38/decrypt").handler(bip38Handler.decrypt());

        // API Routes - Paper Wallet
        router.post("/api/paper-wallet").handler(paperWalletHandler.generate());

        // Health check endpoint
        router.get("/api/health").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"status\":\"ok\",\"service\":\"BIP32/BIP38 Generator\"}");
        });

        // Serve static files from webroot
        router.route().handler(StaticHandler.create("webroot"));

        // Create HTTP server
        HttpServerOptions serverOptions = new HttpServerOptions()
            .setCompressionSupported(true);

        vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .listen(8080, http -> {
                if (http.succeeded()) {
                    System.out.println("╔════════════════════════════════════════════════════════════╗");
                    System.out.println("║                                                            ║");
                    System.out.println("║       BIP32/BIP38 Generator Server Started                 ║");
                    System.out.println("║                                                            ║");
                    System.out.println("║       HTTP Server listening on port 8080                   ║");
                    System.out.println("║       Open: http://localhost:8080                          ║");
                    System.out.println("║                                                            ║");
                    System.out.println("║       API Endpoints:                                       ║");
                    System.out.println("║       - POST /api/bip32/generate                           ║");
                    System.out.println("║       - POST /api/bip32/derive                             ║");
                    System.out.println("║       - POST /api/bip32/derive-multiple                    ║");
                    System.out.println("║       - POST /api/bip32/validate-mnemonic                  ║");
                    System.out.println("║       - POST /api/bip38/encrypt                            ║");
                    System.out.println("║       - POST /api/bip38/decrypt                            ║");
                    System.out.println("║       - POST /api/paper-wallet                             ║");
                    System.out.println("║       - GET  /api/health                                   ║");
                    System.out.println("║                                                            ║");
                    System.out.println("╚════════════════════════════════════════════════════════════╝");
                    startPromise.complete();
                } else {
                    System.err.println("Failed to start HTTP server: " + http.cause());
                    startPromise.fail(http.cause());
                }
            });
    }
}
