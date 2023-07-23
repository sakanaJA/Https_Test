import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class SimpleHttpsServer {

    public static void main(String[] args) throws Exception {
        // ソケットアドレスを設定
        InetSocketAddress address = new InetSocketAddress(8000);

        // HTTPSサーバを初期化
        HttpsServer server = HttpsServer.create(address, 0);
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // キーストアを初期化
        char[] password = "password".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream("keystore.jks");
        ks.load(fis, password);

        // キーマネージャーファクトリを設定
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        // 信頼できるマネージャーファクトリを設定
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        // HTTPSコンテキストとパラメータを設定
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            public void configure(HttpsParameters params) {
                try {
                    // SSLコンテキストを初期化
                    SSLContext context = getSSLContext();
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    // デフォルトのSSLパラメータを取得
                    SSLParameters defaultSSLParameters = context.getDefaultSSLParameters();
                    params.setSSLParameters(defaultSSLParameters);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // デフォルトのexecutorを使用
        server.start(); // サーバを開始
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String response = "Hello, world!"; // レスポンスメッセージを設定
            t.sendResponseHeaders(200, response.length()); // HTTPステータスコードとレスポンスボディの長さを設定
            OutputStream os = t.getResponseBody(); // レスポンスボディを取得
            os.write(response.getBytes()); // レスポンスボディにレスポンスメッセージを書き込む
            os.close(); // レスポンスボディを閉じる（送信完了）
        }
    }
}
