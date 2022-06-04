package com.dtflys.test.http

import com.dtflys.forest.annotation.Address
import com.dtflys.forest.annotation.Body
import com.dtflys.forest.annotation.Get
import com.dtflys.forest.annotation.JSONBody
import com.dtflys.forest.annotation.Post
import com.dtflys.forest.annotation.Query
import com.dtflys.forest.backend.HttpBackend
import com.dtflys.forest.callback.OnSuccess
import com.dtflys.forest.config.ForestConfiguration
import com.dtflys.forest.http.ForestRequest
import com.dtflys.forest.http.ForestResponse
import com.dtflys.forest.mock.MockServerRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class TestKotlinClient(backend: HttpBackend?) : BaseClientTest(backend, configuration) {
    companion object {
        const val EXPECTED = "{\"status\":\"ok\"}"
        @JvmStatic
        private var configuration: ForestConfiguration? = null

        @JvmStatic
        @BeforeClass
        fun prepareClient() {
            configuration = ForestConfiguration.createConfiguration()
        }
    }

    @Rule @JvmField
    val server = MockWebServer()

    var client : Client?

    init {
        configuration?.setVariableValue("port", server.port)
        client = configuration?.client(Client::class.java)
    }


    @Address(host = "127.0.0.1", port = "{port}")
    interface Client {

        @Get("/")
        fun getText() : String

        @Get("/")
        fun getWithQuery(@Query("name") name: String) : String

        @Post("/")
        fun postText(@Body("text") text: String) : String

        @Post("/")
        fun postJson(@JSONBody text: Map<String, String>) : String

        @Get("/")
        fun callSuccess(onSuccess: OnSuccess<String>)

    }

    @Test
    fun testKotlinGet() {
        server.enqueue(MockResponse().setBody(EXPECTED))
        val result = client?.getText()
        assertThat(result).isNotNull.isEqualTo(EXPECTED)
    }

    @Test
    fun testKotlinGetWithQuery() {
        server.enqueue(MockResponse().setBody(EXPECTED))
        val result = client?.getWithQuery("ok")
        assertThat(result).isNotNull.isEqualTo(EXPECTED)
        MockServerRequest.mockRequest(server)
            .assertPathEquals("/")
            .assertQueryEquals("name", "ok")
    }


    @Test
    fun testKotlinPost() {
        server.enqueue(MockResponse().setBody(EXPECTED))
        val result = client?.postText("ok")
        assertThat(result).isNotNull.isEqualTo(EXPECTED)
        MockServerRequest.mockRequest(server)
            .assertPathEquals("/")
            .assertBodyEquals("text=ok")
    }


    @Test
    fun testKotlinPostJson() {
        server.enqueue(MockResponse().setBody(EXPECTED))
        val map = mapOf("name" to "ok")
        val result = client?.postJson(map)
        assertThat(result).isNotNull.isEqualTo(EXPECTED)
        MockServerRequest.mockRequest(server)
            .assertPathEquals("/")
            .assertBodyEquals("{\"name\":\"ok\"}")
    }


    @Test
    fun testKotlinOnSuccess() {
        server.enqueue(MockResponse().setBody(EXPECTED))
        var called = false
        client?.callSuccess { data, req, res ->
            assertThat(data).isNotNull.isEqualTo(EXPECTED)
            assertThat(req).isNotNull
            assertThat(res).isNotNull
            called = true
        }
        assertThat(called).isTrue
    }


}
