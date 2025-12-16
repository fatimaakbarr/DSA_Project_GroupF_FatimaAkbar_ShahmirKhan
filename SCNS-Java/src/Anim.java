import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public final class Anim {
    private Anim() {}

    public static double easeOutCubic(double t) {
        double p = 1.0 - t;
        return 1.0 - p * p * p;
    }

    public static double clamp01(double t) {
        return Math.max(0.0, Math.min(1.0, t));
    }

    public interface Tick {
        void onTick(double t);
    }

    public static void run(int ms, int fps, Tick tick, Runnable done) {
        final long start = System.currentTimeMillis();
        final int delay = Math.max(5, 1000 / Math.max(15, fps));
        Timer timer = new Timer(delay, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long now = System.currentTimeMillis();
                double t = (double) (now - start) / (double) ms;
                t = clamp01(t);
                tick.onTick(t);
                if (t >= 1.0) {
                    timer.stop();
                    if (done != null) done.run();
                }
            }
        });
        timer.start();
    }
}
