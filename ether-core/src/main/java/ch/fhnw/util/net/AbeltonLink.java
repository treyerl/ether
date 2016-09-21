package ch.fhnw.util.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.CollectionUtilities;
import ch.fhnw.util.Log;

public class AbeltonLink {
	private static final Log log = Log.create();

	private final MulticastSocket           socket;
	private final InetAddress               addr;
	private       AtomicBoolean             joined = new AtomicBoolean();
	private       List<IAbeltonLinkHandler> handlers = new ArrayList<>();

	public AbeltonLink() throws IOException {
		socket = new MulticastSocket(20808);
		addr   = NetworkUtilities.multicastAddress('L','N','K');

		final Thread receiveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					try {
						if(joined.get()) {
							byte[] buffer = new byte[socket.getReceiveBufferSize()];
							DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
							socket.receive(packet);
							process(new AbeltonLinkPacket(System.nanoTime() / 1000, packet));
						} else {
							Thread.sleep(100);
						}
					} catch (Throwable t) {
						log.warning(t);
					}
				}
			}

		}, "Abelton Link");
		receiveThread.setDaemon(true);
		receiveThread.setPriority(Thread.MAX_PRIORITY);
		receiveThread.start();		
	}

	private void process(AbeltonLinkPacket linkPacket) {
		IAbeltonLinkHandler[] handlers = null; 
		synchronized (this) {
			handlers = this.handlers.toArray(new IAbeltonLinkHandler[this.handlers.size()]);
		}
		for(IAbeltonLinkHandler handler : handlers)
			handler.handle(linkPacket);
	}

	public synchronized void addHandler(IAbeltonLinkHandler handler) {
		handlers.add(handler);
	}

	public synchronized void removeHandler(IAbeltonLinkHandler handler) {
		CollectionUtilities.removeAll(handlers, handler);
	}

	public void join(boolean leafOnShutdown) throws UnknownHostException, IOException {
		if(!(joined.get())) {
			for(InetAddress ifaddr : NetworkUtilities.getLocalAddresses(false)) {
				try {
					socket.setInterface(ifaddr);
					socket.setReuseAddress(true);
					socket.setLoopbackMode(true);
					socket.joinGroup(addr);
					log.info("Abelton Link group joined " + ifaddr.getHostName() + ":" + socket.getLocalPort() + " " + addr);
				} catch(Throwable t) {}
			}
			joined.set(true);
		}

		if(leafOnShutdown) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {leave();} catch (IOException e) {log.warning(e);}
				}
			});	
		}
	}

	public void leave() throws IOException {
		if(joined.getAndSet(false)) {
			for(InetAddress ifaddr : NetworkUtilities.getLocalAddresses(false)) {
				try {
					socket.setInterface(ifaddr);
					socket.leaveGroup(addr);
				} catch(Throwable t) {}
			}
			log.info("Abelton Link group left");
		}
	}

	public boolean isJoined() {
		return joined.get();
	}

	public static void main(String[] args) throws IOException {
		Platform.get().init();

		AbeltonLink link = new AbeltonLink();
		link.join(true);
		link.addHandler(packet->{
			System.out.println(packet);
		});
		
		Platform.get().run();
	}
}
