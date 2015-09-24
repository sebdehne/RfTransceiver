package com.dehnes.rest.demo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class SerialConnection {
    private static final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

    private static final byte MY_DST = 1;

    private final ExecutorService threadPool;
    private final Set<Consumer<RfPacket>> listeners = new HashSet<>();
    private final SocketAddress dst;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    private Thread readerThread; // guarded by "this"

    // local to the readerThread
    private Socket socket;
    private InputStream in;

    private final ReentrantLock outputStreamLock = new ReentrantLock();
    private final Condition sendingSyncer = outputStreamLock.newCondition();
    private OutputStream out;
    private final LinkedList<SendRequest> sendingQueue = new LinkedList<>();

    public SerialConnection(ExecutorService threadPool) {
        this(threadPool, new InetSocketAddress(
                System.getProperty("DST_HOST", "localhost"),
                Integer.parseInt(System.getProperty("DST_PORT", "23000"))));
    }

    public SerialConnection(ExecutorService threadPool, SocketAddress dst) {
        this.threadPool = threadPool;
        this.dst = dst;
    }

    public class RfPacket {
        private final int remoteAddr;
        private final int[] message;

        public RfPacket(int remoteAddr, int[] message) {
            this.remoteAddr = remoteAddr;
            this.message = message;
        }

        @Override
        public String toString() {
            return "RfPacket{" +
                    "remoteAddr=" + remoteAddr +
                    ", message=" + Arrays.toString(message) +
                    '}';
        }
    }

    public class SendRequest {
        private final RfPacket packet;
        private final Queue<Boolean> result;

        public SendRequest(RfPacket packet, Queue<Boolean> result) {
            this.packet = packet;
            this.result = result;
        }
    }

    public void registerListener(Consumer<RfPacket> listener) {
        synchronized (listeners) {
            if (listeners.contains(listener)) {
                throw new RuntimeException("Listener already registered");
            }
            listeners.add(listener);
        }
    }

    public void unregisterListener(Consumer<RfPacket> listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                throw new RuntimeException("Listener does not exist");
            }
            listeners.remove(listener);
        }
    }

    public boolean send(RfPacket rfPacket, long timeout, TimeUnit timeUnit) {
        LinkedBlockingQueue<Boolean> q = new LinkedBlockingQueue<>();

        outputStreamLock.lock();
        try {

            if (!isStarted.get()) {
                return false;
            }

            sendingQueue.offer(new SendRequest(
                    rfPacket,
                    q
            ));

            sendingSyncer.signalAll();

        } finally {
            outputStreamLock.unlock();
        }

        try {
            return q.poll(timeout, timeUnit);
        } catch (InterruptedException ignored) {
        }
        return false;
    }

    @PostConstruct
    public synchronized void start() {
        if (isStarted.get()) {
            return;
        }

        isStarted.set(true);

        readerThread = new Thread(() -> {
            try {
                readTask().run();
            } catch (Exception e) {
                logger.error("read thread failed permanently", e);
            }
        }, "readerThread");
        readerThread.start();

        new Thread(() -> {
            try {
                writeTask().run();
            } catch (Exception e) {
                logger.info("writer thread failed permanently", e);
            }
        }, "writerThread").start();
    }

    private Runnable writeTask() {
        return () -> {
            while (isStarted.get()) {

                outputStreamLock.lock();
                try {

                    SendRequest sendRequest = null;
                    while (isStarted.get() && (sendRequest = sendingQueue.poll()) == null) {
                        try {
                            sendingSyncer.await();
                        } catch (InterruptedException ignored) {
                        }
                    }

                    if (sendRequest == null) {
                        return; // is shutting down
                    }

                    if (out == null) { // not connected, report error
                        sendRequest.result.offer(false);
                        continue;
                    }

                    sendRequest.result.offer(writePacket(sendRequest.packet, out));
                } finally {
                    outputStreamLock.unlock();
                }
            }
        };
    }

    private Runnable readTask() {
        return () -> {

            while (isStarted.get()) { // keep trying to connect to the transceiver
                // prevent tight loop
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }

                // (re)connect
                if (!reconnect()) {
                    continue;
                }

                byte[] buf = new byte[1024];

                RfPacket nextPacket;
                while (isStarted.get()) { // keep reading packets while connected
                    nextPacket = tryReadNextPacket(in, buf);
                    if (nextPacket == null) {
                        break; // Some I/O error - give up
                    }
                    distributeNewMessage(nextPacket);
                }
            }

            // something went wrong OR we are asked to shut down

            // close out stream
            outputStreamLock.lock();
            try {
                close(out);
                out = null;
            } finally {
                outputStreamLock.unlock();
            }
            close(in);
            in = null;
            close(socket);
            socket = null;

        };
    }

    private boolean writePacket(RfPacket packet, OutputStream out) {
        byte[] buf = new byte[packet.message.length + 1];
        buf[0] = (byte) packet.remoteAddr;
        for (int i = 0; i < packet.message.length; i++) {
            buf[i + 1] = (byte) packet.message[i];
        }

        try {
            out.write(buf);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    private RfPacket tryReadNextPacket(InputStream in, byte[] buf) {
        // packet format: <errorCode>,<dst>,<from>,<msgLen>,msg...

        int writePos = 0;

        while (true) {

            // we need at least 4 bytes
            if (writePos >= 4) {
                int errorCode = buf[0] & 0xFF;
                int dst = buf[1] & 0xFF;
                int from = buf[2] & 0xFF;
                byte msgLen = buf[3];

                if (errorCode != 0) { // skip error
                    logger.debug("Skipping error " + errorCode);
                    compact(buf, 1);
                    writePos = 0;
                    continue;
                }

                // do we have a complete packet?
                if (writePos >= 4 + msgLen) {
                    byte[] msg = new byte[msgLen];
                    System.arraycopy(buf, 4, msg, 0, msgLen);
                    RfPacket p = new RfPacket(
                            from,
                            convert(msg)
                    );
                    compact(buf, 4 + msgLen);
                    if (dst == MY_DST) {
                        logger.info("Received packet " + p);
                        return p;
                    } else {
                        logger.debug("Packet not for me " + p);
                        writePos = 0;
                    }
                }
            }

            try {
                writePos += in.read(buf, writePos, buf.length - writePos);
            } catch (IOException e) {
                return null;
            }

            if (writePos == buf.length) {
                // forced to give up
                logger.info("Bugger is full, giving up");
                return null;
            }
        }
    }

    private int[] convert(byte[] in) {
        if (in.length > 0) {
            int[] a = new int[in.length];
            for (int i = 0; i < in.length; i++) {
                a[i] = in[i] & 0xFF;
            }
            return a;
        } else {
            return new int[0];
        }
    }

    private void compact(byte[] buf, int firstByte) {
        System.arraycopy(buf, firstByte, buf, 0, buf.length - firstByte);
    }

    @PreDestroy
    public synchronized void stop() {
        if (!isStarted.get()) {
            return;
        }

        isStarted.set(false);

        // notify the writer
        outputStreamLock.lock();
        try {
            sendingSyncer.signalAll();
        } finally {
            outputStreamLock.unlock();
        }

        // notify the reader
        close(socket);
    }

    private void distributeNewMessage(RfPacket rfPacket) {
        synchronized (listeners) {
            listeners.forEach(l -> threadPool.submit(() -> l.accept(rfPacket)));
        }
    }

    private boolean reconnect() {
        if (in != null) {
            close(in);
            in = null;
        }
        if (out != null) {
            close(out);
            out = null;
        }
        if (socket != null) {
            close(socket);
            socket = null;
        }

        try {
            socket = new Socket();
            socket.connect(dst, 10000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            logger.info("Connected to " + dst);
            return true;
        } catch (IOException e) {
            logger.info("Could not connect - " + e.getMessage(), e);
            return false;
        }
    }

    private void close(Object closable) {
        try {
            Method method = closable.getClass().getMethod("close");
            if (method != null) {
                method.invoke(closable);
            }
        } catch (Exception ignored) {
        }
    }

}
