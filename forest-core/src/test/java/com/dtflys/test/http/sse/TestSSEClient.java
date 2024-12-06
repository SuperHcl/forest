package com.dtflys.test.http.sse;

import com.dtflys.forest.config.ForestConfiguration;
import com.dtflys.forest.http.ForestSSE;
import com.dtflys.test.http.BaseClientTest;
import com.dtflys.test.model.Contact;
import com.dtflys.test.sse.MySSEHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Rule;
import org.junit.Test;

import javax.naming.Name;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestSSEClient extends BaseClientTest {

    @Rule
    public MockWebServer server = new MockWebServer();

    private SSEClient sseClient;

    private static ForestConfiguration configuration = ForestConfiguration.createConfiguration();

    public TestSSEClient(String backend, String jsonConverter) {
        super(backend, jsonConverter, configuration);
        configuration.setVariableValue("port", server.getPort());
        sseClient = configuration.client(SSEClient.class);
    }

    @Test
    public void testSSE() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "data:start\n" +
                "data:hello\n" +
                "event:{\"name\":\"Peter\",\"age\": \"18\",\"phone\":\"12345678\"}\n" +
                "event:close\n" +
                "data:dont show"
        ));

        StringBuffer buffer = new StringBuffer();

        sseClient.testSSE()
            .setOnOpen(eventSource -> {
                buffer.append("SSE Open\n");
            }).setOnClose((req, res) -> {
                buffer.append("SSE Close");
            }).addOnData((eventSource, name, value) -> {
                buffer.append("Receive data: ").append(value).append("\n");
            }).addOnEvent((eventSource, name, value) -> {
                buffer.append("Receive event: ").append(value).append("\n");
                if ("close".equals(value)) {
                    eventSource.close();
                }
            }).addConsumer("event", Contact.class, (eventSource, name, value) -> {
                buffer.append("name: ").append(value.getName()).append("; age: ").append(value.getAge()).append("\n");
            })
        .listen();

        System.out.println(buffer);
        assertThat(buffer.toString()).isEqualTo(
                "SSE Open\n" +
                "Receive data: start\n" +
                "Receive data: hello\n" +
                "Receive event: {\"name\":\"Peter\",\"age\": \"18\",\"phone\":\"12345678\"}\n" +
                "name: Peter; age: 18\n" +
                "Receive event: close\n" +
                "SSE Close"
        );
    }

    @Test
    public void testSSE_withCustomClass() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
                "data:start\n" +
                "data:hello\n" +
                "event:{\"name\":\"Peter\",\"age\": \"18\",\"phone\":\"12345678\"}\n" +
                "event:close\n" +
                "data:dont show"
        ));

        MySSEHandler sse = sseClient.testSSE_withCustomClass()
                .addConsumer("name", (nameSource, name, value) -> {
                    System.out.println("------: " + value);
                })
                .listen();
        System.out.println(sse.getStringBuffer());
        assertThat(sse.getStringBuffer().toString()).isEqualTo(
                "SSE Open\n" +
                "start ---- start\n" +
                "hello ---- hello\n" +
                "name: Peter; age: 18; phone: 12345678\n" +
                "receive close --- close\n" +
                "SSE Close"
        );
    }

}
