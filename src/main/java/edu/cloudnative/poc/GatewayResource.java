package edu.cloudnative.poc;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/")
public class GatewayResource {

	@Inject
	GatewayService service;

	private AtomicLong counter = new AtomicLong(0);

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/hello")
	public String hello() {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @hello";
		long timer = System.currentTimeMillis();
		message += " - done in " + (System.currentTimeMillis() - timer) + "[ms]";
		System.out.println(message);

		return "hello";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/fault")
	public String fault() {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @fault";
		long timer = System.currentTimeMillis();

		final Long invocationNumber = counter.getAndIncrement();

		maybeFail(String.format(message + " - invocation #%d failed", invocationNumber));

		message += " - invocation #%d returning successfully " + invocationNumber + " - done in "
				+ (System.currentTimeMillis() - timer) + "[ms]";

		System.out.println(message);

		return message;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/circuitbreaker")
	@CircuitBreaker(requestVolumeThreshold = 4)
	public String circuitbreaker() {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @circuitbreaker";
		long timer = System.currentTimeMillis();

		try {
			final Long invocationNumber = counter.getAndIncrement();

			maybeFail(String.format(message + " - invocation #%d failed", invocationNumber));

			message += " - invocation #%d returning successfully " + invocationNumber + " - done in "
					+ (System.currentTimeMillis() - timer) + "[ms]";

			System.out.println(message);

			return message;
		} catch (RuntimeException e) {
			message += " - CircuitBreaker mode";
			System.out.println(message);
			return message;
		}
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/retry")
	@Retry(maxRetries = 4)
	public String retry() {

		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @retry";
		long timer = System.currentTimeMillis();

		final Long invocationNumber = counter.getAndIncrement();

		maybeFail(String.format(message + " - invocation #%d failed", invocationNumber));

		message += " - invocation #%d returning successfully " + invocationNumber + " -  done in "
				+ (System.currentTimeMillis() - timer) + "[ms]";

		System.out.println(message);

		return "message";
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/timeout")
	@Timeout(250)
	public String timeout() {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @timeout";
		long timer = System.currentTimeMillis();

		final long invocationNumber = counter.getAndIncrement();

		try {
			randomDelay();
			message += " - invocation " + invocationNumber + " returning successfully - done in "
					+ (System.currentTimeMillis() - timer) + "[ms]";
		} catch (InterruptedException e) {
			message += " - invocation " + invocationNumber + " timed out after " + (System.currentTimeMillis() - timer)
					+ "[ms]";
		} finally {
			System.out.println(message);
			return message;
		}
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/fallback")
	@Timeout(15)
	@Fallback(fallbackMethod = "fallback")
	public String fallback() {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "GatewayService on host " + hostname + " @fallback";
		long timer = System.currentTimeMillis();

		final long invocationNumber = counter.getAndIncrement();

		try {
			randomDelay();
			message += " - invocation " + invocationNumber + " returning successfully - done in "
					+ (System.currentTimeMillis() - timer) + "[ms]";
			System.out.println(message);
			return message;
		} catch (InterruptedException e) {
			message += " - invocation " + invocationNumber + " timed out after " + (System.currentTimeMillis() - timer)
					+ "[ms]";
			message += " - fallback message/action-url";
			System.out.println(message);
			return message;
		}
	}

	public String fallback(String message) {
		message += " - Falling back to GatewayService#fallbackRecommendations()";
		return message;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/cpustress/{cpucounter}")
	public String cpustress(@PathParam Integer cpucounter) {
		return service.cpustress(cpucounter);
	}

	private void maybeFail(String failureLogMessage) {
		if (new Random().nextBoolean()) {
			System.out.println(failureLogMessage);
			throw new RuntimeException("Resource failure.");
		}
	}

	private void randomDelay() throws InterruptedException {
		Thread.sleep(new Random().nextInt(500));
	}
}