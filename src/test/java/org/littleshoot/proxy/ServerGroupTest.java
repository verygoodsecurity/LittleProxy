package org.littleshoot.proxy;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ThreadPoolConfiguration;
import org.littleshoot.proxy.test.HttpClientUtil;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


// set up two server responses that will execute more or less simultaneously. the first request has a small
// delay, to reduce the chance that the first request will finish entirely before the second  request is finished
// (and thus be somewhat more likely to be serviced by the same thread, even if the ThreadPoolConfiguration is
// not behaving properly).


// save the names of the threads that execute the filter methods. filter methods are executed by the worker thread
// handling the request/response, so if there is only one worker thread, the filter methods should be executed
// by the same thread.

public class ServerGroupTest {
    private ClientAndServer mockServer;
    private int mockServerPort;

    final String firstRequestPath = "/testSingleThreadFirstRequest";
    final String secondRequestPath = "/testSingleThreadSecondRequest";
    final String messageProcessingThreadName = UUID.randomUUID().toString();

    final AtomicReference<String> firstClientThreadName = new AtomicReference<String>();
    final AtomicReference<String> secondClientThreadName = new AtomicReference<String>();

    final AtomicReference<String> firstProxyThreadName = new AtomicReference<String>();
    final AtomicReference<String> secondProxyThreadName = new AtomicReference<String>();

    @Before
    public void setUp() {
        mockServer = new ClientAndServer(0);
        mockServerPort = mockServer.getPort();

        mockServer.when(request()
                .withMethod("GET")
                .withPath(firstRequestPath),
            Times.exactly(1))
            .respond(response()
                .withStatusCode(200)
                .withBody("first")
                .withDelay(TimeUnit.MILLISECONDS, 500)
            );

        mockServer.when(request()
                .withMethod("GET")
                .withPath(secondRequestPath),
            Times.exactly(1))
            .respond(response()
                .withStatusCode(200)
                .withBody("second")
            );
    }

    @After
    public void tearDown() {
        firstClientThreadName.set(null);
        secondClientThreadName.set(null);
        firstProxyThreadName.set(null);
        secondProxyThreadName.set(null);
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    public void testChunkedRequest() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(2, false,
            false, false, false, false,
            false, false, false, true);

        final Futures futures = runTwoRequests(proxyServer);

        futures.getFirstFuture().get();
        futures.getSecondFuture().get();

        assertEquals("Expected clientToProxy filter methods to be executed on the same thread for both requests", firstClientThreadName.get(), secondClientThreadName.get());
        assertEquals("Expected serverToProxy filter methods to be executed on the same thread for both requests", firstProxyThreadName.get(), secondProxyThreadName.get());

        assertNotEquals(firstClientThreadName.get(), messageProcessingThreadName);
        assertNotEquals(firstProxyThreadName.get(), messageProcessingThreadName);
    }

    @Test
    public void testBlockFirstRequest() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(2, true,
            false, false, false, false,
            false, false, false, false);

        final Futures futures = runTwoRequests(proxyServer);

        try {
            futures.getSecondFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Second request took longer than expected");
        }

        boolean firstStillExecuting = false;
        try {
            futures.getFirstFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            firstStillExecuting = true;
        }

        Assert.assertTrue("First request must be still executing", firstStillExecuting);

        try {
            futures.getFirstFuture().get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("First request took longer than expected");
        }

        assertEquals("Expected clientToProxy filter methods to be executed on the same thread for both requests", firstClientThreadName.get(), secondClientThreadName.get());
        assertEquals("Expected serverToProxy filter methods to be executed on the same thread for both requests", firstProxyThreadName.get(), secondProxyThreadName.get());

