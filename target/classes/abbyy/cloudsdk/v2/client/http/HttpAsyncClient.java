package abbyy.cloudsdk.v2.client.http;

import abbyy.cloudsdk.v2.client.models.AuthInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class HttpAsyncClient {
    private String host;
    private String applicationId;
    private String password;

    public HttpAsyncClient(AuthInfo authInfo) {
        this.host = authInfo.getHost();
        this.applicationId = authInfo.getApplicationId();
        this.password = authInfo.getPassword();
    }

    public <T> CompletableFuture<HttpAsyncResponse<T>> sendRequest(HttpAsyncRequest<T> request) {
        return CompletableFuture.supplyAsync(() ->
        {
            try {
                URL url = new URL(host + request.getRequestUri());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                HttpRequestMethod method = request.getMethod();
                connection.setRequestMethod(method.getValue());
                addAuthenticationProperty(connection);
                request.getRequestProperties().forEach(connection::setRequestProperty);

                if (method == HttpRequestMethod.Post) {
                    HttpAsyncFileWrapper fileWrapper = request.getFileWrapper();
                    connection.setDoOutput(true);
                    if (fileWrapper != null) {
                        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + fileWrapper.getBoundary());
                        fileWrapper.transferFileTo(connection.getOutputStream());
                    }
                    else {
                        connection.getOutputStream().flush();
                        connection.getOutputStream().close();
                    }
                }
                HttpAsyncResponse<T> response = HttpAsyncResponse.buildResponse(connection);
                connection.disconnect();
                return response;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private void addAuthenticationProperty(HttpURLConnection connection) {
        String encoded = Base64.getEncoder().encodeToString((applicationId + ":" + password).getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encoded);
    }
}
