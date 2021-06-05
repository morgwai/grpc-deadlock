// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.samples.grpc.deadlock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.Status.Code;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import pl.morgwai.samples.grpc.deadlock.EchoServiceGrpc.EchoServiceImplBase;



public class EchoService extends EchoServiceImplBase {



	@Override
	public void multiEcho(EchoRequest verbalVomit, StreamObserver<EchoResposne> responseObserver) {
		log.fine("someone has just emitted an inconsiderated verbal vomit");
		var callMonitor = new Object();
		var echoObserver = (ServerCallStreamObserver<EchoResposne>) responseObserver;
		echoObserver.setOnReadyHandler(() -> {
			log.finer("sink ready");
			synchronized (callMonitor) { callMonitor.notifyAll(); }
		});
		echoObserver.setOnCancelHandler(() -> {
			log.fine("client cancelled the call 1");
			synchronized (callMonitor) { callMonitor.notifyAll(); }
		});

//		cpuIntensiveOpExecutor.execute(() -> {
			try {
				for (int i = 1; i <= verbalVomit.getReps(); i++) {
					if (echoObserver.isCancelled()) {
						log.fine("client cancelled the call 2");
						return;
					}
					synchronized (callMonitor) {
						while( ! echoObserver.isReady()) {
							log.finer("sink clogged at rep " + i);
							callMonitor.wait();
						}
					}

					// multiply the content to fill the buffer faster
					var echoBuilder = new StringBuilder();
					for (int j = 0; j < MULTIPLY_FACTOR; j++) {
						echoBuilder.append(verbalVomit.getInconsideratedVerbalVomit());
					}
					var echoedVomit =
						EchoResposne.newBuilder().setEchoedVomit(echoBuilder.toString()).build();

					if (log.isLoggable(Level.FINEST)) log.finest("echo");
					echoObserver.onNext(echoedVomit);
				}
				echoObserver.onCompleted();
			} catch (StatusRuntimeException e) {
				if (e.getStatus().getCode() == Code.CANCELLED) {
					log.fine("client cancelled the call 3");
				} else {
					log.severe("server error: " + e);
					e.printStackTrace();
				}
			} catch (Exception e) {
				log.severe("server error: " + e);
				e.printStackTrace();
				echoObserver.onError(Status.INTERNAL.withCause(e).asException());
			}
//		});
	}



	static final int MULTIPLY_FACTOR = 100;

	ExecutorService cpuIntensiveOpExecutor =
			new ThreadPoolExecutor(4, 4, 1, TimeUnit.DAYS, new LinkedBlockingQueue<>());

	static final Logger log = Logger.getLogger(EchoService.class.getName());
	static {
		var handler = new ConsoleHandler();
		handler.setLevel(Level.FINER);
		log.addHandler(handler);
		log.setLevel(Level.FINER);
	}
}