        assertEquals(firstClientThreadName.get(), messageProcessingThreadName);
        assertEquals(firstProxyThreadName.get(), messageProcessingThreadName);
    }

    @Test
    public void testBlockFirstResponse() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(2, false,
            false, true, false, false,
            false, false, false, false);

        final Futures futures = runTwoRequests(proxyServer);

        try {
            futures.getSecondFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Second request took longer than expected");
        }

        boolean firstStillExecuting = false;
        try {
            futures.getFirstFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            firstStillExecuting = true;
        }

        Assert.assertTrue("First request must be still executing", firstStillExecuting);

        try {
            futures.getFirstFuture().get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("First request took longer than expected");
        }

        assertEquals("Expected clientToProxy filter methods to be executed on the same thread for both requests", firstClientThreadName.get(), secondClientThreadName.get());
        assertEquals("Expected serverToProxy filter methods to be executed on the same thread for both requests", firstProxyThreadName.get(), secondProxyThreadName.get());

        assertEquals(firstClientThreadName.get(), messageProcessingThreadName);
        assertEquals(firstProxyThreadName.get(), messageProcessingThreadName);
    }

    @Test(expected = ExecutionException.class)
    public void testExceptionFirstRequest() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(2, false,
            false, false, false, true,
            false, false, false, false);

        final Futures futures = runTwoRequests(proxyServer);

        futures.getFirstFuture().get();
        futures.getSecondFuture().get();
    }

    @Test(expected = ExecutionException.class)
    @Ignore // for some reason the test hangs even with original logic
    public void testExceptionFirstResponse() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(2, false,
            false, false, false, false,
            false, true, false, false);

        final Futures futures = runTwoRequests(proxyServer);

        futures.getFirstFuture().get();
        futures.getSecondFuture().get();
    }

    @Test
    public void testBlockFirstRequestSingleProcessingThread() throws ExecutionException, InterruptedException {

        final HttpProxyServer proxyServer = getProxy(1, true,
            false, false, false, false,
            false, false, false, false);

        final Futures futures = runTwoRequests(proxyServer);

        boolean secondStillExecuting = false;
        try {
            futures.getSecondFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            secondStillExecuting = true;
        }

        Assert.assertTrue("Second request must be still executing", secondStillExecuting);

        boolean firstStillExecuting = false;
        try {
            futures.getFirstFuture().get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            firstStillExecuting = true;
        }

        Assert.assertTrue("First request must be still executing", firstStillExecuting);

        try {
            futures.getFirstFuture().get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("First request took longer than expected");
        }

        try {
            futures.getSecondFuture().get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Second request took longer than expected");
        }

        assertEquals("Expected clientToProxy filter methods to be executed on the same thread for both requests", firstClientThreadName.get(), secondClientThreadName.get());
        assertEquals("Expected serverToProxy filter methods to be executed on the same thread for both requests", firstProxyThreadName.get(), secondProxyThreadName.get());

        assertEquals(firstClientThreadName.get(), messageProcessingThreadName);
        assertEquals(firstProxyThreadName.get(), messageProcessingThreadName);
    }

    private HttpProxyServer getProxy(int processingThreads,
                                     boolean blockFirstRequest,
                                     boolean blockSecondRequest,
                                     boolean blockFirstResponse,
                                     boolean blockSecondResponse,
                                     boolean throwFirstRequestException,
                                     boolean throwSecondRequestException,
                                     boolean throwFirstResponseException,
                                     boolean throwSecondResponseException,
                                     boolean withChunks) {
        return DefaultHttpProxyServer.bootstrap()
                .withPort(0)
                .withFiltersSource(new HttpFiltersSourceAdapter() {

                    // required so chinks for used
                    @Override
                    public int getMaximumRequestBufferSizeInBytes() {
                        if (withChunks) {
                            return 0;
                        }
                        return 8388608 * 2;
                    }

                    @Override
                    public int getMaximumResponseBufferSizeInBytes() {
                        if (withChunks) {
                            return 0;
                        }
                        return 8388608 * 2;
                    }


                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public io.netty.handler.codec.http.HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                if (originalRequest.getUri().endsWith(firstRequestPath)) {
                                    firstClientThreadName.set(Thread.currentThread().getName());

                                    if (throwFirstRequestException) {
                                        throw new RuntimeException("first-request");
                                    }

                                    if (blockFirstRequest) {
                                        block();
                                    }

                                } else if (originalRequest.getUri().endsWith(secondRequestPath)) {
                                    secondClientThreadName.set(Thread.currentThread().getName());

                                    if (throwSecondRequestException) {
                                        throw new RuntimeException("second-request");
                                    }

                                    if (blockSecondRequest) {
                                        block();
                                    }
                                }

                                return super.clientToProxyRequest(httpObject);
                            }

                            @Override
                            public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                if (originalRequest.getUri().endsWith(firstRequestPath)) {
                                    firstProxyThreadName.set(Thread.currentThread().getName());

                                    if (throwFirstResponseException) {
                                        throw new RuntimeException("first-response");
                                    }

                                    if (blockFirstResponse) {
                                        block();
                                    }

                                } else if (originalRequest.getUri().endsWith(secondRequestPath)) {
                                    secondProxyThreadName.set(Thread.currentThread().getName());

                                    if (throwSecondResponseException) {
                                        throw new RuntimeException("second-response");
                                    }

                                    if (blockSecondResponse) {
                                        block();
                                    }
                                }
                                return httpObject;
                            }
                        };
                    }
                })
                .withThreadPoolConfiguration(new ThreadPoolConfiguration()
                        .withAcceptorThreads(1)
                        .withClientToProxyWorkerThreads(1)
                        .withProxyToServerWorkerThreads(1))
                .withMessageProcessingExecutor(Executors.newFixedThreadPool(processingThreads, r -> {
                    final Thread thread = new Thread(r);
                    thread.setName(messageProcessingThreadName);
                    return thread;
                }))
                .start();
    }

    private void block() {
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Futures runTwoRequests(HttpProxyServer proxyServer) throws InterruptedException {
        // execute both requests in parallel, to increase the chance of blocking due to the single-threaded ThreadPoolConfiguration

        final Runnable firstRequest = () -> {
            HttpResponse response = HttpClientUtil.performHttpGet("http://localhost:" + mockServerPort + firstRequestPath, proxyServer);
            assertEquals(200, response.getStatusLine().getStatusCode());
        };

        final Runnable secondRequest = () -> {
            HttpResponse response = HttpClientUtil.performHttpGet("http://localhost:" + mockServerPort + secondRequestPath, proxyServer);
            assertEquals(200, response.getStatusLine().getStatusCode());
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> firstFuture = executor.submit(firstRequest);
        Thread.sleep(500);
        Future<?> secondFuture = executor.submit(secondRequest);

        return new Futures(firstFuture, secondFuture);
    }

    private static class Futures {
        Future<?> getFirstFuture() {
            return firstFuture;
        }

        Future<?> getSecondFuture() {
            return secondFuture;
        }

        final Future<?> firstFuture;
        final Future<?> secondFuture;

        private Futures(Future<?> firstFuture, Future<?> secondFuture) {
            this.firstFuture = firstFuture;
            this.secondFuture = secondFuture;
        }
    }


}
