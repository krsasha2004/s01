package ru.steam.volley;

import android.os.Handler;
import android.os.Looper;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RequestQueue {
    private final Cache mCache;
    private CacheDispatcher mCacheDispatcher;
    private final PriorityBlockingQueue<Request<?>> mCacheQueue;
    private final Set<Request<?>> mCurrentRequests;
    private final ResponseDelivery mDelivery;
    private NetworkDispatcher[] mDispatchers;
    private final Network mNetwork;
    private final PriorityBlockingQueue<Request<?>> mNetworkQueue;
    private AtomicInteger mSequenceGenerator;
    private final Map<String, Queue<Request<?>>> mWaitingRequests;

    public RequestQueue(Cache cache, Network network, int threadPoolSize, ResponseDelivery delivery) {
        this.mSequenceGenerator = new AtomicInteger();
        this.mWaitingRequests = new HashMap();
        this.mCurrentRequests = new HashSet();
        this.mCacheQueue = new PriorityBlockingQueue();
        this.mNetworkQueue = new PriorityBlockingQueue();
        this.mCache = cache;
        this.mNetwork = network;
        this.mDispatchers = new NetworkDispatcher[threadPoolSize];
        this.mDelivery = delivery;
    }

    public RequestQueue(Cache cache, Network network, int threadPoolSize) {
        this(cache, network, threadPoolSize, new ExecutorDelivery(new Handler(Looper.getMainLooper())));
    }

    public RequestQueue(Cache cache, Network network) {
        this(cache, network, 4);
    }

    public void start() {
        stop();
        this.mCacheDispatcher = new CacheDispatcher(this.mCacheQueue, this.mNetworkQueue, this.mCache, this.mDelivery);
        this.mCacheDispatcher.start();
        for (int i = 0; i < this.mDispatchers.length; i++) {
            NetworkDispatcher networkDispatcher = new NetworkDispatcher(this.mNetworkQueue, this.mNetwork, this.mCache, this.mDelivery);
            this.mDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }

    public void stop() {
        if (this.mCacheDispatcher != null) {
            this.mCacheDispatcher.quit();
        }
        for (int i = 0; i < this.mDispatchers.length; i++) {
            if (this.mDispatchers[i] != null) {
                this.mDispatchers[i].quit();
            }
        }
    }

    public int getSequenceNumber() {
        return this.mSequenceGenerator.incrementAndGet();
    }

    public <T> Request<T> add(Request<T> request) {
        request.setRequestQueue(this);
        synchronized (this.mCurrentRequests) {
            this.mCurrentRequests.add(request);
        }
        request.setSequence(getSequenceNumber());
        request.addMarker("add-to-queue");
        if (request.shouldCache()) {
            synchronized (this.mWaitingRequests) {
                String cacheKey = request.getCacheKey();
                if (this.mWaitingRequests.containsKey(cacheKey)) {
                    Queue<Request<?>> stagedRequests = (Queue) this.mWaitingRequests.get(cacheKey);
                    if (stagedRequests == null) {
                        stagedRequests = new LinkedList();
                    }
                    stagedRequests.add(request);
                    this.mWaitingRequests.put(cacheKey, stagedRequests);
                    if (VolleyLog.DEBUG) {
                        VolleyLog.m3v("Request for cacheKey=%s is in flight, putting on hold.", cacheKey);
                    }
                } else {
                    this.mWaitingRequests.put(cacheKey, null);
                    this.mCacheQueue.add(request);
                }
            }
        } else {
            this.mNetworkQueue.add(request);
        }
        return request;
    }

    void finish(Request<?> request) {
        synchronized (this.mCurrentRequests) {
            this.mCurrentRequests.remove(request);
        }
        if (request.shouldCache()) {
            synchronized (this.mWaitingRequests) {
                Queue<Request<?>> waitingRequests = (Queue) this.mWaitingRequests.remove(request.getCacheKey());
                if (waitingRequests != null) {
                    if (VolleyLog.DEBUG) {
                        VolleyLog.m3v("Releasing %d waiting requests for cacheKey=%s.", Integer.valueOf(waitingRequests.size()), cacheKey);
                    }
                    this.mCacheQueue.addAll(waitingRequests);
                }
            }
        }
    }
}
