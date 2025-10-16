package net.eqozqq.mcpeproxy;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UdpProxy {

    private final String srcAddress;
    private final int srcPort;
    private final int dstPort;
    private volatile boolean running;
    private DatagramSocket socket;

    public UdpProxy(String srcAddress, int srcPort) {
        this.srcAddress = srcAddress;
        this.srcPort = srcPort;
        this.dstPort = 19132;
        this.running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void run() {
        running = true;
        InetSocketAddress clientAddr = null;

        try {
            InetAddress srcInetAddr = InetAddress.getByName(srcAddress);
            InetSocketAddress srcAddr = new InetSocketAddress(srcInetAddr, srcPort);

            socket = new DatagramSocket(dstPort);
            socket.setSoTimeout(100);

            byte[] buffer = new byte[4096];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    InetSocketAddress recvAddr = new InetSocketAddress(
                            packet.getAddress(),
                            packet.getPort()
                    );

                    if (recvAddr.equals(srcAddr)) {
                        if (clientAddr != null) {
                            DatagramPacket sendPacket = new DatagramPacket(
                                    packet.getData(),
                                    packet.getLength(),
                                    clientAddr.getAddress(),
                                    clientAddr.getPort()
                            );
                            socket.send(sendPacket);
                        }
                    } else {
                        if (clientAddr == null || clientAddr.getAddress().equals(recvAddr.getAddress())) {
                            clientAddr = recvAddr;
                            DatagramPacket sendPacket = new DatagramPacket(
                                    packet.getData(),
                                    packet.getLength(),
                                    srcAddr.getAddress(),
                                    srcAddr.getPort()
                            );
                            socket.send(sendPacket);
                        }
                    }
                } catch (java.net.SocketTimeoutException e) {
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            running = false;
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}