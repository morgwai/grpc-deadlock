// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.samples.grpc.deadlock;

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
		int[] repsRemainingHolder = { verbalVomit.getReps() };
		var echoObserver = (ServerCallStreamObserver<EchoResposne>) responseObserver;
		echoObserver.setOnReadyHandler(() -> {
			log.finer("sink ready");
			try {
				while (
					repsRemainingHolder[0] > 0
					&&	echoObserver.isReady()
					&& ! echoObserver.isCancelled()
				) {
					// multiply the content to fill the buffer faster
					var echoBuilder = new StringBuilder();
					for (int j = 0; j < MULTIPLY_FACTOR; j++) echoBuilder.append(verbalVomit);
					var echoedVomit =
						EchoResposne.newBuilder().setEchoedVomit(echoBuilder.toString()).build();

					if (log.isLoggable(Level.FINEST)) log.finest("echo");
					repsRemainingHolder[0]--;
					echoObserver.onNext(echoedVomit);
				}
				if (echoObserver.isCancelled()) {
					log.fine("client cancelled the call 2");
					return;
				}
				if (repsRemainingHolder[0] == 0) {
					echoObserver.onCompleted();
					log.fine("done");
					return;
				}
				log.finer("sink clogged at rep "
						+ (verbalVomit.getReps() - repsRemainingHolder[0] + 1));
			} catch (StatusRuntimeException e) {
				if (e.getStatus().getCode() == Code.CANCELLED) {
					log.fine("client cancelled the call 1");
				} else {
					log.severe("server error: " + e);
					e.printStackTrace();
				}
			} catch (Exception e) {
				log.severe("server error: " + e);
				e.printStackTrace();
				echoObserver.onError(Status.INTERNAL.withCause(e).asException());
			}
		});
	}



	static final int MULTIPLY_FACTOR = 100;

	static final Logger log = Logger.getLogger(EchoService.class.getName());
	static {
		var handler = new ConsoleHandler();
		handler.setLevel(Level.FINER);
		log.addHandler(handler);
		log.setLevel(Level.FINER);
	}
}
