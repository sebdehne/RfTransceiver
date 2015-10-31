package com.dehnes.rest.demo.clients.serial;

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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class SerialConnection {
    private static final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

    private static final byte MY_DST = 1;

    private final Set<Function<RfPacket, Boolean>> listeners = new HashSet<>();
    private final SocketAddress dst;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    // local to the readerThread
    private Socket socket;
    private InputStream in;

    private final ReentrantLock outputStreamLock = new ReentrantLock();
    private final Condition sendingSyncer = outputStreamLock.newCondition();
    private OutputStream out;
    private final LinkedList<SendRequest> sendingQueue = new LinkedList<>();

    public SerialConnection() {
        this.dst = new InetSocketAddress(
                System.getProperty("DST_HOST", "localhost"),
                Integer.parseInt(System.getProperty("DST_PORT", "23000")));
    }

    public static class RfPacket {
        private final int remoteAddr;
        private final int[] message;

        public RfPacket(int remoteAddr, int[] message) {
            this.remoteAddr = remoteAddr;
            this.message = message;
        }

        public int getRemoteAddr() {
            return remoteAddr;
        }

        public int[] getMessage() {
            return message;
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

    public void registerListener(Function<RfPacket, Boolean> listener) {
        synchronized (listeners) {
            if (listeners.contains(listener)) {
                throw new RuntimeException("Listener already registered");
            }
            listeners.add(listener);
        }
    }

    public void unregisterListener(Function<RfPacket, Boolean> listener) {
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

            sendingQueue.offer(new SendRequest(
                    rfPacket,
                    q
            ));

            sendingSyncer.signalAll();

        } finally {
            outputStreamLock.unlock();
        }

        try {
            Boolean poll = q.poll(timeout, timeUnit);
            return poll == null ? false : poll;
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

        Thread readerThread = new Thread(() -> {
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

                    if (out == null) { // not connected yet
                        sendingQueue.addFirst(sendRequest);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        continue;
                    }

                    sendRequest.result.offer(writePacket(sendRequest.packet, out));

                    // do not flood the sender
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
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
                AtomicInteger writePos = new AtomicInteger(0);

                RfPacket nextPacket;
                while (isStarted.get()) { // keep reading packets while connected
                    nextPacket = tryReadNextPacket(in, buf, writePos);
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

    private RfPacket tryReadNextPacket(InputStream in, byte[] buf, AtomicInteger writePos) {
        // packet format: <errorCode>,<dst>,<from>,<msgLen>,msg...

        while (true) {

            // we need at least 4 bytes
            if (writePos.get() >= 4) {
                int errorCode = buf[0] & 0xFF;
                int dst = buf[1] & 0xFF;
                int from = buf[2] & 0xFF;
                int msgLen = buf[3];

                byte[] debug = new byte[4];
                System.arraycopy(buf, 0, debug, 0, 4);
                logger.debug("Got {}", debug);

                if (errorCode != 0) { // skip error
                    logger.debug("Skipping error " + errorCode);
                    compact(buf, 1, writePos);
                    continue;
                }

                if (writePos.get() >= 4 && msgLen < 1) {
                    logger.debug("Skipping negative msgLen");
                    compact(buf, 4, writePos);
                    continue;
                }

                // do we have a complete packet?
                if (writePos.get() >= 4 + msgLen) {
                    byte[] msg = new byte[msgLen];
                    System.arraycopy(buf, 4, msg, 0, msgLen);
                    RfPacket p = new RfPacket(
                            from,
                            convert(msg)
                    );
                    compact(buf, 4 + msgLen, writePos);

                    if (dst == MY_DST) {
                        logger.info("Received packet " + p);
                        return p;
                    } else {
                        logger.debug("Packet not for me " + p);
                    }
                }
            }

            try {
                writePos.set(writePos.get() + in.read(buf, writePos.get(), buf.length - writePos.get()));
            } catch (IOException e) {
                return null;
            }

            if (writePos.get() == buf.length) {
                // forced to give up
                logger.info("Buffer is full, giving up");
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

    private void compact(byte[] buf, int firstByte, AtomicInteger writePos) {
        System.arraycopy(buf, firstByte, buf, 0, buf.length - firstByte);
        writePos.set(writePos.get() - firstByte);
    }

    private void distributeNewMessage(RfPacket rfPacket) {
        synchronized (listeners) {
            if (!listeners.stream().filter(l -> exceptionLogger(l, rfPacket)).findFirst().isPresent()) {
                logger.info("No handler found for this sensor " + rfPacket.getRemoteAddr());
            }
        }
    }

    private Boolean exceptionLogger(Function<RfPacket, Boolean> r, RfPacket rfPacket) {
        try {
            return r.apply(rfPacket);
        } catch (Exception e) {
            logger.error("", e);
            return false;
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
