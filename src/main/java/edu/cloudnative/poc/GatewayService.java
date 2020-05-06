package edu.cloudnative.poc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GatewayService {

	public String cpustress(Integer cpucounter) {
		String hostname = System.getenv().getOrDefault("HOSTNAME", "unknown");
		String message = "Facade on host " + hostname + " - CPU stress counter:" + cpucounter;
		long timer = System.currentTimeMillis();
		generateCpu(cpucounter);
		message += " done in " + (System.currentTimeMillis() - timer) + "[ms]";
		System.out.println(message);
		return message;
	}
	
//	private void generateCPU(Integer loopNumber) {
//		for (int i = 0; i < loopNumber; i++) {
//			try {
//				byte[] iv = new byte[16];
//				new SecureRandom().nextBytes(iv);
//				// IV
//				IvParameterSpec ivSpec = new IvParameterSpec(iv);oc start-build api-gateway-health-quarkus-native --from-dir=. --follow
//				// Key
//				KeyGenerator generator = KeyGenerator.getInstance("AES");
//				generator.init(128);
//				SecretKey secretKey = generator.generateKey();
//				// Encrypt
//				Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
//				cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
//				cipher.update("0123456789012345".getBytes());
//				cipher.doFinal();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	private void generateCpu(int loopNumber) {
    	LongAdder counter = new LongAdder();

        List<CalculationThread> runningCalcs = new ArrayList<>();
        List<Thread> runningThreads = new ArrayList<>();

        System.out.printf("Starting %d threads\n", loopNumber);

        for (int i = 0; i < loopNumber; i++)
        {
            CalculationThread r = new CalculationThread(counter);
            Thread t = new Thread(r);
            runningCalcs.add(r);
            runningThreads.add(t);
            t.start();
        }

        for (int i = 0; i < 15; i++)
        {
            counter.reset();
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                break;
            }
            System.out.printf("[%d] Calculations per second: %d (%.2f per thread)\n",
                              i,
                              counter.longValue(),
                              (double)(counter.longValue()) / loopNumber
            );
        }

        for (int i = 0; i < runningCalcs.size(); i++)
        {
            runningCalcs.get(i).stop();
            try {
				runningThreads.get(i).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

    }

    public static class CalculationThread implements Runnable
    {
        private final Random rng;
        private final LongAdder calculationsPerformed;
        private boolean stopped;
        private double store;

        public CalculationThread(LongAdder calculationsPerformed)
        {
            this.calculationsPerformed = calculationsPerformed;
            this.stopped = false;
            this.rng = new Random();
            this.store = 1;
        }

        public void stop()
        {
            this.stopped = true;
        }

        @Override
        public void run()
        {
            while (! this.stopped)
            {
                double r = this.rng.nextFloat();
                double v = Math.sin(Math.cos(Math.sin(Math.cos(r))));
                this.store *= v;
                this.calculationsPerformed.add(1);
            }
        }
    }
}
